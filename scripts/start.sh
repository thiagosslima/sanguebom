#!/usr/bin/env bash
#
# Sobe o ambiente do Sangue Bom (PostgreSQL + API + frontend) via Docker Compose.
#
#   ./scripts/start.sh            sobe mantendo os dados
#   ./scripts/start.sh --menu     menu interativo
#   ./scripts/start.sh --clean    apaga o banco e sobe do zero
#   ./scripts/start.sh --keep     sobe mantendo os dados
#   ./scripts/start.sh --down     para o ambiente (preserva os dados)
#   ./scripts/start.sh --clean --yes   nao pergunta nada (CI)
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; BLUE=$'\033[0;34m'; BOLD=$'\033[1m'; NC=$'\033[0m'

info()  { printf '%s\n' "${BLUE}==>${NC} $*"; }
ok()    { printf '%s\n' "${GREEN}==>${NC} $*"; }
warn()  { printf '%s\n' "${YELLOW}==>${NC} $*"; }
fail()  { printf '%s\n' "${RED}==>${NC} $*" >&2; exit 1; }

usage() {
    cat <<'USAGE'
Uso: ./scripts/start.sh [opcao]

  --menu     Abre o menu interativo.
  --clean    Sobe com o banco LIMPO: remove o volume e reaplica as migrations.
  --keep     Sobe normalmente, mantendo os dados existentes.
  --down     Para o ambiente. Os dados sao preservados no volume postgres-data.
  --yes      Nao pede confirmacao (use junto de --clean em automacao).
  --help     Mostra esta mensagem.

Sem opcao, sobe banco, API e frontend mantendo os dados.
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

# .env e opcional: o Compose possui valores locais padrao.
require_env_file() {
    if [[ ! -f .env && -f .env.example ]]; then
        cp .env.example .env
        info ".env criado com configuracoes locais."
    fi
}

published_port() {
    local service="$1" container_port="$2"
    docker compose port "$service" "$container_port" | tail -1
}

summary() {
    printf '\n'
    ok "${BOLD}Ambiente pronto.${NC}"
    printf '    Frontend  : http://%s\n' "$(published_port frontend 3000)"
    printf '    API       : http://%s\n' "$(published_port sanguebom 8080)"
    printf '    Swagger   : http://%s/swagger-ui/index.html\n' "$(published_port sanguebom 8080)"
    printf '    Banco     : %s\n' "$(published_port postgres 5432)"
    printf '    Logs      : docker compose logs -f\n'
    printf '    Parar     : ./scripts/start.sh --down\n\n'
}

# ---------------------------------------------------------------- acoes

do_up() {
    info "Construindo e subindo os containers..."
    if ! docker compose up --build -d --wait --wait-timeout 300; then
        docker compose logs --tail=60
        fail "Falha na inicializacao. Confira os logs acima e execute novamente."
    fi
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
        --menu) action="menu" ;;
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
    menu)  menu ;;
    "")    do_up ;;
esac
