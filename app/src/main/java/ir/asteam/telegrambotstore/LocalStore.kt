package ir.asteam.telegrambotstore

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("telegram_bot_store", Context.MODE_PRIVATE)

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
                put("id", item.id); put("title", item.title); put("emoji", item.emoji)
            })
        }
        prefs.edit().putString("categories", array.toString()).apply()
    }

    fun loadProducts(): List<StoreProduct> = runCatching {
        val array = JSONArray(prefs.getString("products", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(StoreProduct(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    price = o.getLong("price"),
                    category = o.optString("category"),
                    description = o.optString("description"),
                    active = o.optBoolean("active", true)
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveProducts(items: List<StoreProduct>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("price", item.price)
                put("category", item.category); put("description", item.description); put("active", item.active)
            })
        }
        prefs.edit().putString("products", array.toString()).apply()
    }
}
