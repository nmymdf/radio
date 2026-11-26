#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 車機桌面 v0.6：YouTube 小格／全畫面／浮動
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

adb shell wm size > $OUT/size.txt; adb shell wm density >> $OUT/size.txt
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/radio.apk
adb install -r -g apks/carhome.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do
  adb shell pm grant com.archiekuo.carhome android.permission.$p || true
done
adb shell appops set com.archiekuo.carhome SYSTEM_ALERT_WINDOW allow || true
adb shell appops set com.archiekuo.carhome GET_USAGE_STATS allow || true
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb emu geo fix 121.5419 24.9676
adb shell dumpsys package com.google.android.webview | grep versionName > $OUT/webview-version.txt
adb logcat -c
ytlog() { adb logcat -d -s CarHomeYT:* | tail -40 > $OUT/yt-log-$1.txt; }
playing() { adb logcat -d -s CarHomeYT:* | grep "playing=" | tail -1; }
VID="https://m.youtube.com/watch?v=aqz-KE-bpKQ"

home
shot 01-桌面-神盾沒有自動開 25
tapText "設定"
sleep 3
scrollTap "網頁引擎（點一下切換）"
sleep 1
back; sleep 1
tapText "設定"; sleep 3
for i in 1 2; do adb shell input swipe 640 600 640 260 500; sleep 1; done
shot 02-設定-YouTube與神盾 2
back; sleep 2
# 剛剛點了一次切換（自動→車機內建），切回自動
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_engine auto
sleep 3

# ---- 自動選引擎（模擬器的 WebView 很舊 → 應該用自帶引擎） ----
tapText "開始看"
shot 03-小格-YouTube首頁 30
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$VID"
sleep 30
echo "after url: $(playing)"
# 還沒播就點一下影片
if ! playing | grep -q "playing=true"; then XY=$(findText "放大"); adb shell input tap 1050 380; sleep 8; fi
shot 04-小格-播放中 3
echo "card: $(playing)" | tee $OUT/state.txt
ytlog card
adb shell dumpsys audio | grep -A8 "Audio Focus stack" > $OUT/audio-focus-card.txt

tapText "放大"
shot 05-全畫面 8
echo "full: $(playing)" | tee -a $OUT/state.txt
tapText "縮小"
sleep 3

# ---- 開別的 App：自動變浮動小視窗 ----
adb shell am start -a android.settings.SETTINGS
shot 06-開別的App-浮動小視窗 10
echo "float: $(playing)" | tee -a $OUT/state.txt
adb shell dumpsys window windows | grep -E "Window\{.*(carhome|Settings)" > $OUT/windows.txt
# 拖曳把手移到左上
adb shell input swipe 760 384 200 60 1200
shot 07-浮動-拖到左上 4
# 其他 App 搶聲音（網路收音機開始播）
adb shell am start -n com.archiekuo.radio/.MainActivity
sleep 10
shot 08-浮動在收音機上 2
adb shell dumpsys audio | grep -A8 "Audio Focus stack" > $OUT/audio-focus-float.txt
echo "float over radio: $(playing)" | tee -a $OUT/state.txt
# 按小視窗的「回桌面」
tapText "回桌面"
shot 09-回桌面-回到小格 6
echo "back home: $(playing)" | tee -a $OUT/state.txt
ytlog float

# ---- 車機首頁鍵 ＋ 下方 YouTube 方塊開全畫面 ----
tapText "YouTube"
shot 10-下方方塊-全畫面 6
home
shot 11-按首頁鍵-縮回小格 4
tapText "關閉"
shot 12-關閉-回到照片 3

# ---- 強制用車機內建 WebView 試試 ----
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_engine system
sleep 2
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$VID"
shot 13-車機內建WebView 30
ytlog system
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_engine auto
adb logcat -d -s CarHomeYT:* > $OUT/yt-log-all.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
adb logcat -d | grep -iE "gecko|chromium" | tail -80 > $OUT/engine-log.txt
