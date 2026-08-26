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

data class StoreProduct(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val price: Long,
    val category: String,
    val description: String = "",
    val active: Boolean = true
)

data class StoreCategory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "🛍️"
)

enum class BotPlatform(val faName: String) {
    TELEGRAM("تلگرام"),
    WHATSAPP("واتساپ"),
    RUBIKA("روبیکا"),
    BALE("بله")
}

data class ConnectedBot(
    val id: String = UUID.randomUUID().toString(),
    val platform: BotPlatform,
    val name: String,
    val username: String = "",
    val token: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val planLabel: String = "",
    val active: Boolean = true
)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("telegram_bot_store", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("onboarding_done_v12", false)
        set(value) = prefs.edit().putBoolean("onboarding_done_v12", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("user_logged_in", false)
        set(value) = prefs.edit().putBoolean("user_logged_in", value).apply()

    var isAdmin: Boolean
        get() = prefs.getBoolean("user_is_admin", false)
        set(value) = prefs.edit().putBoolean("user_is_admin", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userPhone: String
        get() = prefs.getString("user_phone", "") ?: ""
        set(value) = prefs.edit().putString("user_phone", value).apply()

    private var userPasswordHash: String
        get() = prefs.getString("user_password_hash", "") ?: ""
        set(value) = prefs.edit().putString("user_password_hash", value).apply()

    // Legacy single-bot fields are retained so updates from v1.0 do not lose data.
    var token: String
        get() = prefs.getString("bot_token", "") ?: ""
        set(value) = prefs.edit().putString("bot_token", value).apply()

    var botUsername: String
        get() = prefs.getString("bot_username", "") ?: ""
        set(value) = prefs.edit().putString("bot_username", value).apply()

    var botName: String
        get() = prefs.getString("bot_name", "فروشگاه من") ?: "فروشگاه من"
        set(value) = prefs.edit().putString("bot_name", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    fun register(name: String, phone: String, password: String): Boolean {
        if (name.trim().length < 2 || phone.trim().length < 5 || password.length < 4) return false
        userName = name.trim()
        userPhone = phone.trim()
        userPasswordHash = sha256(password)
        isLoggedIn = true
        isAdmin = false
        hasCompletedOnboarding = true
        return true
    }

    fun login(identifier: String, password: String): Boolean {
        val normalized = identifier.trim()
        if (normalized.equals(ADMIN_USERNAME, ignoreCase = true) && verifyAdminPassword(password)) {
            userName = "Administrator"
            userPhone = "دسترسی مدیر"
            isLoggedIn = true
            isAdmin = true
            hasCompletedOnboarding = true
            return true
        }

        val validUser = normalized == userPhone && userPasswordHash.isNotBlank() && sha256(password) == userPasswordHash
        if (validUser) {
            isLoggedIn = true
            isAdmin = false
            hasCompletedOnboarding = true
        }
        return validUser
    }

    fun skipAuth() {
        hasCompletedOnboarding = true
        isLoggedIn = false
        isAdmin = false
    }

    fun logout() {
        isLoggedIn = false
        isAdmin = false
    }

    fun loadBots(): List<ConnectedBot> {
        val stored = runCatching {
            val array = JSONArray(prefs.getString("connected_bots_v12", "[]"))
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        ConnectedBot(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            platform = runCatching { BotPlatform.valueOf(o.optString("platform")) }.getOrDefault(BotPlatform.TELEGRAM),
                            name = o.optString("name", "ربات من"),
                            username = o.optString("username"),
                            token = o.optString("token"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            expiresAt = o.optLong("expiresAt", 0L),
                            planLabel = o.optString("planLabel"),
                            active = o.optBoolean("active", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        if (stored.isNotEmpty()) return stored

        // Automatic migration from the v1.0 single Telegram bot.
        if (token.isNotBlank()) {
            val migrated = ConnectedBot(
                platform = BotPlatform.TELEGRAM,
                name = botName.ifBlank { "فروشگاه من" },
                username = botUsername,
                token = token,
                planLabel = "انتقال از نسخه قبلی",
                expiresAt = 0L
            )
            saveBots(listOf(migrated))
            return listOf(migrated)
        }
        return emptyList()
    }

    fun saveBots(items: List<ConnectedBot>) {
        val array = JSONArray()
        items.forEach { bot ->
            array.put(JSONObject().apply {
                put("id", bot.id)
                put("platform", bot.platform.name)
                put("name", bot.name)
                put("username", bot.username)
                put("token", bot.token)
                put("createdAt", bot.createdAt)
                put("expiresAt", bot.expiresAt)
                put("planLabel", bot.planLabel)
                put("active", bot.active)
            })
        }
        prefs.edit().putString("connected_bots_v12", array.toString()).apply()
    }

    fun clearBot(botId: String) {
        val bots = loadBots().filterNot { it.id == botId }
        saveBots(bots)
        if (bots.none { it.platform == BotPlatform.TELEGRAM }) {
            prefs.edit().remove("bot_token").remove("bot_username").apply()
        }
    }

    // Kept for source compatibility with the v1.0 screen while v1.2 uses multi-bot removal.
    fun clearBot() {
        prefs.edit().remove("bot_token").remove("bot_username").apply()
    }

    fun loadCategories(): List<StoreCategory> = runCatching {
        val array = JSONArray(prefs.getString("categories", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(StoreCategory(o.getString("id"), o.getString("title"), o.optString("emoji", "🛍️")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveCategories(items: List<StoreCategory>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("emoji", item.emoji)
            })
        }
        prefs.edit().putString("categories", array.toString()).apply()
    }

    fun loadProducts(): List<StoreProduct> = runCatching {
        val array = JSONArray(prefs.getString("products", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    StoreProduct(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        price = o.getLong("price"),
                        category = o.optString("category"),
                        description = o.optString("description"),
                        active = o.optBoolean("active", true)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun saveProducts(items: List<StoreProduct>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("price", item.price)
                put("category", item.category)
                put("description", item.description)
                put("active", item.active)
            })
        }
        prefs.edit().putString("products", array.toString()).apply()
    }

    companion object {
        private const val ADMIN_USERNAME = "administrator"
        private const val ADMIN_SALT = "AS_TEAM_TELEGRAMBOTSTORE_ADMIN_V1_2"
        private const val ADMIN_DERIVED_KEY = "d54fef0b21d4ac5620174174d5903f996d1cb0563c8cf3b17dc552491f6172a7"

        fun formatDate(time: Long): String {
            if (time <= 0L) return "نامحدود"
            return SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR")).format(Date(time))
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

        private fun verifyAdminPassword(password: String): Boolean = runCatching {
            val spec = PBEKeySpec(password.toCharArray(), ADMIN_SALT.toByteArray(), 120_000, 256)
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            key.joinToString("") { "%02x".format(it) } == ADMIN_DERIVED_KEY
        }.getOrDefault(false)
    }
}
