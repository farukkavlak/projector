#!/usr/bin/env bash
# Lamp off/on. The device stays online.
# Usage: scripts/lamp.sh off|on|status
#
# Lamp off stops the fan. A fan guard then shuts the device down,
# so turn the guard off first and back on after the fan spins up.
# See docs/findings.md.

source "$(dirname "$0")/lib.sh"

BL_POWER='/sys/devices/platform/soc@2900000/5240000.ge2d/backlight/tv/bl_power'
LAMP_OFF=5
LAMP_ON=6
FAN_SPINUP_SECONDS=12   # the guard trips after ~7 s of slow readings

connect

case "${1:-}" in
  off)
    run setprop sys.shutdown_backlight 1
    run setprop sys.start_fan_fun 0
    run "echo $LAMP_OFF > $BL_POWER"
    log "Lamp off"
    ;;
  on)
    run "echo $LAMP_ON > $BL_POWER"
    log "Lamp on; waiting ${FAN_SPINUP_SECONDS}s for the fan before re-arming the fan guard"
    sleep "$FAN_SPINUP_SECONDS"
    run setprop sys.start_fan_fun 1
    run setprop sys.shutdown_backlight 0
    log "Fan guard re-armed"
    ;;
  status)
    case "$(query cat "$BL_POWER")" in
      "$LAMP_OFF") echo "off" ;;
      0|"$LAMP_ON") echo "on" ;;
      *) echo "unknown" ;;
    esac
    ;;
  *)
    echo "usage: $0 off|on|status" >&2
    exit 2
    ;;
esac
