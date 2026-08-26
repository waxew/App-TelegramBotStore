// این فایل Activity اصلی، Splash، ورود/ثبت‌نام، منوی همبرگری و مسیریابی نسخه ۱.۲.۱ را مدیریت می‌کند.
package ir.asteam.telegrambotstore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Activity لانچر برنامه است.
class V12Activity : ComponentActivity() {
    // رابط Compose هنگام ایجاد Activity بارگذاری می‌شود.
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { V12App() } }
}

// ریشه برنامه stateهای حساب، داده‌ها و مسیریابی را نگه می‌دارد.
@Composable
fun V12App() {
    // Context و LocalStore یک‌بار ساخته می‌شوند.
    val context = LocalContext.current; val store = remember { LocalStore(context) }
    // stateهای اصلی از حافظه مقداردهی می‌شوند.
    var splash by remember { mutableStateOf(true) }; var authVisible by remember { mutableStateOf(!store.hasCompletedOnboarding) }; var loggedIn by remember { mutableStateOf(store.isLoggedIn) }; var admin by remember { mutableStateOf(store.isAdmin) }; var userName by remember { mutableStateOf(store.userName) }; var userPhone by remember { mutableStateOf(store.userPhone) }; var bots by remember { mutableStateOf(store.loadBots()) }; var products by remember { mutableStateOf(store.loadProducts()) }; var categories by remember { mutableStateOf(store.loadCategories()) }; var notifications by remember { mutableStateOf(store.notificationsEnabled) }; var page by remember { mutableStateOf(V12Page.DASHBOARD) }; var selectedBot by remember { mutableStateOf<ConnectedBot?>(bots.firstOrNull()) }; var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    // Splash دو ثانیه نمایش داده می‌شود.
    LaunchedEffect(Unit) { delay(2_000); splash = false }
    // کل برنامه راست‌به‌چپ نمایش داده می‌شود.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // تم تیره برنامه تنظیم می‌شود.
        MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Bg, surface = Surface, surfaceVariant = Surface2, onBackground = Color.White, onSurface = Color.White)) {
            // محتوای اصلی بر اساس وضعیت برنامه انتخاب می‌شود.
            Surface(Modifier.fillMaxSize(), color = Bg) {
                when {
                    // لوگوموشن شروع نمایش داده می‌شود.
                    splash -> V12Splash()
                    // ورود/ثبت‌نام در صورت نیاز نمایش داده می‌شود.
                    authVisible -> V12Auth(onLogin = { id, pass -> val ok = store.login(id, pass); if (ok) { loggedIn = store.isLoggedIn; admin = store.isAdmin; userName = store.userName; userPhone = store.userPhone; authVisible = false }; ok }, onRegister = { name, phone, pass -> val ok = store.register(name, phone, pass); if (ok) { loggedIn = true; admin = false; userName = store.userName; userPhone = store.userPhone; authVisible = false }; ok }, onSkip = { store.skipAuth(); loggedIn = false; admin = false; authVisible = false })
                    // پوسته اصلی برنامه نمایش داده می‌شود.
                    else -> V12Shell(page, { page = it }, bots, products, categories, loggedIn, admin, userName, userPhone, selectedBot, selectedPlan, notifications, { authVisible = true }, { selectedPlan = it; page = V12Page.CONNECT }, { selectedBot = it; page = V12Page.BOT_MANAGER }, { bot -> bots = bots + bot; store.saveBots(bots); if (bot.platform == BotPlatform.TELEGRAM) { store.token = bot.token; store.botUsername = bot.username; store.botName = bot.name }; selectedBot = bot; selectedPlan = null; page = V12Page.BOT_MANAGER }, { bot -> bots = bots.map { if (it.id == bot.id) bot else it }; store.saveBots(bots); selectedBot = bot }, { bot -> bots = bots.filterNot { it.id == bot.id }; store.saveBots(bots); store.clearBot(bot.id); selectedBot = bots.firstOrNull(); page = V12Page.DASHBOARD }, { products = it; store.saveProducts(it) }, { categories = it; store.saveCategories(it) }, { notifications = it; store.notificationsEnabled = it }, { name, phone -> val ok = store.updateProfile(name, phone); if (ok) { userName = store.userName; userPhone = store.userPhone }; ok }, { store.logout(); loggedIn = false; admin = false; userName = ""; userPhone = ""; page = V12Page.DASHBOARD })
                }
            }
        }
    }
}

// لوگوموشن شروع با برند جدید نمایش داده می‌شود.
@Composable
private fun V12Splash() {
    // انیمیشن ضربان لوگو ساخته می‌شود.
    val transition = rememberInfiniteTransition(label = "appBotStoreLogoMotion"); val scale by transition.animateFloat(initialValue = .92f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse), label = "logoPulse")
    // لوگو و شعار در مرکز صفحه نمایش داده می‌شوند.
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF172554), Bg, Color(0xFF030712)))), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.scale(scale)) { RobotLogo(132) }; Spacer(Modifier.height(26.dp)); Text("App BotStore", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(7.dp)); Text("ربات ساز شخصی شبکه های اجتماعی شما در یک پلتفرم", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp)); Spacer(Modifier.height(28.dp)); CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } }
}

// صفحه ورود، ثبت‌نام و رد کردن نمایش داده می‌شود.
@Composable
private fun V12Auth(onLogin: (String, String) -> Boolean, onRegister: (String, String, String) -> Boolean, onSkip: () -> Unit) {
    // state فرم تعریف می‌شود.
    var loginMode by remember { mutableStateOf(true) }; var name by remember { mutableStateOf("") }; var identifier by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    // فرم روی پس‌زمینه گرادیانی قرار می‌گیرد.
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10223D), Bg)))) { Column(Modifier.fillMaxWidth().align(Alignment.Center).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { RobotLogo(88); Spacer(Modifier.height(17.dp)); Text("ورود به App BotStore", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("برای اتصال ربات باید حساب کاربری داشته باشید.", color = TextMuted, textAlign = TextAlign.Center, fontSize = 12.sp); Spacer(Modifier.height(22.dp)); Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(19.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { loginMode = true; error = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (loginMode) Blue else Surface2)) { Text("ورود") }; Button(onClick = { loginMode = false; error = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (!loginMode) Blue else Surface2)) { Text("ثبت نام") } }; Spacer(Modifier.height(14.dp)); if (!loginMode) { OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), label = { Text("نام و نام خانوادگی") }, leadingIcon = { Icon(Icons.Outlined.Person, null) }, singleLine = true); Spacer(Modifier.height(9.dp)) }; OutlinedTextField(identifier, { identifier = it.trim(); error = null }, Modifier.fillMaxWidth(), label = { Text(if (loginMode) "شماره موبایل یا نام کاربری" else "شماره موبایل") }, leadingIcon = { Icon(Icons.Outlined.AccountCircle, null) }, singleLine = true); Spacer(Modifier.height(9.dp)); OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("رمز عبور") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true); error?.let { Text(it, color = Danger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }; Spacer(Modifier.height(14.dp)); Button(onClick = { val ok = if (loginMode) onLogin(identifier, password) else onRegister(name, identifier, password); if (!ok) error = if (loginMode) "اطلاعات ورود صحیح نیست." else "اطلاعات ثبت نام را کامل وارد کنید." }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Icon(if (loginMode) Icons.Outlined.Login else Icons.Outlined.PersonAdd, null); Spacer(Modifier.width(7.dp)); Text(if (loginMode) "ورود" else "ساخت حساب") } } }; Spacer(Modifier.height(10.dp)); TextButton(onClick = onSkip) { Text("رد کردن / بعداً ثبت نام می‌کنم", color = TextMuted) }; Text("در حالت مهمان امکان اتصال ربات وجود ندارد.", color = TextMuted, fontSize = 10.sp) } }
}

// APIهای آزمایشی Material3 برای TopAppBar و Drawer فعال می‌شوند.
@OptIn(ExperimentalMaterial3Api::class)
// پوسته اصلی و ناوبری برنامه ساخته می‌شود.
@Composable
private fun V12Shell(page: V12Page, onPage: (V12Page) -> Unit, bots: List<ConnectedBot>, products: List<StoreProduct>, categories: List<StoreCategory>, loggedIn: Boolean, admin: Boolean, userName: String, userPhone: String, selectedBot: ConnectedBot?, selectedPlan: SubscriptionPlan?, notifications: Boolean, onNeedAuth: () -> Unit, onSelectPlan: (SubscriptionPlan) -> Unit, onSelectBot: (ConnectedBot) -> Unit, onBotConnected: (ConnectedBot) -> Unit, onBotUpdate: (ConnectedBot) -> Unit, onBotDelete: (ConnectedBot) -> Unit, onProducts: (List<StoreProduct>) -> Unit, onCategories: (List<StoreCategory>) -> Unit, onNotifications: (Boolean) -> Unit, onProfileUpdate: (String, String) -> Boolean, onLogout: () -> Unit) {
    // Drawer، coroutine scope، Context و Snackbar ساخته می‌شوند.
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope(); val context = LocalContext.current; val snack = remember { SnackbarHostState() }
    // Drawer سمت راست نمایش داده می‌شود.
    ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet(drawerContainerColor = Color(0xFF0C1727), modifier = Modifier.width(315.dp)) { DrawerHeader(userName, userPhone, loggedIn, admin); HorizontalDivider(color = Color.White.copy(alpha = .07f)); DrawerItem(Icons.Outlined.Home, "خانه", page == V12Page.DASHBOARD) { onPage(V12Page.DASHBOARD); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.WorkspacePremium, "اشتراک ربات‌ها", page == V12Page.SUBSCRIPTIONS) { onPage(V12Page.SUBSCRIPTIONS); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.Sync, "همگام‌سازی ربات‌ها", page == V12Page.SYNC) { onPage(V12Page.SYNC); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.Settings, "تنظیمات", page == V12Page.SETTINGS) { onPage(V12Page.SETTINGS); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.Share, "معرفی به دوستان", false) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "App BotStore - ربات ساز شخصی شبکه های اجتماعی شما در یک پلتفرم") }, "اشتراک‌گذاری")) }; DrawerItem(Icons.Outlined.Info, "درباره ما", page == V12Page.ABOUT) { onPage(V12Page.ABOUT); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.Email, "تماس با ما", page == V12Page.CONTACT) { onPage(V12Page.CONTACT); scope.launch { drawer.close() } }; DrawerItem(Icons.Outlined.Android, "درباره نرم‌افزار", page == V12Page.APP_INFO) { onPage(V12Page.APP_INFO); scope.launch { drawer.close() } }; Spacer(Modifier.weight(1f)); DrawerItem(if (loggedIn) Icons.Outlined.Logout else Icons.Outlined.Login, if (loggedIn) "خروج از حساب" else "ورود / ثبت نام", false) { if (loggedIn) onLogout() else onNeedAuth(); scope.launch { drawer.close() } }; Text("App BotStore • v${BuildConfig.VERSION_NAME}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(22.dp)) } }) {
        // Scaffold نوار بالا، پایین و محتوای صفحات را می‌سازد.
        Scaffold(containerColor = Bg, snackbarHost = { SnackbarHost(snack) }, topBar = { CenterAlignedTopAppBar(title = { Text(page.title, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "منوی همبرگری") } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Bg)) }, bottomBar = { NavigationBar(containerColor = Surface) { BottomNavItem(V12Page.DASHBOARD, page, Icons.Outlined.Home, onPage); BottomNavItem(V12Page.SUBSCRIPTIONS, page, Icons.Outlined.WorkspacePremium, onPage); BottomNavItem(V12Page.SETTINGS, page, Icons.Outlined.Settings, onPage) } }) { pad ->
            // صفحه فعلی بر اساس enum رندر می‌شود.
            Box(Modifier.padding(pad).fillMaxSize()) { when (page) { V12Page.DASHBOARD -> V12Dashboard(bots, loggedIn, admin, userName, onNeedAuth, onSelectBot, { onPage(V12Page.SUBSCRIPTIONS) }) { platform -> when { platform != BotPlatform.TELEGRAM -> scope.launch { snack.showSnackbar("اتصال ${platform.faName} به‌زودی فعال می‌شود.") }; !loggedIn -> onNeedAuth(); admin -> onPage(V12Page.CONNECT); else -> onPage(V12Page.SUBSCRIPTIONS) } }; V12Page.SUBSCRIPTIONS -> V12Subscriptions(admin, loggedIn, onNeedAuth, onSelectPlan) { onPage(V12Page.CONNECT) }; V12Page.CONNECT -> V12Connect(admin, selectedPlan, { onPage(V12Page.SUBSCRIPTIONS) }, onBotConnected); V12Page.BOT_MANAGER -> selectedBot?.let { V12BotManager(it, onBotUpdate, onBotDelete, { onPage(V12Page.PRODUCTS) }, { onPage(V12Page.CATEGORIES) }, { onPage(V12Page.PREVIEW) }) } ?: EmptyState("رباتی انتخاب نشده", "از داشبورد یک ربات را انتخاب کنید.", Icons.Outlined.Android); V12Page.PRODUCTS -> V12Products(products, categories, onProducts); V12Page.CATEGORIES -> V12Categories(categories, onCategories); V12Page.PREVIEW -> V12Preview(selectedBot, categories, products); V12Page.SETTINGS -> V12Settings(loggedIn, admin, userName, userPhone, notifications, onNotifications, onNeedAuth, onLogout, onProfileUpdate); V12Page.SYNC -> V12SyncSoon(); V12Page.ABOUT -> V12Info("گروه توسعه و برنامه نویسی AS Team", "تمامی حقوق مربوط به این برنامه انحصاری میباشد"); V12Page.CONTACT -> V12Info("گروه توسعه و برنامه نویسی AS Team", "ایمیل پشتیبانی\nas.team.support@gmail.com"); V12Page.APP_INFO -> V12Info("App BotStore", "ربات ساز شخصی شبکه های اجتماعی شما در یک پلتفرم\n\nنسخه ${BuildConfig.VERSION_NAME}\nتلگرام • واتساپ • روبیکا • بله") } }
        }
    }
}
