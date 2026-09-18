#!/usr/bin/env python3
"""Monta o comentario de cobertura do PR.

Junta duas medidas que respondem perguntas diferentes:

- cobertura do PR: so as linhas adicionadas/alteradas (vem do diff-cover);
  e a que bloqueia o merge.
- cobertura do projeto: o total do repositorio (vem do jacoco.xml);
  e informativa, serve para acompanhar a tendencia.
"""

import argparse
import json
import os
import xml.etree.ElementTree as ET

# Quanto a cobertura do projeto pode cair antes de virar aviso.
PROJECT_DRIFT_WARN = 0.5


def jacoco_totals(path):
    """Contadores raiz do relatorio JaCoCo, por tipo (LINE, BRANCH, ...)."""
    root = ET.parse(path).getroot()
    totals = {}
    for counter in root.findall("counter"):
        missed = int(counter.get("missed", 0))
        covered = int(counter.get("covered", 0))
        total = missed + covered
        totals[counter.get("type")] = {
            "covered": covered,
            "total": total,
            "percent": (100.0 * covered / total) if total else None,
        }
    return totals


def bar(percent, width=20):
    if percent is None:
        return ""
    filled = int(round(width * percent / 100.0))
    return "█" * filled + "░" * (width - filled)


def fmt(stat):
    if not stat or stat["percent"] is None:
        return "n/d"
    return f"{stat['percent']:.1f}% ({stat['covered']}/{stat['total']})"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jacoco", required=True)
    ap.add_argument("--diff-json", required=True)
    ap.add_argument("--min-coverage", type=float, required=True)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--pr", required=True)
    ap.add_argument("--run-url", default="")
    ap.add_argument("--base-ref", default="")
    args = ap.parse_args()

    out = ["## 🩸 Cobertura de testes", ""]

    # --- cobertura do PR (bloqueante) ---
    diff = None
    if os.path.exists(args.diff_json):
        with open(args.diff_json) as fh:
            diff = json.load(fh)

    if diff is None:
        pr_row = "| **Linhas novas do PR** | relatorio nao gerado | " \
                 f"{args.min_coverage:.0f}% | ⚠️ |"
        pr_ok = None
    elif diff["total_num_lines"] == 0:
        pr_row = "| **Linhas novas do PR** | sem linhas mensuraveis | " \
                 f"{args.min_coverage:.0f}% | ➖ |"
        pr_ok = None
    else:
        total = diff["total_num_lines"]
        missed = diff["total_num_violations"]
        covered = total - missed
        pct = 100.0 * covered / total
        pr_ok = pct >= args.min_coverage
        icon = "✅" if pr_ok else "❌"
        pr_row = (
            f"| **Linhas novas do PR** | `{bar(pct)}` {pct:.1f}% "
            f"({covered}/{total}) | {args.min_coverage:.0f}% | {icon} |"
        )

    # --- cobertura do projeto (informativa) ---
    project = jacoco_totals(args.jacoco) if os.path.exists(args.jacoco) else {}
    line = project.get("LINE")
    proj_row = (
        f"| Projeto (linhas) | `{bar(line['percent'] if line else None)}` "
        f"{fmt(line)} | — | ℹ️ |"
    )

    out += [
        "| Escopo | Cobertura | Mínimo | |",
        "|---|---|---|:-:|",
        pr_row,
        proj_row,
        "",
    ]

    if pr_ok is False:
        out += [
            f"> ❌ **O gate reprovou.** As linhas que este PR adiciona ou altera "
            f"precisam de pelo menos **{args.min_coverage:.0f}%** de cobertura. "
            "A cobertura do projeto é informativa e não bloqueia.",
            "",
        ]
    elif pr_ok is None:
        out += [
            "> ➖ Este PR não altera linhas de `src/main/java` que o JaCoCo "
            "consiga medir, então o gate não se aplica.",
            "",
        ]

    # --- detalhamento do projeto ---
    rows = []
    for label, key in (("Linhas", "LINE"), ("Branches", "BRANCH"),
                       ("Métodos", "METHOD"), ("Classes", "CLASS")):
        if key in project:
            rows.append(f"| {label} | {fmt(project[key])} |")
    if rows:
        out += [
            "<details>",
            f"<summary>Cobertura do projeto por métrica (base: <code>{args.base_ref}</code>)</summary>",
            "",
            "| Métrica | Cobertura |",
            "|---|---|",
            *rows,
            "",
            "</details>",
            "",
        ]

    # --- arquivos com linhas descobertas ---
    if diff and diff.get("src_stats"):
        faltando = [
            (p, s) for p, s in sorted(diff["src_stats"].items())
            if s.get("violation_lines")
        ]
        if faltando:
            det = [
                "<details>",
                f"<summary>Arquivos com linhas novas sem cobertura ({len(faltando)})</summary>",
                "",
                "| Arquivo | Cobertura no diff | Linhas descobertas |",
                "|---|---|---|",
            ]
            for path, stat in faltando:
                linhas = ", ".join(str(n) for n in stat["violation_lines"][:25])
                if len(stat["violation_lines"]) > 25:
                    linhas += f", … (+{len(stat['violation_lines']) - 25})"
                det.append(
                    f"| `{path}` | {stat['percent_covered']:.0f}% | {linhas} |"
                )
            out += det + ["", "</details>", ""]

    # --- links ---
    base = f"https://github.com/{args.repo}"
    scanning = f"{base}/security/code-scanning?query=is%3Aopen+pr%3A{args.pr}"
    links = [
        "---",
        "",
        f"🔎 [Alertas de código deste PR]({scanning}) "
        f"· [Todos os alertas do repositório]({base}/security/code-scanning) "
        "— CodeQL e SpotBugs",
    ]
    if args.run_url:
        links.append(
            f"📊 [Relatório HTML completo da cobertura]({args.run_url}) "
            "— baixe o artefato `coverage-diff`"
        )
    out += links

    print("\n".join(out))


if __name__ == "__main__":
    main()
