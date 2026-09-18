#!/usr/bin/env bash
#
# Sobe o ambiente do Sangue Bom (PostgreSQL + aplicacao) via Docker Compose.
#
#   ./scripts/start.sh            menu interativo
#   ./scripts/start.sh --clean    apaga o banco e sobe do zero
#   ./scripts/start.sh --keep     sobe mantendo os dados
#   ./scripts/start.sh --down     para o ambiente (preserva os dados)
#   ./scripts/start.sh --clean --yes   nao pergunta nada (CI)
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_URL="http://localhost:8080"
DB_WAIT_SECONDS=60
APP_WAIT_SECONDS=300

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; BLUE=$'\033[0;34m'; BOLD=$'\033[1m'; NC=$'\033[0m'

info()  { printf '%s\n' "${BLUE}==>${NC} $*"; }
ok()    { printf '%s\n' "${GREEN}==>${NC} $*"; }
warn()  { printf '%s\n' "${YELLOW}==>${NC} $*"; }
fail()  { printf '%s\n' "${RED}==>${NC} $*" >&2; exit 1; }

usage() {
    cat <<'USAGE'
Uso: ./scripts/start.sh [opcao]

  --clean    Sobe com o banco LIMPO: remove o volume e reaplica as migrations.
  --keep     Sobe normalmente, mantendo os dados existentes.
  --down     Para o ambiente. Os dados sao preservados no volume postgres-data.
  --yes      Nao pede confirmacao (use junto de --clean em automacao).
  --help     Mostra esta mensagem.

Sem opcao, o script abre um menu interativo.
USAGE
}

# ---------------------------------------------------------------- pre-checagens

require_docker() {
    command -v docker >/dev/null 2>&1 \
        || fail "Docker nao encontrado no PATH. Instale o Docker para continuar."
    docker info >/dev/null 2>&1 \
        || fail "O daemon do Docker nao esta respondendo. Inicie o Docker e tente de novo."
    docker compose version >/dev/null 2>&1 \
        || fail "O plugin 'docker compose' nao esta disponivel. Instale o Docker Compose v2."
}

# O compose usa .env de duas formas: env_file nos dois servicos e interpolacao
# de ${DB_PORT}/${DB_USERNAME}/... no proprio YAML. Sem o arquivo, o up quebra.
require_env_file() {
    if [[ -f .env ]]; then
        return
    fi
    if [[ -f .env.example ]]; then
        warn ".env nao encontrado. Copiando de .env.example."
        cp .env.example .env
    else
        warn ".env nao encontrado. Criando um com os valores padrao do README."
        cat > .env <<'ENVFILE'
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_NAME=sanguebom
ENVFILE
    fi
    ok ".env criado. Ajuste as credenciais se precisar e rode o script de novo."
}

db_port() {
    local port
    port="$(grep -E '^DB_PORT=' .env | tail -1 | cut -d= -f2- | tr -d '[:space:]')"
    printf '%s' "${port:-5432}"
}

# ---------------------------------------------------------------- espera

wait_for_postgres() {
    info "Aguardando o PostgreSQL ficar saudavel..."
    local waited=0
    while (( waited < DB_WAIT_SECONDS )); do
        local state
        state="$(docker compose ps --format '{{.Service}} {{.Health}}' 2>/dev/null \
                 | awk '$1 == "postgres" { print $2 }')"
        if [[ "$state" == "healthy" ]]; then
            ok "PostgreSQL saudavel."
            return 0
        fi
        sleep 2
        waited=$(( waited + 2 ))
    done
    docker compose logs --tail=30 postgres || true
    fail "O PostgreSQL nao ficou saudavel em ${DB_WAIT_SECONDS}s."
}

# O servico sanguebom nao tem healthcheck e o actuator nao esta configurado,
# entao GET / (HomeResource) e o sinal de readiness disponivel.
wait_for_app() {
    info "Aguardando a aplicacao responder em ${APP_URL} (pode compilar na primeira vez)..."
    local waited=0
    while (( waited < APP_WAIT_SECONDS )); do
        if curl -fsS -m 3 "${APP_URL}/" >/dev/null 2>&1; then
            ok "Aplicacao no ar."
            return 0
        fi
        if [[ "$(docker compose ps --format '{{.Service}} {{.State}}' 2>/dev/null \
                 | awk '$1 == "sanguebom" { print $2 }')" == "exited" ]]; then
            printf '\n'
            docker compose logs --tail=50 sanguebom || true
            fail "O container da aplicacao parou durante a subida."
        fi
        sleep 3
        waited=$(( waited + 3 ))
    done
    printf '\n'
    docker compose logs --tail=50 sanguebom || true
    fail "A aplicacao nao respondeu em ${APP_WAIT_SECONDS}s."
}

summary() {
    local port; port="$(db_port)"
    printf '\n'
    ok "${BOLD}Ambiente pronto.${NC}"
    printf '    Aplicacao : %s\n' "${APP_URL}"
    printf '    Banco     : localhost:%s\n' "$port"
    printf '    Logs      : docker compose logs -f sanguebom\n'
    printf '    Testes    : ./scripts/api-test.sh\n'
    # O Swagger fica de fora de proposito: springdoc.pathsToMatch=/ limita o
    # documento OpenAPI ao path "/", entao a UI nao mostra a API real.
    printf '\n'
}

# ---------------------------------------------------------------- acoes

do_up() {
    info "Construindo e subindo os containers..."
    docker compose up --build -d
    wait_for_postgres
    wait_for_app
    summary
}

do_clean() {
    local assume_yes="$1"
    if [[ "$assume_yes" != "yes" ]]; then
        warn "Isso vai APAGAR todos os dados do banco (volume postgres-data)."
        printf '%s' "Digite ${BOLD}limpar${NC} para confirmar: "
        local answer; read -r answer
        if [[ "$answer" != "limpar" ]]; then
            fail "Cancelado. Nada foi alterado."
        fi
    fi
    info "Removendo containers e o volume do banco..."
    docker compose down -v --remove-orphans
    do_up
    ok "Banco recriado do zero: o Flyway reaplicou schema e seeds."
}

do_down() {
    info "Parando o ambiente..."
    docker compose down --remove-orphans
    ok "Ambiente parado. Os dados continuam no volume postgres-data."
}

menu() {
    printf '\n%s\n\n' "${BOLD}Sangue Bom - subir ambiente${NC}"
    printf '  1) Subir com banco LIMPO  (apaga tudo e reaplica as migrations)\n'
    printf '  2) Subir normalmente      (mantem os dados existentes)\n'
    printf '  3) Parar o ambiente\n'
    printf '  4) Sair\n\n'
    printf '%s' "Escolha [1-4]: "
    local choice; read -r choice
    printf '\n'
    case "$choice" in
        1) do_clean "ask" ;;
        2) do_up ;;
        3) do_down ;;
        4) exit 0 ;;
        *) fail "Opcao invalida: '$choice'" ;;
    esac
}

# ---------------------------------------------------------------- main

action=""
assume_yes="ask"
for arg in "$@"; do
    case "$arg" in
        --clean) action="clean" ;;
        --keep|--up) action="keep" ;;
        --down|--stop) action="down" ;;
        --yes|-y) assume_yes="yes" ;;
        --help|-h) usage; exit 0 ;;
        *) usage; fail "Argumento desconhecido: '$arg'" ;;
    esac
done

require_docker
require_env_file

case "$action" in
    clean) do_clean "$assume_yes" ;;
    keep)  do_up ;;
    down)  do_down ;;
    "")    menu ;;
esac
