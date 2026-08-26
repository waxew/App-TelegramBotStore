// این فایل یک ContentProvider داخلی برای شروع خودکار موتور همگام‌سازی Catalog و چرخه عمر Bot در زمان اجرای Process برنامه است.
// Provider هیچ داده‌ای در اختیار برنامه‌های دیگر قرار نمی‌دهد و فقط تغییرات ربات‌ها، محصولات و دسته‌بندی‌ها را به Backend واقعی منتقل می‌کند.
package ir.asteam.telegrambotstore

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// این Provider همراه Application ساخته می‌شود و بدون نیاز به تغییر در UI، Sync و حذف Runtime ربات را در پس‌زمینه فعال می‌کند.
class CatalogSyncProvider : ContentProvider() {
    // Scope مستقل I/O برای درخواست‌های شبکه ساخته می‌شود تا Main Thread مسدود نشود.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Job آخرین Sync نگهداری می‌شود تا تغییرات پشت‌سرهم با debounce به یک درخواست تبدیل شوند.
    private var pendingSync: Job? = null

    // SharedPreferences برنامه برای شنیدن تغییرات داخلی نگهداری می‌شود.
    private var preferences: SharedPreferences? = null

    // LocalStore همان لایه ذخیره‌سازی فعلی برنامه است و مدل‌های موجود را بدون تکرار Parser می‌خواند.
    private var localStore: LocalStore? = null

    // Snapshot قبلی Botها نگهداری می‌شود تا حذف یک Bot از APK به حذف واقعی Runtime روی Backend تبدیل شود.
    private var knownBots: List<ConnectedBot> = emptyList()

    // Listener باید به‌صورت property نگهداری شود تا Garbage Collector آن را حذف نکند.
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // فقط تغییراتی که می‌توانند Runtime یا ظاهر فروشگاه Telegram را عوض کنند باعث Reconcile می‌شوند.
        if (key == KEY_BOTS || key == KEY_PRODUCTS || key == KEY_CATEGORIES) {
            scheduleBackendReconcile()
        }
    }

    // هنگام ایجاد Process برنامه، Migration مالکیت، Snapshot، Listener و Sync اولیه آماده می‌شوند.
    override fun onCreate(): Boolean {
        // Context برنامه دریافت می‌شود؛ در نبود Context راه‌اندازی Provider ناموفق است.
        val appContext = context?.applicationContext ?: return false

        // LocalStore برای خواندن Botها و Catalog ساخته می‌شود.
        val store = LocalStore(appContext)
        localStore = store

        // Snapshot قبل از ثبت Listener خوانده می‌شود تا بعداً Bot حذف‌شده قابل تشخیص باشد.
        knownBots = store.loadBots()

        // Catalog نسخه‌های قدیمی ممکن است botId نداشته باشد؛ load آن را به Bot اصلی نسبت می‌دهد و save این مالکیت را دائمی می‌کند.
        // این کار قبل از ثبت Listener انجام می‌شود تا حذف Bot اول باعث انتقال تصادفی داده legacy به Bot دوم نشود.
        store.saveCategories(store.loadCategories())
        store.saveProducts(store.loadProducts())

        // همان SharedPreferences تاریخی پروژه باز می‌شود تا داده نسخه‌های قبلی نیز حفظ شود.
        preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).also { prefs ->
            // Listener تغییرات روی Preferences ثبت می‌شود.
            prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        }

        // Reconcile اولیه باعث می‌شود Catalog ذخیره‌شده پس از Update نیز به Backend صحیح منتقل شود.
        scheduleBackendReconcile(initialDelayMillis = INITIAL_SYNC_DELAY_MS)

        // true یعنی Provider با موفقیت آماده شده است.
        return true
    }

    // این تابع چرخه عمر Bot و Sync Catalog را با debounce در یک موج شبکه‌ای هماهنگ می‌کند.
    private fun scheduleBackendReconcile(initialDelayMillis: Long = DEBOUNCE_MS) {
        // Job قبلی که هنوز در انتظار است لغو می‌شود تا تغییرات متوالی ادغام شوند.
        pendingSync?.cancel()

        // Job جدید روی Dispatcher.IO اجرا می‌شود.
        pendingSync = syncScope.launch {
            // کمی صبر می‌شود تا چند SharedPreferences update متوالی کامل شوند.
            delay(initialDelayMillis)

            // LocalStore آماده باید در دسترس باشد.
            val store = localStore ?: return@launch

            // Snapshot فعلی Botها از حافظه محلی خوانده می‌شود.
            val currentBots = store.loadBots()

            // Botهایی که حذف شده‌اند یا Token آن‌ها با همان id عوض شده است باید Runtime قبلی‌شان خاموش شود.
            val retiredBots = knownBots.filter { oldBot ->
                oldBot.platform == BotPlatform.TELEGRAM &&
                    oldBot.token.isNotBlank() &&
                    currentBots.none { current ->
                        current.id == oldBot.id && current.token == oldBot.token
                    }
            }

            // Botهایی که Disconnect آن‌ها شکست بخورد برای Retry در Snapshot نگه داشته می‌شوند.
            val retryRetiredBots = mutableListOf<ConnectedBot>()

            // برای هر Bot حذف‌شده، deleteWebhook و حذف رکورد Backend اجرا می‌شود.
            retiredBots.forEach { retiredBot ->
                TelegramApi.disconnectBot(retiredBot.token)
                    .onSuccess {
                        // Token در Log نوشته نمی‌شود؛ فقط Username برای عیب‌یابی کافی است.
                        Log.i(TAG, "Backend disconnected for removed bot @${retiredBot.username}")
                    }
                    .onFailure { error ->
                        // در شکست شبکه، Bot داخل Snapshot باقی می‌ماند تا تغییر بعدی دوباره تلاش کند.
                        retryRetiredBots += retiredBot
                        Log.w(TAG, "Backend disconnect failed for @${retiredBot.username}: ${error.message}")
                    }
            }

            // Snapshot به Botهای فعلی به‌علاوه حذف‌های ناموفق تغییر می‌کند تا Retry ممکن بماند.
            knownBots = currentBots + retryRetiredBots

            // همه ربات‌های فعال Telegram که Token معتبر محلی دارند برای Sync انتخاب می‌شوند.
            val telegramBots = currentBots.filter { bot ->
                bot.platform == BotPlatform.TELEGRAM && bot.active && bot.token.isNotBlank()
            }

            // اگر Bot فعالی باقی نمانده باشد، مرحله Catalog تمام می‌شود؛ Disconnectهای بالا همچنان انجام شده‌اند.
            if (telegramBots.isEmpty()) return@launch

            // کل دسته‌بندی‌ها یک بار از SharedPreferences خوانده می‌شوند.
            val allCategories = store.loadCategories()

            // کل محصولات یک بار از SharedPreferences خوانده می‌شوند.
            val allProducts = store.loadProducts()

            // هر Bot فقط Catalog خودش را دریافت می‌کند؛ داده فروشگاه‌ها بین Botها مخلوط نمی‌شود.
            telegramBots.forEach { bot ->
                // دسته‌بندی‌های همین Bot بر اساس botId جدا می‌شوند.
                val botCategories = allCategories.filter { category -> category.botId == bot.id }

                // محصولات همین Bot بر اساس botId جدا می‌شوند.
                val botProducts = allProducts.filter { product -> product.botId == bot.id }

                // درخواست Sync فقط با Catalog اختصاصی همین Token اجرا می‌شود.
                TelegramApi.syncCatalog(bot.token, botCategories, botProducts)
                    .onSuccess { result ->
                        // فقط Username و تعداد آیتم‌ها Log می‌شوند و Token هیچ‌وقت چاپ نمی‌شود.
                        Log.i(
                            TAG,
                            "Catalog synced for @${bot.username}: ${result.categoriesSynced} categories, ${result.productsSynced} products"
                        )
                    }
                    .onFailure { error ->
                        // فقط نام Bot و متن خطا ثبت می‌شود؛ Token هرگز وارد Log نمی‌شود.
                        Log.w(TAG, "Catalog sync failed for @${bot.username}: ${error.message}")
                    }
            }
        }
    }

    // این Provider API خواندن عمومی ندارد و همیشه null برمی‌گرداند.
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    // این Provider MIME Type عمومی ارائه نمی‌کند.
    override fun getType(uri: Uri): String? = null

    // درج مستقیم در Provider مجاز نیست.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    // حذف مستقیم از Provider مجاز نیست.
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    // بروزرسانی مستقیم از Provider مجاز نیست.
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    // ثابت‌های داخلی Provider در companion object نگهداری می‌شوند.
    companion object {
        // Tag بدون اطلاعات حساس برای Logcat تعریف می‌شود.
        private const val TAG = "AppBotStoreCatalogSync"

        // نام SharedPreferences باید دقیقاً با LocalStore یکی بماند تا داده کاربران قدیمی خوانده شود.
        private const val PREFERENCES_NAME = "telegram_bot_store"

        // کلید لیست ربات‌ها همان کلید نسخه فعلی LocalStore است.
        private const val KEY_BOTS = "connected_bots_v12"

        // کلید محصولات همان کلید موجود LocalStore است.
        private const val KEY_PRODUCTS = "products"

        // کلید دسته‌بندی‌ها همان کلید موجود LocalStore است.
        private const val KEY_CATEGORIES = "categories"

        // debounce کوتاه تغییرات متوالی UI را در یک Reconcile جمع می‌کند.
        private const val DEBOUNCE_MS = 650L

        // Reconcile اولیه کمی دیرتر اجرا می‌شود تا Activity و داده‌های مهاجرت‌شده فرصت آماده‌شدن داشته باشند.
        private const val INITIAL_SYNC_DELAY_MS = 1_200L
    }
}
