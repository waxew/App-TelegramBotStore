// این فایل API اختصاصی ارسال همگانی App BotStore را نگه می‌دارد.
// هیچ service_role یا کلید دیتابیس داخل APK قرار نمی‌گیرد؛ مالکیت در MVP با Token همان Bot بررسی می‌شود.
package ir.asteam.telegrambotstore

// Coroutines عملیات شبکه‌ای را خارج از Thread اصلی اجرا می‌کنند.
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSONObject/JSONArray پاسخ‌های Backend را بدون وابستگی اضافی parse می‌کنند.
import org.json.JSONArray
import org.json.JSONObject
// HttpURLConnection برای POST امن به Supabase Edge Function استفاده می‌شود.
import java.net.HttpURLConnection
import java.net.URL

// وضعیت یک ارسال همگانی برای تاریخچه و Progress صفحه Android نگهداری می‌شود.
data class BotStoreBroadcast(
    val id: Long,
    val messageText: String,
    val status: String,
    val totalRecipientCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val createdAt: String,
    val startedAt: String,
    val completedAt: String
) {
    // تعداد گیرنده‌های پردازش‌شده از جمع موفق و ناموفق محاسبه می‌شود.
    val processedCount: Int get() = sentCount + failedCount
    // Progress در بازه صفر تا یک برای LinearProgressIndicator محاسبه می‌شود.
    val progress: Float
        get() = if (totalRecipientCount <= 0) 1f
        else (processedCount.toFloat() / totalRecipientCount.toFloat()).coerceIn(0f, 1f)
    // وضعیت‌های نهایی دیگر نیاز به Resume ندارند.
    val isDone: Boolean get() = status in setOf("completed", "partial", "failed")
}

// نتیجه هر Batch علاوه بر وضعیت Broadcast، تعداد پردازش همان درخواست و پایان صف را مشخص می‌کند.
data class BotStoreBroadcastProcessResult(
    val broadcast: BotStoreBroadcast,
    val processed: Int,
    val pending: Int,
    val done: Boolean
)

// تمام فراخوانی‌های ارسال همگانی در این object متمرکز شده‌اند.
object BotStoreBroadcastApi {
    // Endpoint عمومی Function محرمانه نیست؛ Secret مدیریتی فقط سمت Supabase باقی می‌ماند.
    private const val ENDPOINT = "https://spncmjuvnvfkrahjnyjm.supabase.co/functions/v1/botstore-broadcast"

    // آخرین Broadcastهای همان Bot برای تاریخچه و Resume دریافت می‌شوند.
    suspend fun list(token: String, limit: Int = 20): Result<List<BotStoreBroadcast>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request(
                token = token,
                action = "list",
                extra = JSONObject().put("limit", limit.coerceIn(1, 50))
            )
            val array = response.optJSONArray("broadcasts") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    add(parseBroadcast(array.getJSONObject(index)))
                }
            }
        }
    }

    // ایجاد صف فقط Snapshot گیرنده‌ها را می‌سازد و هیچ پیام Telegram ارسال نمی‌کند.
    suspend fun create(token: String, message: String): Result<BotStoreBroadcast> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanMessage = message.trim()
            require(cleanMessage.isNotEmpty()) { "متن پیام خالی است." }
            require(cleanMessage.length <= 4000) { "متن پیام حداکثر ۴۰۰۰ نویسه می‌تواند باشد." }

            val response = request(
                token = token,
                action = "create",
                extra = JSONObject().put("message", cleanMessage)
            )
            parseBroadcast(response.getJSONObject("broadcast"))
        }
    }

    // یک Batch کوچک از گیرنده‌های pending پردازش می‌شود؛ فراخوانی دوباره از همان صف ادامه می‌دهد.
    suspend fun process(token: String, broadcastId: Long, limit: Int = 20): Result<BotStoreBroadcastProcessResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(broadcastId > 0) { "شناسه ارسال همگانی نامعتبر است." }
            val response = request(
                token = token,
                action = "process",
                extra = JSONObject()
                    .put("broadcast_id", broadcastId)
                    .put("limit", limit.coerceIn(1, 20))
            )
            BotStoreBroadcastProcessResult(
                broadcast = parseBroadcast(response.getJSONObject("broadcast")),
                processed = response.optInt("processed"),
                pending = response.optInt("pending"),
                done = response.optBoolean("done")
            )
        }
    }

    // وضعیت یک Broadcast بدون ارسال پیام خوانده می‌شود.
    suspend fun status(token: String, broadcastId: Long): Result<BotStoreBroadcast> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request(
                token = token,
                action = "status",
                extra = JSONObject().put("broadcast_id", broadcastId)
            )
            parseBroadcast(response.getJSONObject("broadcast"))
        }
    }

    // مدل JSON استاندارد Backend به مدل Kotlin تبدیل می‌شود.
    private fun parseBroadcast(json: JSONObject): BotStoreBroadcast = BotStoreBroadcast(
        id = json.getLong("id"),
        messageText = json.optString("message_text"),
        status = json.optString("status", "queued"),
        totalRecipientCount = json.optInt("total_recipient_count"),
        sentCount = json.optInt("sent_count"),
        failedCount = json.optInt("failed_count"),
        createdAt = json.optString("created_at"),
        startedAt = json.optString("started_at"),
        completedAt = json.optString("completed_at")
    )

    // درخواست مشترک Token/action را با فیلدهای اختیاری به Edge Function می‌فرستد.
    private fun request(token: String, action: String, extra: JSONObject? = null): JSONObject {
        // فرمت Token قبل از شبکه بررسی می‌شود تا مقدار نامعتبر بی‌دلیل به Backend نرود.
        require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست." }

        val payload = JSONObject()
            .put("token", token)
            .put("action", action)

        // فیلدهای اضافه بدون اجازه بازنویسی token/action منتقل می‌شوند.
        if (extra != null) {
            val keys = extra.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "token" && key != "action") payload.put(key, extra.get(key))
            }
        }

        // اتصال HTTP برای POST ساخته می‌شود.
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 12_000
        // Batch ممکن است چند پیام Telegram بفرستد؛ timeout خواندن کمی بیشتر از APIهای معمول است.
        connection.readTimeout = 45_000

        return try {
            // JSON با UTF-8 در body نوشته می‌شود.
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (text.isBlank()) error("پاسخ خالی از Backend دریافت شد.")

            val json = JSONObject(text)
            if (!json.optBoolean("ok")) {
                error(json.optString("message", "عملیات ارسال همگانی ناموفق بود."))
            }
            json
        } finally {
            // اتصال در هر شرایطی آزاد می‌شود.
            connection.disconnect()
        }
    }
}
