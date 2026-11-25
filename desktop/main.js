// 網路收音機 電腦版 — 作者：ArchieKUO
const { app, BrowserWindow, ipcMain, screen, shell } = require('electron');
const path = require('path');
const fs = require('fs');

const W = 430; // 跟手機一樣的窄視窗
const SERVERS = [
  'https://all.api.radio-browser.info',
  'https://de1.api.radio-browser.info',
  'https://de2.api.radio-browser.info',
  'https://fi1.api.radio-browser.info',
];
let good = null;
let win = null;
const boundsFile = () => path.join(app.getPath('userData'), 'window.json');

function loadBounds() {
  try { return JSON.parse(fs.readFileSync(boundsFile(), 'utf8')); } catch { return null; }
}

function createWindow() {
  const area = screen.getPrimaryDisplay().workArea;
  const saved = loadBounds();
  const h = Math.min(saved?.height || 900, area.height - 20);
  const opts = {
    width: W, height: h, useContentSize: true,
    minWidth: W, maxWidth: W, minHeight: 620,
    resizable: true, maximizable: false, fullscreenable: false,
    backgroundColor: '#070C26', title: '網路收音機', autoHideMenuBar: true,
    icon: path.join(__dirname, 'build', 'icon.ico'), show: false,
    alwaysOnTop: !!saved?.onTop,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true, nodeIntegration: false,
      autoplayPolicy: 'no-user-gesture-required', backgroundThrottling: false,
    },
  };
  // 回到上次的位置（還在螢幕範圍內才用）
  if (saved && typeof saved.x === 'number') {
    const ok = screen.getAllDisplays().some(d => {
      const a = d.workArea;
      return saved.x >= a.x - 50 && saved.x < a.x + a.width - 100 && saved.y >= a.y - 20 && saved.y < a.y + a.height - 100;
    });
    if (ok) { opts.x = saved.x; opts.y = saved.y; }
  }
  win = new BrowserWindow(opts);
  win.removeMenu();
  win.loadFile(path.join(__dirname, 'app', 'index.html'));
  win.once('ready-to-show', () => win.show());
  const save = () => {
    if (!win || win.isMinimized()) return;
    const b = win.getContentBounds();
    const pos = win.getBounds();
    fs.writeFileSync(boundsFile(), JSON.stringify({ x: pos.x, y: pos.y, height: b.height, onTop: win.isAlwaysOnTop() }));
  };
  win.on('moved', save);
  win.on('resized', save);
  win.on('close', save);
}

ipcMain.handle('rb', async (_e, p) => {
  const order = [good, ...SERVERS.filter(s => s !== good)].filter(Boolean);
  let last = null;
  for (const s of order) {
    try {
      const r = await fetch(s + p, { headers: { 'User-Agent': 'ArchieKUO-NetRadio-Desktop/1.1' }, signal: AbortSignal.timeout(15000) });
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const j = await r.json();
      good = s;
      return j;
    } catch (e) { last = e; }
  }
  throw last || new Error('無法連線');
});

const CATALOG_URL = 'https://raw.githubusercontent.com/nmymdf/radio/main/catalog.json';
const catalogCache = () => path.join(app.getPath('userData'), 'catalog.json');
// 分類清單：先用存好的（或內建的），背景再到 GitHub 抓最新版，下次打開生效
ipcMain.handle('catalog', async () => {
  // 清單的檢查日期；沒有各國精選電台的舊清單當作最舊
  const stamp = (t) => { try { const o = JSON.parse(t); return o.featured ? (o.featuredChecked || '0') : ''; } catch { return ''; } };
  let cached = null;
  try { cached = fs.readFileSync(catalogCache(), 'utf8'); JSON.parse(cached).categories.length; } catch { cached = null; }
  const bundled = fs.readFileSync(path.join(__dirname, 'app', 'catalog.json'), 'utf8');
  const text = cached && stamp(cached) >= stamp(bundled) ? cached : bundled;
  fetch(CATALOG_URL, { signal: AbortSignal.timeout(10000) }).then((r) => r.text()).then((t) => {
    try { if (JSON.parse(t).categories.length && stamp(t) >= stamp(text)) fs.writeFileSync(catalogCache(), t); } catch { /* 內容不對就不存 */ }
  }).catch(() => {});
  return text;
});
ipcMain.handle('openUrl', (_e, u) => { if (/^https?:\/\//.test(u)) shell.openExternal(u); });

ipcMain.handle('onTop', (_e, v) => { if (win) { win.setAlwaysOnTop(!!v); } return win ? win.isAlwaysOnTop() : false; });
ipcMain.handle('isOnTop', () => (win ? win.isAlwaysOnTop() : false));
ipcMain.handle('version', () => app.getVersion());

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
