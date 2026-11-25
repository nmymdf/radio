#!/bin/bash
# 網路收音機 v1.5 手機版＋車機版測試 — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
tap() { adb shell input tap $1 $2; }
findText() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" "${2:-exact}" "${3:-1}" <<'PY'
import re, sys
t, mode, nth = sys.argv[1], sys.argv[2], int(sys.argv[3])
try: xml = open('/tmp/ui.xml', encoding='utf-8').read()
except Exception: sys.exit(0)
k = 0
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    vals = re.findall(r'(?:text|content-desc)="([^"]*)"', n)
    ok = any(v == t for v in vals) if mode == 'exact' else any(v.startswith(t) for v in vals)
    if ok:
        k += 1
        if k < nth: continue
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
}
tapText() { local xy; xy=$(findText "$1" "${2:-exact}" "${3:-1}"); if [ -n "$xy" ]; then tap $xy; echo "tapText OK: $1 @ $xy"; else echo "tapText MISSING: $1"; fi; }
scrollTo() { for i in 1 2 3 4 5; do [ -n "$(findText "$1" "${2:-exact}")" ] && return; adb shell input swipe 360 1000 360 500 500; sleep 1; done; }

# 手機（直式）
adb shell wm size 720x1280
adb shell wm density 280
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/radio.apk
adb logcat -c
adb shell am start -n com.archiekuo.radio/.MainActivity
shot p01-首頁 15
scrollTo "熱門電台"
adb shell input swipe 360 900 360 600 500
shot p02-首頁-熱門電台國家卡 2
tapText "台灣"
shot p03-熱門-台灣 12
adb shell input swipe 360 1000 360 400 600
shot p04-熱門-台灣-往下 3
tapText "美國"
shot p05-熱門-美國 12
tapText "英國"
shot p06-熱門-英國 10
tapText "台灣"
sleep 4
tapText "ICRT"
shot p07-播放ICRT 15
adb shell input keyevent 4
sleep 2
adb shell input keyevent 4
sleep 2
# 首頁按得回來嗎：點分類 → 按下方「首頁」，重複兩次
for k in 1 2; do
  adb shell input swipe 360 400 360 1000 400
  scrollTo "台語歌曲"
  tapText "台語歌曲"
  sleep 4
  tapText "首頁"
  shot p08-首頁按回來-第${k}次 3
done
# 查看更多：音樂分類
adb shell input swipe 360 400 360 1000 400
scrollTo "音樂分類"
xy=$(python3 - <<'PY'
PY
)
tapText "查看更多" exact 2
shot p09-音樂分類查看更多 6
tapText "首頁"
sleep 2

# 車機（橫式）
adb shell wm size reset
adb shell wm density reset
adb shell am force-stop com.archiekuo.radio
adb shell am start -n com.archiekuo.radio/.MainActivity
shot c01-車機首頁 15
tapText "美國"
shot c02-車機-熱門美國 12
tapText "Jazz24"
shot c03-車機-播放Jazz24 12

adb logcat -d -b crash > $OUT/crash-log.txt || true
echo done
# rerun 1795592907
