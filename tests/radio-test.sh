#!/bin/bash
# 網路收音機 v1.6 手機測試 — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
tap() { adb shell input tap $1 $2; }
findText() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" <<'PY'
import re, sys
t = sys.argv[1]
try: xml = open('/tmp/ui.xml', encoding='utf-8').read()
except Exception: sys.exit(0)
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    if any(v == t for v in re.findall(r'(?:text|content-desc)="([^"]*)"', n)):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups()); print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
}
tapText() { local xy; xy=$(findText "$1"); if [ -n "$xy" ]; then tap $xy; echo "tapText OK: $1"; else echo "tapText MISSING: $1"; fi; }
scrollTo() { for i in 1 2 3 4 5; do [ -n "$(findText "$1")" ] && return; adb shell input swipe 540 1700 540 1000 500; sleep 1; done; }
run() {
  tag=$1
  adb shell am force-stop com.archiekuo.radio
  adb shell am start -n com.archiekuo.radio/.MainActivity
  sleep 12
  scrollTo "熱門電台"
  adb shell input swipe 540 1500 540 1100 500
  shot ${tag}1-首頁國旗-沒有下方播放器 2
  tapText "美國"
  shot ${tag}2-熱門美國 12
  tapText "NPR"
  shot ${tag}3-播放頁一整頁 12
  adb shell input swipe 540 1300 540 500 500
  shot ${tag}3b-播放頁往下滑 2
  adb shell input keyevent 4
  shot ${tag}4-有下方播放器 3
}
adb shell wm size 1080x2340
adb shell wm density 440
sleep 3
adb shell wm size; adb shell wm density
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/radio.apk
adb shell settings put system font_scale 1.0
run a
adb shell settings put system font_scale 1.6
run b
# 模擬一般高手機（dp 高度約 770）看是否剛好一整頁
adb shell settings put system font_scale 1.0
adb shell wm density 300
sleep 3
run t
adb shell settings put system font_scale 1.0
adb shell wm size reset
adb shell wm density reset
adb shell am force-stop com.archiekuo.radio
adb shell am start -n com.archiekuo.radio/.MainActivity
shot c1-車機首頁 15
adb logcat -d -b crash > $OUT/crash-log.txt || true
# rerun
