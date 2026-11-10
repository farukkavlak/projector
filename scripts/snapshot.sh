#!/usr/bin/env bash
# Save the projector's current state to snapshots/<date>/, with device
# identifiers (serial numbers, MAC addresses) masked so it is safe to commit.
#
# Usage: scripts/snapshot.sh [output-dir]

source "$(dirname "$0")/lib.sh"

out="${1:-$ROOT/snapshots/$(date +%F)}"
mkdir -p "$out"
connect

# The serial number also appears inside other values (e.g. persist.adb.wifi.guid),
# so it is replaced wherever it occurs, not only on its own line.
serial="$(query getprop ro.serialno)"
redact() {
  sed -E \
    -e "${serial:+s/$serial/REDACTED/g}" \
    -e 's/^(\[ro\.pc\.MAC\]: \[).*\]$/\1REDACTED]/' \
    -e 's/([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}/XX:XX:XX:XX:XX:XX/g'
}

log "Writing snapshot to $out"
query getprop | redact > "$out/getprop.txt"
query pm list packages -f -u > "$out/packages-all.txt"
query pm list packages -3    > "$out/packages-user.txt"
query pm list packages -s    > "$out/packages-system.txt"
query pm list packages -d    > "$out/packages-disabled.txt"
query getprop | grep -E 'persist\.display\.keystone_|persist\.sys\.zoom\.value' > "$out/keystone.txt"
query dumpsys meminfo | grep -E 'Total RAM|Free RAM|Used RAM|Lost RAM' > "$out/meminfo.txt"
ls -1 "$out"
