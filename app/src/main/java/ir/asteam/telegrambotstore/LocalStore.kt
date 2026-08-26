// این فایل مسئول ذخیره‌ی محلی حساب کاربر، ربات‌ها، محصولات، دسته‌بندی‌ها و تنظیمات برنامه است.
package ir.asteam.telegrambotstore

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// مدل اطلاعات محصول فروشگاه ربات است.
data class StoreProduct(val id: String = UUID.randomUUID().toString(), val title: String, val price: Long, val category: String, val description: String = "", val active: Boolean = true)
// مدل اطلاعات دسته‌بندی فروشگاه است.
data class StoreCategory(val id: String = UUID.randomUUID().toString(), val title: String, val emoji: String = "🛍️")
// پلتفرم‌های قابل پشتیبانی تعریف می‌شوند.
enum class BotPlatform(val faName: String) { TELEGRAM("تلگرام"), WHATSAPP("واتساپ"), RUBIKA("روبیکا"), BALE("بله") }
// اطلاعات هر ربات و اشتراک مستقل آن نگهداری می‌شود.
data class ConnectedBot(val id: String = UUID.randomUUID().toString(), val platform: BotPlatform, val name: String, val username: String = "", val token: String = "", val createdAt: Long = System.currentTimeMillis(), val expiresAt: Long = 0L, val planLabel: String = "", val active: Boolean = true)

// مدیریت ذخیره‌سازی محلی برنامه انجام می‌شود.
class LocalStore(context: Context) {
    // نام SharedPreferences قدیمی عمداً حفظ شده تا داده کاربران قبلی باقی بماند.
    private val prefs = context.getSharedPreferences("telegram_bot_store", Context.MODE_PRIVATE)
    // وضعیت تکمیل onboarding ذخیره می‌شود.
    var hasCompletedOnboarding: Boolean get() = prefs.getBoolean("onboarding_done_v12", false); set(value) = prefs.edit().putBoolean("onboarding_done_v12", value).apply()
    // وضعیت ورود کاربر ذخیره می‌شود.
    var isLoggedIn: Boolean get() = prefs.getBoolean("user_logged_in", false); set(value) = prefs.edit().putBoolean("user_logged_in", value).apply()
    // وضعیت مدیر ذخیره می‌شود.
    var isAdmin: Boolean get() = prefs.getBoolean("user_is_admin", false); set(value) = prefs.edit().putBoolean("user_is_admin", value).apply()
    // نام کاربر ذخیره می‌شود.
    var userName: String get() = prefs.getString("user_name", "") ?: ""; set(value) = prefs.edit().putString("user_name", value).apply()
    // شماره موبایل کاربر ذخیره می‌شود.
    var userPhone: String get() = prefs.getString("user_phone", "") ?: ""; set(value) = prefs.edit().putString("user_phone", value).apply()
    // هش رمز کاربر عادی ذخیره می‌شود.
    private var userPasswordHash: String get() = prefs.getString("user_password_hash", "") ?: ""; set(value) = prefs.edit().putString("user_password_hash", value).apply()
    // توکن legacy تلگرام برای مهاجرت نسخه‌های قبلی حفظ می‌شود.
    var token: String get() = prefs.getString("bot_token", "") ?: ""; set(value) = prefs.edit().putString("bot_token", value).apply()
    // username legacy تلگرام حفظ می‌شود.
    var botUsername: String get() = prefs.getString("bot_username", "") ?: ""; set(value) = prefs.edit().putString("bot_username", value).apply()
    // نام legacy ربات تلگرام حفظ می‌شود.
    var botName: String get() = prefs.getString("bot_name", "فروشگاه من") ?: "فروشگاه من"; set(value) = prefs.edit().putString("bot_name", value).apply()
    // وضعیت اعلان‌ها ذخیره می‌شود.
    var notificationsEnabled: Boolean get() = prefs.getBoolean("notifications", true); set(value) = prefs.edit().putBoolean("notifications", value).apply()

    // ثبت‌نام محلی کاربر انجام می‌شود.
    fun register(name: String, phone: String, password: String): Boolean {
        // اعتبار اولیه ورودی‌ها بررسی می‌شود.
        if (name.trim().length < 2 || phone.trim().length < 5 || password.length < 4) return false
        // اطلاعات معتبر ذخیره می‌شوند.
        userName = name.trim(); userPhone = phone.trim(); userPasswordHash = sha256(password); isLoggedIn = true; isAdmin = false; hasCompletedOnboarding = true
        // موفقیت اعلام می‌شود.
        return true
    }

    // اطلاعات حساب کاربر عادی ویرایش می‌شود.
    fun updateProfile(name: String, phone: String): Boolean {
        // حساب مدیر داخلی ویرایش‌پذیر نیست.
        if (isAdmin) return false
        // اعتبار حداقلی بررسی می‌شود.
        if (name.trim().length < 2 || phone.trim().length < 5) return false
        // اطلاعات جدید ذخیره می‌شوند.
        userName = name.trim(); userPhone = phone.trim()
        // موفقیت اعلام می‌شود.
        return true
    }

    // ورود حساب کاربر یا مدیر بررسی می‌شود.
    fun login(identifier: String, password: String): Boolean {
        // شناسه پاک‌سازی می‌شود.
        val normalized = identifier.trim()
        // دسترسی مدیر با کلید مشتق‌شده بررسی می‌شود.
        if (normalized.equals(ADMIN_USERNAME, ignoreCase = true) && verifyAdminPassword(password)) { userName = "Administrator"; userPhone = "دسترسی مدیر"; isLoggedIn = true; isAdmin = true; hasCompletedOnboarding = true; return true }
        // ورود کاربر عادی با هش رمز بررسی می‌شود.
        val validUser = normalized == userPhone && userPasswordHash.isNotBlank() && sha256(password) == userPasswordHash
        // در صورت موفقیت نشست فعال می‌شود.
        if (validUser) { isLoggedIn = true; isAdmin = false; hasCompletedOnboarding = true }
        // نتیجه برگردانده می‌شود.
        return validUser
    }

    // کاربر وارد حالت مهمان می‌شود.
    fun skipAuth() { hasCompletedOnboarding = true; isLoggedIn = false; isAdmin = false }
    // نشست فعلی بسته می‌شود.
    fun logout() { isLoggedIn = false; isAdmin = false }

    // ربات‌های ذخیره‌شده بازیابی می‌شوند و در صورت نیاز داده نسخه قدیمی مهاجرت می‌شود.
    fun loadBots(): List<ConnectedBot> {
        // JSON ساختار جدید خوانده می‌شود.
        val stored = runCatching { val array = JSONArray(prefs.getString("connected_bots_v12", "[]")); buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); add(ConnectedBot(id = o.optString("id", UUID.randomUUID().toString()), platform = runCatching { BotPlatform.valueOf(o.optString("platform")) }.getOrDefault(BotPlatform.TELEGRAM), name = o.optString("name", "ربات من"), username = o.optString("username"), token = o.optString("token"), createdAt = o.optLong("createdAt", System.currentTimeMillis()), expiresAt = o.optLong("expiresAt", 0L), planLabel = o.optString("planLabel"), active = o.optBoolean("active", true))) } } }.getOrDefault(emptyList())
        // اگر داده جدید وجود داشته باشد همان داده برگردانده می‌شود.
        if (stored.isNotEmpty()) return stored
        // در غیر این صورت ربات تلگرام نسخه قدیمی به ساختار جدید تبدیل می‌شود.
        if (token.isNotBlank()) { val migrated = ConnectedBot(platform = BotPlatform.TELEGRAM, name = botName.ifBlank { "فروشگاه من" }, username = botUsername, token = token, planLabel = "انتقال از نسخه قبلی", expiresAt = 0L); saveBots(listOf(migrated)); return listOf(migrated) }
        // در نبود داده لیست خالی برگردانده می‌شود.
        return emptyList()
    }

    // ربات‌ها به JSON تبدیل و ذخیره می‌شوند.
    fun saveBots(items: List<ConnectedBot>) { val array = JSONArray(); items.forEach { bot -> array.put(JSONObject().apply { put("id", bot.id); put("platform", bot.platform.name); put("name", bot.name); put("username", bot.username); put("token", bot.token); put("createdAt", bot.createdAt); put("expiresAt", bot.expiresAt); put("planLabel", bot.planLabel); put("active", bot.active) }) }; prefs.edit().putString("connected_bots_v12", array.toString()).apply() }
    // یک ربات مشخص حذف می‌شود.
    fun clearBot(botId: String) { val bots = loadBots().filterNot { it.id == botId }; saveBots(bots); if (bots.none { it.platform == BotPlatform.TELEGRAM }) prefs.edit().remove("bot_token").remove("bot_username").apply() }
    // overload قدیمی برای سازگاری سورس حفظ شده است.
    fun clearBot() { prefs.edit().remove("bot_token").remove("bot_username").apply() }

    // دسته‌بندی‌ها بازیابی می‌شوند.
    fun loadCategories(): List<StoreCategory> = runCatching { val array = JSONArray(prefs.getString("categories", "[]")); buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); add(StoreCategory(o.getString("id"), o.getString("title"), o.optString("emoji", "🛍️"))) } } }.getOrDefault(emptyList())
    // دسته‌بندی‌ها ذخیره می‌شوند.
    fun saveCategories(items: List<StoreCategory>) { val array = JSONArray(); items.forEach { item -> array.put(JSONObject().apply { put("id", item.id); put("title", item.title); put("emoji", item.emoji) }) }; prefs.edit().putString("categories", array.toString()).apply() }
    // محصولات بازیابی می‌شوند.
    fun loadProducts(): List<StoreProduct> = runCatching { val array = JSONArray(prefs.getString("products", "[]")); buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); add(StoreProduct(id = o.getString("id"), title = o.getString("title"), price = o.getLong("price"), category = o.optString("category"), description = o.optString("description"), active = o.optBoolean("active", true))) } } }.getOrDefault(emptyList())
    // محصولات ذخیره می‌شوند.
    fun saveProducts(items: List<StoreProduct>) { val array = JSONArray(); items.forEach { item -> array.put(JSONObject().apply { put("id", item.id); put("title", item.title); put("price", item.price); put("category", item.category); put("description", item.description); put("active", item.active) }) }; prefs.edit().putString("products", array.toString()).apply() }

    // مقادیر ثابت و توابع امنیتی در companion object نگهداری می‌شوند.
    companion object {
        // نام کاربری مدیر داخلی ثابت است.
        private const val ADMIN_USERNAME = "administrator"
        // salt برای PBKDF2 ثابت می‌ماند تا دسترسی مدیر نسخه قبلی خراب نشود.
        private const val ADMIN_SALT = "AS_TEAM_TELEGRAMBOTSTORE_ADMIN_V1_2"
        // کلید مشتق‌شده رمز مدیر به‌جای رمز خام ذخیره شده است.
        private const val ADMIN_DERIVED_KEY = "d54fef0b21d4ac5620174174d5903f996d1cb0563c8cf3b17dc552491f6172a7"
        // زمان میلی‌ثانیه‌ای به تاریخ فارسی تبدیل می‌شود.
        fun formatDate(time: Long): String { if (time <= 0L) return "نامحدود"; return SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR")).format(Date(time)) }
        // رشته با SHA-256 هش می‌شود.
        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
        // رمز مدیر با PBKDF2 بررسی می‌شود.
        private fun verifyAdminPassword(password: String): Boolean = runCatching { val spec = PBEKeySpec(password.toCharArray(), ADMIN_SALT.toByteArray(), 120_000, 256); val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded; key.joinToString("") { "%02x".format(it) } == ADMIN_DERIVED_KEY }.getOrDefault(false)
    }
}
