#!/usr/bin/env bash
# Undo the debloat: restore every package and setting in config/, or only the
# packages given as arguments.
#
# Usage: scripts/restore.sh [--dry-run] [package ...]

source "$(dirname "$0")/lib.sh"

if [[ "${1:-}" == "--dry-run" ]]; then DRY_RUN=1; shift; fi
connect

restore_pkg() {
  local action="$1" pkg="$2"
  case "$action" in
    remove)
      echo "  install-existing $pkg"
      # Fails for store apps, whose APK is gone; those need the Play Store.
      run cmd package install-existing "$pkg" >/dev/null \
        || echo "    -> not on the system image, reinstall from the Play Store"
      ;;
    disable)
      echo "  enable   $pkg"
      run pm enable "$pkg" >/dev/null
      ;;
  esac
}

if (( $# > 0 )); then
  for pkg in "$@"; do
    action="$(rows "$PACKAGES_FILE" | awk -F'\t' -v p="$pkg" '$2 == p { print $1 }')"
    [[ -n "$action" ]] || die "$pkg is not in $PACKAGES_FILE"
    restore_pkg "$action" "$pkg"
  done
  remind_flush
  exit 0
fi

log "Packages"
while IFS=$'\t' read -r action pkg _reason; do
  restore_pkg "$action" "$pkg"
done < <(rows "$PACKAGES_FILE")

log "Home screen"
run cmd package set-home-activity "$STOCK_LAUNCHER" >/dev/null

log "Settings"
while IFS=$'\t' read -r ns key _value stock; do
  echo "  $ns $key = $stock"
  run settings put "$ns" "$key" "$stock"
done < <(rows "$SETTINGS_FILE")

remind_flush
