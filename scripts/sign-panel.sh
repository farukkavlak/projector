#!/usr/bin/env bash
# Sign the Panel APK with the platform key, so it gets HARDWARE_TEST and can set the
# 4 corners live (SurfaceFlinger transact 1050) instead of restarting the screen.
#
# This device trusts the public AOSP platform key:
#   build/target/product/security/platform.{pk8,x509.pem}
# The key is only downloaded once, to ~/.local/android/platform-key (never committed).
#
# Usage: scripts/sign-panel.sh [apk] [out]
#   APKSIGNER=/path/to/apksigner KEYDIR=/path/to/key-dir scripts/sign-panel.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-$ROOT/panel/app/build/outputs/apk/debug/app-debug.apk}"
OUT="${2:-$ROOT/panel/app-platform.apk}"
KEYDIR="${KEYDIR:-$HOME/.local/android/platform-key}"
APKSIGNER="${APKSIGNER:-$HOME/.local/android/build-tools/34.0.0/apksigner}"
REF=android-11.0.0_r48
BASE="https://android.googlesource.com/platform/build/+/refs/tags/$REF/target/product/security"

[[ -f "$APK" ]] || { echo "error: $APK not found (run ./gradlew assembleDebug)" >&2; exit 1; }
[[ -x "$APKSIGNER" ]] || { echo "error: apksigner not found: $APKSIGNER" >&2; exit 1; }

if [[ ! -f "$KEYDIR/platform.pk8" || ! -f "$KEYDIR/platform.x509.pem" ]]; then
  echo "==> Downloading the AOSP platform key to $KEYDIR"
  mkdir -p "$KEYDIR"
  curl -fsSL "$BASE/platform.pk8?format=TEXT" | base64 -d > "$KEYDIR/platform.pk8"
  curl -fsSL "$BASE/platform.x509.pem?format=TEXT" | base64 -d > "$KEYDIR/platform.x509.pem"
fi

"$APKSIGNER" sign \
  --key "$KEYDIR/platform.pk8" \
  --cert "$KEYDIR/platform.x509.pem" \
  --out "$OUT" "$APK"

echo "==> Signed: $OUT"
echo "    Uninstall the old app first if its signature differs:"
echo "    adb uninstall local.projector.panel && adb install '$OUT'"
