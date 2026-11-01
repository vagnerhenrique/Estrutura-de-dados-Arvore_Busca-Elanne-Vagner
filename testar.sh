#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
javac --release 17 -encoding UTF-8 Principal.java
contador=0
for entrada in testes/*.in; do
    nome="$(basename "$entrada" .in)"
    arquivo_temporario="$(mktemp)"
    if java Principal "$entrada" > "$arquivo_temporario" && cmp -s "testes/$nome.out" "$arquivo_temporario"; then
        printf '%s: APROVADO (arquivos exatamente iguais)\n' "$nome"
        contador=$((contador + 1))
    else
        printf '%s: REPROVADO\n' "$nome"
        diff -u "testes/$nome.out" "$arquivo_temporario" || true
        rm -f "$arquivo_temporario"
        exit 1
    fi
    rm -f "$arquivo_temporario"
done
printf 'Total: %s/5 aprovados\n' "$contador"
