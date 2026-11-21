#!/usr/bin/env bash
# Launch an emulator, install the APK and run the UI smoke script in one session.
# The emulator is started and stopped by this script so no process is left behind.
set -uo pipefail
AVD=${AVD:-Pixel_10_Pro}
LABEL=${LABEL:-api37}
SDK=${SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
# adb.exe and python.exe are native Windows binaries and need Windows-style paths.
WIN_ROOT=$(cygpath -w "$ROOT")
ADB="$SDK/platform-tools/adb.exe"
EMU="$SDK/emulator/emulator.exe"
PYTHON="${PYTHON:-python3}"

"$EMU" -avd "$AVD" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
  -no-snapshot-load -memory 4096 -cores 4 > "$ROOT/outputs/emulator-$LABEL.log" 2>&1 &
EMU_PID=$!
cleanup() { kill "$EMU_PID" 2>/dev/null; wait "$EMU_PID" 2>/dev/null; }
trap cleanup EXIT

for _ in $(seq 1 120); do
  if [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
    break
  fi
  sleep 5
done
"$ADB" shell am wait-for-broadcast-idle
"$ADB" install -r "$WIN_ROOT\\outputs\\pocket-toolbox-debug.apk"
"$PYTHON" "$WIN_ROOT\\tools\\smoke_test.py" --serial "${SERIAL:-emulator-5554}" --label "$LABEL"
