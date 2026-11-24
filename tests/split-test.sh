#!/bin/bash
# 導航小卡驗證 — 作者：ArchieKUO
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
sleep 25
( for i in $(seq 1 20); do adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10001 --ei ICON 2 --ei SEG_REMAIN_DIS 1250 --es NEXT_ROAD_NAME "中興路三段" --ei ROUTE_REMAIN_DIS 128000 --ei ROUTE_REMAIN_TIME 5400 >/dev/null 2>&1; sleep 2; done ) &
shot n01-導航小卡 8
adb logcat -d -b crash > $OUT/crash-log.txt || true
