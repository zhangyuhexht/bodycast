#!/usr/bin/env bash
# 模拟器操作封装。
#
# 这台机器上的 adb 连接会间歇性掉成 offline，直接跑命令经常失败。
# 这里统一加重试与重连，避免每次手工敲一长串。
#
#   tools/dev.sh shot <输出文件>      截图
#   tools/dev.sh tap <x> <y>          点击
#   tools/dev.sh install <apk>        安装
#   tools/dev.sh restart              重启应用
#   tools/dev.sh info                 设备信息
#   tools/dev.sh ui <输出文件>        导出 UI 层级
set -uo pipefail

# 可用 ADB=... 或 ANDROID_HOME / ANDROID_SDK_ROOT 指定本机 adb。
ADB="${ADB:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/platform-tools/adb}"
PKG="cn.pocket.toolbox"
ACTIVITY="$PKG/.ui.MainActivity"

ensure() {
  for i in 1 2 3 4 5 6 7 8; do
    local state
    state="$("$ADB" get-state 2>/dev/null || true)"
    if [ "$state" = "device" ]; then return 0; fi
    "$ADB" kill-server >/dev/null 2>&1
    sleep 2
    "$ADB" start-server >/dev/null 2>&1
    sleep 3
  done
  echo "设备始终不可用" >&2
  return 1
}

run() {
  ensure || return 1
  for i in 1 2 3 4 5; do
    if "$ADB" "$@" 2>/dev/null; then return 0; fi
    sleep 2
    ensure >/dev/null 2>&1
  done
  echo "命令重试后仍失败: adb $*" >&2
  return 1
}

case "${1:-}" in
  shot)
    out="${2:?需要输出文件}"
    ensure || exit 1
    for i in 1 2 3 4 5; do
      if "$ADB" exec-out screencap -p > "$out" 2>/dev/null && [ -s "$out" ]; then
        echo "已截图 $out"
        exit 0
      fi
      sleep 2
      ensure >/dev/null 2>&1
    done
    echo "截图失败" >&2
    exit 1
    ;;
  tap)
    run shell input tap "$2" "$3" >/dev/null
    ;;
  install)
    run install -r "$2" | tail -1
    ;;
  restart)
    run shell am force-stop "$PKG" >/dev/null
    run shell am start -n "$ACTIVITY" >/dev/null
    ;;
  ui)
    out="${2:?需要输出文件}"
    run shell uiautomator dump /sdcard/ui.xml >/dev/null
    run pull /sdcard/ui.xml "$out" >/dev/null
    echo "已导出 $out"
    ;;
  info)
    ensure || exit 1
    "$ADB" devices
    "$ADB" shell wm size
    "$ADB" shell wm density
    "$ADB" shell getprop ro.product.model
    "$ADB" shell dumpsys window displays 2>/dev/null | grep -E "cur=" | head -1
    ;;
  *)
    sed -n '2,12p' "$0"
    ;;
esac
