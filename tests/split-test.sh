#!/bin/bash
# 分割畫面逐步診斷 — 作者：ArchieKUO
set -x
OUT=results
mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
dump() { adb shell dumpsys activity activities | grep -E "Stack #|mResumedActivity" > $OUT/$1-stacks.txt; adb shell dumpsys window windows | grep -E "Window #|mFrame=" | grep -B1 "mFrame" | head -40 > $OUT/$1-windows.txt; }
adb shell wm size 1280x720
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/carhome.apk
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb shell settings put secure enabled_accessibility_services com.archiekuo.carhome/com.archiekuo.carhome.split.SplitService
adb shell settings put secure accessibility_enabled 1
adb logcat -c
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_nav_pkg com.android.settings
shot s01-桌面 20
# A：Settings 全畫面 → 切分割（只切，不開桌面）
adb shell am start -n com.android.settings/.Settings
shot s02-設定全畫面 5
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_split toggle
shot s03-切分割後 6
dump s03
# B：開精簡桌面
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_split desk
shot s04-開精簡桌面後 6
dump s04
# C：離開分割
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_split toggle
shot s05-離開分割 5
dump s05
# D：用 App 內流程（導航選單 → 分割畫面）
adb shell input keyevent 3
sleep 3
adb shell input tap 468 618
sleep 2
adb shell input tap 1091 371
shot s06-選單分割 15
dump s06
adb logcat -d | grep -E "CarHome|ActivityTaskManager|Divider|split" | tail -200 > $OUT/split-log.txt
adb logcat -d -b crash > $OUT/crash-log.txt || true
echo done
