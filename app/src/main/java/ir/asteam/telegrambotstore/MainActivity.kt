package ir.asteam.telegrambotstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.launch

private val TgBlue = Color(0xFF229ED9)
private val Bg = Color(0xFF0B1220)
private val Surface = Color(0xFF111B2D)
private val Surface2 = Color(0xFF17243A)
private val TextMuted = Color(0xFF94A3B8)
private val Green = Color(0xFF22C55E)

enum class Page(val title: String) {
    DASHBOARD("داشبورد"), PRODUCTS("محصولات"), CATEGORIES("دسته‌بندی‌ها"), ORDERS("سفارش‌ها"),
    USERS("کاربران"), PREVIEW("پیش‌نمایش ربات"), SETTINGS("تنظیمات"), ABOUT("درباره ما"),
    CONTACT("تماس با ما"), APP_INFO("درباره نرم‌افزار")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TelegramStoreApp() }
    }
}

@Composable
fun TelegramStoreApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var token by remember { mutableStateOf(store.token) }
    var botUsername by remember { mutableStateOf(store.botUsername) }
    var botName by remember { mutableStateOf(store.botName) }
    var products by remember { mutableStateOf(store.loadProducts()) }
    var categories by remember { mutableStateOf(store.loadCategories()) }
    var page by remember { mutableStateOf(Page.DASHBOARD) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = TgBlue,
                background = Bg,
                surface = Surface,
                surfaceVariant = Surface2,
                onBackground = Color.White,
                onSurface = Color.White
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                if (token.isBlank()) {
                    ConnectBotScreen(onConnected = { newToken, info ->
                        store.token = newToken
                        store.botUsername = info.username
                        token = newToken
                        botUsername = info.username
                    })
                } else {
                    MainShell(
                        page = page,
                        onPage = { page = it },
                        botName = botName,
                        botUsername = botUsername,
                        products = products,
                        categories = categories,
                        onBotNameChange = { botName = it; store.botName = it },
                        onProductsChange = { products = it; store.saveProducts(it) },
                        onCategoriesChange = { categories = it; store.saveCategories(it) },
                        notificationsEnabled = store.notificationsEnabled,
                        onNotifications = { store.notificationsEnabled = it },
                        onDisconnect = { store.clearBot(); token = ""; botUsername = ""; page = Page.DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectBotScreen(onConnected: (String, TelegramBotInfo) -> Unit) {
    var token by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF132238), Bg, Color(0xFF07101D)))
        ).padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(92.dp).background(TgBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Send, null, modifier = Modifier.size(46.dp), tint = Color.White)
            }
            Spacer(Modifier.height(24.dp))
            Text("فروشگاه‌ساز تلگرام", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("ربات فروشگاهی خودت را از همین‌جا مدیریت کن", color = TextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("اتصال ربات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("توکن رباتی که در BotFather ساخته‌ای را وارد کن.", color = TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it.trim(); error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bot Token") },
                        placeholder = { Text("123456789:AA...") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Outlined.Key, null) },
                        singleLine = true,
                        isError = error != null
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                TelegramApi.validateToken(token).fold(
                                    onSuccess = { loading = false; onConnected(token, it) },
                                    onFailure = { loading = false; error = it.message ?: "اتصال برقرار نشد" }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = token.isNotBlank() && !loading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        else { Icon(Icons.Filled.Link, null); Spacer(Modifier.width(8.dp)); Text("اتصال و بررسی ربات") }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, null, tint = Green, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("توکن فقط برای مدیریت ربات شما ذخیره می‌شود", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    page: Page,
    onPage: (Page) -> Unit,
    botName: String,
    botUsername: String,
    products: List<StoreProduct>,
    categories: List<StoreCategory>,
    onBotNameChange: (String) -> Unit,
    onProductsChange: (List<StoreProduct>) -> Unit,
    onCategoriesChange: (List<StoreCategory>) -> Unit,
    notificationsEnabled: Boolean,
    onNotifications: (Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0E192A),
                modifier = Modifier.width(310.dp)
            ) {
                DrawerHeader(botName, botUsername)
                HorizontalDivider(color = Color.White.copy(alpha = .08f))
                DrawerItem(Icons.Outlined.Home, "خانه", page == Page.DASHBOARD) { onPage(Page.DASHBOARD); scope.launch { drawerState.close() } }
                DrawerItem(Icons.Outlined.Settings, "تنظیمات", page == Page.SETTINGS) { onPage(Page.SETTINGS); scope.launch { drawerState.close() } }
                DrawerItem(Icons.Outlined.Share, "معرفی به دوستان", false) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "فروشگاه من در تلگرام: https://t.me/$botUsername")
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                }
                DrawerItem(Icons.Outlined.Info, "درباره ما", page == Page.ABOUT) { onPage(Page.ABOUT); scope.launch { drawerState.close() } }
                DrawerItem(Icons.Outlined.Email, "تماس با ما", page == Page.CONTACT) { onPage(Page.CONTACT); scope.launch { drawerState.close() } }
                DrawerItem(Icons.Outlined.Android, "درباره نرم‌افزار", page == Page.APP_INFO) { onPage(Page.APP_INFO); scope.launch { drawerState.close() } }
                Spacer(Modifier.weight(1f))
                Text("AS Team • v${BuildConfig.VERSION_NAME}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(22.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(page.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, "منوی همبرگری")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Bg)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Surface) {
                    BottomNavItem(Page.DASHBOARD, page, Icons.Outlined.Home, onPage)
                    BottomNavItem(Page.PRODUCTS, page, Icons.Outlined.Inventory2, onPage)
                    BottomNavItem(Page.ORDERS, page, Icons.Outlined.ReceiptLong, onPage)
                    BottomNavItem(Page.PREVIEW, page, Icons.Outlined.SmartToy, onPage)
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Page.DASHBOARD -> Dashboard(botName, botUsername, products, categories, onPage)
                    Page.PRODUCTS -> ProductsScreen(products, categories, onProductsChange)
                    Page.CATEGORIES -> CategoriesScreen(categories, onCategoriesChange)
                    Page.ORDERS -> EmptyFeature("هنوز سفارشی ثبت نشده", "وقتی فروشگاه به بک‌اند متصل شود، سفارش‌های واقعی اینجا نمایش داده می‌شوند.", Icons.Outlined.ReceiptLong)
                    Page.USERS -> EmptyFeature("کاربران فروشگاه", "لیست کاربران و سابقه خرید بعد از اتصال سرویس ربات در این بخش قرار می‌گیرد.", Icons.Outlined.Groups)
                    Page.PREVIEW -> PreviewScreen(botName, botUsername, categories, products)
                    Page.SETTINGS -> SettingsScreen(botName, onBotNameChange, notificationsEnabled, onNotifications, onDisconnect)
                    Page.ABOUT -> CenterInfo("گروه توسعه و برنامه نویسی AS Team", "تمامی حقوق مربوط به این برنامه انحصاری میباشد")
                    Page.CONTACT -> CenterInfo("گروه توسعه و برنامه نویسی AS Team", "ایمیل پشتیبانی\nas.team.support@gmail.com")
                    Page.APP_INFO -> AppInfoScreen()
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(botName: String, username: String) {
    Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).background(TgBlue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Send, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(botName, fontWeight = FontWeight.Bold)
            Text(if (username.isBlank()) "ربات متصل" else "@$username", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(text) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = TgBlue.copy(alpha = .18f))
    )
}

@Composable
private fun RowScope.BottomNavItem(target: Page, current: Page, icon: ImageVector, onPage: (Page) -> Unit) {
    NavigationBarItem(
        selected = target == current,
        onClick = { onPage(target) },
        icon = { Icon(icon, null) },
        label = { Text(target.title, fontSize = 10.sp) }
    )
}

@Composable
private fun Dashboard(botName: String, username: String, products: List<StoreProduct>, categories: List<StoreCategory>, onPage: (Page) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(TgBlue, Color(0xFF1577B5)))).fillMaxWidth().padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).background(Color.White.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SmartToy, null)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(botName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text(if (username.isBlank()) "ربات فعال" else "@$username", color = Color.White.copy(alpha = .78f))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).background(Green, CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text("اتصال تلگرام برقرار است", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("محصول", products.size.toString(), Icons.Outlined.Inventory2, Modifier.weight(1f))
                StatCard("دسته‌بندی", categories.size.toString(), Icons.Outlined.Category, Modifier.weight(1f))
                StatCard("سفارش", "۰", Icons.Outlined.ReceiptLong, Modifier.weight(1f))
            }
        }
        item { Text("مدیریت فروشگاه", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard("محصولات", "افزودن و ویرایش کالاهای فروشگاه", Icons.Outlined.Inventory2, TgBlue) { onPage(Page.PRODUCTS) }
                ActionCard("دسته‌بندی‌ها", "ساخت منوی مرتب برای محصولات", Icons.Outlined.Category, Color(0xFFA855F7)) { onPage(Page.CATEGORIES) }
                ActionCard("سفارش‌ها", "مشاهده و مدیریت خرید مشتری‌ها", Icons.Outlined.ReceiptLong, Color(0xFFF59E0B)) { onPage(Page.ORDERS) }
                ActionCard("کاربران", "مشتری‌ها، کیف پول و سوابق", Icons.Outlined.Groups, Color(0xFF22C55E)) { onPage(Page.USERS) }
                ActionCard("پیش‌نمایش ربات", "ظاهر منوی فروشگاه را قبل از انتشار ببین", Icons.Outlined.Visibility, Color(0xFF06B6D4)) { onPage(Page.PREVIEW) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp)) {
            Icon(icon, null, tint = TgBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(accent.copy(alpha = .16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted)
        }
    }
}

@Composable
private fun ProductsScreen(products: List<StoreProduct>, categories: List<StoreCategory>, onChange: (List<StoreProduct>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (products.isEmpty()) {
            EmptyFeature("هنوز محصولی نداری", "اولین محصول فروشگاه را بساز و قیمت و دسته‌بندی‌اش را مشخص کن.", Icons.Outlined.Inventory2)
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(products, key = { it.id }) { product ->
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(TgBlue.copy(alpha = .14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.ShoppingBag, null, tint = TgBlue)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.title, fontWeight = FontWeight.Bold)
                                Text("${product.price.toPersianNumber()} تومان", color = TgBlue, fontSize = 13.sp)
                                if (product.category.isNotBlank()) Text(product.category, color = TextMuted, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onChange(products.filterNot { it.id == product.id }) }) {
                                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFF87171))
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp), containerColor = TgBlue) {
            Icon(Icons.Filled.Add, "افزودن محصول")
        }
    }
    if (showAdd) ProductDialog(categories, onDismiss = { showAdd = false }) {
        onChange(products + it); showAdd = false
    }
}

@Composable
private fun ProductDialog(categories: List<StoreCategory>, onDismiss: () -> Unit, onAdd: (StoreProduct) -> Unit) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull()?.title ?: "") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("محصول جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("نام محصول") }, singleLine = true)
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("قیمت (تومان)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("دسته‌بندی") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(StoreProduct(title = title.trim(), price = price.toLongOrNull() ?: 0, category = category.trim(), description = description.trim())) }, enabled = title.isNotBlank() && price.toLongOrNull() != null) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun CategoriesScreen(categories: List<StoreCategory>, onChange: (List<StoreCategory>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (categories.isEmpty()) EmptyFeature("دسته‌بندی بساز", "مثلاً پوشاک، دیجیتال، خدمات یا هر دسته‌ای که فروشگاهت نیاز دارد.", Icons.Outlined.Category)
        else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories, key = { it.id }) { cat ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.emoji, fontSize = 27.sp)
                        Spacer(Modifier.width(12.dp)); Text(cat.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onChange(categories.filterNot { it.id == cat.id }) }) { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFF87171)) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp), containerColor = TgBlue) { Icon(Icons.Filled.Add, null) }
    }
    if (showAdd) {
        var title by remember { mutableStateOf("") }; var emoji by remember { mutableStateOf("🛍️") }
        AlertDialog(
            onDismissRequest = { showAdd = false }, title = { Text("دسته‌بندی جدید") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("نام دسته‌بندی") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it }, label = { Text("ایموجی") }, singleLine = true)
            } },
            confirmButton = { Button(onClick = { onChange(categories + StoreCategory(title = title.trim(), emoji = emoji.ifBlank { "🛍️" })); showAdd = false }, enabled = title.isNotBlank()) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun PreviewScreen(botName: String, username: String, categories: List<StoreCategory>, products: List<StoreProduct>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A26)), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF17212B)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(TgBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.SmartToy, null) }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(botName, fontWeight = FontWeight.Bold); Text(if (username.isBlank()) "bot" else "@$username", color = TextMuted, fontSize = 11.sp) }
                    }
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.End) {
                        Surface(color = Color(0xFF182533), shape = RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("سلام 👋\nبه $botName خوش آمدید.")
                                Spacer(Modifier.height(7.dp)); Text("یکی از گزینه‌های زیر را انتخاب کنید:", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val buttons = if (categories.isEmpty()) listOf("🛍️ محصولات", "🧾 سفارش‌های من", "💰 کیف پول", "☎️ پشتیبانی") else categories.map { "${it.emoji} ${it.title}" }
                        buttons.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                row.forEach { text ->
                                    Surface(Modifier.weight(1f).padding(vertical = 3.dp), color = Color(0xFF26394C), shape = RoundedCornerShape(8.dp)) {
                                        Text(text, textAlign = TextAlign.Center, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item { Text("${products.size.toPersianNumber()} محصول و ${categories.size.toPersianNumber()} دسته‌بندی در پیش‌نمایش", color = TextMuted, fontSize = 12.sp) }
    }
}

@Composable
private fun SettingsScreen(botName: String, onBotNameChange: (String) -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, onDisconnect: () -> Unit) {
    var name by remember(botName) { mutableStateOf(botName) }
    var checking by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingCard("مشخصات فروشگاه", Icons.Outlined.Storefront) {
                OutlinedTextField(name, { name = it }, label = { Text("نام فروشگاه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp)); Button(onClick = { onBotNameChange(name.trim()) }, enabled = name.isNotBlank()) { Text("ذخیره نام") }
            }
        }
        item {
            SettingCard("اعلان‌ها", Icons.Outlined.Notifications) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("اعلان‌های برنامه", fontWeight = FontWeight.Medium); Text("سفارش و بروزرسانی‌ها", color = TextMuted, fontSize = 12.sp) }
                    Switch(checked = notifications, onCheckedChange = onNotifications)
                }
            }
        }
        item {
            SettingCard("بروزرسانی", Icons.Outlined.SystemUpdate) {
                Text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = {
                    checking = true; updateMessage = null
                    scope.launch {
                        UpdateChecker.check().fold(
                            onSuccess = { info ->
                                checking = false
                                if (info == null) updateMessage = "آخرین نسخه نصب است."
                                else {
                                    updateMessage = "نسخه ${info.latestVersion} موجود است."
                                    if (info.downloadUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                                }
                            },
                            onFailure = { checking = false; updateMessage = it.message ?: "بررسی بروزرسانی ناموفق بود" }
                        )
                    }
                }, enabled = !checking) {
                    if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Refresh, null)
                    Spacer(Modifier.width(7.dp)); Text("بررسی نسخه جدید")
                }
                if (updateMessage != null) Text(updateMessage!!, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            SettingCard("اتصال ربات", Icons.Outlined.LinkOff) {
                Text("با قطع اتصال، توکن این ربات از گوشی حذف می‌شود. محصولات محلی پاک نمی‌شوند.", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onDisconnect) { Icon(Icons.Outlined.LinkOff, null); Spacer(Modifier.width(7.dp)); Text("قطع اتصال ربات") }
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = TgBlue); Spacer(Modifier.width(9.dp)); Text(title, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(14.dp)); content()
        }
    }
}

@Composable
private fun EmptyFeature(title: String, subtitle: String, icon: ImageVector) {
    Column(Modifier.fillMaxSize().padding(34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(82.dp).background(TgBlue.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = TgBlue, modifier = Modifier.size(39.dp)) }
        Spacer(Modifier.height(18.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Text(subtitle, color = TextMuted, textAlign = TextAlign.Center, lineHeight = 21.sp)
    }
}

@Composable
private fun CenterInfo(title: String, body: String) {
    Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(76.dp).background(TgBlue.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Code, null, tint = TgBlue, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(20.dp)); Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp)); Text(body, color = TextMuted, lineHeight = 24.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AppInfoScreen() {
    CenterInfo("فروشگاه‌ساز تلگرام", "نسخه ${BuildConfig.VERSION_NAME}\n\nابزار مدیریت فروشگاه و ربات تلگرامی برای ساخت دسته‌بندی، محصول، سفارش و مدیریت کاربران.")
}

private fun Long.toPersianNumber(): String = String.format("%,d", this).map { c -> when (c) { '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'; '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'; else -> c } }.joinToString("")
private fun Int.toPersianNumber(): String = toLong().toPersianNumber()
