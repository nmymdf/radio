#!/bin/bash
# 分割畫面診斷（不改螢幕大小，拖分隔線）— 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
div() { adb shell dumpsys window windows | grep -A3 "DockedStackDivider" | grep -o "mFrame=\[[-0-9]*,[-0-9]*\]\[[-0-9]*,[-0-9]*\]" | head -1; }
adb shell wm size > $OUT/size.txt; adb shell wm density >> $OUT/size.txt
adb shell settings put system accelerometer_rotation 0
adb install -r -g apks/carhome.apk
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb shell settings put secure enabled_accessibility_services com.archiekuo.carhome/com.archiekuo.carhome.split.SplitService
adb shell settings put secure accessibility_enabled 1
adb logcat -c
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_nav_pkg com.android.settings
shot d01-桌面 20
adb shell input keyevent 3
sleep 2
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_split start
shot d02-分割 15
echo "after start: $(div)" >> $OUT/size.txt
F=$(div); X=$(echo $F | sed 's/mFrame=\[\([-0-9]*\),.*\]\[\([-0-9]*\),.*/\1 \2/' | awk '{x=int(($1+$2)/2); if (x<3) x=3; print x}')
W=$(adb shell wm size | grep -o "[0-9]*x[0-9]*" | tail -1 | cut -dx -f1); H=$(adb shell wm size | grep -o "[0-9]*x[0-9]*" | tail -1 | cut -dx -f2)
echo "divider x=$X W=$W H=$H" >> $OUT/size.txt
adb shell input swipe $X $((H/2)) $((W/2)) $((H/2)) 1500
shot d03-拖到中間 6
echo "after drag mid: $(div)" >> $OUT/size.txt
adb shell input swipe $((W/2)) $((H/2)) $((W/3)) $((H/2)) 1500
shot d04-拖到三分之一 6
echo "after drag 1/3: $(div)" >> $OUT/size.txt
adb shell input swipe $((W/3)) $((H/2)) $((W*2/3)) $((H/2)) 1500
shot d05-拖到三分之二 6
echo "after drag 2/3: $(div)" >> $OUT/size.txt
adb shell dumpsys activity activities | grep -E "Stack #|mResumedActivity" > $OUT/stacks.txt
adb logcat -d | grep -E "CarHome|Divider" | tail -100 > $OUT/split-log.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
echo done
