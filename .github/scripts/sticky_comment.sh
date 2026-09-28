#!/usr/bin/env bash
# Publica (ou atualiza) um comentario fixo no PR.
#
# Uso: sticky_comment.sh <marcador> <arquivo-markdown>
#
# O marcador e um comentario HTML invisivel usado para reencontrar o
# comentario nas execucoes seguintes, em vez de empilhar um novo a cada push.
# Requer GH_TOKEN, GITHUB_REPOSITORY e PR_NUMBER no ambiente.
set -euo pipefail

MARKER_ID="$1"
BODY_FILE="$2"
MARKER="<!-- sanguebom:${MARKER_ID} -->"

if [ -z "${PR_NUMBER:-}" ]; then
  echo "PR_NUMBER nao definido; nada a comentar."
  exit 0
fi

TMP="$(mktemp)"
{ printf '%s\n' "$MARKER"; cat "$BODY_FILE"; } > "$TMP"

EXISTING="$(gh api "repos/${GITHUB_REPOSITORY}/issues/${PR_NUMBER}/comments" \
  --paginate --jq "[.[] | select(.body | startswith(\"${MARKER}\")) | .id] | first // empty")"

if [ -n "$EXISTING" ]; then
  gh api -X PATCH "repos/${GITHUB_REPOSITORY}/issues/comments/${EXISTING}" \
    -F body=@"$TMP" --silent
  echo "Comentario ${MARKER_ID} atualizado (id=${EXISTING})."
else
  gh api -X POST "repos/${GITHUB_REPOSITORY}/issues/${PR_NUMBER}/comments" \
    -F body=@"$TMP" --silent
  echo "Comentario ${MARKER_ID} criado."
fi
