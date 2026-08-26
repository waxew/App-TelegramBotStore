package ir.asteam.telegrambotstore

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun V12Dashboard(
    bots: List<ConnectedBot>,
    loggedIn: Boolean,
    admin: Boolean,
    userName: String,
    onNeedAuth: () -> Unit,
    onSelectBot: (ConnectedBot) -> Unit,
    onSubscriptions: () -> Unit,
    onPlatform: (BotPlatform) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF3157D5), Color(0xFF7048E8)))).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RobotLogo(54)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (loggedIn) "سلام ${userName.ifBlank { "کاربر" }}" else "حالت مهمان", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    when {
                                        admin -> "Administrator • دسترسی نامحدود"
                                        loggedIn -> "${bots.count { it.active }.toPersian()} ربات فعال"
                                        else -> "برای اتصال ربات وارد حساب شوید"
                                    },
                                    color = Color.White.copy(alpha = .78f), fontSize = 11.sp
                                )
                            }
                            if (admin) StatusPill("نامحدود", Success)
                        }
                        if (!loggedIn) {
                            Spacer(Modifier.height(13.dp))
                            OutlinedButton(onClick = onNeedAuth, border = BorderStroke(1.dp, Color.White.copy(alpha = .35f))) {
                                Icon(Icons.Outlined.Login, null); Spacer(Modifier.width(6.dp)); Text("ورود / ثبت نام", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("پلتفرم‌های ربات") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlatformBox(BotPlatform.TELEGRAM, Icons.Filled.Send, TelegramBlue, bots.count { it.platform == BotPlatform.TELEGRAM }, false, Modifier.weight(1f)) { onPlatform(BotPlatform.TELEGRAM) }
                PlatformBox(BotPlatform.WHATSAPP, Icons.Filled.PhoneInTalk, WhatsAppGreen, bots.count { it.platform == BotPlatform.WHATSAPP }, true, Modifier.weight(1f)) { onPlatform(BotPlatform.WHATSAPP) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlatformBox(BotPlatform.RUBIKA, Icons.Outlined.Language, RubikaPurple, bots.count { it.platform == BotPlatform.RUBIKA }, true, Modifier.weight(1f)) { onPlatform(BotPlatform.RUBIKA) }
                PlatformBox(BotPlatform.BALE, Icons.Filled.Chat, BaleGreen, bots.count { it.platform == BotPlatform.BALE }, true, Modifier.weight(1f)) { onPlatform(BotPlatform.BALE) }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("ربات‌های متصل", Modifier.weight(1f))
                TextButton(onClick = onSubscriptions) { Text("مشاهده اشتراک‌ها") }
            }
        }

        if (bots.none { it.active }) {
            item { EmptyCard("هیچ ربات فعالی ندارید", "برای شروع، ربات تلگرام خود را متصل کنید.", Icons.Filled.SmartToy) }
        } else {
            items(bots.filter { it.active }, key = { it.id }) { bot -> BotCard(bot) { onSelectBot(bot) } }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171B36)),
                border = BorderStroke(1.dp, RubikaPurple.copy(alpha = .35f)),
                shape = RoundedCornerShape(21.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(RubikaPurple.copy(alpha = .16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Sync, null, tint = RubikaPurple) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("همگام‌سازی بین ربات‌ها", fontWeight = FontWeight.Bold)
                        Text("یک تغییر، همزمان روی همه ربات‌ها", color = TextMuted, fontSize = 11.sp)
                    }
                    StatusPill("به‌زودی", Warning)
                }
            }
        }
    }
}

@Composable
private fun PlatformBox(platform: BotPlatform, icon: ImageVector, accent: Color, count: Int, soon: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(accent.copy(alpha = .15f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
                Spacer(Modifier.weight(1f))
                if (soon) StatusPill("به‌زودی", Warning) else StatusPill(count.toPersian(), accent)
            }
            Spacer(Modifier.height(11.dp))
            Text(platform.faName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(if (count == 0) "بدون ربات فعال" else "${count.toPersian()} ربات متصل", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun BotCard(bot: ConnectedBot, onEdit: () -> Unit) {
    val accent = platformColor(bot.platform)
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(47.dp).background(accent.copy(alpha = .15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(platformIcon(bot.platform), null, tint = accent) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(bot.name, fontWeight = FontWeight.ExtraBold)
                    Text(bot.platform.faName + if (bot.username.isBlank()) "" else " • @${bot.username}", color = TextMuted, fontSize = 10.sp)
                }
                StatusPill(if (bot.active) "فعال" else "غیرفعال", if (bot.active) Success else Danger)
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color.White.copy(alpha = .06f)); Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetaBox("زمان ساخت", LocalStore.formatDate(bot.createdAt), Modifier.weight(1f))
                MetaBox("پایان اشتراک", LocalStore.formatDate(bot.expiresAt), Modifier.weight(1f))
            }
            Spacer(Modifier.height(11.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("ویرایش ربات") }
        }
    }
}

@Composable
private fun MetaBox(title: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, color = Surface2, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(9.dp)) { Text(title, color = TextMuted, fontSize = 9.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
    }
}

@Composable
fun V12Subscriptions(admin: Boolean, loggedIn: Boolean, onNeedAuth: () -> Unit, onChoose: (SubscriptionPlan) -> Unit, onAdminConnect: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp)) {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF7448E8), Color(0xFF355BDF)))).padding(20.dp)) {
                    Column { Icon(Icons.Outlined.WorkspacePremium, null, modifier = Modifier.size(39.dp)); Spacer(Modifier.height(10.dp)); Text("اشتراک برای هر ربات", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold); Text("هر ربات، دوره اشتراک مستقل خودش را دارد.", color = Color.White.copy(alpha = .78f), fontSize = 11.sp) }
                }
            }
        }
        if (admin) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = .10f)), border = BorderStroke(1.dp, Success.copy(alpha = .35f)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Success); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text("دسترسی مدیر نامحدود است", fontWeight = FontWeight.Bold); Text("برای تست نیازی به پرداخت نیست.", color = TextMuted, fontSize = 10.sp) }; TextButton(onClick = onAdminConnect) { Text("اتصال") }
                    }
                }
            }
        }
        items(subscriptionPlans) { plan -> PlanBox(plan) { if (!loggedIn) onNeedAuth() else onChoose(plan) } }
        item {
            Surface(color = Warning.copy(alpha = .08f), shape = RoundedCornerShape(15.dp)) {
                Text("در نسخه ۱.۲ انتخاب پلن و ثبت مدت اشتراک آماده است؛ درگاه پرداخت واقعی در مرحله بعد متصل می‌شود.", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(13.dp))
            }
        }
    }
}

@Composable
private fun PlanBox(plan: SubscriptionPlan, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (plan.badge != null) Color(0xFF151E38) else Surface),
        border = if (plan.badge != null) BorderStroke(1.dp, Blue.copy(alpha = .55f)) else null,
        shape = RoundedCornerShape(21.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Text(plan.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text("برای یک ربات", color = TextMuted, fontSize = 10.sp) }
                plan.badge?.let { StatusPill(it, Blue) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) { Text(plan.price.money(), fontSize = 25.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(5.dp)); Text("تومان", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp)) }
            if (plan.oldPrice != null) {
                Spacer(Modifier.height(5.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text(plan.oldPrice.money() + " تومان", color = TextMuted, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough); Spacer(Modifier.width(8.dp)); StatusPill("${plan.discount}٪ تخفیف".toPersian(), Success) }
            }
            Spacer(Modifier.height(13.dp)); Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text("انتخاب این پلن") }
        }
    }
}

@Composable
fun V12Connect(admin: Boolean, plan: SubscriptionPlan?, onNeedPlan: () -> Unit, onConnected: (ConnectedBot) -> Unit) {
    var token by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Box(Modifier.size(80.dp).background(TelegramBlue.copy(alpha = .16f), RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Send, null, tint = TelegramBlue, modifier = Modifier.size(42.dp)) }
            Spacer(Modifier.height(14.dp)); Text("اتصال ربات تلگرام", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold); Text("توکن BotFather را وارد کنید.", color = TextMuted, fontSize = 11.sp); Spacer(Modifier.height(17.dp))
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    when {
                        admin -> InfoStrip("دسترسی مدیر", "نامحدود و بدون پرداخت", Success)
                        plan != null -> InfoStrip(plan.title, "${plan.price.money()} تومان • ${plan.days.toPersian()} روز", Blue)
                        else -> {
                            InfoStrip("پلن انتخاب نشده", "برای اتصال ربات اشتراک انتخاب کنید.", Warning); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onNeedPlan, modifier = Modifier.fillMaxWidth()) { Text("مشاهده پلن‌ها") }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(token, { token = it.trim(); error = null }, Modifier.fillMaxWidth(), label = { Text("Bot Token") }, leadingIcon = { Icon(Icons.Outlined.Key, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = admin || plan != null, isError = error != null)
                    error?.let { Text(it, color = Danger, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp)) }
                    Spacer(Modifier.height(13.dp))
                    Button(
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                TelegramApi.validateToken(token).fold(
                                    onSuccess = { info ->
                                        val now = System.currentTimeMillis()
                                        val expires = if (admin) 0L else now + ((plan?.days ?: 0) * 86_400_000L)
                                        onConnected(ConnectedBot(platform = BotPlatform.TELEGRAM, name = info.firstName.ifBlank { "ربات تلگرام" }, username = info.username, token = token, createdAt = now, expiresAt = expires, planLabel = if (admin) "مدیر - نامحدود" else plan?.title.orEmpty()))
                                        loading = false
                                    },
                                    onFailure = { loading = false; error = it.message ?: "اتصال برقرار نشد" }
                                )
                            }
                        },
                        enabled = token.isNotBlank() && !loading && (admin || plan != null), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp) else { Icon(Icons.Outlined.Link, null); Spacer(Modifier.width(6.dp)); Text("بررسی و اتصال ربات") }
                    }
                }
            }
        }
    }
}

@Composable
fun V12BotManager(bot: ConnectedBot, onUpdate: (ConnectedBot) -> Unit, onDelete: (ConnectedBot) -> Unit, onProducts: () -> Unit, onCategories: () -> Unit, onPreview: () -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var name by remember(bot.id) { mutableStateOf(bot.name) }
    val accent = platformColor(bot.platform)

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp)) {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent, Color(0xFF3157D5)))).padding(19.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).background(Color.White.copy(alpha = .15f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(platformIcon(bot.platform), null, modifier = Modifier.size(29.dp)) }
                            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(bot.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp); Text(bot.platform.faName + if (bot.username.isBlank()) "" else " • @${bot.username}", color = Color.White.copy(alpha = .8f), fontSize = 11.sp) }; IconButton(onClick = { edit = true }) { Icon(Icons.Filled.Edit, null) }
                        }
                        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { InfoChip("ساخت: ${LocalStore.formatDate(bot.createdAt)}"); InfoChip("پایان: ${LocalStore.formatDate(bot.expiresAt)}") }
                    }
                }
            }
        }
        item { ActionCard("محصولات", "مدیریت محصولات فروشگاه", Icons.Outlined.Inventory2, TelegramBlue, onProducts) }
        item { ActionCard("دسته‌بندی‌ها", "ساخت منو و دسته‌ها", Icons.Outlined.Category, RubikaPurple, onCategories) }
        item { ActionCard("پیش‌نمایش ربات", "نمای تقریبی منوی کاربر", Icons.Outlined.Visibility, Success, onPreview) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.WorkspacePremium, null, tint = Blue); Spacer(Modifier.width(9.dp)); Column { Text("اشتراک ${bot.planLabel.ifBlank { "ثبت نشده" }}", fontWeight = FontWeight.Bold); Text("پایان: ${LocalStore.formatDate(bot.expiresAt)}", color = TextMuted, fontSize = 10.sp) } }
            }
        }
        item { OutlinedButton(onClick = { deleting = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(alpha = .5f))) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("حذف اتصال ربات") } }
    }

    if (edit) AlertDialog(onDismissRequest = { edit = false }, title = { Text("ویرایش نام ربات") }, text = { OutlinedTextField(name, { name = it }, label = { Text("نام نمایشی") }, singleLine = true) }, confirmButton = { Button(onClick = { onUpdate(bot.copy(name = name.trim().ifBlank { bot.name })); edit = false }) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = { edit = false }) { Text("انصراف") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("حذف اتصال؟") }, text = { Text("اطلاعات اتصال این ربات از برنامه حذف می‌شود.") }, confirmButton = { Button(onClick = { onDelete(bot); deleting = false }, colors = ButtonDefaults.buttonColors(containerColor = Danger)) { Text("حذف") } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("انصراف") } })
}

@Composable
fun V12Products(products: List<StoreProduct>, categories: List<StoreCategory>, onChange: (List<StoreProduct>) -> Unit) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (products.isEmpty()) EmptyState("هنوز محصولی ندارید", "اولین محصول فروشگاه را اضافه کنید.", Icons.Outlined.Inventory2)
        else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(products, key = { it.id }) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(45.dp).background(TelegramBlue.copy(alpha = .14f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ShoppingBag, null, tint = TelegramBlue) }
                        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(p.title, fontWeight = FontWeight.Bold); Text(p.price.money() + " تومان", color = TelegramBlue, fontSize = 11.sp); if (p.category.isNotBlank()) Text(p.category, color = TextMuted, fontSize = 9.sp) }; IconButton(onClick = { onChange(products.filterNot { it.id == p.id }) }) { Icon(Icons.Outlined.Delete, null, tint = Danger) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp), containerColor = Blue) { Icon(Icons.Filled.Add, null) }
    }
    if (add) {
        var title by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var category by remember { mutableStateOf(categories.firstOrNull()?.title ?: "") }
        AlertDialog(onDismissRequest = { add = false }, title = { Text("محصول جدید") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("نام محصول") }, singleLine = true); OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("قیمت (تومان)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); OutlinedTextField(category, { category = it }, label = { Text("دسته‌بندی") }, singleLine = true) } }, confirmButton = { Button(onClick = { onChange(products + StoreProduct(title = title.trim(), price = price.toLongOrNull() ?: 0, category = category.trim())); add = false }, enabled = title.isNotBlank() && price.toLongOrNull() != null) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = { add = false }) { Text("انصراف") } })
    }
}

@Composable
fun V12Categories(categories: List<StoreCategory>, onChange: (List<StoreCategory>) -> Unit) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (categories.isEmpty()) EmptyState("هنوز دسته‌بندی ندارید", "برای منوی مرتب دسته‌بندی بسازید.", Icons.Outlined.Category)
        else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(categories, key = { it.id }) { c ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(c.emoji, fontSize = 24.sp); Spacer(Modifier.width(10.dp)); Text(c.title, Modifier.weight(1f), fontWeight = FontWeight.Bold); IconButton(onClick = { onChange(categories.filterNot { it.id == c.id }) }) { Icon(Icons.Outlined.Delete, null, tint = Danger) } }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp), containerColor = Blue) { Icon(Icons.Filled.Add, null) }
    }
    if (add) {
        var title by remember { mutableStateOf("") }; var emoji by remember { mutableStateOf("🛍️") }
        AlertDialog(onDismissRequest = { add = false }, title = { Text("دسته‌بندی جدید") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("نام دسته") }, singleLine = true); OutlinedTextField(emoji, { emoji = it }, label = { Text("ایموجی") }, singleLine = true) } }, confirmButton = { Button(onClick = { onChange(categories + StoreCategory(title = title.trim(), emoji = emoji.ifBlank { "🛍️" })); add = false }, enabled = title.isNotBlank()) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = { add = false }) { Text("انصراف") } })
    }
}

@Composable
fun V12Preview(bot: ConnectedBot?, categories: List<StoreCategory>, products: List<StoreProduct>) {
    if (bot == null) { EmptyState("رباتی انتخاب نشده", "از داشبورد ربات را انتخاب کنید.", Icons.Filled.SmartToy); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A26)), shape = RoundedCornerShape(27.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF17212B)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(41.dp).background(platformColor(bot.platform), CircleShape), contentAlignment = Alignment.Center) { Icon(platformIcon(bot.platform), null) }; Spacer(Modifier.width(9.dp)); Column { Text(bot.name, fontWeight = FontWeight.Bold); Text(if (bot.username.isBlank()) bot.platform.faName else "@${bot.username}", color = TextMuted, fontSize = 9.sp) } }
                    Column(Modifier.padding(15.dp)) {
                        Surface(color = Color(0xFF182533), shape = RoundedCornerShape(16.dp)) { Text("سلام 👋\nبه ${bot.name} خوش آمدید.\nیکی از گزینه‌ها را انتخاب کنید:", modifier = Modifier.padding(13.dp), fontSize = 12.sp) }
                        Spacer(Modifier.height(10.dp))
                        val buttons = if (categories.isEmpty()) listOf("🛍️ محصولات", "🧾 سفارش‌های من", "☎️ پشتیبانی") else categories.map { "${it.emoji} ${it.title}" }
                        buttons.forEach { b -> Surface(color = Color(0xFF23384C), shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(b, textAlign = TextAlign.Center, modifier = Modifier.padding(9.dp), fontSize = 11.sp) } }
                    }
                }
            }
        }
        item { Text("${products.size.toPersian()} محصول • ${categories.size.toPersian()} دسته‌بندی", color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
fun V12Settings(loggedIn: Boolean, admin: Boolean, userName: String, userPhone: String, notifications: Boolean, onNotifications: (Boolean) -> Unit, onLogin: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf("بررسی بروزرسانی") }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { SectionTitle("حساب کاربری") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AccountCircle, null, tint = Blue, modifier = Modifier.size(33.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(if (loggedIn) userName.ifBlank { "کاربر" } else "مهمان", fontWeight = FontWeight.Bold); Text(if (admin) "Administrator • نامحدود" else if (loggedIn) userPhone else "برای اتصال ربات وارد شوید", color = TextMuted, fontSize = 10.sp) }; if (!loggedIn) TextButton(onClick = onLogin) { Text("ورود") } }
            }
        }
        item { SectionTitle("تنظیمات") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Notifications, null, tint = Blue); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("اعلان‌ها", fontWeight = FontWeight.Bold); Text("وضعیت ربات و پایان اشتراک", color = TextMuted, fontSize = 10.sp) }; Switch(notifications, onNotifications) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable {
                scope.launch {
                    update = "در حال بررسی..."
                    UpdateChecker.check().fold(onSuccess = { info -> if (info == null) update = "برنامه بروز است" else { update = "نسخه ${info.latestVersion} موجود است"; if (info.downloadUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))) } }, onFailure = { update = "خطا در بررسی بروزرسانی" })
                }
            }) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Android, null, tint = Success); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("بروزرسانی برنامه", fontWeight = FontWeight.Bold); Text(update, color = TextMuted, fontSize = 10.sp) }; Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted) }
            }
        }
        if (loggedIn) item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(alpha = .45f))) { Icon(Icons.Outlined.Logout, null); Spacer(Modifier.width(6.dp)); Text("خروج از حساب") } }
    }
}

@Composable
fun V12SyncSoon() {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = Surface), border = BorderStroke(1.dp, RubikaPurple.copy(alpha = .35f)), shape = RoundedCornerShape(27.dp)) {
            Column(Modifier.padding(27.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(80.dp).background(RubikaPurple.copy(alpha = .16f), RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Sync, null, tint = RubikaPurple, modifier = Modifier.size(44.dp)) }; Spacer(Modifier.height(17.dp)); Text("همگام‌سازی ربات‌ها", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(7.dp)); Text("در آپدیت بعدی، اطلاعاتی که به یک ربات اضافه می‌کنید می‌تواند همزمان روی ربات‌های دیگر هم اعمال شود.", color = TextMuted, textAlign = TextAlign.Center, fontSize = 11.sp); Spacer(Modifier.height(14.dp)); StatusPill("به‌زودی", Warning) }
        }
    }
}

@Composable
fun V12Info(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { RobotLogo(64); Spacer(Modifier.height(15.dp)); Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center); Spacer(Modifier.height(9.dp)); Text(body, color = TextMuted, textAlign = TextAlign.Center, lineHeight = 21.sp) }
        }
    }
}
