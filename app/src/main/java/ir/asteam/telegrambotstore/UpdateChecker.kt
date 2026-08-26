package ir.asteam.telegrambotstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val latestVersion: String, val downloadUrl: String)

object UpdateChecker {
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/waxew/App-TelegramBotStore/releases/latest").openConnection() as HttpURLConnection
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.connectTimeout = 8_000
            c.readTimeout = 8_000
            try {
                if (c.responseCode == 404) return@runCatching null
                if (c.responseCode !in 200..299) error("خطا در بررسی بروزرسانی")
                val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                val latest = json.optString("tag_name").removePrefix("v")
                val current = BuildConfig.VERSION_NAME
                if (isNewer(latest, current)) UpdateInfo(latest, json.optString("html_url")) else null
            } finally { c.disconnect() }
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
