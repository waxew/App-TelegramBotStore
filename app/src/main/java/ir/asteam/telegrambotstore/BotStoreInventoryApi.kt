// این فایل API فقط‌خواندنی موجودی واقعی Productهای یک Bot را برای Android نگه می‌دارد.
// موجودی Backend منبع واقعی فروش است؛ Android آن را با UUID محلی Product تطبیق می‌دهد.
package ir.asteam.telegrambotstore

// Coroutines درخواست شبکه را خارج از Thread اصلی اجرا می‌کنند.
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON پاسخ Supabase بدون وابستگی اضافی parse می‌شود.
import org.json.JSONArray
import org.json.JSONObject
// HttpURLConnection برای POST به Edge Function استفاده می‌شود.
import java.net.HttpURLConnection
import java.net.URL

// وضعیت موجودی یک Product بر اساس source_id پایدار Android مدل می‌شود.
data class BotStoreInventoryItem(
    val sourceId: String,
    val stockEnabled: Boolean,
    val stockQuantity: Int
)

// API موجودی فقط دریافت وضعیت فعلی Backend را انجام می‌دهد؛ تغییر Stock از مسیر Catalog Sync نسخه‌دار انجام می‌شود.
object BotStoreInventoryApi {
    // آدرس Function عمومی است اما هیچ service_role یا Secret مدیریتی داخل APK قرار ندارد.
    private const val ENDPOINT = "https://spncmjuvnvfkrahjnyjm.supabase.co/functions/v1/botstore-inventory"

    // فهرست موجودی Productهای همان Bot دریافت و با source_id به Map تبدیل می‌شود.
    suspend fun fetch(token: String): Result<Map<String, BotStoreInventoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست." }

            val payload = JSONObject().put("token", token)
            val response = postJson(payload)
            val array = response.optJSONArray("inventory") ?: JSONArray()

            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val sourceId = item.optString("source_id").trim()
                    if (sourceId.isBlank()) continue
                    put(
                        sourceId,
                        BotStoreInventoryItem(
                            sourceId = sourceId,
                            stockEnabled = item.optBoolean("stock_enabled"),
                            stockQuantity = item.optInt("stock_quantity").coerceAtLeast(0)
                        )
                    )
                }
            }
        }
    }

    // درخواست POST استاندارد به Function موجودی اجرا می‌شود.
    private fun postJson(payload: JSONObject): JSONObject {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000

        return try {
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
                error(json.optString("message", "دریافت موجودی ناموفق بود."))
            }
            json
        } finally {
            connection.disconnect()
        }
    }
}
