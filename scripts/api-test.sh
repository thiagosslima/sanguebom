#!/usr/bin/env bash
#
# Roda a collection Postman do Sangue Bom com o Newman.
#
#   ./scripts/api-test.sh           recria o ambiente do zero e roda (padrao)
#   ./scripts/api-test.sh --keep     roda contra o ambiente que ja estiver de pe
#
# Recriar por padrao mantem a execucao reproduzivel: o projeto nao expoe nenhum
# endpoint DELETE, entao a collection nao tem como limpar o que cria. Com
# --keep ela roda sobre o banco atual, o que tambem funciona, so acumula dados.
#
# A pasta "99 - SSE (manual)" fica de fora: o emissor SSE mantem a conexao
# aberta por 30 minutos e travaria a execucao.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COLLECTION="postman/sanguebom.postman_collection.json"
ENVIRONMENT="postman/sanguebom.local.postman_environment.json"
REPORT_DIR="target/newman"
BASE_URL="${BASE_URL:-http://localhost:8080}"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; BLUE=$'\033[0;34m'; NC=$'\033[0m'
info() { printf '%s\n' "${BLUE}==>${NC} $*"; }
warn() { printf '%s\n' "${YELLOW}==>${NC} $*"; }
fail() { printf '%s\n' "${RED}==>${NC} $*" >&2; exit 1; }

# Ordem importa: fixtures antes dos fluxos, fluxos antes das consultas derivadas.
FOLDERS=(
    "00 - Smoke"
    "01 - Catalogo"
    "02 - Fixtures"
    "03 - Ingestao de exame e motor de risco"
    "04 - Cidadao"
    "05 - Notificacoes"
    "06 - Medico"
    "07 - Risco"
    "08 - Gamificacao"
    "09 - CRUD administrativo"
)

reset="yes"
for arg in "$@"; do
    case "$arg" in
        --keep|--no-reset) reset="no" ;;
        --fresh) reset="yes" ;;
        --help|-h) sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) fail "Argumento desconhecido: '$arg'" ;;
    esac
done

command -v npx >/dev/null 2>&1 \
    || fail "npx nao encontrado. Instale o Node.js para rodar o Newman."
[[ -f "$COLLECTION" ]] || fail "Collection nao encontrada em $COLLECTION"

if [[ "$reset" == "yes" ]]; then
    info "Recriando o ambiente do zero para uma execucao reproduzivel..."
    ./scripts/start.sh --clean --yes
else
    warn "Rodando sobre o banco atual (--keep), sem recriar nada."
fi

if ! curl -fsS -m 5 "${BASE_URL}/" >/dev/null 2>&1; then
    fail "A API nao respondeu em ${BASE_URL}. Rode ./scripts/start.sh antes (ou remova --keep)."
fi

mkdir -p "$REPORT_DIR"

folder_args=()
for f in "${FOLDERS[@]}"; do
    folder_args+=(--folder "$f")
done

info "Executando a collection (${#FOLDERS[@]} pastas)..."
printf '\n'

set +e
npx --yes newman run "$COLLECTION" \
    -e "$ENVIRONMENT" \
    --env-var "baseUrl=${BASE_URL}" \
    "${folder_args[@]}" \
    --reporters cli,json \
    --reporter-json-export "${REPORT_DIR}/report.json"
status=$?
set -e

printf '\n'
info "Relatorio detalhado: ${REPORT_DIR}/report.json"
info "Historico dos bugs ja corrigidos: postman/RELATORIO.md"

exit "$status"
