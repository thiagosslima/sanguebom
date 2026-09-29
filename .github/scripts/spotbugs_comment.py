#!/usr/bin/env python3
"""Monta o comentario de analise estatica do PR a partir do SARIF do SpotBugs.

O CodeQL nao e resumido aqui: ele publica os proprios alertas direto no code
scanning, e o link no fim do comentario ja leva para a lista filtrada pelo PR.
"""

import argparse
import collections
import json
import os

SEVERITY = {"error": "🔴", "warning": "🟡", "note": "🔵", "none": "⚪"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sarif", required=True)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--pr", required=True)
    args = ap.parse_args()

    out = ["## 🔎 Análise estática", ""]

    if not os.path.exists(args.sarif):
        out.append("> ⚠️ SARIF do SpotBugs não foi gerado — veja o log do job.")
        print("\n".join(out))
        return

    with open(args.sarif) as fh:
        sarif = json.load(fh)

    run = sarif["runs"][0]
    results = run.get("results", [])

    # Mapeia ruleId -> descricao curta, para o comentario nao ser so sigla.
    rules = {}
    for rule in run.get("tool", {}).get("driver", {}).get("rules", []):
        text = (rule.get("shortDescription") or {}).get("text", "")
        rules[rule["id"]] = text.strip()

    if not results:
        out += [
            "✅ **SpotBugs: nenhum achado.**",
            "",
        ]
    else:
        counts = collections.Counter()
        levels = {}
        for res in results:
            rid = res.get("ruleId", "?")
            counts[rid] += 1
            levels[rid] = res.get("level", "warning")

        out += [
            f"**SpotBugs: {len(results)} achado(s)** em {len(counts)} regra(s).",
            "",
            "| | Regra | Ocorrências | Descrição |",
            "|:-:|---|:-:|---|",
        ]
        for rid, n in counts.most_common():
            icon = SEVERITY.get(levels.get(rid), "⚪")
            desc = rules.get(rid, "")
            if len(desc) > 90:
                desc = desc[:87] + "…"
            out.append(f"| {icon} | `{rid}` | {n} | {desc} |")
        out.append("")

        por_arquivo = collections.Counter()
        for res in results:
            for loc in res.get("locations", []):
                uri = loc.get("physicalLocation", {}).get(
                    "artifactLocation", {}).get("uri")
                if uri:
                    por_arquivo[uri] += 1
        if por_arquivo:
            out += [
                "<details>",
                f"<summary>Achados por arquivo ({len(por_arquivo)})</summary>",
                "",
                "| Arquivo | Achados |",
                "|---|:-:|",
            ]
            for uri, n in por_arquivo.most_common():
                out.append(f"| `{uri}` | {n} |")
            out += ["", "</details>", ""]

    out += [
        "> Os achados **não bloqueiam o merge** nesta fase. Eles ficam "
        "registrados no code scanning para serem tratados aos poucos.",
        "",
        "---",
        "",
        f"🔎 [Alertas de código deste PR]"
        f"(https://github.com/{args.repo}/security/code-scanning"
        f"?query=is%3Aopen+pr%3A{args.pr}) "
        f"· [Todos os alertas do repositório]"
        f"(https://github.com/{args.repo}/security/code-scanning) "
        "— CodeQL e SpotBugs",
    ]

    print("\n".join(out))


if __name__ == "__main__":
    main()
