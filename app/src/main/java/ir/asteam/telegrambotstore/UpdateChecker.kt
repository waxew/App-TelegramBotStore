// این فایل آخرین نسخه برنامه را از GitHub Releases بررسی می‌کند و برای تغییر نام مخزن fallback دارد.
package ir.asteam.telegrambotstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// اطلاعات نسخه جدید شامل شماره نسخه و لینک دانلود است.
data class UpdateInfo(val latestVersion: String, val downloadUrl: String)

// منطق بررسی بروزرسانی در این object قرار دارد.
object UpdateChecker {
    // نام جدید مخزن بعد از Rename نهایی استفاده می‌شود.
    private const val NEW_REPO = "App-BotStore"
    // نام قدیمی مخزن برای دوره انتقال نگه داشته می‌شود.
    private const val LEGACY_REPO = "App-TelegramBotStore"
    // مالک مخزن GitHub ثابت است.
    private const val OWNER = "waxew"

    // آخرین Release بررسی می‌شود و فقط نسخه جدیدتر برگردانده می‌شود.
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            // ابتدا نام جدید و سپس نام قدیمی بررسی می‌شود تا Rename باعث قطعی نشود.
            val json = fetchLatest(NEW_REPO) ?: fetchLatest(LEGACY_REPO) ?: return@runCatching null
            // tag نسخه از GitHub خوانده می‌شود.
            val latest = json.optString("tag_name").removePrefix("v")
            // نسخه نصب‌شده از BuildConfig خوانده می‌شود.
            val current = BuildConfig.VERSION_NAME
            // اگر نسخه جدیدتر باشد اطلاعات آن برگردانده می‌شود.
            if (isNewer(latest, current)) UpdateInfo(latest, json.optString("html_url")) else null
        }
    }

    // آخرین Release یک مخزن مشخص دریافت می‌شود.
    private fun fetchLatest(repo: String): JSONObject? {
        // اتصال به API رسمی GitHub ساخته می‌شود.
        val connection = URL("https://api.github.com/repos/$OWNER/$repo/releases/latest").openConnection() as HttpURLConnection
        // هدر API تنظیم می‌شود.
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        // timeout اتصال تعیین می‌شود.
        connection.connectTimeout = 8_000
        // timeout خواندن تعیین می‌شود.
        connection.readTimeout = 8_000
        // پاسخ دریافت و در پایان connection بسته می‌شود.
        return try {
            // 404 یعنی این نام مخزن هنوز Release ندارد و fallback باید امتحان شود.
            if (connection.responseCode == 404) return null
            // سایر خطاهای HTTP متوقف می‌شوند.
            if (connection.responseCode !in 200..299) error("خطا در بررسی بروزرسانی")
            // پاسخ JSON ساخته می‌شود.
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            // منابع شبکه آزاد می‌شوند.
            connection.disconnect()
        }
    }

    // نسخه‌های نقطه‌ای با هم مقایسه می‌شوند.
    private fun isNewer(latest: String, current: String): Boolean {
        // نسخه جدید به بخش‌های عددی تبدیل می‌شود.
        val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
        // نسخه فعلی به بخش‌های عددی تبدیل می‌شود.
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        // بخش‌های نسخه به ترتیب مقایسه می‌شوند.
        for (i in 0 until maxOf(a.size, b.size)) { val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }; if (x != y) return x > y }
        // نسخه برابر جدیدتر نیست.
        return false
    }
}
