#!/bin/bash
# 車機桌面 v0.6：自帶引擎兩種畫法比較 — 作者：ArchieKUO
set -x
OUT=results; mkdir -p $OUT
shot() { sleep ${2:-3}; adb exec-out screencap -p > $OUT/$1.png; echo "shot $1"; }
adb install -r -g apks/carhome.apk
adb shell appops set com.archiekuo.carhome SYSTEM_ALERT_WINDOW allow || true
adb shell cmd package set-home-activity com.archiekuo.carhome/.MainActivity || true
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO READ_EXTERNAL_STORAGE; do adb shell pm grant com.archiekuo.carhome android.permission.$p || true; done
mkdir -p www
ffmpeg -loglevel error -f lavfi -i "testsrc=size=640x360:rate=25:duration=300" -c:v libx264 -preset veryfast -pix_fmt yuv420p www/v.mp4
cat > www/t.html <<'HTML'
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;background:#000"><div id="s" style="color:#ff0;font:bold 40px sans-serif;text-align:center">...</div>
<video id="v" src="v.mp4" autoplay muted playsinline loop style="width:100%;display:block"></video>
<script>setInterval(function(){var v=document.getElementById('v');document.getElementById('s').textContent=(v.paused?'PAUSED ':'PLAY ')+v.currentTime.toFixed(0)+'s '+v.videoWidth+'x'+v.videoHeight+' '+innerWidth+'x'+innerHeight+' dpr'+devicePixelRatio;},500);</script>
</body></html>
HTML
(cd www && python3 -m http.server 8000 >/dev/null 2>&1 &)
U="http://10.0.2.2:8000/t.html"
adb shell input keyevent 3; sleep 20
for tex in true false; do
  adb shell am start -n com.archiekuo.carhome/.MainActivity --ez test_gecko_texture $tex --es test_yt_engine gecko
  sleep 2
  adb shell am start -n com.archiekuo.carhome/.MainActivity --es test_yt_url "$U"
  shot tex-$tex-1-小格 20
  adb shell am start -a android.settings.SETTINGS
  shot tex-$tex-2-浮動 8
  adb shell input keyevent 3
  shot tex-$tex-3-回小格 5
done
adb logcat -d -b crash > $OUT/crash-log.txt || true
pkill -f "http.server 8000" || true
