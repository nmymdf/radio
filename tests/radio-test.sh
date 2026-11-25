#!/bin/bash
# 網路收音機 v1.5.1：從 v1.4 升級＋手機字體放大 — 作者：ArchieKUO
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
scrollTo() { for i in 1 2 3 4 5; do [ -n "$(findText "$1")" ] && return; adb shell input swipe 360 1000 360 500 500; sleep 1; done; }

adb shell wm size 1080x2340
adb shell wm density 440
adb shell settings put system font_scale 1.6
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
# 先裝舊版 v1.4，讓它存下舊的分類清單（跟使用者手機一樣）
curl -sL -o old.apk https://raw.githubusercontent.com/nmymdf/radio/test-assets/radio-v1.4.apk
adb install -r -g old.apk
adb shell am start -n com.archiekuo.radio/.MainActivity
sleep 25
adb shell am force-stop com.archiekuo.radio
# 升級到新版
adb install -r -g apks/radio.apk
adb shell am start -n com.archiekuo.radio/.MainActivity
shot u01-升級後首頁 15
scrollTo "熱門電台"
adb shell input swipe 540 1500 540 1000 500
shot u02-熱門國家卡 2
tapText "美國"
shot u03-熱門美國 12
adb shell input swipe 540 1800 540 900 600
shot u04-熱門美國-往下 3
tapText "台灣"
shot u05-熱門台灣 12
adb shell settings put system font_scale 1.0
adb logcat -d -b crash > $OUT/crash-log.txt || true
