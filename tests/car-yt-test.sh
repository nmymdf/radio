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
# YouTube 在雲端機房會要求登入（擋機器人），播放測試改用一般影片檔
# 在測試機器上自己架一個小網站：有畫面計時器＋聲音的影片
mkdir -p www
# 模擬器沒有喇叭（-noaudio），有聲音的影片在自帶引擎會卡住，所以測試用無聲影片
ffmpeg -loglevel error -f lavfi -i "testsrc=size=640x360:rate=25:duration=300" -c:v libx264 -preset veryfast -pix_fmt yuv420p www/v.mp4
cat > www/t.html <<'HTML'
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>test</title></head>
<body style="margin:0;background:#000"><div id="s" style="color:#ff0;font:bold 40px sans-serif;padding:6px;text-align:center">...</div>
<video id="v" src="v.mp4" autoplay muted controls playsinline loop style="width:100%"></video>
<script>setInterval(function(){var v=document.getElementById('v');document.getElementById('s').textContent=(v.paused?'PAUSED ':'PLAY ')+v.currentTime.toFixed(0)+'s';},500);</script>
</body></html>
HTML
(cd www && python3 -m http.server 8000 >/dev/null 2>&1 &)
MP4="http://10.0.2.2:8000/t.html"
floatFrame() { adb shell dumpsys window windows | grep -A25 "Window{[0-9a-f]* u0 com.archiekuo.carhome}:" | grep -o "mFrame=\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]" | head -1 | sed 's/mFrame=\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\1 \2 \3 \4/'; }
# 小視窗上的按鈕：$1＝離右邊多遠
floatBtn() { set -- $1 $(floatFrame); [ -n "$5" ] && tap $(( $4 - $1 )) $(( $3 + 28 )) && echo "floatBtn @ $(( $4 - $1 )),$(( $3 + 28 ))"; }

home
shot 01-桌面-神盾沒有自動開 25
tapText "設定"
sleep 3
scrollTap "網頁引擎（點一下切換）"
sleep 1
back; sleep 1
tapText "設定"; sleep 3
shot 02a-設定-神盾 2
adb shell input swipe 640 600 640 330 500; sleep 1
shot 02b-設定-YouTube 2
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

# ---- 播放測試：一般影片檔，看搬來搬去會不會停 ----
adb logcat -c
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$MP4"
sleep 20
if ! playing | grep -q "playing=true"; then adb shell input tap 1060 380; sleep 8; fi
shot 06a-小格-影片播放中 2
shot 06b-小格-5秒後 5
echo "card mp4: $(playing)" | tee -a $OUT/state.txt
tapText "放大"
shot 07-全畫面-影片 6
echo "full mp4: $(playing)" | tee -a $OUT/state.txt
tapText "縮小"
sleep 4
echo "card again: $(playing)" | tee -a $OUT/state.txt

# ---- 開別的 App：自動變浮動小視窗 ----
adb shell am start -a android.settings.SETTINGS
shot 08-開別的App-浮動小視窗 8
echo "float: $(playing)" | tee -a $OUT/state.txt
echo "frame: $(floatFrame)" | tee -a $OUT/state.txt
adb shell dumpsys window windows | grep -E "Window\{.*(carhome|Settings)" > $OUT/windows.txt
# 拖曳把手移到左上
set -- $(floatFrame); adb shell input swipe $(( $1 + 60 )) $(( $2 + 28 )) 120 40 1200
shot 09-浮動-拖到左上 4
echo "frame after drag: $(floatFrame)" | tee -a $OUT/state.txt
# 其他 App 搶聲音（網路收音機開始播一台）
adb shell am start -n com.archiekuo.radio/.MainActivity
sleep 10
tapText "熱門推薦"; sleep 6
adb shell input tap 900 300; sleep 10
shot 10a-浮動在收音機上-收音機也在播 2
shot 10b-浮動-5秒後 5
adb shell dumpsys audio | grep -A8 "Audio Focus stack" > $OUT/audio-focus-float.txt
echo "float over radio: $(playing)" | tee -a $OUT/state.txt
# 按小視窗的「放大」
floatBtn 140
shot 11-小視窗按放大 6
echo "after 放大: $(playing)" | tee -a $OUT/state.txt
tapText "浮動"
sleep 2
adb shell am start -a android.settings.SETTINGS
sleep 4
# 按小視窗的「回桌面」
floatBtn 225
shot 12-小視窗按回桌面 6
echo "after 回桌面: $(playing)" | tee -a $OUT/state.txt
ytlog float

# ---- 下方 YouTube 方塊開全畫面 ＋ 車機首頁鍵 ----
tapText "YouTube"
shot 13-下方方塊-全畫面 5
home
shot 14-按首頁鍵-縮回小格 4
echo "after home key: $(playing)" | tee -a $OUT/state.txt
tapText "關閉"
shot 15-關閉-回到照片 3

# ---- 強制用車機內建 WebView 試試 ----
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_engine system
sleep 2
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$VID"
shot 16-車機內建WebView 30
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$MP4"
sleep 15
if ! playing | grep -q "playing=true"; then adb shell input tap 1060 380; sleep 8; fi
echo "system mp4: $(playing)" | tee -a $OUT/state.txt
adb shell am start -a android.settings.SETTINGS
shot 17-車機內建-浮動 8
echo "system float: $(playing)" | tee -a $OUT/state.txt
home; sleep 3
ytlog system
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_engine auto
adb logcat -d -s CarHomeYT:* > $OUT/yt-log-all.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
adb logcat -d | grep -iE "gecko|chromium" | tail -80 > $OUT/engine-log.txt
pkill -f "http.server 8000" || true
