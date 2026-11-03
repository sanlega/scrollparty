#!/usr/bin/env bash
say() { echo "$1"; exit 0; }

git rev-parse --git-dir >/dev/null 2>&1 || say "Esto no es un repositorio git. Ejecuta el bootstrap (git init + AGENTS.md + scripts/verify) antes de empezar."
[ -f AGENTS.md ] || [ -f CLAUDE.md ] || say "Faltan AGENTS.md y CLAUDE.md. Ejecuta el bootstrap para prepararlos."

if [ -f scripts/verify ] && grep -q "TODO" scripts/verify 2>/dev/null; then
  say "scripts/verify sigue vacío. En cuanto haya stack, rellénalo con typecheck + tests + build; sin él Claude no puede comprobar su trabajo."
fi

if ! git rev-parse HEAD >/dev/null 2>&1; then
  say "Proyecto sin commits. Si la idea es difusa: /grill-me. Si ya está clara: un primer corte de extremo a extremo con verify en verde, y commit."
fi

if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  say "Hay cambios sin commitear. Ejecuta scripts/verify; si pasa, /code-review antes del commit. Si el rumbo se ha torcido, /clear y un prompt mejor."
fi

COMMITS=$(git rev-list --count HEAD 2>/dev/null || echo 0)
if [ ! -f docs/agents/issue-tracker.md ] && [ "$COMMITS" -ge 10 ]; then
  say "Proyecto con $COMMITS commits: si va a durar varias sesiones, ejecuta /setup-matt-pocock-skills una vez y trabaja con /to-spec y /to-tickets."
fi

say "Todo limpio. Tarea nueva: pequeña, pídela directa con su verificación; difusa, /grill-me; para varias sesiones, /to-spec y /to-tickets."
