// این فایل اعتبارسنجی واقعی توکن تلگرام را از طریق Bot API انجام می‌دهد.
package ir.asteam.telegrambotstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// اطلاعات پایه ربات تاییدشده نگهداری می‌شود.
data class TelegramBotInfo(val id: Long, val username: String, val firstName: String)

// توابع ارتباط با Telegram Bot API در این object قرار دارند.
object TelegramApi {
    // توکن در Dispatcher مربوط به I/O بررسی می‌شود.
    suspend fun validateToken(token: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        // تمام خطاها در Result بسته‌بندی می‌شوند تا UI کرش نکند.
        runCatching {
            // ساختار اولیه توکن قبل از درخواست شبکه بررسی می‌شود.
            require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست" }
            // اتصال getMe ساخته می‌شود.
            val connection = URL("https://api.telegram.org/bot$token/getMe").openConnection() as HttpURLConnection
            // متد GET استفاده می‌شود.
            connection.requestMethod = "GET"
            // timeout اتصال تنظیم می‌شود.
            connection.connectTimeout = 10_000
            // timeout خواندن تنظیم می‌شود.
            connection.readTimeout = 10_000
            // پاسخ خوانده و در پایان اتصال بسته می‌شود.
            try {
                // stream موفق یا خطا بر اساس status code انتخاب می‌شود.
                val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
                // متن پاسخ به JSON تبدیل می‌شود.
                val json = JSONObject(body)
                // پاسخ ناموفق تلگرام به خطا تبدیل می‌شود.
                if (!json.optBoolean("ok")) error(json.optString("description", "توکن توسط تلگرام تایید نشد"))
                // شیء result استخراج می‌شود.
                val result = json.getJSONObject("result")
                // مدل اطلاعات ربات ساخته می‌شود.
                TelegramBotInfo(id = result.getLong("id"), username = result.optString("username"), firstName = result.optString("first_name"))
            } finally {
                // connection همیشه بسته می‌شود.
                connection.disconnect()
            }
        }
    }
}
