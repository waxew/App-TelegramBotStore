// این فایل Activity اصلی، Splash، ورود/ثبت‌نام، منوی همبرگری و مسیریابی نسخه ۱.۳.۰ را مدیریت می‌کند.
package ir.asteam.telegrambotstore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

// Activity لانچر برنامه است و تنها نقطه ورود Android به رابط Compose محسوب می‌شود.
class V12Activity : ComponentActivity() {
    // این متد هنگام ساخته شدن Activity اجرا می‌شود.
    override fun onCreate(savedInstanceState: Bundle?) {
        // پیاده‌سازی استاندارد Activity ابتدا اجرا می‌شود.
        super.onCreate(savedInstanceState)
        // ریشه رابط کاربری Compose روی پنجره Activity قرار می‌گیرد.
        setContent { V12App() }
    }
}

// ریشه برنامه stateهای حساب، داده‌ها، صفحه جاری و history ناوبری را نگه می‌دارد.
@Composable
fun V12App() {
    // Context فعلی برای ساخت LocalStore دریافت می‌شود.
    val context = LocalContext.current
    // LocalStore فقط یک‌بار برای طول عمر این Composition ساخته می‌شود.
    val store = remember { LocalStore(context) }

    // نمایش Splash تا پایان زمان لوگوموشن کنترل می‌شود.
    var splash by remember { mutableStateOf(true) }
    // نمایش صفحه ورود/ثبت‌نام بر اساس وضعیت onboarding ذخیره‌شده تعیین می‌شود.
    var authVisible by remember { mutableStateOf(!store.hasCompletedOnboarding) }
    // وضعیت ورود کاربر از حافظه محلی بازیابی می‌شود.
    var loggedIn by remember { mutableStateOf(store.isLoggedIn) }
    // دسترسی مدیر از حافظه محلی بازیابی می‌شود.
    var admin by remember { mutableStateOf(store.isAdmin) }
    // نام کاربر برای Drawer و داشبورد نگهداری می‌شود.
    var userName by remember { mutableStateOf(store.userName) }
    // شماره کاربر برای پروفایل نگهداری می‌شود.
    var userPhone by remember { mutableStateOf(store.userPhone) }
    // فهرست ربات‌های متصل از حافظه محلی بارگذاری می‌شود.
    var bots by remember { mutableStateOf(store.loadBots()) }
    // فهرست محصولات فروشگاه از حافظه محلی بارگذاری می‌شود.
    var products by remember { mutableStateOf(store.loadProducts()) }
    // فهرست دسته‌بندی‌های فروشگاه از حافظه محلی بارگذاری می‌شود.
    var categories by remember { mutableStateOf(store.loadCategories()) }
    // وضعیت اعلان‌ها از تنظیمات ذخیره‌شده خوانده می‌شود.
    var notifications by remember { mutableStateOf(store.notificationsEnabled) }
    // صفحه پیش‌فرض برنامه داشبورد است.
    var page by remember { mutableStateOf(V12Page.DASHBOARD) }
    // history صفحات برای عملکرد صحیح دکمه Back نگهداری می‌شود.
    val pageHistory = remember { mutableStateListOf<V12Page>() }
    // ربات انتخاب‌شده برای صفحات مدیریت، محصول، دسته‌بندی و پیش‌نمایش نگهداری می‌شود.
    var selectedBot by remember { mutableStateOf<ConnectedBot?>(bots.firstOrNull()) }
    // پلن انتخاب‌شده تا زمان اتصال ربات نگهداری می‌شود.
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    // این تابع انتقال استاندارد به صفحه جدید را انجام می‌دهد و صفحه قبلی را داخل history می‌گذارد.
    fun navigateTo(target: V12Page) {
        // از ثبت دوباره همان صفحه در history جلوگیری می‌شود.
        if (target == page) return
        // صفحه فعلی برای Back بعدی ذخیره می‌شود.
        pageHistory.add(page)
        // صفحه مقصد فعال می‌شود.
        page = target
    }

    // این تابع برای انتقال‌هایی استفاده می‌شود که نباید کاربر را دوباره به فرم قبلی برگردانند.
    fun replaceWith(target: V12Page, backTarget: V12Page? = null) {
        // history قدیمی پاک می‌شود تا مسیر نامعتبر باقی نماند.
        pageHistory.clear()
        // در صورت مشخص شدن صفحه برگشت، فقط همان صفحه در history قرار می‌گیرد.
        if (backTarget != null && backTarget != target) pageHistory.add(backTarget)
        // صفحه مقصد فعال می‌شود.
        page = target
    }

    // این تابع یک مرحله در history به عقب برمی‌گردد.
    fun navigateBack() {
        // اگر history خالی نباشد آخرین صفحه حذف و فعال می‌شود.
        if (pageHistory.isNotEmpty()) {
            page = pageHistory.removeAt(pageHistory.lastIndex)
        } else {
            // در نبود history، کاربر به داشبورد برگردانده می‌شود.
            page = V12Page.DASHBOARD
        }
    }

    // Splash برای مدت کوتاه نمایش داده می‌شود و سپس کنار می‌رود.
    LaunchedEffect(Unit) {
        delay(2_000)
        splash = false
    }

    // روی تمام صفحات داخلی به‌جز داشبورد، Back سیستم به history برنامه متصل می‌شود.
    BackHandler(enabled = !splash && !authVisible && page != V12Page.DASHBOARD) {
        navigateBack()
    }

    // رابط برنامه به‌طور کامل راست‌به‌چپ رندر می‌شود.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // رنگ‌بندی اصلی Material 3 تعریف می‌شود.
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
            // سطح ریشه تمام صفحه را می‌پوشاند.
            Surface(Modifier.fillMaxSize(), color = Bg) {
                // محتوای مناسب بر اساس state فعلی انتخاب می‌شود.
                when {
                    // تا زمانی که Splash فعال است لوگوموشن نمایش داده می‌شود.
                    splash -> V12Splash()
                    // اگر onboarding کامل نشده یا کاربر درخواست ورود داده باشد فرم حساب نمایش داده می‌شود.
                    authVisible -> V12Auth(
                        onLogin = { id, pass ->
                            // ورود در LocalStore بررسی می‌شود.
                            val ok = store.login(id, pass)
                            // در ورود موفق stateهای حساب با حافظه محلی همگام می‌شوند.
                            if (ok) {
                                loggedIn = store.isLoggedIn
                                admin = store.isAdmin
                                userName = store.userName
                                userPhone = store.userPhone
                                authVisible = false
                            }
                            // نتیجه ورود برای نمایش خطای فرم برگردانده می‌شود.
                            ok
                        },
                        onRegister = { name, phone, pass ->
                            // ثبت‌نام محلی انجام می‌شود.
                            val ok = store.register(name, phone, pass)
                            // در ثبت‌نام موفق stateهای حساب تازه‌سازی می‌شوند.
                            if (ok) {
                                loggedIn = true
                                admin = false
                                userName = store.userName
                                userPhone = store.userPhone
                                authVisible = false
                            }
                            // نتیجه ثبت‌نام برای فرم برگردانده می‌شود.
                            ok
                        },
                        onSkip = {
                            // حالت مهمان در LocalStore ثبت می‌شود.
                            store.skipAuth()
                            // state حساب به حالت مهمان می‌رود.
                            loggedIn = false
                            admin = false
                            authVisible = false
                        }
                    )
                    // در حالت عادی پوسته اصلی، Drawer و صفحات برنامه نمایش داده می‌شوند.
                    else -> V12Shell(
                        page = page,
                        onPage = ::navigateTo,
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
                        onSelectPlan = { plan ->
                            // پلن انتخاب‌شده ذخیره می‌شود.
                            selectedPlan = plan
                            // سپس کاربر به صفحه اتصال منتقل می‌شود.
                            navigateTo(V12Page.CONNECT)
                        },
                        onSelectBot = { bot ->
                            // ربات انتخاب‌شده برای صفحه مدیریت ثبت می‌شود.
                            selectedBot = bot
                            // صفحه مدیریت ربات باز می‌شود.
                            navigateTo(V12Page.BOT_MANAGER)
                        },
                        onBotConnected = { bot ->
                            // ربات تازه‌متصل‌شده به لیست فعلی افزوده می‌شود.
                            bots = bots + bot
                            // لیست جدید در حافظه محلی ذخیره می‌شود.
                            store.saveBots(bots)
                            // فیلدهای legacy تلگرام برای سازگاری نسخه‌های قبل حفظ می‌شوند.
                            if (bot.platform == BotPlatform.TELEGRAM) {
                                store.token = bot.token
                                store.botUsername = bot.username
                                store.botName = bot.name
                            }
                            // ربات جدید به‌عنوان ربات انتخاب‌شده قرار می‌گیرد.
                            selectedBot = bot
                            // پلن موقت بعد از اتصال دیگر لازم نیست.
                            selectedPlan = null
                            // پس از اتصال، Back مستقیماً به داشبورد برمی‌گردد و دوباره فرم توکن باز نمی‌شود.
                            replaceWith(V12Page.BOT_MANAGER, V12Page.DASHBOARD)
                        },
                        onBotUpdate = { bot ->
                            // فقط ربات هم‌ شناسه با نسخه ویرایش‌شده جایگزین می‌شود.
                            bots = bots.map { old -> if (old.id == bot.id) bot else old }
                            // تغییرات در حافظه محلی ذخیره می‌شوند.
                            store.saveBots(bots)
                            // نسخه تازه ربات برای UI انتخاب می‌شود.
                            selectedBot = bot
                        },
                        onBotDelete = { bot ->
                            // ربات از state فعلی حذف می‌شود.
                            bots = bots.filterNot { it.id == bot.id }
                            // لیست باقی‌مانده ذخیره می‌شود.
                            store.saveBots(bots)
                            // اطلاعات اتصال ربات از حافظه محلی پاک می‌شود.
                            store.clearBot(bot.id)
                            // در صورت وجود، اولین ربات باقی‌مانده انتخاب می‌شود.
                            selectedBot = bots.firstOrNull()
                            // پس از حذف، کاربر به داشبورد منتقل می‌شود.
                            replaceWith(V12Page.DASHBOARD)
                        },
                        onProducts = { newProducts ->
                            // state محصولات جایگزین می‌شود.
                            products = newProducts
                            // فهرست محصولات ذخیره می‌شود.
                            store.saveProducts(newProducts)
                        },
                        onCategories = { newCategories ->
                            // state دسته‌بندی‌ها جایگزین می‌شود.
                            categories = newCategories
                            // فهرست دسته‌بندی‌ها ذخیره می‌شود.
                            store.saveCategories(newCategories)
                        },
                        onNotifications = { enabled ->
                            // state اعلان‌ها تغییر می‌کند.
                            notifications = enabled
                            // تنظیم اعلان‌ها در حافظه محلی ذخیره می‌شود.
                            store.notificationsEnabled = enabled
                        },
                        onProfileUpdate = { name, phone ->
                            // LocalStore اطلاعات حساب را اعتبارسنجی و ذخیره می‌کند.
                            val ok = store.updateProfile(name, phone)
                            // در صورت موفقیت stateهای نمایشی تازه‌سازی می‌شوند.
                            if (ok) {
                                userName = store.userName
                                userPhone = store.userPhone
                            }
                            // نتیجه برای Dialog برگردانده می‌شود.
                            ok
                        },
                        onLogout = {
                            // نشست محلی بسته می‌شود.
                            store.logout()
                            // state حساب پاک می‌شود.
                            loggedIn = false
                            admin = false
                            userName = ""
                            userPhone = ""
                            // بعد از خروج history پاک و داشبورد نمایش داده می‌شود.
                            replaceWith(V12Page.DASHBOARD)
                        }
                    )
                }
            }
        }
    }
}

// لوگوموشن شروع با برند App BotStore نمایش داده می‌شود.
@Composable
private fun V12Splash() {
    // transition بی‌نهایت برای ضربان ملایم لوگو ساخته می‌شود.
    val transition = rememberInfiniteTransition(label = "appBotStoreLogoMotion")
    // مقدار scale بین دو اندازه رفت و برگشت می‌کند.
    val scale by transition.animateFloat(
        initialValue = .92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "logoPulse"
    )

    // پس‌زمینه Splash با گرادیان تیره نمایش داده می‌شود.
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF172554), Bg, Color(0xFF030712)))
        ),
        contentAlignment = Alignment.Center
    ) {
        // لوگو، نام برنامه، شعار و Progress به‌صورت عمودی چیده می‌شوند.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // لوگوی ربات با scale انیمیشنی نمایش داده می‌شود.
            Box(Modifier.scale(scale)) { RobotLogo(132) }
            // فاصله عمودی زیر لوگو ایجاد می‌شود.
            Spacer(Modifier.height(26.dp))
            // نام برنامه نمایش داده می‌شود.
            Text("App BotStore", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            // فاصله کوتاه زیر عنوان قرار می‌گیرد.
            Spacer(Modifier.height(7.dp))
            // شعار برنامه نمایش داده می‌شود.
            Text(
                "ربات ساز شخصی شبکه های اجتماعی شما در یک پلتفرم",
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            // فاصله تا نشانگر بارگذاری قرار می‌گیرد.
            Spacer(Modifier.height(28.dp))
            // نشانگر بارگذاری کوچک نمایش داده می‌شود.
            androidx.compose.material3.CircularProgressIndicator(
                Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

// صفحه ورود، ثبت‌نام و ورود به حالت مهمان را نمایش می‌دهد.
@Composable
private fun V12Auth(
    onLogin: (String, String) -> Boolean,
    onRegister: (String, String, String) -> Boolean,
    onSkip: () -> Unit
) {
    // حالت فعلی فرم بین ورود و ثبت‌نام نگهداری می‌شود.
    var loginMode by remember { mutableStateOf(true) }
    // نام برای فرم ثبت‌نام نگهداری می‌شود.
    var name by remember { mutableStateOf("") }
    // شماره موبایل یا شناسه ورود نگهداری می‌شود.
    var identifier by remember { mutableStateOf("") }
    // رمز عبور فرم نگهداری می‌شود.
    var password by remember { mutableStateOf("") }
    // پیام خطا در صورت نامعتبر بودن فرم نگهداری می‌شود.
    var error by remember { mutableStateOf<String?>(null) }

    // کل فرم روی پس‌زمینه گرادیانی تیره قرار می‌گیرد.
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF10223D), Bg))
        )
    ) {
        // ستون اصلی فرم در مرکز صفحه قرار می‌گیرد.
        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // لوگوی کوچک بالای فرم نمایش داده می‌شود.
            RobotLogo(88)
            // فاصله بعد از لوگو ایجاد می‌شود.
            Spacer(Modifier.height(17.dp))
            // عنوان فرم نمایش داده می‌شود.
            Text("ورود به App BotStore", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            // متن راهنما نمایش داده می‌شود.
            Text(
                "برای اتصال ربات باید حساب کاربری داشته باشید.",
                color = TextMuted,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
            // فاصله قبل از کارت فرم قرار می‌گیرد.
            Spacer(Modifier.height(22.dp))

            // کارت اصلی شامل Tabها و فیلدهای فرم است.
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // محتوای کارت با padding استاندارد چیده می‌شود.
                Column(Modifier.padding(19.dp)) {
                    // دو دکمه برای تغییر حالت ورود و ثبت‌نام قرار می‌گیرند.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // دکمه ورود حالت loginMode را فعال می‌کند.
                        Button(
                            onClick = {
                                loginMode = true
                                error = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (loginMode) Blue else Surface2
                            )
                        ) { Text("ورود") }

                        // دکمه ثبت‌نام حالت loginMode را غیرفعال می‌کند.
                        Button(
                            onClick = {
                                loginMode = false
                                error = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!loginMode) Blue else Surface2
                            )
                        ) { Text("ثبت نام") }
                    }

                    // فاصله تا فیلدها قرار می‌گیرد.
                    Spacer(Modifier.height(14.dp))

                    // فیلد نام فقط در حالت ثبت‌نام نمایش داده می‌شود.
                    if (!loginMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام و نام خانوادگی") },
                            leadingIcon = { Icon(Icons.Outlined.Person, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(9.dp))
                    }

                    // شناسه ورود یا شماره موبایل دریافت می‌شود.
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it.trim()
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(if (loginMode) "شماره موبایل یا نام کاربری" else "شماره موبایل")
                        },
                        leadingIcon = { Icon(Icons.Outlined.AccountCircle, null) },
                        singleLine = true
                    )
                    // فاصله بین فیلدها ایجاد می‌شود.
                    Spacer(Modifier.height(9.dp))

                    // رمز عبور به‌صورت مخفی دریافت می‌شود.
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رمز عبور") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    // خطای اعتبارسنجی زیر فیلدها نمایش داده می‌شود.
                    error?.let { message ->
                        Text(
                            message,
                            color = Danger,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // فاصله تا دکمه اصلی قرار می‌گیرد.
                    Spacer(Modifier.height(14.dp))

                    // دکمه اصلی عملیات ورود یا ثبت‌نام را اجرا می‌کند.
                    Button(
                        onClick = {
                            // بر اساس حالت فرم callback مناسب فراخوانی می‌شود.
                            val ok = if (loginMode) {
                                onLogin(identifier, password)
                            } else {
                                onRegister(name, identifier, password)
                            }
                            // در صورت شکست پیام مناسب نمایش داده می‌شود.
                            if (!ok) {
                                error = if (loginMode) {
                                    "اطلاعات ورود صحیح نیست."
                                } else {
                                    "اطلاعات ثبت نام را کامل وارد کنید."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        // آیکون متناسب با حالت فرم انتخاب می‌شود.
                        Icon(if (loginMode) Icons.Outlined.Login else Icons.Outlined.PersonAdd, null)
                        // فاصله بین آیکون و متن قرار می‌گیرد.
                        Spacer(Modifier.width(7.dp))
                        // متن دکمه متناسب با حالت فرم نمایش داده می‌شود.
                        Text(if (loginMode) "ورود" else "ساخت حساب")
                    }
                }
            }

            // فاصله تا گزینه مهمان قرار می‌گیرد.
            Spacer(Modifier.height(10.dp))
            // کاربر می‌تواند ثبت‌نام را برای بعد بگذارد.
            TextButton(onClick = onSkip) {
                Text("رد کردن / بعداً ثبت نام می‌کنم", color = TextMuted)
            }
            // محدودیت حالت مهمان به‌صورت شفاف نمایش داده می‌شود.
            Text(
                "در حالت مهمان امکان اتصال ربات وجود ندارد.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

// APIهای آزمایشی Material3 برای TopAppBar و Drawer فعال می‌شوند.
@OptIn(ExperimentalMaterial3Api::class)
// این Composable پوسته اصلی شامل Drawer، AppBar، BottomBar و صفحات را می‌سازد.
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
    onProfileUpdate: (String, String) -> Boolean,
    onLogout: () -> Unit
) {
    // state منوی همبرگری ایجاد می‌شود.
    val drawer = rememberDrawerState(DrawerValue.Closed)
    // scope برای باز و بسته کردن Drawer و Snackbar ساخته می‌شود.
    val scope = rememberCoroutineScope()
    // Context برای Share Sheet استفاده می‌شود.
    val context = LocalContext.current
    // SnackbarHostState برای پیام‌های کوتاه ساخته می‌شود.
    val snack = remember { SnackbarHostState() }

    // اگر Drawer باز باشد، Back سیستم ابتدا Drawer را می‌بندد و صفحه را عوض نمی‌کند.
    BackHandler(enabled = drawer.isOpen) {
        scope.launch { drawer.close() }
    }

    // ModalNavigationDrawer در محیط RTL از سمت راست باز می‌شود.
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            // پنل Drawer با عرض ثابت و رنگ تیره ساخته می‌شود.
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0C1727),
                modifier = Modifier.width(315.dp)
            ) {
                // اطلاعات حساب در بالای منو نمایش داده می‌شود.
                DrawerHeader(userName, userPhone, loggedIn, admin)
                // خط جداکننده زیر هدر قرار می‌گیرد.
                HorizontalDivider(color = Color.White.copy(alpha = .07f))

                // صفحه خانه از منوی همبرگری قابل دسترس است.
                DrawerItem(Icons.Outlined.Home, "خانه", page == V12Page.DASHBOARD) {
                    onPage(V12Page.DASHBOARD)
                    scope.launch { drawer.close() }
                }
                // بخش ربات‌های من کاربر را به داشبورد مدیریت ربات‌ها برمی‌گرداند.
                DrawerItem(Icons.Outlined.SmartToy, "ربات‌های من", page == V12Page.BOT_MANAGER) {
                    if (selectedBot != null) onPage(V12Page.BOT_MANAGER) else onPage(V12Page.DASHBOARD)
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
                // پیش‌نمایش ربات از منوی اصلی در دسترس قرار می‌گیرد.
                DrawerItem(Icons.Outlined.Visibility, "پیش‌نمایش ربات", page == V12Page.PREVIEW) {
                    onPage(V12Page.PREVIEW)
                    scope.launch { drawer.close() }
                }
                // صفحه اشتراک ربات‌ها باز می‌شود.
                DrawerItem(
                    Icons.Outlined.WorkspacePremium,
                    "اشتراک ربات‌ها",
                    page == V12Page.SUBSCRIPTIONS
                ) {
                    onPage(V12Page.SUBSCRIPTIONS)
                    scope.launch { drawer.close() }
                }
                // قابلیت آینده همگام‌سازی نمایش داده می‌شود.
                DrawerItem(Icons.Outlined.Sync, "همگام‌سازی ربات‌ها", page == V12Page.SYNC) {
                    onPage(V12Page.SYNC)
                    scope.launch { drawer.close() }
                }
                // تنظیمات برنامه باز می‌شود.
                DrawerItem(Icons.Outlined.Settings, "تنظیمات", page == V12Page.SETTINGS) {
                    onPage(V12Page.SETTINGS)
                    scope.launch { drawer.close() }
                }
                // Share Sheet سیستم برای معرفی برنامه باز می‌شود.
                DrawerItem(Icons.Outlined.Share, "معرفی به دوستان", false) {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "App BotStore - ربات ساز شخصی شبکه های اجتماعی شما در یک پلتفرم"
                                )
                            },
                            "اشتراک‌گذاری"
                        )
                    )
                }
                // صفحه درباره ما باز می‌شود.
                DrawerItem(Icons.Outlined.Info, "درباره ما", page == V12Page.ABOUT) {
                    onPage(V12Page.ABOUT)
                    scope.launch { drawer.close() }
                }
                // صفحه تماس با ما باز می‌شود.
                DrawerItem(Icons.Outlined.Email, "تماس با ما", page == V12Page.CONTACT) {
                    onPage(V12Page.CONTACT)
                    scope.launch { drawer.close() }
                }
                // صفحه درباره نرم‌افزار فقط توضیح کوتاه و نسخه را نمایش می‌دهد.
                DrawerItem(Icons.Outlined.Android, "درباره نرم‌افزار", page == V12Page.APP_INFO) {
                    onPage(V12Page.APP_INFO)
                    scope.launch { drawer.close() }
                }

                // Spacer گزینه حساب را پایین Drawer نگه می‌دارد.
                Spacer(Modifier.weight(1f))

                // بسته به وضعیت حساب، خروج یا ورود/ثبت‌نام نمایش داده می‌شود.
                DrawerItem(
                    if (loggedIn) Icons.Outlined.Logout else Icons.Outlined.Login,
                    if (loggedIn) "خروج از حساب" else "ورود / ثبت نام",
                    false
                ) {
                    if (loggedIn) onLogout() else onNeedAuth()
                    scope.launch { drawer.close() }
                }

                // نام برنامه و نسخه در پایین Drawer نمایش داده می‌شوند.
                Text(
                    "App BotStore • v${BuildConfig.VERSION_NAME}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(22.dp)
                )
            }
        }
    ) {
        // Scaffold نوار بالا، پایین، Snackbar و محتوای صفحه را کنار هم قرار می‌دهد.
        Scaffold(
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                // عنوان صفحه جاری در AppBar نمایش داده می‌شود.
                CenterAlignedTopAppBar(
                    title = { Text(page.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        // در محیط RTL این navigationIcon در سمت راست قرار می‌گیرد و Drawer راست را باز می‌کند.
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Filled.Menu, "منوی همبرگری")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Bg)
                )
            },
            bottomBar = {
                // سه مقصد اصلی همیشه در NavigationBar پایین برنامه قابل دسترس‌اند.
                NavigationBar(containerColor = Surface) {
                    BottomNavItem(V12Page.DASHBOARD, page, Icons.Outlined.Home, onPage)
                    BottomNavItem(
                        V12Page.SUBSCRIPTIONS,
                        page,
                        Icons.Outlined.WorkspacePremium,
                        onPage
                    )
                    BottomNavItem(V12Page.SETTINGS, page, Icons.Outlined.Settings, onPage)
                }
            }
        ) { pad ->
            // محتوای صفحه با padding خود Scaffold رندر می‌شود.
            Box(Modifier.padding(pad).fillMaxSize()) {
                // Composable متناظر با صفحه جاری انتخاب می‌شود.
                when (page) {
                    // داشبورد اصلی و مدیریت پلتفرم‌ها نمایش داده می‌شود.
                    V12Page.DASHBOARD -> V12Dashboard(
                        bots,
                        loggedIn,
                        admin,
                        userName,
                        onNeedAuth,
                        onSelectBot,
                        { onPage(V12Page.SUBSCRIPTIONS) }
                    ) { platform ->
                        // در نسخه فعلی فقط اتصال تلگرام عملیاتی است.
                        when {
                            platform != BotPlatform.TELEGRAM -> scope.launch {
                                snack.showSnackbar("اتصال ${platform.faName} به‌زودی فعال می‌شود.")
                            }
                            !loggedIn -> onNeedAuth()
                            admin -> onPage(V12Page.CONNECT)
                            else -> onPage(V12Page.SUBSCRIPTIONS)
                        }
                    }

                    // صفحه پلن‌ها و خرید اشتراک نمایش داده می‌شود.
                    V12Page.SUBSCRIPTIONS -> V12Subscriptions(
                        admin,
                        loggedIn,
                        onNeedAuth,
                        onSelectPlan
                    ) { onPage(V12Page.CONNECT) }

                    // فرم اتصال BotFather Token نمایش داده می‌شود.
                    V12Page.CONNECT -> V12Connect(
                        admin,
                        selectedPlan,
                        { onPage(V12Page.SUBSCRIPTIONS) },
                        onBotConnected
                    )

                    // صفحه مدیریت ربات انتخاب‌شده نمایش داده می‌شود.
                    V12Page.BOT_MANAGER -> selectedBot?.let { bot ->
                        V12BotManager(
                            bot,
                            onBotUpdate,
                            onBotDelete,
                            { onPage(V12Page.PRODUCTS) },
                            { onPage(V12Page.CATEGORIES) },
                            { onPage(V12Page.PREVIEW) }
                        )
                    } ?: EmptyState(
                        "رباتی انتخاب نشده",
                        "از داشبورد یک ربات را انتخاب کنید.",
                        Icons.Outlined.Android
                    )

                    // مدیریت محصولات فروشگاه نمایش داده می‌شود.
                    V12Page.PRODUCTS -> V12Products(products, categories, onProducts)
                    // مدیریت دسته‌بندی‌ها نمایش داده می‌شود.
                    V12Page.CATEGORIES -> V12Categories(categories, onCategories)
                    // پیش‌نمایش منوی ربات نمایش داده می‌شود.
                    V12Page.PREVIEW -> V12Preview(selectedBot, categories, products)
                    // تنظیمات، حساب، اعلان‌ها و بروزرسانی نمایش داده می‌شوند.
                    V12Page.SETTINGS -> V12Settings(
                        loggedIn,
                        admin,
                        userName,
                        userPhone,
                        notifications,
                        onNotifications,
                        onNeedAuth,
                        onLogout,
                        onProfileUpdate
                    )
                    // صفحه Coming Soon همگام‌سازی نمایش داده می‌شود.
                    V12Page.SYNC -> V12SyncSoon()
                    // متن درباره ما مطابق ساختار ثابت AS Team نمایش داده می‌شود.
                    V12Page.ABOUT -> V12Info(
                        "گروه توسعه و برنامه نویسی AS Team",
                        "تمامی حقوق مربوط به این برنامه انحصاری میباشد"
                    )
                    // اطلاعات پشتیبانی نمایش داده می‌شود.
                    V12Page.CONTACT -> V12Info(
                        "گروه توسعه و برنامه نویسی AS Team",
                        "ایمیل پشتیبانی\nas.team.support@gmail.com"
                    )
                    // درباره نرم‌افزار فقط توضیح کوتاه و شماره نسخه را نمایش می‌دهد؛ اطلاعات فنی بسته عمداً نشان داده نمی‌شود.
                    V12Page.APP_INFO -> V12Info(
                        "App BotStore",
                        "App BotStore برای ساخت، اتصال و مدیریت ربات‌های فروشگاهی و خدماتی طراحی شده است.\n\nاز داخل برنامه می‌توانید ربات، محصولات، دسته‌بندی‌ها و تنظیمات اصلی فروشگاه را مدیریت کنید.\n\nنسخه ${BuildConfig.VERSION_NAME}"
                    )
                }
            }
        }
    }
}
