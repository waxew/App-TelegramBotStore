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

// آمار خلاصه فروشگاه برای هدر صفحات مدیریتی صاحب Bot استفاده می‌شود.
data class BotStoreOverview(
    val customers: Int,
    val orders: Int,
    val newOrders: Int
)

// مدل سفارش Backend برای صفحه مدیریت سفارش‌های فروشنده است.
data class BotStoreOrder(
    val id: Long,
    val orderCode: String,
    val telegramUserId: Long,
    val status: String,
    val totalPrice: Long,
    val createdAt: String,
    val customerName: String,
    val customerUsername: String
)

// مدل مشتری Backend برای صفحه کاربران فروشگاه است.
data class BotStoreCustomer(
    val id: Long,
    val telegramUserId: Long,
    val firstName: String,
    val username: String,
    val blocked: Boolean,
    val createdAt: String
)

// تنظیمات عمومی هر فروشگاه مستقل از نام فنی BotFather نگهداری می‌شوند.
data class BotStoreSettings(
    val storeName: String,
    val welcomeText: String,
    val supportText: String,
    val aboutText: String,
    val botUsername: String
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

    // Endpoint مدیریت فروشنده سفارش‌ها، مشتری‌ها و تنظیمات عمومی را فقط برای همان Token برمی‌گرداند.
    private const val MANAGE_ENDPOINT = "$BACKEND_BASE/botstore-manage"

    // نام قدیمی این تابع برای سازگاری UI حفظ شده است، اما حالا اتصال واقعی Backend و setWebhook را انجام می‌دهد.
    suspend fun validateToken(token: String): Result<TelegramBotInfo> = connectBot(token)

    // این تابع اتصال واقعی را انجام می‌دهد: Token را به Backend می‌فرستد، getMe سروری انجام می‌شود و Webhook فعال می‌شود.
    suspend fun connectBot(token: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        runCatching {
            requireValidToken(token)
            val payload = JSONObject().put("token", token)
            val response = postJson(REGISTER_ENDPOINT, payload)
            ensureBackendOk(response, "فعال‌سازی Webhook ربات ناموفق بود")

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
            requireValidToken(token)
            val payload = JSONObject().put("token", token)
            val response = postJson(DISCONNECT_ENDPOINT, payload)
            ensureBackendOk(response, "حذف اتصال ربات از Backend ناموفق بود")
            response.optBoolean("disconnected") || response.optBoolean("already_removed")
        }
    }

    // Catalog فعلی اپ را با UUIDهای پایدار Android برای همان Bot به Backend ارسال می‌کند.
    suspend fun syncCatalog(
        token: String,
        categories: List<StoreCategory>,
        products: List<StoreProduct>
    ): Result<TelegramCatalogSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            requireValidToken(token)

            // UUID Category منبع اصلی ارتباط Product است؛ نگاشت عنوان فقط برای مهاجرت داده‌های قدیمی نگه داشته می‌شود.
            val categoryById = categories.associateBy { category -> category.id }
            val categoryByTitle = categories.associateBy { category -> category.title.trim().lowercase() }

            // UUID هر Category به‌عنوان source_id ارسال می‌شود تا PK Backend در Syncهای بعدی تغییر نکند.
            val categoryArray = JSONArray().apply {
                categories.forEach { category ->
                    put(
                        JSONObject()
                            .put("id", category.id)
                            .put("source_id", category.id)
                            .put("title", category.title)
                            .put("emoji", category.emoji)
                    )
                }
            }

            // UUID Product و UUID Category همراه اطلاعات نمایشی ارسال می‌شوند؛ Product غیرفعال نیز برای حفظ هویت Sync می‌شود.
            val productArray = JSONArray().apply {
                products.forEach { product ->
                    // ابتدا categoryId پایدار مصرف می‌شود؛ title قدیمی فقط fallback برای کاربران نسخه‌های قبل است.
                    val category = categoryById[product.categoryId]
                        ?: categoryByTitle[product.category.trim().lowercase()]
                    put(
                        JSONObject()
                            .put("id", product.id)
                            .put("source_id", product.id)
                            .put("title", product.title)
                            .put("price", product.price)
                            .put("category", category?.title.orEmpty())
                            .put("category_source_id", category?.id.orEmpty())
                            .put("description", product.description)
                            .put("active", product.active)
                            .put("stock_enabled", product.stockEnabled)
                            .put("stock_quantity", product.stockQuantity.coerceAtLeast(0))
                            .put("stock_version", product.stockVersion)
                    )
                }
            }

            val payload = JSONObject()
                .put("token", token)
                .put("categories", categoryArray)
                .put("products", productArray)

            val response = postJson(SYNC_ENDPOINT, payload)
            ensureBackendOk(response, "همگام‌سازی فروشگاه ناموفق بود")

            TelegramCatalogSyncResult(
                categoriesSynced = response.optInt("categories_synced"),
                productsSynced = response.optInt("products_synced")
            )
        }
    }

    // تنظیمات عمومی همین Bot از Backend خوانده می‌شوند تا صفحه مدیریت عمومی با وضعیت واقعی سرور باز شود.
    suspend fun fetchStoreSettings(token: String): Result<BotStoreSettings> = withContext(Dispatchers.IO) {
        runCatching {
            val response = manageRequest(token, "get_settings")
            val settings = response.getJSONObject("settings")
            val bot = response.optJSONObject("bot")
            BotStoreSettings(
                storeName = settings.optString("store_name"),
                welcomeText = settings.optString("welcome_text"),
                supportText = settings.optString("support_text"),
                aboutText = settings.optString("about_text"),
                botUsername = bot?.optString("username").orEmpty()
            )
        }
    }

    // متن‌های قابل شخصی‌سازی همان فروشگاه در Backend ذخیره می‌شوند و بلافاصله توسط Webhook مصرف خواهند شد.
    suspend fun updateStoreSettings(token: String, settings: BotStoreSettings): Result<BotStoreSettings> = withContext(Dispatchers.IO) {
        runCatching {
            val extra = JSONObject()
                .put("store_name", settings.storeName)
                .put("welcome_text", settings.welcomeText)
                .put("support_text", settings.supportText)
                .put("about_text", settings.aboutText)

            val response = manageRequest(token, "set_settings", extra)
            val saved = response.getJSONObject("settings")
            BotStoreSettings(
                storeName = saved.optString("store_name"),
                welcomeText = saved.optString("welcome_text"),
                supportText = saved.optString("support_text"),
                aboutText = saved.optString("about_text"),
                botUsername = settings.botUsername
            )
        }
    }

    // لینک مستقیم Telegram برای Product با UUID پایدار ساخته می‌شود؛ این لینک با تغییر نام/قیمت Product ثابت می‌ماند.
    fun productDeepLink(botUsername: String, productSourceId: String): String? {
        val username = botUsername.trim().removePrefix("@")
        val sourceId = productSourceId.trim()
        if (!username.matches(Regex("^[A-Za-z0-9_]{5,32}$"))) return null
        if (!sourceId.matches(Regex("^[A-Za-z0-9_-]{1,60}$"))) return null
        return "https://t.me/$username?start=p_$sourceId"
    }

    // آمار خلاصه مشتری‌ها و سفارش‌های همان Bot از Backend خوانده می‌شود.
    suspend fun fetchOverview(token: String): Result<BotStoreOverview> = withContext(Dispatchers.IO) {
        runCatching {
            val response = manageRequest(token, "overview")
            val overview = response.getJSONObject("overview")
            BotStoreOverview(
                customers = overview.optInt("customers"),
                orders = overview.optInt("orders"),
                newOrders = overview.optInt("new_orders")
            )
        }
    }

    // آخرین سفارش‌های همان Bot برای پنل فروشنده دریافت می‌شوند.
    suspend fun fetchOrders(token: String, limit: Int = 50): Result<List<BotStoreOrder>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = manageRequest(token, "orders", JSONObject().put("limit", limit.coerceIn(1, 100)))
            val array = response.optJSONArray("orders") ?: JSONArray()

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val customer = item.optJSONObject("customer")
                    add(
                        BotStoreOrder(
                            id = item.getLong("id"),
                            orderCode = item.optString("order_code"),
                            telegramUserId = item.optLong("telegram_user_id"),
                            status = item.optString("status", "new"),
                            totalPrice = item.optLong("total_price"),
                            createdAt = item.optString("created_at"),
                            customerName = customer?.optString("first_name").orEmpty(),
                            customerUsername = customer?.optString("username").orEmpty()
                        )
                    )
                }
            }
        }
    }

    // مشتری‌های همان فروشگاه برای CRM اولیه دریافت می‌شوند.
    suspend fun fetchCustomers(token: String, limit: Int = 100): Result<List<BotStoreCustomer>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = manageRequest(token, "customers", JSONObject().put("limit", limit.coerceIn(1, 100)))
            val array = response.optJSONArray("customers") ?: JSONArray()

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BotStoreCustomer(
                            id = item.getLong("id"),
                            telegramUserId = item.optLong("telegram_user_id"),
                            firstName = item.optString("first_name"),
                            username = item.optString("username"),
                            blocked = item.optBoolean("blocked"),
                            createdAt = item.optString("created_at")
                        )
                    )
                }
            }
        }
    }

    // وضعیت یک سفارش فقط از allow-list Backend و برای همان Bot تغییر می‌کند.
    suspend fun setOrderStatus(token: String, orderId: Long, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val extra = JSONObject()
                .put("order_id", orderId)
                .put("status", status)
            val response = manageRequest(token, "set_order_status", extra)
            response.optJSONObject("order") != null
        }
    }

    // Block یا Unblock یک مشتری فقط در همان فروشگاه اعمال می‌شود.
    suspend fun setCustomerBlocked(token: String, customerId: Long, blocked: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val extra = JSONObject()
                .put("customer_id", customerId)
                .put("blocked", blocked)
            val response = manageRequest(token, "set_customer_blocked", extra)
            response.optJSONObject("customer") != null
        }
    }

    // درخواست مدیریتی مشترک Token و action را همراه فیلدهای اضافه به Endpoint فروشنده ارسال می‌کند.
    private fun manageRequest(token: String, action: String, extra: JSONObject? = null): JSONObject {
        requireValidToken(token)

        val payload = JSONObject()
            .put("token", token)
            .put("action", action)

        if (extra != null) {
            val keys = extra.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "token" && key != "action") payload.put(key, extra.get(key))
            }
        }

        val response = postJson(MANAGE_ENDPOINT, payload)
        ensureBackendOk(response, "مدیریت فروشگاه ناموفق بود")
        return response
    }

    // فرمت Token پیش از هر درخواست Backend بررسی می‌شود.
    private fun requireValidToken(token: String) {
        require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست" }
    }

    // خطای JSON استاندارد Backend به Exception قابل مدیریت در Result تبدیل می‌شود.
    private fun ensureBackendOk(response: JSONObject, fallback: String) {
        if (!response.optBoolean("ok")) {
            error(response.optString("message", fallback))
        }
    }

    // درخواست POST JSON برای Endpointهای Backend با timeout و مدیریت خطای مشترک اجرا می‌شود.
    private fun postJson(url: String, payload: JSONObject): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
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

            if (text.isBlank()) error("پاسخ خالی از Backend دریافت شد")

            val json = JSONObject(text)
            if (status !in 200..299 && !json.has("message") && !json.has("error")) {
                error("خطای Backend با کد $status")
            }
            json
        } finally {
            connection.disconnect()
        }
    }
}
