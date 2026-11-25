#!/bin/bash
# 網路收音機 v1.5：實際播放 ICRT、NPR — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
tap() { adb shell input tap $1 $2; }
adb shell wm size 720x1280
adb shell wm density 280
adb shell settings put system accelerometer_rotation 0
adb install -r -g apks/radio.apk
adb shell am start -n com.archiekuo.radio/.MainActivity
sleep 15
adb shell am start -n com.archiekuo.radio/.MainActivity
adb shell input swipe 360 900 360 600 500
sleep 2
shot q01-首頁 1
tap 133 484
sleep 10
tap 650 562
shot q02-播放ICRT 15
adb shell input keyevent 4
sleep 2
tap 360 244
sleep 10
tap 650 562
shot q03-播放NPR 15
adb shell input keyevent 4
sleep 2
tap 133 360
sleep 10
tap 650 562
shot q04-播放韓國第一台 15
adb shell input keyevent 4
sleep 1
tap 587 360
sleep 10
tap 650 562
shot q05-播放香港第一台 15
adb logcat -d -b crash > $OUT/crash-log.txt || true
