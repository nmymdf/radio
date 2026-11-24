#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 v0.3
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
tap() { adb shell input tap $1 $2; }

adb shell wm size 1280x720
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb install -r -g apks/radio.apk
adb install -r -g apks/carhome.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do
  adb shell pm grant com.archiekuo.carhome android.permission.$p || true
done
adb shell appops set com.archiekuo.carhome SYSTEM_ALERT_WINDOW allow || true
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true

# 準備一首測試歌（440Hz 提示音 + 節拍）給「內部音樂」
ffmpeg -loglevel error -f lavfi -i "sine=frequency=220:duration=90" -f lavfi -i "sine=frequency=880:duration=90" -filter_complex "[0][1]amix=inputs=2,volume=2" -metadata title="測試歌曲" -metadata artist="ArchieKUO" -b:a 128k test-song.mp3
adb shell mkdir -p /sdcard/Music
adb push test-song.mp3 /sdcard/Music/test-song.mp3
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/test-song.mp3 || true

# 新店的位置
adb emu geo fix 121.5419 24.9676
adb logcat -c

adb shell am start -n com.archiekuo.carhome/.MainActivity
shot 01-首頁-剛打開 30
adb emu geo fix 121.5419 24.9676
shot 02-首頁-等天氣路名 20

# 模擬開車：沿北新路往北，時速約 55 公里（30 節）
LAT=24.9676
for i in $(seq 1 15); do
  LAT=$(python3 -c "print(round($LAT+0.00015,6))")
  adb emu geo fix 121.5419 $LAT 20 8 30
  sleep 1
done
shot 03-行駛中 2

# 停車
for i in $(seq 1 10); do adb emu geo fix 121.5419 $LAT 20 8 0; sleep 1; done
shot 04-停車後 3

# 設定（下方第 5 個方塊）
tap 819 618
shot 05-設定 3
adb shell input swipe 640 600 640 150 600
shot 06-設定-往下 2
adb shell input swipe 640 600 640 150 600
shot 07-設定-最下面 2
adb shell input keyevent 4

# 所有 App（第 6 個方塊）
tap 998 618
shot 08-所有App 4
adb shell input keyevent 4

# 內部音樂：切換分頁、播放
tap 620 121
shot 09-內部音樂 12
tap 620 376
shot 10-內部音樂-播放中 6

# 長按導航方塊（第 3 個）→ 選 App 面板
adb shell input swipe 461 618 461 618 1200
shot 11-長按方塊-選App 3
adb shell input keyevent 4

# YouTube：平常是照片 → 點照片開啟 → 放大 → 縮小 → 關閉
shot 12a-YouTube-平常照片 2
tap 1046 370
shot 12b-YouTube-開啟 12
tap 954 502
shot 12c-YouTube-放大 6
tap 340 502
sleep 2
tap 1137 502
shot 12d-YouTube-關閉回照片 3
# 更換照片
tap 954 502
shot 12e-更換照片 3
adb shell input keyevent 4
# 導航四大按鈕（第 3 個方塊）
tap 468 618
shot 12f-導航選單 3
adb shell input keyevent 4

# 網路收音機 App（直式）
adb shell am start -n com.archiekuo.radio/.MainActivity
shot 13-網路收音機 15
# 按收音機首頁的「開始播放」（直式畫面 720x1280）
tap 110 376
shot 13b-收音機播放頁 20
adb shell input keyevent 3
sleep 3
# 回到車機桌面，切到「網路收音機」分頁
tap 490 121
shot 14-桌面控制收音機 15
# 用桌面的暫停鍵
tap 620 376
shot 15-桌面暫停收音機 5
tap 620 376
shot 16-桌面再播放 12

# 記錄
adb logcat -d -b crash > $OUT/crash-log.txt || true
adb logcat -d | grep -E "AndroidRuntime|FATAL|carhome|archiekuo|chromium.*ERROR" | tail -400 > $OUT/app-log.txt || true
adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" > $OUT/activity.txt || true
echo done
