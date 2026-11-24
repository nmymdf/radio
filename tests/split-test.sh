#!/bin/bash
# 分割畫面：結束分割驗證 — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
divX() { adb shell dumpsys window windows | grep -A25 "Window{[0-9a-f]* u0 DockedStackDivider}:" | grep -o "mFrame=\[[-0-9]*,[-0-9]*\]\[[-0-9]*," | head -1 | sed 's/mFrame=\[\([-0-9]*\),[-0-9]*\]\[\([-0-9]*\),/\1 \2/' | awk '{x=int(($1+$2)/2); if (x<3) x=3; print x}'; }
dragDiv() { local x; x=$(divX); [ -n "$x" ] && adb shell input swipe $x 360 $1 360 1500; echo "divider $x -> $1"; }
adb shell settings put system accelerometer_rotation 0
adb install -r -g apks/carhome.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do adb shell pm grant com.archiekuo.carhome android.permission.$p || true; done
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb shell settings put secure enabled_accessibility_services com.archiekuo.carhome/com.archiekuo.carhome.split.SplitService
adb shell settings put secure accessibility_enabled 1
adb emu geo fix 121.5419 24.9676
( for i in $(seq 1 60); do adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10001 --ei ICON 2 --ei SEG_REMAIN_DIS 120 --es NEXT_ROAD_NAME "北新路" --ei ROUTE_REMAIN_DIS 8500 --ei ROUTE_REMAIN_TIME 900 >/dev/null 2>&1; sleep 2; done ) &
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_nav_pkg com.android.settings
shot e01-桌面 25
adb shell input keyevent 3
sleep 2
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_split start
sleep 14
dragDiv 853
shot e02-分割-桌面三分之二 6
dragDiv 427
shot e03-分割-桌面三分之一 6
adb shell input tap 320 655
shot e04-結束分割 8
adb shell dumpsys activity activities | grep -E "Stack #|mResumedActivity" > $OUT/stacks.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
echo done
