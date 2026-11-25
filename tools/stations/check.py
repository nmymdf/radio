#!/usr/bin/env python3
"""電台檢查工具（雲端執行）— 作者：ArchieKUO
替各國大台在 Radio Browser 找播放網址，逐一實際測試能不能播，
把測過能播的寫進 catalog.json 的 "featured"。"""
import json, subprocess, sys, time, urllib.parse, urllib.request

SERVERS = ["https://de1.api.radio-browser.info", "https://de2.api.radio-browser.info", "https://fi1.api.radio-browser.info", "https://all.api.radio-browser.info"]
UA = {"User-Agent": "ArchieKUO-Radio-Check/1.0"}

def api(path):
    for s in SERVERS:
        try:
            with urllib.request.urlopen(urllib.request.Request(s + path, headers=UA), timeout=20) as r:
                return json.load(r)
        except Exception as e:
            print("  api fail", s, e, file=sys.stderr)
    return []

def plays(url):
    """用 ffprobe 真的去連，看有沒有聲音串流"""
    try:
        r = subprocess.run(["ffprobe", "-v", "error", "-rw_timeout", "10000000", "-user_agent", "Mozilla/5.0",
                            "-show_entries", "stream=codec_type", "-of", "json", url],
                           capture_output=True, text=True, timeout=25)
        return '"audio"' in r.stdout
    except Exception:
        return False

def main():
    cands = json.load(open("tools/stations/candidates.json", encoding="utf-8"))
    featured, report = {}, []
    for cc, items in cands.items():
        out, used = [], set()
        for it in items:
            seen, options = set(), []
            for term in it["terms"]:
                q = urllib.parse.quote(term)
                rows = api(f"/json/stations/search?name={q}&countrycode={cc}&order=clickcount&reverse=true&limit=15")
                if not rows:
                    rows = [x for x in api(f"/json/stations/search?name={q}&order=clickcount&reverse=true&limit=15") if x.get("countrycode", "") in (cc, "")]
                for x in rows:
                    for u in [x.get("url_resolved"), x.get("url")]:
                        if u and u not in seen:
                            seen.add(u); options.append((x, u))
                if len(options) >= 30: break
            # 名稱越接近越先試（完全一樣 > 開頭一樣 > 包含），再看點閱數；排除副頻道
            avoid = [a.lower() for a in it.get("avoid", [])]
            def score(o):
                n = o[0].get("name", "").strip().lower()
                best = 3
                for t in it["terms"]:
                    t = t.lower()
                    if n == t: best = min(best, 0)
                    elif n.startswith(t): best = min(best, 1)
                    elif t in n: best = min(best, 2)
                dirty = 1 if ("#" in o[1] or " " in o[1]) else 0
                return (dirty, best, -(o[0].get("clickcount") or 0), -(o[0].get("bitrate") or 0))
            options = [o for o in options if not any(a in o[0].get("name", "").lower() for a in avoid)]
            options = [o for o in options if o[1] not in used]
            options.sort(key=score)
            ok = None
            for x, u in options[:12]:
                if plays(u):
                    ok = (x, u); break
            if ok:
                x, u = ok
                used.add(u)
                out.append({
                    "name": it["name"], "rbName": x.get("name", "").strip(), "url": u,
                    "favicon": x.get("favicon", ""), "homepage": x.get("homepage", ""),
                    "tags": x.get("tags", ""), "language": x.get("language", ""),
                    "codec": x.get("codec", ""), "bitrate": x.get("bitrate") or 0,
                    "hls": bool(x.get("hls")) or u.split("?")[0].endswith(".m3u8"),
                    "state": x.get("state", ""), "uuid": x.get("stationuuid", ""),
                })
                report.append(f"OK   {cc} {it['name']}  ←  {x.get('name','').strip()}  {u}")
            else:
                report.append(f"MISS {cc} {it['name']}  （找到 {len(options)} 個網址，都播不出來）")
            print(report[-1], flush=True)
        featured[cc] = out
    for path in ["catalog.json", "app/src/main/assets/catalog.json", "desktop/app/catalog.json"]:
        c = json.load(open(path, encoding="utf-8"))
        if c.get("featured") == featured: continue
        c["featured"] = featured
        c["featuredChecked"] = time.strftime("%Y-%m-%d")
        json.dump(c, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    open("tools/stations/report.txt", "w", encoding="utf-8").write("\n".join(report) + "\n")

if __name__ == "__main__":
    main()
