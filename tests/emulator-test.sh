#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 車機桌面 v0.5
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

# 模擬器本來就是 1280x720、160dpi（不要用 wm size 強制改，會讓分割畫面的分隔線跑到邊邊）
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

# 準備兩首測試歌給「內部音樂」（不同資料夾）
ffmpeg -loglevel error -f lavfi -i "sine=frequency=220:duration=90" -f lavfi -i "sine=frequency=880:duration=90" -filter_complex "[0][1]amix=inputs=2,volume=2" -metadata title="測試歌曲" -metadata artist="ArchieKUO" -b:a 128k test-song.mp3
ffmpeg -loglevel error -f lavfi -i "sine=frequency=330:duration=80" -metadata title="第二首老歌" -metadata artist="測試歌手" -b:a 128k test-song2.mp3
adb shell mkdir -p /sdcard/Music/台語老歌
adb push test-song.mp3 /sdcard/Music/test-song.mp3
adb push test-song2.mp3 /sdcard/Music/台語老歌/test-song2.mp3
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/test-song.mp3 || true

# 診斷：直接查天氣服務，看回應
curl -s -m 20 "https://api.open-meteo.com/v1/forecast?latitude=24.9676&longitude=121.5419&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,uv_index&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&timezone=auto&forecast_days=7" | head -c 3000 > $OUT/weather-api.txt
curl -s -m 20 "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=24.9676&longitude=121.5419&current=us_aqi,pm2_5&timezone=auto" | head -c 1000 > $OUT/air-api.txt

# 新店的位置
adb emu geo fix 121.5419 24.9676
adb logcat -c

adb shell am start -n com.archiekuo.carhome/.MainActivity
shot 01-首頁-剛打開 30
adb emu geo fix 121.5419 24.9676
shot 02-首頁-等天氣路名 15

# 模擬開車再停車（讓指南針有方向）
LAT=24.9676
for i in $(seq 1 12); do
  LAT=$(python3 -c "print(round($LAT+0.00015,6))")
  adb emu geo fix 121.5419 $LAT 20 8 30
  sleep 1
done
shot 03-行駛中 2
for i in $(seq 1 12); do adb emu geo fix 121.5419 $LAT 20 8 0; sleep 1; done

# ---- 右上角：沒在導航＝三個大按鈕 ----
shot 04-右上角三個大按鈕 2

# ---- 圓圈三種顯示 ----
tap 200 224
shot 05a-圓圈-大字時鐘 2
tap 200 224
tap 200 224
sleep 1

# ---- 詳細天氣 ----
tap 200 467
shot 06-詳細天氣 8
back

# ---- YouTube：點照片 → 打開 YouTube App ----
tap 1060 380
shot 07a-點YouTube-打開App 10
adb shell dumpsys activity activities | grep -E "mResumedActivity" > $OUT/youtube-activity.txt || true
home
shot 07b-首頁回桌面 4

# ---- 全黑 → 午夜藍（之前會閃） ----
tap 819 618
sleep 2
scrollTap "全黑桌面"
sleep 1
back
shot 08a-全黑桌面 3
tap 819 618
sleep 2
scrollTap "全黑桌面"
sleep 1
back
shot 08b-切回午夜藍 2
shot 08c-切回午夜藍-2秒後 2
shot 08d-切回午夜藍-4秒後 2

# ---- 內部音樂 ----
tap 620 121
sleep 10
tap 492 234
shot 09a-內部音樂-全畫面 4
tapText "test-song2"
shot 09b-點歌播放 4
home
shot 09c-首頁-歌繼續播 4

# ---- 所有 App ----
tap 998 618
shot 10a-所有App 4
tap 850 61
sleep 1
adb shell input text "you"
shot 10b-搜尋you 3
hold 124 169
shot 10c-長按選單 2
back
sleep 1
back

# ---- 設定、記憶體 ----
tap 819 618
shot 11a-設定 3
scrollTap "記憶體與背景 App"
shot 11b-記憶體與背景App 5
back
sleep 1
back

# ---- 導航中：右上角換成導航小卡 ----
( for i in $(seq 1 40); do
    adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10001 --ei ICON 3 --ei SEG_REMAIN_DIS $((400 - i * 5)) --es NEXT_ROAD_NAME "中正路" --ei ROUTE_REMAIN_DIS 12000 --ei ROUTE_REMAIN_TIME 1080 >/dev/null 2>&1
    sleep 2
  done ) &
NAVLOOP=$!
shot 12a-導航中-右上角導航小卡 6
# 導航區放左邊
tap 819 618
sleep 2
scrollTap "導航區和 YouTube 放左邊"
sleep 1
back
shot 12b-導航區放左邊 3
tap 468 618
shot 13-導航選單-四個按鈕 3
back
kill $NAVLOOP 2>/dev/null
sleep 25
shot 14-導航結束-回到三個大按鈕 2

# 記錄
adb logcat -d -b crash > $OUT/crash-log.txt || true
adb logcat -d | grep -E "AndroidRuntime|FATAL|CarHome" | tail -300 > $OUT/app-log.txt || true
echo done
