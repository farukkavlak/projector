#!/usr/bin/env bash
# Apply the debloat in config/ to the projector. Safe to re-run.
#
# Usage: scripts/apply.sh [--dry-run]
#   ADB=/path/to/adb DEV=ip:port scripts/apply.sh

source "$(dirname "$0")/lib.sh"

[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1
connect

# The stock launcher may only go once another home screen is set, otherwise
# the device boots to a black screen.
can_drop_stock_launcher=0
if is_installed "${FLAUNCHER%%/*}"; then
  log "Setting FLauncher as home"
  run cmd package set-home-activity "$FLAUNCHER" >/dev/null
  can_drop_stock_launcher=1
else
  log "FLauncher not installed; keeping the stock launcher"
fi

log "Packages"
disabled="$(query pm list packages -d)"
while IFS=$'\t' read -r action pkg _reason; do
  if [[ "$pkg" == "${STOCK_LAUNCHER%%/*}" ]] && (( ! can_drop_stock_launcher )); then
    echo "  skip     $pkg (no replacement launcher)"
    continue
  fi
  if ! is_installed "$pkg"; then
    echo "  ok       $pkg (already gone)"
    continue
  fi
  if [[ "$action" == disable ]] && grep -qx "package:$pkg" <<<"$disabled"; then
    echo "  ok       $pkg (already disabled)"
    continue
  fi
  case "$action" in
    remove)  echo "  remove   $pkg"; run pm uninstall --user 0 "$pkg" >/dev/null ;;
    disable) echo "  disable  $pkg"; run pm disable-user --user 0 "$pkg" >/dev/null ;;
    *)       die "unknown action '$action' for $pkg in $PACKAGES_FILE" ;;
  esac
done < <(rows "$PACKAGES_FILE")

log "Settings"
while IFS=$'\t' read -r ns key value _stock; do
  echo "  $ns $key = $value"
  run settings put "$ns" "$key" "$value"
done < <(rows "$SETTINGS_FILE")

remind_flush
