#!/bin/bash
# 詳細天氣驗證 — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
adb shell settings put system accelerometer_rotation 0
adb install -r -g apks/carhome.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do adb shell pm grant com.archiekuo.carhome android.permission.$p || true; done
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb emu geo fix 121.5419 24.9676
adb shell am start -n com.archiekuo.carhome/.MainActivity
sleep 20
adb emu geo fix 121.5419 24.9676
for i in 1 2 3 4 5 6 7 8; do
  adb logcat -d | grep -q "weather detail failed" && echo "fail seen $i"
  sleep 15
done
shot w01-桌面 2
adb shell input tap 200 467
shot w02-詳細天氣 6
adb shell input swipe 700 330 300 330 600
shot w03-詳細天氣-12小時往左滑 3
adb logcat -d | grep -E "CarHome" > $OUT/log.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
