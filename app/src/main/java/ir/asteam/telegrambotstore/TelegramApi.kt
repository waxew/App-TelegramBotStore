package ir.asteam.telegrambotstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TelegramBotInfo(val id: Long, val username: String, val firstName: String)

object TelegramApi {
    suspend fun validateToken(token: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(token.matches(Regex("^[0-9]{6,12}:[A-Za-z0-9_-]{20,}$"))) { "فرمت توکن صحیح نیست" }
            val connection = URL("https://api.telegram.org/bot$token/getMe").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            try {
                val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                    .bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) error(json.optString("description", "توکن توسط تلگرام تایید نشد"))
                val result = json.getJSONObject("result")
                TelegramBotInfo(
                    id = result.getLong("id"),
                    username = result.optString("username"),
                    firstName = result.optString("first_name")
                )
            } finally {
                connection.disconnect()
            }
        }
    }
}
