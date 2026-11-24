package com.archiekuo.carhome.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object Http {
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "ArchieKUO-CarHome/0.1 (personal car launcher)")
        c.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9")
        try {
            val code = c.responseCode
            if (code >= 400) {
                val err = runCatching { c.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
                throw java.io.IOException("HTTP $code ${err?.take(300) ?: ""}")
            }
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
