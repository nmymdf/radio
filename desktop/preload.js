const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('native', {
  rb: (p) => ipcRenderer.invoke('rb', p),
  setOnTop: (v) => ipcRenderer.invoke('onTop', v),
  isOnTop: () => ipcRenderer.invoke('isOnTop'),
  version: () => ipcRenderer.invoke('version'),
});
