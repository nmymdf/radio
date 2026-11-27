#!/bin/bash
# 雲端模擬車機測試（Android 10、橫向 1280x720）— 作者：ArchieKUO
# 測試 車機桌面 v0.7
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

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r -g apks/radio.apk
adb install -r -g apks/carhome.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do
  adb shell pm grant com.archiekuo.carhome android.permission.$p || true
done
adb shell appops set com.archiekuo.carhome SYSTEM_ALERT_WINDOW allow || true
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
adb emu geo fix 121.5419 24.9676
# 神盾用 shell 的通知假裝（模擬器沒有神盾）
adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_shield_pkg com.android.shell
home
shot 01-平時-行車資訊 25

# ---- 開車：用 NMEA 送有車速的 GPS（往北約 54 km/h）----
nmea() { # $1 lat(deg) $2 lon(deg)
  python3 - "$1" "$2" <<'PY'
import sys,time
lat=float(sys.argv[1]); lon=float(sys.argv[2])
def dm(v,w): d=int(abs(v)); m=(abs(v)-d)*60; return ("%02d%07.4f" if w==2 else "%03d%07.4f")%(d,m)
t=time.strftime("%H%M%S",time.gmtime()); dte=time.strftime("%d%m%y",time.gmtime())
body="GPRMC,%s.00,A,%s,N,%s,E,29.2,0.0,%s,,,A"%(t,dm(lat,2),dm(lon,3),dte)
cs=0
for ch in body: cs^=ord(ch)
print("$%s*%02X"%(body,cs))
PY
}
LAT=24.9676
for i in $(seq 1 40); do
  LAT=$(python3 -c "print($LAT+0.000135)")
  adb emu geo fix 121.5419 $LAT >/dev/null
  sleep 1
done
shot 02-開了一段-行車資訊 2

# ---- 導航中（假裝高德送轉彎資訊）----
navb() { adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10001 --ei ICON 3 --ei SEG_REMAIN_DIS $1 --es NEXT_ROAD_NAME "中正路" --ei ROUTE_REMAIN_DIS 12400 --ei ROUTE_REMAIN_TIME 1800 >/dev/null; }
navb 300
shot 03-導航中-大字導航 3
navb 80
shot 04-導航中-80公尺 2
adb shell am broadcast -a AUTONAVI_STANDARD_BROADCAST_SEND --ei KEY_TYPE 10019 --ei EXTRA_STATE 9 >/dev/null
shot 05-結束導航-回行車資訊 3

# ---- YouTube 方塊 ----
tapText "YouTube"
shot 06-YouTube方塊-兩顆按鈕 3
tap 640 60
shot 07-點旁邊取消 2

# ---- 神盾：還沒允許讀取通知 ----
tapText "神盾"
shot 08-神盾-引導開啟讀取通知 3
tap 640 60
# 允許讀取通知
adb shell cmd notification allow_listener com.archiekuo.carhome/com.archiekuo.carhome.shield.ShieldWatcher
sleep 4
shot 09-允許後-神盾關 3
adb shell cmd notification post -S bigtext -t "神盾測速照相" shieldtest "背景執行中" >/dev/null
shot 10-神盾開-綠色 4
tapText "神盾 開"
shot 11-神盾開-打開畫面或關閉 3
tapText "關閉神盾"
shot 12-按關閉神盾 3
adb shell cmd notification cancel shieldtest 2>/dev/null || adb shell service call notification 1 >/dev/null
home
shot 13-通知消失-神盾關 4

# ---- 設定 ----
tapText "設定"; sleep 3
shot 14-設定 2
back; sleep 1
# ---- 左右調換 ----
adb shell am start -n com.archiekuo.carhome/.MainActivity
tapText "設定"; sleep 3
scrollTap "導航區放左邊"; sleep 1
back; sleep 2
shot 15-左右調換 2
tapText "設定"; sleep 3
scrollTap "導航區放左邊"; sleep 1
back
adb logcat -d -b crash > $OUT/crash-log.txt || true
# rerun
# rerun2
