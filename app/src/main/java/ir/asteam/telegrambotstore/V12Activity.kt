// این فایل Activity اصلی و پوسته ناوبری نسخه ۱.۳.۱ را مدیریت می‌کند.
package ir.asteam.telegrambotstore

// BackHandler برای کنترل اصولی دکمه Back و جلوگیری از خروج ناخواسته استفاده می‌شود.
import androidx.activity.compose.BackHandler
// ComponentActivity میزبان اصلی رابط Compose است.
import androidx.activity.ComponentActivity
// setContent محتوای Compose را به Activity متصل می‌کند.
import androidx.activity.compose.setContent
// تنظیم رنگ StatusBar و NavigationBar در اندروید استفاده می‌شود.
import android.os.Bundle
// Intent برای اشتراک‌گذاری لینک برنامه با سایر اپ‌ها استفاده می‌شود.
import android.content.Intent
// کلاس Build برای بررسی نسخه سیستم‌عامل استفاده می‌شود.
import android.os.Build
// API کنترل رنگ نوارهای سیستمی روی Window است.
import androidx.core.view.WindowCompat
// بسته Layout کامپوننت‌های پایه چیدمان Compose را فراهم می‌کند.
import androidx.compose.foundation.layout.*
// آیکون‌های Material در منو، نوار بالا و پایین استفاده می‌شوند.
import androidx.compose.material.icons.Icons
// مجموعه آیکون‌های پرشده Material است.
import androidx.compose.material.icons.filled.*
// مجموعه آیکون‌های Outline Material است.
import androidx.compose.material.icons.outlined.*
// کامپوننت‌های Material 3 مثل Scaffold و Drawer را فراهم می‌کند.
import androidx.compose.material3.*
// State و APIهای Runtime Compose را فراهم می‌کند.
import androidx.compose.runtime.*
// Color برای تنظیم StatusBar و NavigationBar استفاده می‌شود.
import androidx.compose.ui.graphics.Color
// Context فعلی Compose برای Intent اشتراک‌گذاری استفاده می‌شود.
import androidx.compose.ui.platform.LocalContext
// واحد dp برای padding استاندارد Compose استفاده می‌شود.
import androidx.compose.ui.unit.dp
// CoroutineScope برای باز و بسته کردن Drawer استفاده می‌شود.
import kotlinx.coroutines.launch
// ArrayDeque برای نگهداری history صفحات استفاده می‌شود.
import java.util.ArrayDeque

// Activity اصلی برنامه از ComponentActivity ارث‌بری می‌کند.
class V12Activity : ComponentActivity() {
    // این تابع هنگام ساخت Activity فراخوانی می‌شود.
    override fun onCreate(savedInstanceState: Bundle?) {
        // پیاده‌سازی اصلی Android اجرا می‌شود.
        super.onCreate(savedInstanceState)

        // اجازه داده می‌شود رابط زیر نوارهای سیستم رسم شود تا ظاهر مدرن‌تر شود.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // رنگ StatusBar با پس‌زمینه اپ هماهنگ می‌شود.
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // رنگ NavigationBar با پس‌زمینه تیره اپ هماهنگ می‌شود.
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // رابط Compose به Activity متصل می‌شود.
        setContent {
            // Theme اصلی Material 3 تعریف می‌شود.
            MaterialTheme(
                // ColorScheme تیره‌ی اختصاصی برنامه استفاده می‌شود.
                colorScheme = darkColorScheme(
                    primary = Blue,
                    secondary = TelegramBlue,
                    background = Bg,
                    surface = Surface,
                    error = Danger
                )
            ) {
                // پوسته اصلی نسخه ۱.۳.۱ نمایش داده می‌شود.
                V12Root()
            }
        }
    }
}

// این Composable وضعیت کل برنامه، لاگین و ناوبری بین صفحات را مدیریت می‌کند.
@Composable
private fun V12Root() {
    // Context برای LocalStore و Intentها دریافت می‌شود.
    val context = LocalContext.current
    // LocalStore یک بار ساخته و بین recompositionها حفظ می‌شود.
    val store = remember { LocalStore(context) }

    // وضعیت نمایش Splash نگهداری می‌شود.
    var splash by remember { mutableStateOf(true) }
    // وضعیت نمایش فرم Auth نگهداری می‌شود.
    var authVisible by remember { mutableStateOf(!store.hasCompletedOnboarding) }
    // نوع فرم Auth؛ true یعنی ورود و false یعنی ثبت‌نام.
    var loginMode by remember { mutableStateOf(true) }
    // پیام خطای Auth نگهداری می‌شود.
    var authError by remember { mutableStateOf<String?>(null) }

    // صفحه فعلی برنامه نگهداری می‌شود.
    var page by remember { mutableStateOf(V12Page.DASHBOARD) }
    // history صفحات داخلی برای بازگشت صحیح نگهداری می‌شود.
    val pageHistory = remember { ArrayDeque<V12Page>() }

    // لیست Botها از LocalStore بارگذاری می‌شود.
    var bots by remember { mutableStateOf(store.loadBots()) }
    // لیست دسته‌بندی‌ها از LocalStore بارگذاری می‌شود.
    var categories by remember { mutableStateOf(store.loadCategories()) }
    // لیست محصولات از LocalStore بارگذاری می‌شود.
    var products by remember { mutableStateOf(store.loadProducts()) }
    // Bot انتخاب‌شده برای مدیریت نگهداری می‌شود.
    var selectedBotId by remember { mutableStateOf<String?>(bots.firstOrNull()?.id) }
    // پلن انتخاب‌شده قبل از اتصال Bot نگهداری می‌شود.
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    // پلتفرم انتخاب‌شده برای اتصال نگهداری می‌شود.
    var connectPlatform by remember { mutableStateOf(BotPlatform.TELEGRAM) }

    // وضعیت ورود کاربر از LocalStore خوانده می‌شود.
    var loggedIn by remember { mutableStateOf(store.isLoggedIn) }
    // وضعیت مدیر از LocalStore خوانده می‌شود.
    var admin by remember { mutableStateOf(store.isAdmin) }
    // نام حساب از LocalStore خوانده می‌شود.
    var userName by remember { mutableStateOf(store.userName) }
    // شماره موبایل از LocalStore خوانده می‌شود.
    var userPhone by remember { mutableStateOf(store.userPhone) }
    // وضعیت اعلان‌ها از LocalStore خوانده می‌شود.
    var notifications by remember { mutableStateOf(store.notificationsEnabled) }

    // این تابع stateهای حساب را بعد از ورود/خروج از LocalStore دوباره همگام می‌کند.
    fun refreshAccountState() {
        loggedIn = store.isLoggedIn
        admin = store.isAdmin
        userName = store.userName
        userPhone = store.userPhone
    }

    // این تابع به صفحه جدید می‌رود و صفحه فعلی را در history قرار می‌دهد.
    fun navigateTo(target: V12Page) {
        // اگر مقصد همان صفحه فعلی باشد history تغییر نمی‌کند.
        if (target == page) return
        // صفحه فعلی برای Back ذخیره می‌شود.
        pageHistory.addLast(page)
        // مقصد نمایش داده می‌شود.
        page = target
    }

    // این تابع بدون افزودن history صفحه فعلی را جایگزین می‌کند.
    fun replaceWith(target: V12Page) {
        // صفحه جدید جایگزین صفحه فعلی می‌شود.
        page = target
    }

    // این تابع یک مرحله در history به عقب برمی‌گردد.
    fun navigateBack() {
        // اگر history مقدار داشته باشد همان مقدار برگردانده می‌شود.
        if (pageHistory.isNotEmpty()) {
            page = pageHistory.removeLast()
        } else {
            // در نبود history، داشبورد مقصد امن صفحات داخلی است.
            page = V12Page.DASHBOARD
        }
    }

    // تا زمان پایان Splash، صفحه Splash نمایش داده می‌شود.
    if (splash) {
        // Splash نسخه جدید نمایش داده می‌شود.
        V12Splash { splash = false }
        // ادامه UI در این recomposition اجرا نمی‌شود.
        return
    }

    // اگر Auth لازم باشد فرم ورود/ثبت‌نام نمایش داده می‌شود.
    if (authVisible) {
        // فرم Auth با callbackهای ورود و ثبت‌نام ساخته می‌شود.
        V12Auth(
            login = loginMode,
            error = authError,
            onToggle = {
                // حالت ورود/ثبت‌نام تغییر می‌کند.
                loginMode = !loginMode
                // خطای قبلی پاک می‌شود.
                authError = null
            },
            onSkip = {
                // کاربر وارد حالت مهمان می‌شود.
                store.skipAuth()
                // stateهای حساب تازه می‌شوند.
                refreshAccountState()
                // فرم Auth بسته می‌شود.
                authVisible = false
                // صفحه داشبورد جایگزین می‌شود.
                replaceWith(V12Page.DASHBOARD)
                // history پاک می‌شود چون Auth ریشه مستقلی دارد.
                pageHistory.clear()
            },
            onLogin = { identifier, password ->
                // ورود با LocalStore بررسی می‌شود.
                val ok = store.login(identifier, password)
                // در موفقیت stateها بروزرسانی و فرم بسته می‌شود.
                if (ok) {
                    refreshAccountState()
                    authVisible = false
                    replaceWith(V12Page.DASHBOARD)
                    pageHistory.clear()
                    authError = null
                } else {
                    // در شکست پیام مناسب نمایش داده می‌شود.
                    authError = "شماره موبایل/نام کاربری یا رمز عبور صحیح نیست."
                }
            },
            onRegister = { name, phone, password ->
                // ثبت‌نام محلی انجام می‌شود.
                val ok = store.register(name, phone, password)
                // در موفقیت حساب تازه و فرم بسته می‌شود.
                if (ok) {
                    refreshAccountState()
                    authVisible = false
                    replaceWith(V12Page.DASHBOARD)
                    pageHistory.clear()
                    authError = null
                } else {
                    // خطای اعتبارسنجی نمایش داده می‌شود.
                    authError = "نام، شماره موبایل و رمز عبور را کامل وارد کنید."
                }
            }
        )
        // ادامه رابط اصلی اجرا نمی‌شود.
        return
    }

    // پوسته اصلی برنامه نمایش داده می‌شود.
    V12Shell(
        page = page,
        bots = bots,
        selectedBotId = selectedBotId,
        categories = categories,
        products = products,
        selectedPlan = selectedPlan,
        connectPlatform = connectPlatform,
        loggedIn = loggedIn,
        admin = admin,
        userName = userName,
        userPhone = userPhone,
        notifications = notifications,
        onPage = ::navigateTo,
        onNeedAuth = {
            // فرم ورود باز می‌شود.
            loginMode = true
            authError = null
            authVisible = true
        },
        onSubscriptions = {
            // صفحه اشتراک باز می‌شود.
            navigateTo(V12Page.SUBSCRIPTIONS)
        },
        onPlatform = { platform ->
            // پلتفرم مقصد ذخیره می‌شود.
            connectPlatform = platform
            // اگر پلتفرم هنوز Runtime واقعی ندارد پیام «به‌زودی» نشان داده خواهد شد.
            if (platform != BotPlatform.TELEGRAM) {
                navigateTo(V12Page.CONNECT)
            } else if (admin) {
                // مدیر مستقیماً به اتصال Telegram می‌رود.
                selectedPlan = null
                navigateTo(V12Page.CONNECT)
            } else {
                // کاربر عادی ابتدا پلن انتخاب می‌کند.
                navigateTo(V12Page.SUBSCRIPTIONS)
            }
        },
        onSelectPlan = { plan ->
            // پلن انتخاب‌شده ذخیره می‌شود.
            selectedPlan = plan
            // مقصد اتصال Telegram است.
            connectPlatform = BotPlatform.TELEGRAM
            navigateTo(V12Page.CONNECT)
        },
        onConnected = { connectedBot ->
            // Bot جدید به لیست افزوده یا جایگزین می‌شود.
            bots = (bots.filterNot { it.id == connectedBot.id } + connectedBot)
                .sortedByDescending { it.createdAt }
            // لیست جدید ذخیره می‌شود.
            store.saveBots(bots)
            // Bot تازه انتخاب می‌شود.
            selectedBotId = connectedBot.id
            // پلن موقت پاک می‌شود.
            selectedPlan = null
            // بعد از اتصال history پاک می‌شود تا Back به فرم Token برنگردد.
            pageHistory.clear()
            // داشبورد صفحه ریشه می‌شود.
            replaceWith(V12Page.DASHBOARD)
        },
        onSelectBot = { bot ->
            // شناسه Bot انتخاب‌شده واقعاً تغییر می‌کند تا مدیریت، Catalog، سفارش‌ها و کاربران همان فروشگاه را هدف بگیرند.
            selectedBotId = bot.id
        },
        onBotUpdate = { updated ->
            // Bot ویرایش‌شده داخل لیست جایگزین می‌شود.
            bots = bots.map { if (it.id == updated.id) updated else it }
            // لیست جدید ذخیره می‌شود.
            store.saveBots(bots)
            // همان Bot انتخاب می‌ماند.
            selectedBotId = updated.id
        },
        onBotDelete = { bot ->
            // اتصال Bot از LocalStore پاک می‌شود.
            store.clearBot(bot.id)
            // لیست Botها تازه می‌شود.
            bots = store.loadBots()
            // اولین Bot باقی‌مانده انتخاب می‌شود.
            selectedBotId = bots.firstOrNull()?.id
            // history مرتبط با Bot حذف‌شده پاک می‌شود.
            pageHistory.clear()
            // داشبورد نمایش داده می‌شود.
            replaceWith(V12Page.DASHBOARD)
        },
        onCategories = {
            // دسته‌بندی‌های جدید در state و LocalStore ذخیره می‌شوند.
            categories = it
            store.saveCategories(it)
        },
        onProducts = {
            // محصولات جدید در state و LocalStore ذخیره می‌شوند.
            products = it
            store.saveProducts(it)
        },
        onNotifications = {
            // وضعیت اعلان‌ها در state و LocalStore ذخیره می‌شود.
            notifications = it
            store.notificationsEnabled = it
        },
        onProfileUpdate = { name, phone ->
            // اطلاعات حساب ذخیره می‌شود.
            val ok = store.updateProfile(name, phone)
            // در موفقیت stateهای حساب تازه می‌شوند.
            if (ok) refreshAccountState()
            // نتیجه به صفحه تنظیمات برگردانده می‌شود.
            ok
        },
        onLogout = {
            // نشست کاربر بسته می‌شود.
            store.logout()
            // stateها تازه می‌شوند.
            refreshAccountState()
            // فرم ورود دوباره باز می‌شود.
            loginMode = true
            authError = null
            authVisible = true
        },
        onBack = ::navigateBack
    )
}

// پوسته اصلی Material شامل Drawer، TopBar، صفحات و BottomBar را می‌سازد.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V12Shell(
    page: V12Page,
    bots: List<ConnectedBot>,
    selectedBotId: String?,
    categories: List<StoreCategory>,
    products: List<StoreProduct>,
    selectedPlan: SubscriptionPlan?,
    connectPlatform: BotPlatform,
    loggedIn: Boolean,
    admin: Boolean,
    userName: String,
    userPhone: String,
    notifications: Boolean,
    onPage: (V12Page) -> Unit,
    onNeedAuth: () -> Unit,
    onSubscriptions: () -> Unit,
    onPlatform: (BotPlatform) -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onConnected: (ConnectedBot) -> Unit,
    // انتخاب Bot فقط state انتخاب را تغییر می‌دهد و برخلاف ویرایش، داده Bot را دوباره ذخیره نمی‌کند.
    onSelectBot: (ConnectedBot) -> Unit,
    onBotUpdate: (ConnectedBot) -> Unit,
    onBotDelete: (ConnectedBot) -> Unit,
    onCategories: (List<StoreCategory>) -> Unit,
    onProducts: (List<StoreProduct>) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onProfileUpdate: (String, String) -> Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    // State Drawer در Compose ساخته می‌شود.
    val drawer = rememberDrawerState(DrawerValue.Closed)
    // Scope برای باز و بسته کردن Drawer استفاده می‌شود.
    val scope = rememberCoroutineScope()
    // Context برای Intent اشتراک‌گذاری دریافت می‌شود.
    val context = LocalContext.current
    // Bot انتخاب‌شده از روی شناسه پیدا می‌شود.
    val selectedBot = bots.firstOrNull { it.id == selectedBotId } ?: bots.firstOrNull()

    // اگر Drawer باز باشد Back فقط Drawer را می‌بندد.
    BackHandler(enabled = drawer.isOpen) {
        scope.launch { drawer.close() }
    }

    // در تمام صفحات داخلی، Back به صفحه قبلی برمی‌گردد و Activity را نمی‌بندد.
    BackHandler(enabled = drawer.isClosed && page != V12Page.DASHBOARD) {
        onBack()
    }

    // Drawer سمت Start تعریف می‌شود؛ چون Layout RTL است Start همان سمت راست خواهد بود.
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            // محتوای Drawer داخل Surface تیره نمایش داده می‌شود.
            ModalDrawerSheet(drawerContainerColor = Surface, drawerContentColor = Color.White) {
                // هدر حساب نمایش داده می‌شود.
                DrawerHeader(userName, userPhone, loggedIn, admin)
                // جداکننده هدر و گزینه‌ها نمایش داده می‌شود.
                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                // خانه به داشبورد می‌رود.
                DrawerItem(Icons.Filled.Home, "خانه", page == V12Page.DASHBOARD) {
                    onPage(V12Page.DASHBOARD)
                    scope.launch { drawer.close() }
                }
                // Botهای من به داشبورد می‌رود چون جدول Botها همان‌جا نمایش داده می‌شود.
                DrawerItem(Icons.Filled.SmartToy, "ربات‌های من", page == V12Page.BOT_MANAGER) {
                    selectedBot?.let { onPage(V12Page.BOT_MANAGER) } ?: onPage(V12Page.DASHBOARD)
                    scope.launch { drawer.close() }
                }
                // مدیریت عمومی نام و متن‌های اختصاصی Bot انتخاب‌شده را از Backend مدیریت می‌کند.
                DrawerItem(Icons.Outlined.Tune, "مدیریت عمومی", page == V12Page.GENERAL_MANAGEMENT) {
                    selectedBot?.let { onPage(V12Page.GENERAL_MANAGEMENT) } ?: onPage(V12Page.DASHBOARD)
                    scope.launch { drawer.close() }
                }
                // محصولات از منوی اصلی در دسترس قرار می‌گیرد.
                DrawerItem(Icons.Outlined.Inventory2, "محصولات", page == V12Page.PRODUCTS) {
                    onPage(V12Page.PRODUCTS)
                    scope.launch { drawer.close() }
                }
                // دسته‌بندی‌ها از منوی اصلی در دسترس قرار می‌گیرد.
                DrawerItem(Icons.Outlined.Category, "دسته‌بندی‌ها", page == V12Page.CATEGORIES) {
                    onPage(V12Page.CATEGORIES)
                    scope.launch { drawer.close() }
                }
                // سفارش‌های واقعی فروشگاه انتخاب‌شده از Backend همان Bot خوانده می‌شوند.
                DrawerItem(Icons.Outlined.ReceiptLong, "سفارش‌ها", page == V12Page.ORDERS) {
                    onPage(V12Page.ORDERS)
                    scope.launch { drawer.close() }
                }
                // کاربران فروشگاه و کنترل Block/Unblock از Backend همان Bot مدیریت می‌شوند.
                DrawerItem(Icons.Outlined.Groups, "کاربران فروشگاه", page == V12Page.CUSTOMERS) {
                    onPage(V12Page.CUSTOMERS)
                    scope.launch { drawer.close() }
                }
                // ارسال همگانی فقط به کاربران Block‌نشده Bot انتخاب‌شده انجام می‌شود.
                DrawerItem(Icons.Outlined.Campaign, "ارسال همگانی", page == V12Page.BROADCAST) {
                    selectedBot?.let { onPage(V12Page.BROADCAST) } ?: onPage(V12Page.DASHBOARD)
                    scope.launch { drawer.close() }
                }
                // پیش‌نمایش ربات از منوی اصلی در دسترس قرار می‌گیرد.
                DrawerItem(Icons.Outlined.Visibility, "پیش‌نمایش ربات", page == V12Page.PREVIEW) {
                    onPage(V12Page.PREVIEW)
                    scope.launch { drawer.close() }
                }
                // صفحه اشتراک ربات‌ها باز می‌شود.
                DrawerItem(Icons.Filled.WorkspacePremium, "اشتراک ربات‌ها", page == V12Page.SUBSCRIPTIONS) {
                    onSubscriptions()
                    scope.launch { drawer.close() }
                }
                // صفحه Sync آینده باز می‌شود.
                DrawerItem(Icons.Filled.Sync, "همگام‌سازی ربات‌ها", page == V12Page.SYNC) {
                    onPage(V12Page.SYNC)
                    scope.launch { drawer.close() }
                }
                // تنظیمات باز می‌شود.
                DrawerItem(Icons.Filled.Settings, "تنظیمات", page == V12Page.SETTINGS) {
                    onPage(V12Page.SETTINGS)
                    scope.launch { drawer.close() }
                }
                // جداکننده بخش عمومی نمایش داده می‌شود.
                HorizontalDivider(color = Color.White.copy(alpha = .07f), modifier = androidx.compose.ui.Modifier.padding(vertical = 6.dp))
                // معرفی به دوستان Intent اشتراک‌گذاری می‌سازد.
                DrawerItem(Icons.Outlined.Share, "معرفی به دوستان", false) {
                    // Intent ارسال متن ساخته می‌شود.
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        // نوع محتوای ارسالی متن ساده است.
                        type = "text/plain"
                        // متن معرفی و لینک مخزن فعلی اضافه می‌شود.
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "App BotStore - مدیریت فروشگاه و ربات‌های شما\nhttps://github.com/waxew/App-TelegramBotStore"
                        )
                    }
                    // پنجره انتخاب اپ مقصد باز می‌شود.
                    context.startActivity(Intent.createChooser(sendIntent, "معرفی App BotStore"))
                    // Drawer بسته می‌شود.
                    scope.launch { drawer.close() }
                }
                // درباره ما باز می‌شود.
                DrawerItem(Icons.Outlined.Info, "درباره ما", page == V12Page.ABOUT) {
                    onPage(V12Page.ABOUT)
                    scope.launch { drawer.close() }
                }
                // تماس با ما باز می‌شود.
                DrawerItem(Icons.Outlined.MailOutline, "تماس با ما", page == V12Page.CONTACT) {
                    onPage(V12Page.CONTACT)
                    scope.launch { drawer.close() }
                }
                // درباره نرم‌افزار باز می‌شود.
                DrawerItem(Icons.Outlined.Android, "درباره نرم‌افزار", page == V12Page.APP_INFO) {
                    onPage(V12Page.APP_INFO)
                    scope.launch { drawer.close() }
                }
                // جداکننده حساب نمایش داده می‌شود.
                HorizontalDivider(color = Color.White.copy(alpha = .07f), modifier = androidx.compose.ui.Modifier.padding(vertical = 6.dp))
                // اگر کاربر وارد است خروج وگرنه ورود نمایش داده می‌شود.
                if (loggedIn) {
                    DrawerItem(Icons.Outlined.Logout, "خروج از حساب", false) {
                        onLogout()
                        scope.launch { drawer.close() }
                    }
                } else {
                    DrawerItem(Icons.Outlined.Login, "ورود / ثبت‌نام", false) {
                        onNeedAuth()
                        scope.launch { drawer.close() }
                    }
                }
            }
        }
    ) {
        // Scaffold ساختار TopBar، BottomBar و محتوای صفحات را فراهم می‌کند.
        Scaffold(
            containerColor = Bg,
            topBar = {
                // TopAppBar عنوان صفحه و دکمه منوی همبرگری را نمایش می‌دهد.
                TopAppBar(
                    title = {
                        // عنوان صفحه فعلی نمایش داده می‌شود.
                        Text(page.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    },
                    navigationIcon = {
                        // دکمه منوی همبرگری همیشه در سمت راست RTL قرار می‌گیرد.
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "نوار همبرگری")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface.copy(alpha = .96f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                // نوار پایین فقط در صفحات اصلی نمایش داده می‌شود.
                if (page in listOf(V12Page.DASHBOARD, V12Page.SUBSCRIPTIONS, V12Page.SETTINGS)) {
                    NavigationBar(containerColor = Surface) {
                        // داشبورد در نوار پایین نمایش داده می‌شود.
                        BottomNavItem(V12Page.DASHBOARD, page, Icons.Filled.Home, onPage)
                        // اشتراک‌ها در نوار پایین نمایش داده می‌شود.
                        BottomNavItem(V12Page.SUBSCRIPTIONS, page, Icons.Outlined.WorkspacePremium, onPage)
                        // تنظیمات در نوار پایین نمایش داده می‌شود.
                        BottomNavItem(V12Page.SETTINGS, page, Icons.Filled.Settings, onPage)
                    }
                }
            }
        ) { padding ->
            // Box محتوای صفحه را با padding سیستم و Scaffold نمایش می‌دهد.
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // صفحه مناسب براساس enum فعلی انتخاب می‌شود.
                when (page) {
                    // داشبورد اصلی نمایش داده می‌شود.
                    V12Page.DASHBOARD -> V12Dashboard(
                        bots = bots,
                        loggedIn = loggedIn,
                        admin = admin,
                        userName = userName,
                        onNeedAuth = onNeedAuth,
                        onSelectBot = { bot ->
                            // ابتدا Bot انتخاب‌شده در Root ثبت می‌شود تا تمام صفحات بعدی به همان فروشگاه متصل باشند.
                            onSelectBot(bot)
                            // سپس صفحه مدیریت همان Bot باز می‌شود.
                            onPage(V12Page.BOT_MANAGER)
                        },
                        onSubscriptions = onSubscriptions,
                        onPlatform = onPlatform
                    )
                    // صفحه پلن‌ها نمایش داده می‌شود.
                    V12Page.SUBSCRIPTIONS -> V12Subscriptions(
                        admin = admin,
                        loggedIn = loggedIn,
                        onNeedAuth = onNeedAuth,
                        onChoose = onSelectPlan,
                        onAdminConnect = {
                            onPlatform(BotPlatform.TELEGRAM)
                        }
                    )
                    // صفحه اتصال پلتفرم نمایش داده می‌شود.
                    V12Page.CONNECT -> {
                        // فعلاً Runtime واقعی فقط برای Telegram فعال است.
                        if (connectPlatform == BotPlatform.TELEGRAM) {
                            V12Connect(
                                admin = admin,
                                plan = selectedPlan,
                                onNeedPlan = onSubscriptions,
                                onConnected = onConnected
                            )
                        } else {
                            // سایر پلتفرم‌ها پیام به‌زودی دارند.
                            V12Info(
                                connectPlatform.faName,
                                "اتصال واقعی ${connectPlatform.faName} در نسخه بعدی فعال می‌شود."
                            )
                        }
                    }
                    // مدیریت Bot انتخاب‌شده نمایش داده می‌شود.
                    V12Page.BOT_MANAGER -> {
                        if (selectedBot == null) {
                            EmptyState(
                                "رباتی انتخاب نشده",
                                "ابتدا از داشبورد یک ربات را انتخاب یا متصل کنید.",
                                Icons.Filled.SmartToy
                            )
                        } else {
                            V12BotManager(
                                bot = selectedBot,
                                onUpdate = onBotUpdate,
                                onDelete = onBotDelete,
                                onGeneralManagement = { onPage(V12Page.GENERAL_MANAGEMENT) },
                                onBroadcast = { onPage(V12Page.BROADCAST) },
                                onProducts = { onPage(V12Page.PRODUCTS) },
                                onCategories = { onPage(V12Page.CATEGORIES) },
                                onPreview = { onPage(V12Page.PREVIEW) }
                            )
                        }
                    }
                    // مدیریت عمومی متن‌ها و نام فروشگاه را مستقیم از Backend همان Bot ویرایش می‌کند.
                    V12Page.GENERAL_MANAGEMENT -> {
                        selectedBot?.let { V12GeneralManagement(it) } ?: EmptyState(
                            "رباتی انتخاب نشده",
                            "ابتدا از داشبورد یک ربات را انتخاب کنید.",
                            Icons.Filled.SmartToy
                        )
                    }
                    // مدیریت محصولات بر اساس Category نمایش داده می‌شود و اگر Category وجود نداشته باشد مستقیم به ساخت دسته هدایت می‌کند.
                    V12Page.PRODUCTS -> V12Products(
                        products = products,
                        categories = categories,
                        onChange = onProducts,
                        onOpenCategories = { onPage(V12Page.CATEGORIES) }
                    )
                    // صفحه Category تعداد/ارتباط Productهای هر دسته را نیز می‌شناسد تا حذف والد دارای Product ممکن نباشد.
                    V12Page.CATEGORIES -> V12Categories(categories, products, onCategories)
                    // سفارش‌های واقعی همان Bot از Backend بارگذاری و مدیریت می‌شوند.
                    V12Page.ORDERS -> V12Orders(selectedBot)
                    // کاربران همان فروشگاه و وضعیت Block/Unblock مدیریت می‌شوند.
                    V12Page.CUSTOMERS -> V12Customers(selectedBot)
                    // صف ارسال همگانی قابل Resume فقط برای Bot انتخاب‌شده نمایش داده می‌شود.
                    V12Page.BROADCAST -> {
                        selectedBot?.let { V12Broadcast(it) } ?: EmptyState(
                            "رباتی انتخاب نشده",
                            "ابتدا از داشبورد یک ربات را انتخاب کنید.",
                            Icons.Filled.SmartToy
                        )
                    }
                    // پیش‌نمایش منوی ربات نمایش داده می‌شود.
                    V12Page.PREVIEW -> V12Preview(selectedBot, categories, products)
                    // تنظیمات حساب و اعلان‌ها نمایش داده می‌شود.
                    V12Page.SETTINGS -> V12Settings(
                        loggedIn = loggedIn,
                        admin = admin,
                        userName = userName,
                        userPhone = userPhone,
                        notifications = notifications,
                        onNotifications = onNotifications,
                        onLogin = onNeedAuth,
                        onLogout = onLogout,
                        onProfileUpdate = onProfileUpdate
                    )
                    // صفحه Sync آینده نمایش داده می‌شود.
                    V12Page.SYNC -> V12SyncSoon()
                    // صفحه درباره ما طبق متن استاندارد پروژه نمایش داده می‌شود.
                    V12Page.ABOUT -> V12Info(
                        "گروه توسعه و برنامه نویسی AS Team",
                        "تمامی حقوق مربوط به این برنامه انحصاری میباشد"
                    )
                    // صفحه تماس با ما طبق متن استاندارد پروژه نمایش داده می‌شود.
                    V12Page.CONTACT -> V12Info(
                        "گروه توسعه و برنامه نویسی AS Team",
                        "ایمیل پشتیبانی\nas.team.support@gmail.com"
                    )
                    // درباره نرم‌افزار فقط توضیح کوتاه و شماره نسخه نمایش می‌دهد و اطلاعات فنی Package حذف شده است.
                    V12Page.APP_INFO -> V12Info(
                        "App BotStore",
                        "App BotStore یک پنل فارسی برای مدیریت ربات‌های فروشگاهی و محصولات شماست.\n" +
                            "با این برنامه می‌توانید ربات‌های خود را متصل، دسته‌بندی‌ها و محصولات را مدیریت و وضعیت اشتراک هر ربات را مشاهده کنید.\n\n" +
                            "نسخه ${BuildConfig.VERSION_NAME}"
                    )
                }
            }
        }
    }
}
