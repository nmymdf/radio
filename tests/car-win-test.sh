#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 車機桌面 v0.6.2：YouTube App 小窗開法
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
tap() { adb shell input tap $1 $2; }
hold() { adb shell input swipe $1 $2 $1 $2 ${3:-1300}; }
home() { adb shell input keyevent 3; }
back() { adb shell input keyevent 4; }

# 找畫面上的文字並點它（$1＝文字，$2＝prefix 表示開頭符合即可）
findText() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" "${2:-exact}" <<'PY'
import re, sys
t, mode = sys.argv[1], sys.argv[2]
try: xml = open('/tmp/ui.xml', encoding='utf-8').read()
except Exception: sys.exit(0)
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    vals = re.findall(r'(?:text|content-desc)="([^"]*)"', n)
    ok = any(v == t for v in vals) if mode == 'exact' else any(v.startswith(t) for v in vals)
    if ok:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
}
tapText() {
  local xy; xy=$(findText "$1" "$2")
  if [ -n "$xy" ]; then tap $xy; echo "tapText OK: $1 @ $xy"; else echo "tapText MISSING: $1"; fi
}
# 分隔線位置（x）
divX() { adb shell dumpsys window windows | grep -A25 "Window{[0-9a-f]* u0 DockedStackDivider}:" | grep -o "mFrame=\[[-0-9]*,[-0-9]*\]\[[-0-9]*," | head -1 | sed 's/mFrame=\[\([-0-9]*\),[-0-9]*\]\[\([-0-9]*\),/\1 \2/' | awk '{x=int(($1+$2)/2); if (x<3) x=3; print x}'; }
dragDiv() { local x; x=$(divX); [ -n "$x" ] && adb shell input swipe $x 360 $1 360 1500; echo "divider $x -> $1"; }
# 金色浮動按鈕中心
floatXY() { adb shell dumpsys window windows | grep -A25 "Window{[0-9a-f]* u0 com.archiekuo.carhome}:" | grep -o "mFrame=\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]" | head -1 | sed 's/mFrame=\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\1 \2 \3 \4/' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'; }
# 在設定裡往下捲直到看到文字再點
scrollTap() {
  for i in 1 2 3 4 5 6; do
    local xy; xy=$(findText "$1" "$2")
    if [ -n "$xy" ]; then tap $xy; echo "scrollTap OK: $1"; return; fi
    adb shell input swipe 640 600 640 260 500; sleep 1
  done
  echo "scrollTap MISSING: $1"
}

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/radio.apk
adb install -r -g apks/carhome.apk
setup() {
  for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do
    adb shell pm grant com.archiekuo.carhome android.permission.$p || true
  done
  adb shell appops set com.archiekuo.carhome SYSTEM_ALERT_WINDOW allow || true
  adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
}
setup
adb emu geo fix 121.5419 24.9676
adb shell dumpsys package com.google.android.youtube | grep versionName > $OUT/youtube-version.txt
ytmode() { adb shell dumpsys activity activities | grep -iE "youtube|windowingMode|mode=" | grep -iE -A1 -B1 "youtube" | head -12 > $OUT/mode-$1.txt; adb shell dumpsys window windows | grep -A30 "youtube" | grep -oE "mFrame=\[[0-9,]*\]\[[0-9,]*\]|mWindowingMode=[a-z]*|windowingMode=[a-z]*" | head -3 >> $OUT/mode-$1.txt; }
adb logcat -c

home
shot 01-桌面 25
tapText "設定"; sleep 3
scrollTap "小窗開法（點一下切換）"
sleep 1
shot 02-設定-小窗開法-固定方法1 2
for i in 1 2 3; do tapText "小窗開法（點一下切換）"; sleep 1; done
shot 03-設定-切回輪流 2
back; sleep 2

# ---- A：車機沒有開小窗功能時 ----
tap 1060 380
shot 04-沒小窗功能-點照片 12
ytmode A
home; sleep 3
tapText "全畫面"
shot 05-全畫面按鈕 10
home; sleep 3

# ---- B：打開模擬器的小窗功能（像車機那樣）----
adb shell settings put global enable_freeform_support 1
adb shell settings put global force_resizable_activities 1
adb shell settings put global development_settings_enabled 1
adb reboot
adb wait-for-device
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
sleep 20
adb shell input keyevent 82
setup
adb shell am force-stop com.google.android.youtube
home
shot 06-開了小窗功能-桌面 15
adb logcat -c
for n in 1 2 3 4; do
  adb shell am force-stop com.google.android.youtube
  home; sleep 3
  tap 1060 380
  shot 07-第${n}次點照片-3秒後 3
  shot 08-第${n}次點照片-訊息 1
  ytmode B$n
done
tapText "設定"; sleep 3
adb shell input swipe 640 600 640 380 500
shot 09-設定-自動記住方法 2
back
adb logcat -d -s CarHomeYT:* > $OUT/yt-log.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
