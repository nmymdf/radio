// 網路收音機 電腦版 — 作者：ArchieKUO
'use strict';

/* ---------- 圖示 ---------- */
const P = {
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
  gear: '<circle cx="12" cy="12" r="3"/><circle cx="12" cy="12" r="7"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1"/>',
  play: '<path d="M8 5v14l11-7z"/>',
  pause: '<rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/>',
  prev: '<rect x="4" y="5" width="2.6" height="14" rx="1"/><path d="M20 5L8.5 12 20 19z"/>',
  next: '<rect x="17.4" y="5" width="2.6" height="14" rx="1"/><path d="M4 5l11.5 7L4 19z"/>',
  home: '<path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
  grid: '<rect x="3" y="3" width="8" height="8" rx="2"/><rect x="13" y="3" width="8" height="8" rx="2"/><rect x="3" y="13" width="8" height="8" rx="2"/><rect x="13" y="13" width="8" height="8" rx="2"/>',
  heart: '<path d="M12 20s-7-4.4-9-8.6C1.6 8.3 3.6 5 7 5c2 0 3.3 1.1 5 3 1.7-1.9 3-3 5-3 3.4 0 5.4 3.3 4 6.4C19 15.6 12 20 12 20z"/>',
  user: '<circle cx="12" cy="8" r="4.2"/><path d="M3.5 21c0-4.6 3.8-7.2 8.5-7.2s8.5 2.6 8.5 7.2z"/>',
  back: '<path d="M15 5l-7 7 7 7"/>',
  chev: '<path d="M9 5l7 7-7 7"/>',
  moon: '<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/>',
  vol: '<path d="M4 9h4l5-4v14l-5-4H4z"/><path d="M16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12"/>',
  vol0: '<path d="M4 9h4l5-4v14l-5-4H4z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  flame: '<path d="M12 2c1 4 5.5 5.5 5.5 11.5a5.5 5.5 0 0 1-11 0c0-2.7 1.3-4.4 2.7-5.4 0 2.2 1 3.2 2.1 3.2 0-3.3-1.6-5.5.7-9.3z"/>',
  note: '<path d="M9 18V5l11-2v13" fill="none" stroke="currentColor" stroke-width="2.2"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>',
  eq: '<rect x="3" y="12" width="3" height="9" rx="1"/><rect x="8" y="7" width="3" height="14" rx="1"/><rect x="13" y="10" width="3" height="11" rx="1"/><rect x="18" y="4" width="3" height="17" rx="1"/>',
  check: '<path d="M5 12l5 5 9-10"/>',
  x: '<path d="M6 6l12 12M18 6L6 18"/>',
  trash: '<path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/>',
  mic: '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3M8 21h8"/>',
  news: '<rect x="3" y="4" width="15" height="16" rx="2"/><path d="M18 8h3v10a2 2 0 0 1-2 2M7 8h7M7 12h7M7 16h4"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.7 3.8 5.7 3.8 9s-1.3 6.3-3.8 9c-2.5-2.7-3.8-5.7-3.8-9S9.5 5.7 12 3z"/>',
  school: '<path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11v5c0 1.5 2.7 3 6 3s6-1.5 6-3v-5M22 9v6"/>',
  voice: '<circle cx="9" cy="8" r="4"/><path d="M2 21c0-4 3.1-6.5 7-6.5s7 2.5 7 6.5M17 5a5 5 0 0 1 0 6M19.5 2.5a8.5 8.5 0 0 1 0 11"/>',
  pin: '<path d="M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z"/><circle cx="12" cy="10" r="2.5"/>',
  lang: '<path d="M4 5h9M8.5 3v2M6 5c0 4 3 7 6 8M11 5c-1 4-4 7-7 8"/><path d="M13 21l4-9 4 9M14.5 18h5"/>',
  db: '<ellipse cx="12" cy="6" rx="7" ry="3"/><path d="M5 6v12c0 1.7 3.1 3 7 3s7-1.3 7-3V6M5 12c0 1.7 3.1 3 7 3s7-1.3 7-3"/>',
  radio: '<rect x="3" y="8" width="18" height="12" rx="2"/><path d="M7 8l10-5"/><circle cx="8" cy="14" r="2.5"/><path d="M14 12h4M14 16h4"/>',
  hist: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5M12 7v5l3 2"/>',
};
const FILLED = new Set(['play', 'pause', 'prev', 'next', 'home', 'grid', 'user', 'flame', 'note', 'eq']);
function ic(name, size = 24, fill) {
  const f = fill ?? FILLED.has(name);
  const a = f ? 'fill="currentColor" stroke="none"' : 'fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" ${a} aria-hidden="true">${P[name]}</svg>`;
}
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const A = (n) => `assets/${n}`;
let VERSION = '1.1';

/* ---------- 分類（來自 catalog.json，可從 GitHub 更新） ---------- */
let CATS = [];
const GROUPS = [['music', '音樂', 'note'], ['talk', '講話節目', 'mic'], ['region', '依地區', 'pin']];
const HOTC = () => CATS.filter((c) => c.home === 'hot');
const MUSICC = () => CATS.filter((c) => c.home === 'music');
const TALKC = () => CATS.filter((c) => c.home === 'talk');
const inGroup = (g) => CATS.filter((c) => c.group === g);
const cat = (k) => CATS.find((c) => c.key === k) || CATS[0] || { key: 'hot', title: '熱門推薦', group: 'music', art: 'art_radio', specs: [{ top: '1' }], priority: [] };
const guess = (tags) => { const t = (tags || '').toLowerCase(); return (CATS.find((c) => c.group === 'music' && (c.specs || []).some((s) => s.tag && t.includes(s.tag))) || cat('hot')).key; };
const fromQuery = (q) => CATS.find((c) => q.includes(c.title.slice(0, 2)) || (q.length >= 2 && c.title.includes(q)));
const TALK_ICON = { zhnews: 'news', ennews: 'globe', enlearn: 'school', entalk: 'voice' };

/* 國家、語言轉中文 */
const COUNTRY = { TW: '台灣', HK: '香港', MO: '澳門', CN: '中國', SG: '新加坡', MY: '馬來西亞', JP: '日本', KR: '韓國', US: '美國', GB: '英國', CA: '加拿大', AU: '澳洲', NZ: '紐西蘭', DE: '德國', FR: '法國', IT: '義大利', ES: '西班牙', NL: '荷蘭', CH: '瑞士', AT: '奧地利', BE: '比利時', SE: '瑞典', NO: '挪威', DK: '丹麥', FI: '芬蘭', IE: '愛爾蘭', PT: '葡萄牙', RU: '俄羅斯', IN: '印度', TH: '泰國', VN: '越南', PH: '菲律賓', ID: '印尼', BR: '巴西', MX: '墨西哥', AR: '阿根廷', GR: '希臘', PL: '波蘭', CZ: '捷克', HU: '匈牙利', TR: '土耳其', IL: '以色列' };
const LANGS = [['taiwanese', '台語'], ['hokkien', '台語'], ['min nan', '台語'], ['minnan', '台語'], ['cantonese', '粵語'], ['hakka', '客語'], ['mandarin', '國語'], ['chinese', '中文'], ['english', '英語'], ['japanese', '日語'], ['korean', '韓語'], ['german', '德語'], ['french', '法語'], ['spanish', '西班牙語'], ['italian', '義大利語'], ['portuguese', '葡萄牙語'], ['russian', '俄語'], ['dutch', '荷蘭語'], ['thai', '泰語'], ['vietnamese', '越南語'], ['malay', '馬來語'], ['indonesian', '印尼語']];
const countryZh = (s) => COUNTRY[(s.cc || '').toUpperCase()] || s.country || '';
function langZh(s) {
  const l = (s.lang || '').toLowerCase(); if (!l) return '';
  const f = [...new Set(LANGS.filter(([k]) => l.includes(k)).map(([, v]) => v))];
  return f.length ? f.slice(0, 2).join('、') : (s.lang.split(',')[0].trim().replace(/^./, (c) => c.toUpperCase()));
}
const placeOf = (s) => [...new Set([countryZh(s), (s.state || '').trim()].filter(Boolean))].join(' ・ ');
const qualityOf = (s) => [s.br > 0 ? `${s.br} kbps` : '', (s.codec || '').toUpperCase()].filter(Boolean).join(' ・ ');
const sourceOf = (s) => (s.cur ? '精選電台 ・ Radio Browser' : 'Radio Browser 公開資料庫');
const artOf = (s) => A(`${cat(s.cat).art}.webp`);
const subOf = (s) => { const p = [countryZh(s), langZh(s), s.br > 0 ? `${s.br} kbps` : ''].filter(Boolean); return (p.length ? p : [cat(s.cat).title]).join(' ・ '); };
const liveOf = (s) => ['直播中', countryZh(s), langZh(s)].filter(Boolean).join(' ・ ');

const BANNERS = [
  { img: 'banner_taipei.webp', l1: '今晚，', l2: '聽點', g: '好音樂', sub: '陪伴每一段安靜時光', key: null },
  { img: 'banner_lake.webp', l1: '早安，', l2: '聽見', g: '好心情', sub: '用音樂迎接新的一天', key: 'easy' },
  { img: 'banner_cafe.webp', l1: '雨天，', l2: '聽首', g: '好歌', sub: '窗外下雨，心裡放晴', key: 'jazz' },
  { img: 'banner_zen.webp', l1: '靜心，', l2: '聽見', g: '自在', sub: '讓心慢下來', key: 'zen' },
];

/* ---------- 儲存 ---------- */
const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* 忽略 */ } },
};
const store = {
  fav: LS.get('fav', []),
  hist: LS.get('hist', []),
  recent: LS.get('recent', []),
  autoPlay: LS.get('autoPlay', true),
  last: LS.get('last', null),
  volume: LS.get('volume', 80),
  isFav: (id) => store.fav.some((s) => s.id === id),
  toggleFav(s) { store.fav = store.isFav(s.id) ? store.fav.filter((x) => x.id !== s.id) : [s, ...store.fav]; LS.set('fav', store.fav); },
  removeFav(ids) { store.fav = store.fav.filter((s) => !ids.has(s.id)); LS.set('fav', store.fav); },
  addHist(s) {
    const now = Date.now(); const f = store.hist[0];
    if (!(f && f.s.id === s.id && now - f.t < 30 * 60000)) { store.hist = [{ s, t: now }, ...store.hist].slice(0, 100); LS.set('hist', store.hist); }
  },
  clearHist() { store.hist = []; LS.set('hist', []); },
  addRecent(q) { store.recent = [q, ...store.recent.filter((x) => x !== q)].slice(0, 8); LS.set('recent', store.recent); },
};

/* ---------- 電台資料（Radio Browser） ---------- */
const enc = encodeURIComponent;
function parse(arr, c) {
  const out = [];
  for (const o of arr || []) {
    const url = o.url_resolved || o.url;
    if (!url || o.lastcheckok === 0) continue;
    out.push({
      id: o.stationuuid || url, name: (o.name || '').trim().replace(/\s+/g, ' ') || '未命名電台', url,
      country: o.country || '', cc: o.countrycode || '', state: o.state || '', lang: o.language || '', home: o.homepage || '', codec: o.codec || '',
      br: o.bitrate || 0, hls: o.hls === 1, votes: o.votes || 0, cat: c || guess(o.tags),
    });
  }
  return out;
}
const clean = (l) => { const seen = new Set(); return [...l].sort((a, b) => b.votes - a.votes).filter((s) => { const k = s.name.toLowerCase().trim(); if (seen.has(k)) return false; seen.add(k); return true; }); };
const cache = {};
function prioritize(list, c) {
  const pr = c.priority || []; if (!pr.length) return list;
  const rank = (s) => { const i = pr.findIndex((p) => s.name.toLowerCase().includes(p.toLowerCase())); return i < 0 ? 1e9 : i; };
  return list.map((s) => (rank(s) < 1e9 ? { ...s, cur: true } : s)).sort((a, b) => rank(a) - rank(b) || b.votes - a.votes);
}
function specPath(sp) {
  const limit = sp.limit || '60';
  if (sp.top) return `/json/stations/topvote/${limit}?hidebroken=true`;
  const q = Object.entries(sp).filter(([k]) => k !== 'limit').map(([k, v]) => `${k}=${enc(v)}`).join('&');
  return `/json/stations/search?${q}&hidebroken=true&order=votes&reverse=true&limit=${limit}`;
}
async function loadCat(key, force) {
  if (!force && cache[key]) return cache[key];
  const c = cat(key);
  const specs = (c.specs && c.specs.length) ? c.specs : [{ top: '1' }];
  const parts = await Promise.allSettled(specs.map((sp) => window.native.rb(specPath(sp))));
  if (parts.every((p) => p.status === 'rejected')) throw new Error('net');
  const res = prioritize(clean(parts.flatMap((p) => (p.status === 'fulfilled' ? parse(p.value, key) : []))), c).slice(0, 100);
  if (!res.length) throw new Error('這個分類目前找不到電台');
  cache[key] = res;
  return res;
}
async function searchStations(q) {
  const c = fromQuery(q);
  const [a, b] = await Promise.allSettled([
    window.native.rb(`/json/stations/search?name=${enc(q)}&hidebroken=true&order=votes&reverse=true&limit=60`).then((j) => parse(j, null)),
    c ? loadCat(c.key) : Promise.resolve([]),
  ]);
  if (a.status === 'rejected' && b.status === 'rejected') throw new Error('net');
  return clean([...(b.value || []), ...(a.value || [])]).slice(0, 100);
}
const friendly = (e) => (String(e && e.message).includes('找不到') ? e.message : '連不上電台資料庫，請確認網路後再試一次');

/* ---------- 播放器 ---------- */
const audio = document.getElementById('audio');
let hls = null;
const pl = { st: store.last, queue: [], playing: false, buffering: false, error: null, errors: 0, sleepMin: 0, sleepEnd: 0, sleepTimer: null, retryTimer: null };
audio.volume = store.volume / 100;

function loadSrc(s) {
  if (hls) { hls.destroy(); hls = null; }
  const isHls = s.hls || /\.m3u8(\?|$)/i.test(s.url);
  if (isHls && window.Hls && window.Hls.isSupported()) {
    hls = new window.Hls();
    hls.loadSource(s.url);
    hls.attachMedia(audio);
    hls.on(window.Hls.Events.ERROR, (_e, d) => { if (d.fatal) onAudioError(); });
  } else {
    audio.src = s.url;
  }
  audio.play().catch(() => {});
}
function play(s, list) {
  const l = list && list.some((x) => x.id === s.id) ? list : [s];
  pl.queue = l; pl.st = s; pl.error = null; pl.errors = 0; pl.buffering = true;
  clearTimeout(pl.retryTimer);
  store.last = s; LS.set('last', s); store.addHist(s);
  loadSrc(s);
  updateSession(); refresh();
}
function toggle() {
  if (!pl.st) return;
  if (pl.playing || (pl.buffering && !pl.error)) { clearTimeout(pl.retryTimer); audio.pause(); if (hls) hls.stopLoad(); pl.buffering = false; pl.playing = false; refresh(); }
  else { pl.error = null; pl.errors = 0; pl.buffering = true; loadSrc(pl.st); refresh(); } // 直播：重新接上最新內容
}
function step(d) {
  if (pl.queue.length < 2) return;
  const i = pl.queue.findIndex((x) => x.id === pl.st?.id);
  play(pl.queue[(i + d + pl.queue.length) % pl.queue.length], pl.queue);
}
function onAudioError() {
  if (!pl.st) return;
  pl.errors++;
  pl.playing = false;
  if (pl.errors <= 5) {
    pl.error = '連線不穩，正在重新連線…'; pl.buffering = true;
    clearTimeout(pl.retryTimer);
    pl.retryTimer = setTimeout(() => loadSrc(pl.st), 3000 * pl.errors); // 斷線自動重連
  } else { pl.error = '這個電台目前無法播放，請換一台試試'; pl.buffering = false; }
  refresh();
}
audio.addEventListener('playing', () => { pl.playing = true; pl.buffering = false; pl.error = null; pl.errors = 0; refresh(); });
audio.addEventListener('waiting', () => { pl.buffering = true; refreshMini(); });
audio.addEventListener('pause', () => { pl.playing = false; refresh(); });
audio.addEventListener('error', () => { if (audio.getAttribute('src')) onAudioError(); });
function setSleep(min) {
  clearTimeout(pl.sleepTimer);
  pl.sleepMin = min; pl.sleepEnd = min ? Date.now() + min * 60000 : 0;
  if (min) pl.sleepTimer = setTimeout(() => { audio.pause(); pl.sleepMin = 0; pl.sleepEnd = 0; refresh(); }, min * 60000);
  refresh();
}
function updateSession() {
  if (!('mediaSession' in navigator) || !pl.st) return;
  const s = pl.st;
  navigator.mediaSession.metadata = new MediaMetadata({ title: s.name, artist: `網路收音機 ・ ${cat(s.cat).title}`, artwork: [{ src: new URL(artOf(s), location.href).href, sizes: '512x512' }] });
}
if ('mediaSession' in navigator) {
  navigator.mediaSession.setActionHandler('play', () => { if (!pl.playing) toggle(); });
  navigator.mediaSession.setActionHandler('pause', () => { if (pl.playing) toggle(); });
  navigator.mediaSession.setActionHandler('previoustrack', () => step(-1));
  navigator.mediaSession.setActionHandler('nexttrack', () => step(1));
}
async function playBanner(key) {
  if (!key) {
    if (pl.playing) return;
    const s = pl.st || store.last;
    if (s) { play(s, cache[s.cat] || [s]); return; }
  }
  const k = key || 'hot';
  try { const l = await loadCat(k); if (l[0]) play(l[0], l); } catch { /* 網路問題時不動作 */ }
}

/* ---------- 導覽 ---------- */
const TABS = ['home', 'cats', 'fav', 'mine'];
const nav = { stack: [{ r: 'home' }], catKey: 'classical', catGroup: null };
const cur = () => nav.stack[nav.stack.length - 1];
function goTab(r) { nav.stack = [{ r }]; render(true); }
function open(r, extra) { if (cur().r === r) return; nav.stack.push({ r, ...extra }); render(true); }
function back() { if (nav.stack.length > 1) { nav.stack.pop(); render(true); } }
const tabOf = (r) => ({ home: 0, search: 0, cats: 1, fav: 2, mine: 3, hist: 3, settings: 3 }[r] ?? 0);

/* ---------- 共用畫面片段 ---------- */
const author = () => `<div class="author gold">作者：ArchieKUO　v${VERSION}</div>`;
function header(title, { back: hasBack, actions = '' } = {}) {
  return `<div class="header">${hasBack ? `<button class="ibtn" data-act="back" aria-label="返回">${ic('back', 22)}</button>` : ''}
  <div class="titles"><h1 class="serif gold">${title}</h1>${author()}</div>${actions}</div>`;
}
const loading = () => '<div class="msg"><div class="spin"></div><span>正在找電台…</span></div>';
const msg = (t, d, act, label) => `<div class="msg"><b>${esc(t)}</b>${d ? `<span>${esc(d)}</span>` : ''}${act ? `<button class="bigbtn goldbtn" data-act="${act}">${label}</button>` : ''}</div>`;
const lists = {};
function row(s, l, i, mode = 'normal', extra = '') {
  const curr = pl.st && pl.st.id === s.id;
  let lead = '', trail;
  if (mode === 'check') lead = `<button class="chk ${extra ? 'on' : ''}" data-act="check" data-id="${esc(s.id)}" aria-label="選取"><span>${extra ? ic('check', 20) : ''}</span></button>`;
  if (mode === 'hist') trail = `<span class="tm">${extra}</span>`;
  else if (mode === 'check') trail = '';
  else { const f = store.isFav(s.id); trail = `<button class="fav ${f ? 'on' : ''}" data-act="fav" data-l="${l}" data-i="${i}" aria-label="${f ? '取消收藏' : '收藏'}">${ic('heart', 28, f)}</button>`; }
  return `<div class="row ${curr ? 'cur' : ''}" data-act="play" data-l="${l}" data-i="${i}">${lead}
    <img class="art" src="${artOf(s)}" alt="">
    <div class="info"><div class="nm">${curr ? `<span style="color:var(--gold);display:flex">${ic('eq', 20)}</span>` : ''}${esc(s.name)}</div><div class="sb">${esc(subOf(s))}</div></div>
    ${trail}<button class="playb goldbtn" data-act="play" data-l="${l}" data-i="${i}" aria-label="播放">${ic('play', 26)}</button></div>`;
}
function statusHtml() {
  let dot, text, color;
  if (pl.error) { dot = 'var(--red)'; text = pl.error; color = 'var(--red)'; }
  else if (pl.buffering) { dot = 'var(--gold)'; text = '連線中…'; color = 'var(--gold)'; }
  else if (pl.playing) { dot = 'var(--green)'; text = pl.st ? liveOf(pl.st) : '直播中'; color = 'var(--greenT)'; }
  else { dot = 'var(--dim)'; text = '已暫停'; color = 'var(--sub)'; }
  return `<i style="background:${dot}"></i><span style="color:${color}">${esc(text)}</span>`;
}
const isOn = () => pl.playing || (pl.buffering && !pl.error);

/* ---------- 各頁 ---------- */
const state = { cats: {}, search: null, query: '', sel: new Set(), slide: 0 };

function pageHome() {
  const slides = BANNERS.map((b, i) => `<div class="slide" data-act="banner" data-i="${i}"><img src="${A(b.img)}" alt=""><div class="shade"></div>
    <div class="txt"><div class="t1 serif">${b.l1}<br>${b.l2}<span class="gold">${b.g}</span></div><div class="t2">— ${b.sub} —</div>
    <button class="go goldbtn" data-act="banner" data-i="${i}">${ic('play', 26)}開始播放</button></div></div>`).join('');
  return `<div class="header"><img src="${A('ic_logo.png')}" alt="" style="width:56px;height:56px">
    <div class="titles"><h1 class="serif gold" style="font-size:31px">網路收音機</h1>${author()}</div>
    <button class="ibtn" data-act="open" data-r="search" aria-label="搜尋">${ic('search')}</button>
    <button class="ibtn" data-act="open" data-r="settings" aria-label="設定">${ic('gear')}</button></div>
  <div class="carousel card" id="car"><div class="track" id="track" style="transform:translateX(-${state.slide * 100}%)">${slides}</div>
    <div class="dots">${BANNERS.map((_, i) => `<span class="${i === state.slide ? 'on' : ''}" data-act="dot" data-i="${i}"></span>`).join('')}</div></div>
  <div class="sec"><span style="color:var(--gold);display:flex">${ic('flame', 30)}</span><h2>熱門電台</h2><button class="more" data-act="tab" data-r="cats">查看更多${ic('chev', 20)}</button></div>
  <div class="hot">${HOTC().map((c) => `<div class="hotc card" data-act="cat" data-k="${c.key}"><img src="${A(c.art + '.webp')}" alt=""><div class="lb">${c.title}<span style="color:var(--gold);display:flex">${ic('eq', 18)}</span></div></div>`).join('')}</div>
  <div class="sec"><span style="color:var(--gold);display:flex">${ic('note', 30)}</span><h2>音樂分類</h2><button class="more" data-act="tab" data-r="cats">查看更多${ic('chev', 20)}</button></div>
  <div class="cats">${MUSICC().map((c) => `<div class="catc card" data-act="cat" data-k="${c.key}" style="background:linear-gradient(180deg,${c.tint},#121A46)"><img src="${A(c.icon + '.png')}" alt="">${c.title}</div>`).join('')}</div>
  ${TALKC().length ? `<div class="sec"><span style="color:var(--gold);display:flex">${ic('mic', 30)}</span><h2>講話節目</h2><button class="more" data-act="cat" data-k="${TALKC()[0].key}">查看更多${ic('chev', 20)}</button></div>
  <div class="cats">${TALKC().slice(0, 3).map((c) => `<div class="catc card" data-act="cat" data-k="${c.key}" style="background:linear-gradient(180deg,${c.tint},#121A46)"><span style="color:var(--gold);display:flex">${ic(TALK_ICON[c.key] || 'mic', 42)}</span>${c.title}</div>`).join('')}</div>` : ''}`;
}

function pageCats() {
  const key = nav.catKey; const c = cat(key);
  const l = state.cats[key];
  if (!l) { state.cats[key] = { loading: true }; loadCat(key).then((x) => { state.cats[key] = { list: x }; }).catch((e) => { state.cats[key] = { err: friendly(e) }; }).finally(() => { if (cur().r === 'cats' && nav.catKey === key) render(); }); }
  let body;
  const v = state.cats[key];
  if (v.list) { lists.cat = v.list; body = `<div class="note">${c.title} ・ 共 ${v.list.length} 台</div><div class="list">${v.list.map((s, i) => row(s, 'cat', i)).join('')}</div>`; }
  else if (v.err) body = msg(v.err, null, 'retry-cat', '再試一次');
  else body = loading();
  const grp = nav.catGroup || c.group;
  return header('全部分類', { actions: `<button class="ibtn" data-act="open" data-r="search" aria-label="搜尋">${ic('search')}</button>` }) +
    `<div class="gtabs">${GROUPS.map(([g, lb, icn]) => `<button class="${g === grp ? 'on' : ''}" data-act="group" data-g="${g}">${ic(icn, 22, icn === 'note')}${lb}</button>`).join('')}</div>
    <div class="pgrid">${inGroup(grp).map((x) => `<button class="pill ${x.key === key ? 'on' : ''}" data-act="pill" data-k="${x.key}">${x.title}</button>`).join('')}</div>${body}`;
}

function pagePlayer() {
  const s = pl.st;
  const f = s && store.isFav(s.id);
  let h = `<div class="ptop"><button class="ibtn" data-act="back" aria-label="返回">${ic('back', 22)}</button><div class="t">正在播放</div>
    <button class="ibtn" data-act="favcur" style="color:var(--gold)" aria-label="${f ? '取消收藏' : '收藏'}">${ic('heart', 24, !!f)}</button></div>`;
  if (!s) return h + msg('還沒有選電台', '回到首頁，挑一個喜歡的電台吧', 'back', '回首頁');
  const left = pl.sleepEnd ? Math.max(0, Math.ceil((pl.sleepEnd - Date.now()) / 60000)) : 0;
  h += `<img class="pcover" src="${artOf(s)}" alt="電台封面"><div class="pname">${esc(s.name)}</div>
    <div class="status">${statusHtml()}</div>${sourceCard(s)}
    <div class="ctrls"><button class="ibtn" style="width:64px;height:64px" data-act="prev" aria-label="上一台">${ic('prev', 34)}</button>
    <button class="ring" style="width:96px;height:96px" data-act="toggle" aria-label="${isOn() ? '暫停' : '播放'}">${ic(isOn() ? 'pause' : 'play', 48)}</button>
    <button class="ibtn" style="width:64px;height:64px" data-act="next" aria-label="下一台">${ic('next', 34)}</button></div>
    <div class="vol">${ic('vol0', 30)}<input type="range" min="0" max="100" value="${store.volume}" id="volume" aria-label="音量">${ic('vol', 30)}</div>
    <div class="sleep card"><div class="h">${ic('moon', 24)}睡眠定時</div>
    <div class="opts">${[[0, '關閉'], [15, '15分'], [30, '30分'], [60, '60分'], [90, '90分']].map(([m, t]) => `<button class="pill ${pl.sleepMin === m ? 'on' : ''}" data-act="sleep" data-m="${m}">${t}</button>`).join('')}</div>
    <div class="left" id="sleepleft">${pl.sleepEnd ? `還有 ${left} 分鐘後自動停止` : '選好時間，時間到就會自動停止播放'}</div></div>
    <div class="pauthor">${author()}</div>`;
  return h;
}

function sourceCard(s) {
  const rows = [['radio', '電台', s.name], ['pin', '地區', placeOf(s)], ['lang', '語言', langZh(s)], ['db', '資料', sourceOf(s)], ['eq', '音質', qualityOf(s)]].filter((r) => r[2]);
  return `<div class="src card"><div class="h">節目來源</div>${rows.map(([icn, k, v]) => `<div class="r"><span class="i">${ic(icn, 22, icn === 'eq')}</span><span class="k">${k}</span><span class="v">${esc(v)}</span></div>`).join('')}
    ${/^https?:\/\//.test(s.home || '') ? '<button class="web" data-act="homepage">打開電台官方網站</button>' : ''}</div>`;
}
function pageFav() {
  const favs = store.fav; lists.fav = favs;
  for (const id of [...state.sel]) if (!favs.some((s) => s.id === id)) state.sel.delete(id);
  if (!favs.length) return header('我的收藏') + msg('還沒有收藏的電台', '在電台旁邊按一下愛心，就會收藏到這裡');
  const n = state.sel.size; const all = n === favs.length;
  return header('我的收藏') + `<div class="toolbar"><span class="cnt">${n ? `已選 ${n} 台` : `共 ${favs.length} 台`}</span>
    <button class="tbtn" data-act="selall">${all ? '全不選' : '全選'}</button>
    <button class="tbtn danger" data-act="delfav" ${n ? '' : 'disabled'}>${ic('trash', 20)}移除收藏</button></div>
    <div class="list" style="padding-left:4px">${favs.map((s, i) => row(s, 'fav', i, 'check', state.sel.has(s.id))).join('')}</div>`;
}

function pageSearch() {
  let body = '';
  if (store.recent.length) body += `<div class="lbl">最近搜尋</div><div class="chips">${store.recent.map((r) => `<button class="chip" data-act="recent" data-q="${esc(r)}">${ic('hist', 20)}${esc(r)}</button>`).join('')}</div>`;
  const r = state.search;
  if (!r) { if (!store.recent.length) body += msg('想聽什麼？', '可以輸入「爵士」、「古典」、「鋼琴」，或電台名稱'); }
  else if (r.loading) body += loading();
  else if (r.err) body += msg(r.err, null, 'dosearch', '再試一次');
  else { lists.search = r.list; body += `<div class="note">「${esc(state.query)}」的搜尋結果 ・ 共 ${r.list.length} 台</div><div class="list">${r.list.map((s, i) => row(s, 'search', i)).join('')}</div>`; }
  return `<div class="header"><button class="ibtn" data-act="back" aria-label="返回">${ic('back', 22)}</button>
    <label class="sbox">${ic('search', 26)}<input id="q" placeholder="輸入電台名稱或音樂類型" value="${esc(state.query)}" autocomplete="off">
    ${state.query ? `<button class="clr" data-act="clearq" aria-label="清除">${ic('x', 22)}</button>` : ''}</label></div>${body}`;
}

function pageMine() {
  return header('我的') + `<div class="glabel">我的電台</div><div class="grpcard card">
    <div class="mrow" data-act="tab" data-r="fav"><span style="color:var(--gold);display:flex">${ic('heart', 28, true)}</span><span class="t">我的收藏</span><span class="v">${store.fav.length} 台</span><span style="color:var(--dim);display:flex">${ic('chev', 26)}</span></div>
    <div class="mrow" data-act="open" data-r="hist"><span style="color:var(--gold);display:flex">${ic('hist', 28)}</span><span class="t">播放紀錄</span><span class="v">${store.hist.length} 筆</span><span style="color:var(--dim);display:flex">${ic('chev', 26)}</span></div>
    <div class="mrow" data-act="open" data-r="settings"><span style="color:var(--gold);display:flex">${ic('gear', 28)}</span><span class="t">設定</span><span style="color:var(--dim);display:flex">${ic('chev', 26)}</span></div></div>${about()}`;
}
function about() {
  return `<div class="glabel">關於</div><div class="grpcard card">
    <div class="irow"><span class="t">版本</span><span style="color:var(--sub)">v${VERSION}</span></div>
    <div class="irow"><span class="t">作者</span><span class="gold" style="font-style:italic;font-weight:700">ArchieKUO</span></div>
    <div class="irow"><span class="t">電台資料</span><span style="color:var(--sub);font-size:18px">Radio Browser</span></div></div><div style="height:20px"></div>`;
}
function dayLabel(t) {
  const d = new Date(t); const now = new Date(); const y = new Date(Date.now() - 86400000);
  const same = (a, b) => a.toDateString() === b.toDateString();
  return same(d, now) ? '今天' : same(d, y) ? '昨天' : `${d.getMonth() + 1} 月 ${d.getDate()} 日`;
}
function pageHist() {
  const h = store.hist;
  const act = h.length ? `<button class="tbtn danger" data-act="clearhist">${ic('trash', 20)}清除</button>` : '';
  if (!h.length) return header('播放紀錄', { back: true }) + msg('還沒有播放紀錄', '聽過的電台會自動記在這裡');
  const stations = []; const seen = new Set();
  h.forEach((e) => { if (!seen.has(e.s.id)) { seen.add(e.s.id); stations.push(e.s); } });
  lists.hist = stations;
  let out = ''; let lastLabel = '';
  h.forEach((e) => {
    const lb = dayLabel(e.t);
    if (lb !== lastLabel) { out += `<div class="grp">${lb}</div>`; lastLabel = lb; }
    const d = new Date(e.t); const tm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    out += row(e.s, 'hist', stations.findIndex((x) => x.id === e.s.id), 'hist', tm);
  });
  return header('播放紀錄', { back: true, actions: act }) + `<div class="list">${out}</div>`;
}
function pageSettings() {
  return header('設定', { back: true }) + `<div class="glabel">播放</div><div class="grpcard card">
    <div class="srow" data-act="setauto"><div class="t"><b>打開就播放</b><span>打開程式時，自動接著播上次的電台</span></div><span class="sw ${store.autoPlay ? 'on' : ''}"></span></div></div>
    <div class="glabel">視窗</div><div class="grpcard card">
    <div class="srow" data-act="setontop"><div class="t"><b>視窗永遠在最上層</b><span>開著別的程式時，收音機也不會被蓋住</span></div><span class="sw ${state.onTop ? 'on' : ''}"></span></div></div>${about()}`;
}

/* ---------- 繪製 ---------- */
const content = document.getElementById('content');
function render(resetScroll) {
  const r = cur().r;
  const top = content.scrollTop;
  const html = { home: pageHome, cats: pageCats, player: pagePlayer, fav: pageFav, search: pageSearch, mine: pageMine, hist: pageHist, settings: pageSettings }[r]();
  content.innerHTML = html;
  content.scrollTop = resetScroll ? 0 : top;
  if (r === 'search' && !state.search && resetScroll) document.getElementById('q')?.focus();
  refreshMini(); renderNav();
}
function refresh() { const r = cur().r; if (r === 'player' || r === 'cats' || r === 'search' || r === 'fav' || r === 'hist') render(); else refreshMini(); }
function refreshMini() {
  const box = document.getElementById('mini');
  const s = pl.st;
  if (!s || cur().r === 'player') { box.innerHTML = ''; return; }
  box.innerHTML = `<div class="box card" data-act="open" data-r="player"><img class="art" src="${artOf(s)}" alt="">
    <div class="info"><div class="nm">${esc(s.name)}</div><div class="status">${statusHtml()}</div></div>
    <button class="pbtn" data-act="prev" aria-label="上一台">${ic('prev', 32)}</button>
    <button class="ring" style="width:58px;height:58px;border-width:2.5px" data-act="toggle" aria-label="${isOn() ? '暫停' : '播放'}">${ic(isOn() ? 'pause' : 'play', 28)}</button>
    <button class="pbtn" data-act="next" aria-label="下一台">${ic('next', 32)}</button></div>`;
}
function renderNav() {
  const el = document.getElementById('nav');
  if (cur().r === 'player') { el.style.display = 'none'; return; }
  el.style.display = '';
  const t = tabOf(cur().r);
  el.innerHTML = [['首頁', 'home'], ['分類', 'grid'], ['收藏', 'heart'], ['我的', 'user']].map(([lb, icn], i) =>
    `<button class="${i === t ? 'on' : ''}" data-act="tab" data-r="${TABS[i]}">${ic(icn, 30, true)}<span>${lb}</span></button>`).join('');
}
function confirmBox(title, text, okLabel, onOk) {
  const d = document.getElementById('dialog');
  d.innerHTML = `<div class="bg"><div class="dlg"><h3>${esc(title)}</h3><p>${esc(text)}</p><div class="acts"><button data-dlg="no">取消</button><button class="ok" data-dlg="ok">${esc(okLabel)}</button></div></div></div>`;
  d.onclick = (e) => { const b = e.target.closest('[data-dlg]'); if (!b && e.target.classList.contains('bg')) { d.innerHTML = ''; return; } if (!b) return; d.innerHTML = ''; if (b.dataset.dlg === 'ok') onOk(); };
}

/* ---------- 操作 ---------- */
function doSearch(q) {
  const t = (q ?? state.query).trim(); if (!t) return;
  state.query = t; store.addRecent(t); state.search = { loading: true }; render();
  searchStations(t).then((l) => { state.search = l.length ? { list: l } : { err: `找不到「${t}」相關的電台，換個關鍵字試試` }; })
    .catch((e) => { state.search = { err: friendly(e) }; }).finally(() => { if (cur().r === 'search') render(); });
}
document.addEventListener('click', (e) => {
  const el = e.target.closest('[data-act]');
  if (!el || dragMoved) return;
  const a = el.dataset.act;
  if (el.tagName === 'BUTTON' || a === 'check') e.stopPropagation();
  const L = () => (lists[el.dataset.l] || [])[+el.dataset.i];
  switch (a) {
    case 'back': back(); break;
    case 'tab': goTab(el.dataset.r); break;
    case 'open': open(el.dataset.r); break;
    case 'cat': nav.catKey = el.dataset.k; nav.catGroup = cat(el.dataset.k).group; nav.stack = [{ r: 'cats' }]; render(true); break;
    case 'pill': nav.catKey = el.dataset.k; render(); break;
    case 'group': { const g = el.dataset.g; nav.catGroup = g; if (cat(nav.catKey).group !== g && inGroup(g)[0]) nav.catKey = inGroup(g)[0].key; render(); break; }
    case 'homepage': if (pl.st && pl.st.home) window.native.openUrl(pl.st.home); break;
    case 'retry-cat': delete state.cats[nav.catKey]; delete cache[nav.catKey]; render(); break;
    case 'play': { const s = L(); if (s) { play(s, lists[el.dataset.l]); open('player'); } break; }
    case 'fav': { const s = L(); if (s) { store.toggleFav(s); render(); } break; }
    case 'favcur': if (pl.st) { store.toggleFav(pl.st); render(); } break;
    case 'toggle': toggle(); break;
    case 'prev': step(-1); break;
    case 'next': step(1); break;
    case 'sleep': setSleep(+el.dataset.m); break;
    case 'banner': playBanner(BANNERS[+el.dataset.i].key); open('player'); break;
    case 'dot': setSlide(+el.dataset.i); break;
    case 'check': { const id = el.dataset.id; state.sel.has(id) ? state.sel.delete(id) : state.sel.add(id); render(); break; }
    case 'selall': state.sel = state.sel.size === store.fav.length ? new Set() : new Set(store.fav.map((s) => s.id)); render(); break;
    case 'delfav': if (state.sel.size) confirmBox('移除收藏', `確定要把選取的 ${state.sel.size} 個電台從收藏移除嗎？`, '移除', () => { store.removeFav(state.sel); state.sel = new Set(); render(); }); break;
    case 'clearhist': confirmBox('清除播放紀錄', `確定要清除全部 ${store.hist.length} 筆播放紀錄嗎？`, '清除', () => { store.clearHist(); render(); }); break;
    case 'recent': state.query = el.dataset.q; doSearch(el.dataset.q); break;
    case 'dosearch': doSearch(); break;
    case 'clearq': state.query = ''; state.search = null; render(); document.getElementById('q')?.focus(); break;
    case 'setauto': store.autoPlay = !store.autoPlay; LS.set('autoPlay', store.autoPlay); render(); break;
    case 'setontop': window.native.setOnTop(!state.onTop).then((v) => { state.onTop = v; render(); }); break;
    default: break;
  }
});
document.addEventListener('input', (e) => {
  if (e.target.id === 'volume') { store.volume = +e.target.value; audio.volume = store.volume / 100; LS.set('volume', store.volume); }
  if (e.target.id === 'q') state.query = e.target.value;
});
document.addEventListener('keydown', (e) => {
  if (e.target.id === 'q' && e.key === 'Enter') { e.preventDefault(); e.target.blur(); doSearch(); return; }
  if (e.target.tagName === 'INPUT') return;
  if (e.key === ' ') { e.preventDefault(); toggle(); }
  else if (e.key === 'Escape' || e.key === 'Backspace') back();
});
document.addEventListener('wheel', (e) => { if (e.target.id === 'volume') e.preventDefault(); }, { passive: false }); // 滾輪不能改音量

/* ---------- 輪播（自動換張、可用滑鼠拖曳） ---------- */
let dragMoved = false;
function setSlide(i) {
  state.slide = (i + BANNERS.length) % BANNERS.length;
  const t = document.getElementById('track'); if (!t) return;
  t.style.transform = `translateX(-${state.slide * 100}%)`;
  document.querySelectorAll('.dots span').forEach((d, k) => d.classList.toggle('on', k === state.slide));
}
let hover = false;
setInterval(() => { if (cur().r === 'home' && !hover && !document.hidden) setSlide(state.slide + 1); }, 7000);
document.addEventListener('mouseover', (e) => { hover = !!e.target.closest('#car'); });
let dragX = null;
document.addEventListener('pointerdown', (e) => { dragMoved = false; if (e.target.closest('#car')) dragX = e.clientX; });
document.addEventListener('pointerup', (e) => {
  if (dragX == null) return;
  const dx = e.clientX - dragX; dragX = null;
  if (Math.abs(dx) > 50) { dragMoved = true; setSlide(state.slide + (dx < 0 ? 1 : -1)); setTimeout(() => { dragMoved = false; }, 0); }
});

/* 睡眠定時倒數 */
setInterval(() => {
  const el = document.getElementById('sleepleft');
  if (el && pl.sleepEnd) el.textContent = `還有 ${Math.max(0, Math.ceil((pl.sleepEnd - Date.now()) / 60000))} 分鐘後自動停止`;
}, 1000);

/* ---------- 啟動 ---------- */
(async () => {
  try { const v = await window.native.version(); VERSION = v.split('.').slice(0, 2).join('.'); } catch { /* 用預設 */ }
  try { state.onTop = await window.native.isOnTop(); } catch { state.onTop = false; }
  try { CATS = JSON.parse(await window.native.catalog()).categories || []; } catch { CATS = []; }
  render(true);
  if (store.autoPlay && store.last) play(store.last, [store.last]);
})();
