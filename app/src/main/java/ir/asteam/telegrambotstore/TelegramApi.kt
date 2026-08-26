// این فایل تمام ارتباطات شبکه‌ای Android با Telegram و Backend چندرباته App BotStore را مدیریت می‌کند.
package ir.asteam.telegrambotstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// اطلاعات پایه رباتی که توسط Telegram و Backend تایید شده است نگهداری می‌شود.
data class TelegramBotInfo(
    val id: Long,
    val username: String,
    val firstName: String
)

// نتیجه همگام‌سازی Catalog برای گزارش تعداد آیتم‌های منتقل‌شده نگهداری می‌شود.
data class TelegramCatalogSyncResult(
    val categoriesSynced: Int,
    val productsSynced: Int
)

// توابع ارتباط با Telegram Bot API و Supabase Edge Functions در این object متمرکز شده‌اند.
object TelegramApi {
    // آدرس عمومی پروژه Supabase محرمانه نیست؛ کلید مدیریتی فقط داخل Edge Function نگهداری می‌شود.
    private const val BACKEND_BASE = "https://spncmjuvnvfkrahjnyjm.supabase.co/functions/v1"

    // Endpoint ثبت ربات Token را اعتبارسنجی می‌کند و Webhook واقعی همان Bot را فعال می‌کند.
    private const val REGISTER_ENDPOINT = "$BACKEND_BASE/botstore-register"

    // Endpoint Sync محصولات و دسته‌بندی‌های محلی را به فروشگاه واقعی Telegram منتقل می‌کند.
    private const val SYNC_ENDPOINT = "$BACKEND_BASE/botstore-sync"

    // Endpoint حذف اتصال، Webhook تلگرام و رکورد Backend همان Bot را پاک می‌کند.
    private const val DISCONNECT_ENDPOINT = "$BACKEND_BASE/botstore-disconnect"

    // نام قدیمی این تابع برای سازگاری UI حفظ شده است، اما حالا اتصال واقعی Backend و setWebhook را انجام می‌دهد.
    suspend fun validateToken(token: String): Result<TelegramBotInfo> = connectBot(token)

    // این تابع اتصال واقعی را انجام می‌دهد: Token را به Backend می‌فرستد، getMe سروری انجام می‌شود و Webhook فعال می‌شود.
    suspend fun connectBot(token: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        runCatching {
            // فرمت Token قبل از ارسال کنترل می‌شود تا درخواست بیهوده ایجاد نشود.
            require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست" }

            // Payload فقط شامل Token همان Bot است؛ هیچ Service Key یا Secret سرور در APK وجود ندارد.
            val payload = JSONObject().put("token", token)

            // Backend علاوه بر اعتبارسنجی Token، Webhook چندرباته را نیز ثبت می‌کند.
            val response = postJson(REGISTER_ENDPOINT, payload)

            // خطای Backend به پیام قابل فهم برای کاربر تبدیل می‌شود.
            if (!response.optBoolean("ok")) {
                error(response.optString("message", "فعال‌سازی Webhook ربات ناموفق بود"))
            }

            // اطلاعات تاییدشده Bot از پاسخ Backend خوانده می‌شود.
            val bot = response.getJSONObject("bot")
            TelegramBotInfo(
                id = bot.getLong("id"),
                username = bot.optString("username"),
                firstName = bot.optString("first_name")
            )
        }
    }

    // این تابع اتصال واقعی Bot را از Backend حذف می‌کند تا حذف داخل APK، Bot را روی سرور فعال باقی نگذارد.
    suspend fun disconnectBot(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            // Token خالی یا فرمت نامعتبر به Backend فرستاده نمی‌شود.
            require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست" }

            // Token همان Bot برای اثبات کنترل و lookup رکورد server-only ارسال می‌شود.
            val payload = JSONObject().put("token", token)

            // Endpoint ابتدا deleteWebhook و سپس حذف رکورد Backend را انجام می‌دهد.
            val response = postJson(DISCONNECT_ENDPOINT, payload)

            // خطای Backend به Result شکست تبدیل می‌شود تا Provider بتواند آن را Log کند.
            if (!response.optBoolean("ok")) {
                error(response.optString("message", "حذف اتصال ربات از Backend ناموفق بود"))
            }

            // disconnected=true یعنی رکورد فعلی حذف شد؛ already_removed نیز عملیات موفق محسوب می‌شود.
            response.optBoolean("disconnected") || response.optBoolean("already_removed")
        }
    }

    // این تابع Catalog فعلی اپ را برای Bot انتخاب‌شده به Backend ارسال می‌کند.
    suspend fun syncCatalog(
        token: String,
        categories: List<StoreCategory>,
        products: List<StoreProduct>
    ): Result<TelegramCatalogSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            // Token خالی هرگز به Backend ارسال نمی‌شود.
            require(token.isNotBlank()) { "توکن ربات ثبت نشده است" }

            // دسته‌بندی‌های Android به آرایه JSON تبدیل می‌شوند.
            val categoryArray = JSONArray().apply {
                categories.forEach { category ->
                    put(
                        JSONObject()
                            .put("title", category.title)
                            .put("emoji", category.emoji)
                    )
                }
            }

            // محصولات Android همراه قیمت، دسته و توضیح به آرایه JSON تبدیل می‌شوند.
            val productArray = JSONArray().apply {
                products.filter { it.active }.forEach { product ->
                    put(
                        JSONObject()
                            .put("title", product.title)
                            .put("price", product.price)
                            .put("category", product.category)
                            .put("description", product.description)
                    )
                }
            }

            // Token برای اثبات مالکیت Bot و Catalog برای جایگزینی وضعیت سرور ارسال می‌شوند.
            val payload = JSONObject()
                .put("token", token)
                .put("categories", categoryArray)
                .put("products", productArray)

            // Endpoint همگام‌سازی فراخوانی می‌شود.
            val response = postJson(SYNC_ENDPOINT, payload)

            // در صورت خطا، متن Backend به UI قابل انتقال است.
            if (!response.optBoolean("ok")) {
                error(response.optString("message", "همگام‌سازی فروشگاه ناموفق بود"))
            }

            // تعداد آیتم‌های ثبت‌شده برای گزارش و تست برگردانده می‌شود.
            TelegramCatalogSyncResult(
                categoriesSynced = response.optInt("categories_synced"),
                productsSynced = response.optInt("products_synced")
            )
        }
    }

    // درخواست POST JSON برای Endpointهای Backend با timeout و مدیریت خطای مشترک اجرا می‌شود.
    private fun postJson(url: String, payload: JSONObject): JSONObject {
        // اتصال HTTP ساخته می‌شود.
        val connection = URL(url).openConnection() as HttpURLConnection
        // متد POST مشخص می‌شود.
        connection.requestMethod = "POST"
        // ارسال body فعال می‌شود.
        connection.doOutput = true
        // نوع محتوای درخواست JSON است.
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        // پاسخ JSON درخواست می‌شود.
        connection.setRequestProperty("Accept", "application/json")
        // timeout اتصال تنظیم می‌شود.
        connection.connectTimeout = 12_000
        // timeout خواندن تنظیم می‌شود.
        connection.readTimeout = 12_000

        return try {
            // Payload با UTF-8 داخل body نوشته می‌شود.
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            // status code دریافت می‌شود.
            val status = connection.responseCode
            // بر اساس status، stream درست خوانده می‌شود.
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            // در نبود JSON قابل پردازش، خطای مناسب ساخته می‌شود.
            if (text.isBlank()) error("پاسخ خالی از Backend دریافت شد")

            // JSON Backend حتی در status خطا خوانده می‌شود تا پیام دقیق به کاربر برسد.
            val json = JSONObject(text)

            // اگر status HTTP ناموفق و Backend پیام مشخص ندارد، status در خطا قرار می‌گیرد.
            if (status !in 200..299 && !json.has("message")) {
                error("خطای Backend با کد $status")
            }

            // پاسخ parsed برگردانده می‌شود.
            json
        } finally {
            // اتصال همیشه آزاد می‌شود.
            connection.disconnect()
        }
    }
}
