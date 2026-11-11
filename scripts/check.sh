#!/usr/bin/env bash
# Build the Panel app, run its unit tests, and check the inline page script.
#
# Usage: scripts/check.sh
#   JAVA_HOME=/path/to/jdk17 scripts/check.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/panel"

./gradlew assembleDebug testDebugUnitTest --console=plain

if command -v node >/dev/null && command -v python3 >/dev/null; then
  tmpdir="$(mktemp -d)"
  trap 'rm -rf "$tmpdir"' EXIT
  python3 - "$ROOT/panel/app/src/main/assets/index.html" "$tmpdir/panel.js" <<'PY'
import pathlib
import re
import sys

html = pathlib.Path(sys.argv[1]).read_text()
pathlib.Path(sys.argv[2]).write_text(re.search(r"<script>(.*)</script>", html, re.S).group(1))
PY
  node --check "$tmpdir/panel.js"
  echo "inline script syntax ok"
fi
