package com.archiekuo.carhome.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class Song(val uri: Uri, val title: String, val artist: String, val path: String)

/** 找出車機和隨身碟裡的歌曲 */
object LocalLibrary {
    private val exts = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "wma", "opus")

    suspend fun scan(ctx: Context): List<Song> = withContext(Dispatchers.IO) {
        val out = LinkedHashMap<String, Song>()
        val volumes = runCatching {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(ctx).toList() else listOf("external")
        }.getOrDefault(listOf("external"))
        for (vol in volumes) {
            val base = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(vol) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            runCatching {
                ctx.contentResolver.query(
                    base,
                    arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION),
                    "${MediaStore.Audio.Media.DURATION} >= 30000", null, "${MediaStore.Audio.Media.DATA} ASC",
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val path = c.getString(3) ?: ""
                        val title = c.getString(1)?.takeIf { it.isNotBlank() } ?: File(path).nameWithoutExtension
                        val artist = c.getString(2)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: ""
                        val key = path.ifBlank { "$vol/$id" }
                        out[key] = Song(ContentUris.withAppendedId(base, id), title, artist, path)
                    }
                }
            }
        }
        // 隨身碟有時沒有被系統建立索引，直接掃資料夾補上
        val roots = mutableListOf<File>()
        File("/storage").listFiles()?.filter { it.isDirectory && it.name != "self" && it.name != "emulated" }?.let { roots += it }
        roots += listOf(File("/mnt/usb_storage"), File("/mnt/media_rw"), File("/udisk"), File("/mnt/udisk"))
        roots += runCatching { Environment.getExternalStorageDirectory() }.getOrNull()?.let { listOf(File(it, "Music"), File(it, "Download")) } ?: emptyList()
        roots.distinct().filter { it.exists() && it.canRead() }.forEach { walk(it, 0, out) }
        out.values.toList()
    }

    private fun walk(dir: File, depth: Int, out: LinkedHashMap<String, Song>) {
        if (depth > 6 || out.size > 5000) return
        val files = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (f in files.sortedBy { it.name }) {
            if (f.name.startsWith(".") || f.name.equals("Android", true)) continue
            if (f.isDirectory) walk(f, depth + 1, out)
            else if (f.extension.lowercase() in exts && f.length() > 300_000 && !out.containsKey(f.absolutePath)) {
                out[f.absolutePath] = Song(Uri.fromFile(f), f.nameWithoutExtension, f.parentFile?.name ?: "", f.absolutePath)
            }
        }
    }
}
