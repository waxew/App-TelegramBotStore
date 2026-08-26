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

class V12Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { V12App() }
    }
}

@Composable
fun V12App() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var splash by remember { mutableStateOf(true) }
    var authVisible by remember { mutableStateOf(!store.hasCompletedOnboarding) }
    var loggedIn by remember { mutableStateOf(store.isLoggedIn) }
    var admin by remember { mutableStateOf(store.isAdmin) }
    var userName by remember { mutableStateOf(store.userName) }
    var userPhone by remember { mutableStateOf(store.userPhone) }
    var bots by remember { mutableStateOf(store.loadBots()) }
    var products by remember { mutableStateOf(store.loadProducts()) }
    var categories by remember { mutableStateOf(store.loadCategories()) }
    var notifications by remember { mutableStateOf(store.notificationsEnabled) }
    var page by remember { mutableStateOf(V12Page.DASHBOARD) }
    var selectedBot by remember { mutableStateOf<ConnectedBot?>(bots.firstOrNull()) }
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    LaunchedEffect(Unit) { delay(2_000); splash = false }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Blue,
                background = Bg,
                surface = Surface,
                surfaceVariant = Surface2,
                onBackground = Color.White,
                onSurface = Color.White
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = Bg) {
                when {
                    splash -> V12Splash()
                    authVisible -> V12Auth(
                        onLogin = { id, pass ->
                            val ok = store.login(id, pass)
                            if (ok) {
                                loggedIn = store.isLoggedIn
                                admin = store.isAdmin
                                userName = store.userName
                                userPhone = store.userPhone
                                authVisible = false
                            }
                            ok
                        },
                        onRegister = { name, phone, pass ->
                            val ok = store.register(name, phone, pass)
                            if (ok) {
                                loggedIn = true
                                admin = false
                                userName = store.userName
                                userPhone = store.userPhone
                                authVisible = false
                            }
                            ok
                        },
                        onSkip = {
                            store.skipAuth()
                            loggedIn = false
                            admin = false
                            authVisible = false
                        }
                    )
                    else -> V12Shell(
                        page = page,
                        onPage = { page = it },
                        bots = bots,
                        products = products,
                        categories = categories,
                        loggedIn = loggedIn,
                        admin = admin,
                        userName = userName,
                        userPhone = userPhone,
                        selectedBot = selectedBot,
                        selectedPlan = selectedPlan,
                        notifications = notifications,
                        onNeedAuth = { authVisible = true },
                        onSelectPlan = { selectedPlan = it; page = V12Page.CONNECT },
                        onSelectBot = { selectedBot = it; page = V12Page.BOT_MANAGER },
                        onBotConnected = { bot ->
                            bots = bots + bot
                            store.saveBots(bots)
                            if (bot.platform == BotPlatform.TELEGRAM) {
                                store.token = bot.token
                                store.botUsername = bot.username
                                store.botName = bot.name
                            }
                            selectedBot = bot
                            selectedPlan = null
                            page = V12Page.BOT_MANAGER
                        },
                        onBotUpdate = { bot ->
                            bots = bots.map { if (it.id == bot.id) bot else it }
                            store.saveBots(bots)
                            selectedBot = bot
                        },
                        onBotDelete = { bot ->
                            bots = bots.filterNot { it.id == bot.id }
                            store.saveBots(bots)
                            store.clearBot(bot.id)
                            selectedBot = bots.firstOrNull()
                            page = V12Page.DASHBOARD
                        },
                        onProducts = { products = it; store.saveProducts(it) },
                        onCategories = { categories = it; store.saveCategories(it) },
                        onNotifications = { notifications = it; store.notificationsEnabled = it },
                        onLogout = {
                            store.logout()
                            loggedIn = false
                            admin = false
                            userName = ""
                            userPhone = ""
                            page = V12Page.DASHBOARD
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun V12Splash() {
    val t = rememberInfiniteTransition(label = "logoMotion")
    val scale by t.animateFloat(
        initialValue = .92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17345B), Bg, Color(0xFF040912)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.scale(scale)) { RobotLogo(126) }
            Spacer(Modifier.height(26.dp))
            Text("ربات‌ساز AS Team", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text("مدیریت ربات‌ها در یک برنامه", color = TextMuted)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun V12Auth(
    onLogin: (String, String) -> Boolean,
    onRegister: (String, String, String) -> Boolean,
    onSkip: () -> Unit
) {
    var login by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10223D), Bg)))) {
        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RobotLogo(82)
            Spacer(Modifier.height(17.dp))
            Text("ورود به ربات‌ساز", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("برای اتصال ربات باید حساب کاربری داشته باشید.", color = TextMuted, textAlign = TextAlign.Center, fontSize = 12.sp)
            Spacer(Modifier.height(22.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { login = true; error = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (login) Blue else Surface2)) { Text("ورود") }
                        Button(onClick = { login = false; error = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (!login) Blue else Surface2)) { Text("ثبت نام") }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!login) {
                        OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), label = { Text("نام و نام خانوادگی") }, leadingIcon = { Icon(Icons.Outlined.Person, null) }, singleLine = true)
                        Spacer(Modifier.height(9.dp))
                    }
                    OutlinedTextField(id, { id = it.trim(); error = null }, Modifier.fillMaxWidth(), label = { Text(if (login) "شماره موبایل یا نام کاربری" else "شماره موبایل") }, leadingIcon = { Icon(Icons.Outlined.AccountCircle, null) }, singleLine = true)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(pass, { pass = it; error = null }, Modifier.fillMaxWidth(), label = { Text("رمز عبور") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    error?.let { Text(it, color = Danger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val ok = if (login) onLogin(id, pass) else onRegister(name, id, pass)
                            if (!ok) error = if (login) "اطلاعات ورود صحیح نیست." else "اطلاعات ثبت نام را کامل وارد کنید."
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(if (login) Icons.Outlined.Login else Icons.Outlined.PersonAdd, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (login) "ورود" else "ساخت حساب")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onSkip) { Text("رد کردن / بعداً ثبت نام می‌کنم", color = TextMuted) }
            Text("در حالت مهمان امکان اتصال ربات وجود ندارد.", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V12Shell(
    page: V12Page,
    onPage: (V12Page) -> Unit,
    bots: List<ConnectedBot>,
    products: List<StoreProduct>,
    categories: List<StoreCategory>,
    loggedIn: Boolean,
    admin: Boolean,
    userName: String,
    userPhone: String,
    selectedBot: ConnectedBot?,
    selectedPlan: SubscriptionPlan?,
    notifications: Boolean,
    onNeedAuth: () -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onSelectBot: (ConnectedBot) -> Unit,
    onBotConnected: (ConnectedBot) -> Unit,
    onBotUpdate: (ConnectedBot) -> Unit,
    onBotDelete: (ConnectedBot) -> Unit,
    onProducts: (List<StoreProduct>) -> Unit,
    onCategories: (List<StoreCategory>) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0C1727), modifier = Modifier.width(315.dp)) {
                DrawerHeader(userName, userPhone, loggedIn, admin)
                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                DrawerItem(Icons.Outlined.Home, "خانه", page == V12Page.DASHBOARD) { onPage(V12Page.DASHBOARD); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.WorkspacePremium, "اشتراک ربات‌ها", page == V12Page.SUBSCRIPTIONS) { onPage(V12Page.SUBSCRIPTIONS); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.Sync, "همگام‌سازی ربات‌ها", page == V12Page.SYNC) { onPage(V12Page.SYNC); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.Settings, "تنظیمات", page == V12Page.SETTINGS) { onPage(V12Page.SETTINGS); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.Share, "معرفی به دوستان", false) {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "ربات‌ساز AS Team") }, "اشتراک‌گذاری"))
                }
                DrawerItem(Icons.Outlined.Info, "درباره ما", page == V12Page.ABOUT) { onPage(V12Page.ABOUT); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.Email, "تماس با ما", page == V12Page.CONTACT) { onPage(V12Page.CONTACT); scope.launch { drawer.close() } }
                DrawerItem(Icons.Outlined.Android, "درباره نرم‌افزار", page == V12Page.APP_INFO) { onPage(V12Page.APP_INFO); scope.launch { drawer.close() } }
                Spacer(Modifier.weight(1f))
                DrawerItem(if (loggedIn) Icons.Outlined.Logout else Icons.Outlined.Login, if (loggedIn) "خروج از حساب" else "ورود / ثبت نام", false) {
                    if (loggedIn) onLogout() else onNeedAuth()
                    scope.launch { drawer.close() }
                }
                Text("AS Team • v${BuildConfig.VERSION_NAME}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(22.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(page.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "منوی همبرگری") } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Bg)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Surface) {
                    BottomNavItem(V12Page.DASHBOARD, page, Icons.Outlined.Home, onPage)
                    BottomNavItem(V12Page.SUBSCRIPTIONS, page, Icons.Outlined.WorkspacePremium, onPage)
                    BottomNavItem(V12Page.SETTINGS, page, Icons.Outlined.Settings, onPage)
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (page) {
                    V12Page.DASHBOARD -> V12Dashboard(bots, loggedIn, admin, userName, onNeedAuth, onSelectBot, { onPage(V12Page.SUBSCRIPTIONS) }) { platform ->
                        when {
                            platform != BotPlatform.TELEGRAM -> scope.launch { snack.showSnackbar("اتصال ${platform.faName} به‌زودی فعال می‌شود.") }
                            !loggedIn -> onNeedAuth()
                            admin -> onPage(V12Page.CONNECT)
                            else -> onPage(V12Page.SUBSCRIPTIONS)
                        }
                    }
                    V12Page.SUBSCRIPTIONS -> V12Subscriptions(admin, loggedIn, onNeedAuth, onSelectPlan) { onPage(V12Page.CONNECT) }
                    V12Page.CONNECT -> V12Connect(admin, selectedPlan, { onPage(V12Page.SUBSCRIPTIONS) }, onBotConnected)
                    V12Page.BOT_MANAGER -> selectedBot?.let { V12BotManager(it, onBotUpdate, onBotDelete, { onPage(V12Page.PRODUCTS) }, { onPage(V12Page.CATEGORIES) }, { onPage(V12Page.PREVIEW) }) } ?: EmptyState("رباتی انتخاب نشده", "از داشبورد یک ربات را انتخاب کنید.", Icons.Outlined.Android)
                    V12Page.PRODUCTS -> V12Products(products, categories, onProducts)
                    V12Page.CATEGORIES -> V12Categories(categories, onCategories)
                    V12Page.PREVIEW -> V12Preview(selectedBot, categories, products)
                    V12Page.SETTINGS -> V12Settings(loggedIn, admin, userName, userPhone, notifications, onNotifications, onNeedAuth, onLogout)
                    V12Page.SYNC -> V12SyncSoon()
                    V12Page.ABOUT -> V12Info("گروه توسعه و برنامه نویسی AS Team", "تمامی حقوق مربوط به این برنامه انحصاری میباشد")
                    V12Page.CONTACT -> V12Info("گروه توسعه و برنامه نویسی AS Team", "ایمیل پشتیبانی\nas.team.support@gmail.com")
                    V12Page.APP_INFO -> V12Info("ربات‌ساز AS Team", "نسخه ${BuildConfig.VERSION_NAME}\nتلگرام • واتساپ • روبیکا • بله\n\nPackage: ir.asteam.telegrambotstore")
                }
            }
        }
    }
}
