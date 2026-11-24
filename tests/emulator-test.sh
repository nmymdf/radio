#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 車機桌面 v0.4 全部新功能
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
# 在設定裡往下捲直到看到文字再點
scrollTap() {
  for i in 1 2 3 4 5 6; do
    local xy; xy=$(findText "$1" "$2")
    if [ -n "$xy" ]; then tap $xy; echo "scrollTap OK: $1"; return; fi
    adb shell input swipe 640 600 640 260 500; sleep 1
  done
  echo "scrollTap MISSING: $1"
}

adb shell wm size 1280x720
adb shell wm density 160
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

# ---- 1. 圓圈三種顯示＋長按神盾 ----
tap 200 224
shot 04a-圓圈-大字時鐘 2
tap 200 224
shot 04b-圓圈-大字指南針 2
tap 200 224
shot 04c-圓圈-回到時鐘指南針 2
hold 200 224
shot 04d-長按圓圈-沒有神盾時選神盾 3
back

# ---- 2. 詳細天氣 ----
tap 200 467
shot 05-詳細天氣 10
back

# ---- 4. YouTube：點照片→全螢幕，返回→縮小繼續播，點畫面→全螢幕，首頁→縮小 ----
tap 1060 364
shot 06a-YouTube-點照片全螢幕 15
back
shot 06b-YouTube-返回縮小 4
tap 1060 364
shot 06c-YouTube-點畫面又全螢幕 4
home
shot 06d-YouTube-首頁鍵縮小 4
tapText "關閉 YouTube"
shot 06e-YouTube-關閉 3

# ---- 5. 內部音樂全畫面選歌 ----
tap 620 121
sleep 10
tap 492 234
shot 07a-內部音樂-全畫面 4
tapText "全部歌曲"
shot 07b-全部歌曲 2
tapText "test-song2"
shot 07c-點歌播放 5
tapText "最近播放"
shot 07d-最近播放 2
home
shot 07e-首頁-歌繼續播 4

# ---- 網路收音機：點封面 → 車機版選台 ----
tap 490 121
sleep 4
tap 492 234
shot 08a-點收音機封面-開收音機 12
home
shot 08b-回桌面 4

# ---- 3. 所有 App：搜尋、長按選單、放到方塊 ----
tap 998 618
shot 09a-所有App 4
tapText "搜尋 App"
sleep 1
adb shell input text "set"
shot 09b-搜尋set 3
hold 124 169
shot 09c-長按選單 2
tapText "放到下方方塊"
shot 09d-選方塊 2
tapText "第 7 個" prefix
shot 09e-已放到方塊 2
hold 124 169
shot 09g-系統App選單 2
tapText "App 資訊"
shot 09h-App資訊 4
back
sleep 2
back
shot 09f-桌面方塊換了 3

# ---- 8. 設定：記憶體與背景 App、神盾放左邊 ----
tap 819 618
shot 10a-設定 3
scrollTap "記憶體與背景 App"
shot 10b-記憶體與背景App 6
back
sleep 1
scrollTap "神盾和 YouTube 放左邊"
shot 10c-設定-神盾放左邊 2
back
shot 11-桌面-神盾放左邊 4

# ---- 7. 導航小卡（模擬高德送出的導航資訊）----
( for i in $(seq 1 60); do
    adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10001 --ei ICON 3 --ei SEG_REMAIN_DIS $((400 - i * 5)) --es NEXT_ROAD_NAME "中正路" --ei ROUTE_REMAIN_DIS 12000 --ei ROUTE_REMAIN_TIME 1080 >/dev/null 2>&1
    sleep 2
  done ) &
NAVLOOP=$!
shot 12-導航中-導航小卡 6

# ---- 6. 分割畫面導航（模擬器沒有高德，用「設定」App 代替導航）----
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_nav_pkg com.android.settings
adb shell settings put secure enabled_accessibility_services com.archiekuo.carhome/com.archiekuo.carhome.split.SplitService
adb shell settings put secure accessibility_enabled 1
sleep 4
tap 468 618
shot 13a-導航選單-五個按鈕 3
tapText "分割畫面"
shot 13b-分割畫面-一半一半 14
adb shell dumpsys activity activities | grep -E "mResumedActivity|Stack #|mBounds|bounds=" | head -60 > $OUT/split-activity.txt || true
adb shell dumpsys window windows | grep -E "Window #|mFrame=|mHasSurface=true" | head -80 > $OUT/split-windows.txt || true
# 拖分隔線到 1/3、2/3
adb shell input swipe 640 360 430 360 900
shot 13c-分割畫面-拖到1比2 5
adb shell input swipe 430 360 850 360 900
shot 13d-分割畫面-拖到2比1 5
adb shell input swipe 850 360 640 360 900
sleep 3
# 金色浮動按鈕：分割 → 導航全畫面
tap 54 630
shot 13e-浮動按鈕-導航全畫面 6
# 首頁鍵 → 回到分割
home
shot 13f-首頁鍵-回到分割 14
tapText "結束分割"
shot 13g-結束分割-回完整桌面 8
kill $NAVLOOP 2>/dev/null

# 記錄
adb logcat -d -b crash > $OUT/crash-log.txt || true
adb logcat -d | grep -E "AndroidRuntime|FATAL|carhome|archiekuo|CarHome|ActivityTaskManager.*split|Divider" | tail -400 > $OUT/app-log.txt || true
adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" > $OUT/activity.txt || true
echo done
