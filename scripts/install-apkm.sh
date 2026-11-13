#!/usr/bin/env bash
# Install an APKMirror .apkm bundle over ADB, picking the splits that match the device.
#
# Usage: scripts/install-apkm.sh [--dry-run] <file.apkm>
#   ABI=armeabi-v7a DPI=320 scripts/install-apkm.sh --dry-run file.apkm
#
# The bundle must contain base.apk and split_config.*.apk files.

source "$(dirname "$0")/lib.sh"

DRY_RUN=0
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
  shift
fi

file="${1:-}"
[[ -f "$file" ]] || die "usage: $0 [--dry-run] <file.apkm>"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
unzip -q "$file" -d "$tmp"
[[ -f "$tmp/base.apk" ]] || die "no base.apk in $file"

if (( DRY_RUN )); then
  abi="${ABI:-armeabi-v7a}"
  dpi="${DPI:-320}"
else
  connect
  abi="$(query getprop ro.product.cpu.abi)"
  dpi="$(query wm density | sed -n 's/.*Physical density: *//p' | head -1)"
  [[ -n "$dpi" ]] || dpi=320
fi

abi_us="${abi//-/_}"
dpi_value() {
  case "$1" in
    mdpi) echo 160 ;;
    hdpi) echo 240 ;;
    xhdpi) echo 320 ;;
    xxhdpi) echo 480 ;;
    xxxhdpi) echo 640 ;;
    *) echo 0 ;;
  esac
}
density_split=""
if [[ -n "$dpi" ]]; then
  best=99999
  for f in "$tmp"/split_config.mdpi.apk "$tmp"/split_config.hdpi.apk "$tmp"/split_config.xhdpi.apk \
           "$tmp"/split_config.xxhdpi.apk "$tmp"/split_config.xxxhdpi.apk; do
    [[ -e "$f" ]] || continue
    name="${f##*/}"
    key="${name#split_config.}"
    key="${key%.apk}"
    value="$(dpi_value "$key")"
    diff=$(( value > dpi ? value - dpi : dpi - value ))
    if (( diff < best )); then
      best=$diff
      density_split="$f"
    fi
  done
fi

files=("$tmp/base.apk")
for f in "$tmp"/split_config.*.apk; do
  [[ -e "$f" ]] || continue
  name="${f##*/}"
  case "$name" in
    split_config.arm64_v8a.apk|split_config.armeabi_v7a.apk|split_config.x86.apk|split_config.x86_64.apk)
      [[ "$name" == "split_config.${abi_us}.apk" ]] && files+=("$f")
      ;;
    split_config.mdpi.apk|split_config.hdpi.apk|split_config.xhdpi.apk|split_config.xxhdpi.apk|split_config.xxxhdpi.apk)
      [[ -n "$density_split" && "$f" == "$density_split" ]] && files+=("$f")
      ;;
    *)
      files+=("$f")
      ;;
  esac
done

echo "==> $abi ${dpi}dpi: ${#files[@]} APK files"
printf '    %s\n' "${files[@]##*/}"

if (( DRY_RUN )); then
  exit 0
fi

"$ADB" -s "$DEV" install-multiple -r "${files[@]}"
