#!/usr/bin/env bash
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
WIN_ROOT=$(cygpath -w "$ROOT")
LABEL=${LABEL:-api34}
SDK=${SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}}
ADB="$SDK/platform-tools/adb.exe"
EMU="$SDK/emulator/emulator.exe"
"$EMU" -avd Medium_Desktop -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
  -no-snapshot-load -memory 4096 -cores 4 > "$ROOT/outputs/quick_emu.log" 2>&1 &
EMU_PID=$!
trap 'kill "$EMU_PID" 2>/dev/null; wait "$EMU_PID" 2>/dev/null' EXIT
for _ in $(seq 1 120); do
  if [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then break; fi
  sleep 5
done
"$ADB" install -r "$WIN_ROOT\\outputs\\pocket-toolbox-debug.apk"
"$ADB" shell am force-stop cn.pocket.toolbox
"$ADB" shell am start -W -n cn.pocket.toolbox/.MainActivity -f 0x10008000
sleep 4
"$ADB" exec-out screencap -p > "$ROOT/outputs/quick-$LABEL.png"
echo saved "$ROOT/outputs/quick-$LABEL.png"