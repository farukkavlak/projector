# shellcheck shell=bash
# Shared helpers for the HY320 scripts. Source this file; do not run it.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGES_FILE="$ROOT/config/packages.tsv"
SETTINGS_FILE="$ROOT/config/settings.tsv"

ADB="${ADB:-$(command -v adb || echo "$HOME/.local/android/platform-tools/adb")}"
DEV="${DEV:-192.168.1.50:5555}"

STOCK_LAUNCHER="com.htc.htclauncherhighenglishd08/com.htc.activity.HomeActivity"
FLAUNCHER="me.efesser.flauncher/.MainActivity"

DRY_RUN=0

die() { echo "error: $*" >&2; exit 1; }
log() { echo "==> $*"; }

# stdin is closed on every adb call: adb shell would otherwise swallow the rest
# of the file that a surrounding `while read` loop is reading.

# Run a command on the device, or print it in dry-run mode.
run() {
  if (( DRY_RUN )); then
    echo "  [dry-run] adb shell $*"
  else
    "$ADB" -s "$DEV" shell "$@" </dev/null | tr -d '\r'
  fi
}

# Read-only query on the device; runs even in dry-run mode.
query() { "$ADB" -s "$DEV" shell "$@" </dev/null | tr -d '\r'; }

connect() {
  [[ -x "$ADB" ]] || die "adb not found (set ADB=/path/to/adb)"
  "$ADB" connect "$DEV" >/dev/null || true
  query true >/dev/null 2>&1 || die "cannot reach $DEV (is the projector on and authorized?)"
}

# Print non-comment, non-empty lines of a TSV file.
rows() { grep -vE '^[[:space:]]*(#|$)' "$1"; }

is_installed() { query pm list packages "$1" | grep -qx "package:$1"; }

# Package state changes are flushed to disk late; rebooting too soon loses them.
remind_flush() {
  (( DRY_RUN )) && return
  echo
  echo "Wait ~30s before rebooting so the package changes are saved."
}
