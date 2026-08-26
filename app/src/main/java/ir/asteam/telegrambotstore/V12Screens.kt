// این فایل تمام صفحه‌های اصلی شامل داشبورد، اشتراک، اتصال، مدیریت ربات، Catalog و تنظیمات را می‌سازد.
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

// این object شناسه Bot فعال برای صفحات Catalog را بین صفحه مدیریت و صفحات محصول/دسته نگه می‌دارد.
// داده دائمی داخل LocalStore باقی می‌ماند؛ این object فقط انتخاب لحظه‌ای رابط کاربری است.
private object CatalogSelection {
    // شناسه Bot انتخاب‌شده نگهداری می‌شود و در صورت خالی بودن، صفحات Catalog اولین Bot موجود را fallback می‌کنند.
    var botId: String = ""
}

// داشبورد اصلی برنامه با کارت مستقل برای هر پلتفرم ساخته می‌شود.
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
    // انتخاب Bot در داشبورد علاوه بر callback اصلی، مالک Catalog فعال را نیز مشخص می‌کند.
    val selectBot: (ConnectedBot) -> Unit = { bot ->
        CatalogSelection.botId = bot.id
        onSelectBot(bot)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED))))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RobotLogo(58)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (loggedIn) "سلام ${userName.ifBlank { "کاربر" }}" else "حالت مهمان",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    when {
                                        admin -> "Administrator • دسترسی نامحدود"
                                        loggedIn -> "${bots.count { it.active }.toPersian()} ربات فعال در حساب شما"
                                        else -> "برای اتصال ربات وارد حساب کاربری شوید"
                                    },
                                    color = Color.White.copy(alpha = .82f),
                                    fontSize = 11.sp
                                )
                            }
                            if (admin) StatusPill("نامحدود", Success)
                        }
                        if (!loggedIn) {
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = onNeedAuth,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = .38f))
                            ) {
                                Icon(Icons.Outlined.Login, null)
                                Spacer(Modifier.width(6.dp))
                                Text("ورود / ثبت نام", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("مدیریت ربات‌ها") }

        item {
            val platformBots = bots.filter { it.platform == BotPlatform.TELEGRAM && it.active }
            PlatformSectionCard(
                BotPlatform.TELEGRAM,
                Icons.Filled.Send,
                TelegramBlue,
                platformBots,
                { onPlatform(BotPlatform.TELEGRAM) },
                { platformBots.firstOrNull()?.let(selectBot) ?: onPlatform(BotPlatform.TELEGRAM) },
                selectBot,
                onSubscriptions
            )
        }

        item {
            val platformBots = bots.filter { it.platform == BotPlatform.WHATSAPP && it.active }
            PlatformSectionCard(
                BotPlatform.WHATSAPP,
                Icons.Filled.PhoneInTalk,
                WhatsAppGreen,
                platformBots,
                { onPlatform(BotPlatform.WHATSAPP) },
                { platformBots.firstOrNull()?.let(selectBot) ?: onPlatform(BotPlatform.WHATSAPP) },
                selectBot,
                onSubscriptions
            )
        }

        item {
            val platformBots = bots.filter { it.platform == BotPlatform.RUBIKA && it.active }
            PlatformSectionCard(
                BotPlatform.RUBIKA,
                Icons.Outlined.Language,
                RubikaPurple,
                platformBots,
                { onPlatform(BotPlatform.RUBIKA) },
                { platformBots.firstOrNull()?.let(selectBot) ?: onPlatform(BotPlatform.RUBIKA) },
                selectBot,
                onSubscriptions
            )
        }

        item {
            val platformBots = bots.filter { it.platform == BotPlatform.BALE && it.active }
            PlatformSectionCard(
                BotPlatform.BALE,
                Icons.Filled.Chat,
                BaleGreen,
                platformBots,
                { onPlatform(BotPlatform.BALE) },
                { platformBots.firstOrNull()?.let(selectBot) ?: onPlatform(BotPlatform.BALE) },
                selectBot,
                onSubscriptions
            )
        }

        if (bots.none { it.active }) {
            item {
                EmptyCard(
                    "هیچ ربات فعالی ندارید",
                    "از دکمه اتصال در کارت تلگرام شروع کنید.",
                    Icons.Filled.SmartToy
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C34)),
                border = BorderStroke(1.dp, RubikaPurple.copy(alpha = .34f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).background(RubikaPurple.copy(alpha = .16f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Sync, null, tint = RubikaPurple) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("همگام‌سازی بین ربات‌ها", fontWeight = FontWeight.Bold)
                        Text("کپی انتخابی Catalog بین فروشگاه‌ها در نسخه بعد", color = TextMuted, fontSize = 11.sp)
                    }
                    StatusPill("به‌زودی", Warning)
                }
            }
        }
    }
}

// کارت پلتفرم شامل هدر اتصال/تعداد/تنظیمات و جدول ربات‌ها است.
@Composable
private fun PlatformSectionCard(
    platform: BotPlatform,
    icon: ImageVector,
    accent: Color,
    platformBots: List<ConnectedBot>,
    onConnect: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (ConnectedBot) -> Unit,
    onRenew: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = .30f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(accent.copy(alpha = .10f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).background(accent.copy(alpha = .18f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(platform.faName, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${platformBots.size.toPersian()} ربات فعال", color = TextMuted, fontSize = 10.sp)
                }
                StatusPill("${platformBots.size.toPersian()} فعال", accent)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, "تنظیمات ${platform.faName}", tint = TextMuted)
                }
                FilledTonalButton(
                    onClick = onConnect,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent.copy(alpha = .18f),
                        contentColor = accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Link, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = .06f))

            if (platformBots.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Android, null, tint = accent.copy(alpha = .7f))
                    Spacer(Modifier.width(9.dp))
                    Text("هیچ ربات فعالی در ${platform.faName} ندارید.", color = TextMuted, fontSize = 11.sp)
                }
            } else {
                PlatformTableHeader()
                platformBots.forEachIndexed { index, bot ->
                    PlatformBotRow(bot, accent, { onEdit(bot) }, onRenew)
                    if (index != platformBots.lastIndex) {
                        HorizontalDivider(color = Color.White.copy(alpha = .05f))
                    }
                }
            }
        }
    }
}

// عنوان ستون‌های جدول ربات‌ها نمایش داده می‌شود.
@Composable
private fun PlatformTableHeader() {
    Row(
        Modifier.fillMaxWidth().background(Surface2.copy(alpha = .55f)).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(34.dp))
        Text("نام ربات", color = TextMuted, fontSize = 9.sp, modifier = Modifier.weight(1.35f))
        Text("تاریخ خرید", color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("تاریخ انقضا", color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(38.dp))
    }
}

// یک ردیف ربات شامل ویرایش، نام، تاریخ خرید، انقضا و تمدید است.
@Composable
private fun PlatformBotRow(bot: ConnectedBot, accent: Color, onEdit: () -> Unit, onRenew: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Edit, "ویرایش ربات", tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1.35f)) {
            Text(bot.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
            if (bot.username.isNotBlank()) Text("@${bot.username}", color = TextMuted, fontSize = 8.sp, maxLines = 1)
        }
        Text(LocalStore.formatDate(bot.createdAt), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(LocalStore.formatDate(bot.expiresAt), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onRenew, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Autorenew, "تمدید اشتراک", tint = Success, modifier = Modifier.size(20.dp))
        }
    }
}

// صفحه پلن‌های اشتراک گرافیکی ساخته می‌شود.
@Composable
fun V12Subscriptions(
    admin: Boolean,
    loggedIn: Boolean,
    onNeedAuth: () -> Unit,
    onChoose: (SubscriptionPlan) -> Unit,
    onAdminConnect: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(25.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFF2563EB))))
                        .padding(20.dp)
                ) {
                    Column {
                        Icon(Icons.Outlined.WorkspacePremium, null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("اشتراک اختصاصی هر ربات", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Text("برای هر ربات، پلن و زمان انقضای مستقل ثبت می‌شود.", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            }
        }

        if (admin) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = .09f)),
                    border = BorderStroke(1.dp, Success.copy(alpha = .35f)),
                    shape = RoundedCornerShape(19.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Success)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("دسترسی مدیر نامحدود است", fontWeight = FontWeight.Bold)
                            Text("برای تست برنامه نیازی به خرید اشتراک ندارید.", color = TextMuted, fontSize = 10.sp)
                        }
                        TextButton(onClick = onAdminConnect) { Text("اتصال") }
                    }
                }
            }
        }

        items(subscriptionPlans) { plan ->
            PlanBox(plan) { if (!loggedIn) onNeedAuth() else onChoose(plan) }
        }
    }
}

// کارت پلن قیمت بزرگ، قیمت قبلی قرمز خط‌خورده و درصد تخفیف دارد.
@Composable
private fun PlanBox(plan: SubscriptionPlan, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (plan.badge != null) Color(0xFF14213B) else Surface),
        border = if (plan.badge != null) BorderStroke(1.dp, Blue.copy(alpha = .55f)) else BorderStroke(1.dp, Color.White.copy(alpha = .05f)),
        shape = RoundedCornerShape(23.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(plan.title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text("اشتراک یک ربات", color = TextMuted, fontSize = 10.sp)
                }
                plan.badge?.let { StatusPill(it, Blue) }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(plan.price.money(), fontSize = 31.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text("تومان", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
            if (plan.oldPrice != null) {
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.oldPrice.money() + " تومان",
                        color = Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Spacer(Modifier.width(9.dp))
                    StatusPill("${plan.discount ?: 0}٪ تخفیف".toPersian(), Success)
                }
            }
            Spacer(Modifier.height(15.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.CreditCard, null)
                Spacer(Modifier.width(7.dp))
                Text("انتخاب و خرید این پلن")
            }
        }
    }
}

// اتصال واقعی ربات تلگرام انجام می‌شود.
@Composable
fun V12Connect(
    admin: Boolean,
    plan: SubscriptionPlan?,
    onNeedPlan: () -> Unit,
    onConnected: (ConnectedBot) -> Unit
) {
    // Token فرم نگهداری می‌شود.
    var token by remember { mutableStateOf("") }
    // حالت درخواست شبکه نگهداری می‌شود.
    var loading by remember { mutableStateOf(false) }
    // متن خطای اتصال نگهداری می‌شود.
    var error by remember { mutableStateOf<String?>(null) }
    // Scope برای اتصال suspend استفاده می‌شود.
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                Modifier.size(82.dp).background(TelegramBlue.copy(alpha = .16f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Send, null, tint = TelegramBlue, modifier = Modifier.size(43.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("اتصال ربات تلگرام", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("توکن دریافتی از BotFather را وارد کنید.", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(17.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    when {
                        admin -> InfoStrip("دسترسی مدیر", "نامحدود و بدون پرداخت", Success)
                        plan != null -> InfoStrip(plan.title, "${plan.price.money()} تومان • ${plan.days.toPersian()} روز", Blue)
                        else -> {
                            InfoStrip("پلن انتخاب نشده", "برای اتصال ربات ابتدا اشتراک انتخاب کنید.", Warning)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = onNeedPlan, modifier = Modifier.fillMaxWidth()) {
                                Text("مشاهده پلن‌ها")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        token,
                        {
                            token = it.trim()
                            error = null
                        },
                        Modifier.fillMaxWidth(),
                        label = { Text("Bot Token") },
                        leadingIcon = { Icon(Icons.Outlined.Key, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = admin || plan != null,
                        isError = error != null
                    )

                    error?.let {
                        Text(it, color = Danger, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
                    }

                    Spacer(Modifier.height(13.dp))
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                TelegramApi.validateToken(token).fold(
                                    onSuccess = { info ->
                                        val now = System.currentTimeMillis()
                                        val expires = if (admin) 0L else now + ((plan?.days ?: 0) * 86_400_000L)
                                        val connectedBot = ConnectedBot(
                                            platform = BotPlatform.TELEGRAM,
                                            name = info.firstName.ifBlank { "ربات تلگرام" },
                                            username = info.username,
                                            token = token,
                                            createdAt = now,
                                            expiresAt = expires,
                                            planLabel = if (admin) "مدیر - نامحدود" else plan?.title.orEmpty()
                                        )
                                        // Bot تازه بلافاصله مالک Catalog فعال می‌شود.
                                        CatalogSelection.botId = connectedBot.id
                                        onConnected(connectedBot)
                                        loading = false
                                    },
                                    onFailure = {
                                        loading = false
                                        error = it.message ?: "اتصال برقرار نشد"
                                    }
                                )
                            }
                        },
                        enabled = token.isNotBlank() && !loading && (admin || plan != null),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Link, null)
                            Spacer(Modifier.width(6.dp))
                            Text("بررسی و اتصال ربات")
                        }
                    }
                }
            }
        }
    }
}

// مدیریت ربات انتخاب‌شده نمایش داده می‌شود.
@Composable
fun V12BotManager(
    bot: ConnectedBot,
    onUpdate: (ConnectedBot) -> Unit,
    onDelete: (ConnectedBot) -> Unit,
    onGeneralManagement: () -> Unit,
    onBroadcast: () -> Unit,
    onProducts: () -> Unit,
    onCategories: () -> Unit,
    onPreview: () -> Unit
) {
    // هر بار صفحه مدیریت این Bot نمایش داده شود، Catalog فعال روی همین Bot قرار می‌گیرد.
    SideEffect { CatalogSelection.botId = bot.id }

    // stateهای Dialog و نام نمایشی نگهداری می‌شوند.
    var edit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var name by remember(bot.id) { mutableStateOf(bot.name) }
    val accent = platformColor(bot.platform)

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(accent, Color(0xFF3157D5))))
                        .padding(19.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(52.dp).background(Color.White.copy(alpha = .15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(platformIcon(bot.platform), null, modifier = Modifier.size(29.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(bot.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text(
                                    bot.platform.faName + if (bot.username.isBlank()) "" else " • @${bot.username}",
                                    color = Color.White.copy(alpha = .8f),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { edit = true }) { Icon(Icons.Filled.Edit, null) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            InfoChip("خرید: ${LocalStore.formatDate(bot.createdAt)}")
                            InfoChip("انقضا: ${LocalStore.formatDate(bot.expiresAt)}")
                        }
                    }
                }
            }
        }

        item { ActionCard("مدیریت عمومی", "نام فروشگاه، خوش‌آمدگویی، پشتیبانی و درباره", Icons.Outlined.Tune, Blue, onGeneralManagement) }
        item { ActionCard("ارسال همگانی", "صف امن و قابل ادامه برای کاربران همین Bot", Icons.Outlined.Campaign, Warning, onBroadcast) }
        item { ActionCard("محصولات", "مدیریت محصولات همین فروشگاه", Icons.Outlined.Inventory2, TelegramBlue, onProducts) }
        item { ActionCard("دسته‌بندی‌ها", "ساخت منوی مستقل همین فروشگاه", Icons.Outlined.Category, RubikaPurple, onCategories) }
        item { ActionCard("پیش‌نمایش ربات", "نمای تقریبی منوی همین Bot", Icons.Outlined.Visibility, Success, onPreview) }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = Blue)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("اشتراک ${bot.planLabel.ifBlank { "ثبت نشده" }}", fontWeight = FontWeight.Bold)
                        Text("پایان: ${LocalStore.formatDate(bot.expiresAt)}", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { deleting = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                border = BorderStroke(1.dp, Danger.copy(alpha = .5f))
            ) {
                Icon(Icons.Outlined.Delete, null)
                Spacer(Modifier.width(6.dp))
                Text("حذف اتصال ربات")
            }
        }
    }

    if (edit) {
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { Text("ویرایش نام ربات") },
            text = {
                OutlinedTextField(name, { name = it }, label = { Text("نام نمایشی") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(bot.copy(name = name.trim().ifBlank { bot.name }))
                    edit = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { edit = false }) { Text("انصراف") } }
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("حذف اتصال؟") },
            text = { Text("اطلاعات اتصال و Catalog محلی همین ربات از برنامه حذف می‌شود.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(bot)
                        if (CatalogSelection.botId == bot.id) CatalogSelection.botId = ""
                        deleting = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("انصراف") } }
        )
    }
}

// مدیریت محصولات فقط برای Bot فعال انجام می‌شود؛ ساختار صفحه بر اساس «دسته‌بندی ← محصولات» است.
@Composable
fun V12Products(
    products: List<StoreProduct>,
    categories: List<StoreCategory>,
    onChange: (List<StoreProduct>) -> Unit,
    onOpenCategories: () -> Unit
) {
    // Context برای fallback انتخاب اولین Bot و Share Sheet استفاده می‌شود.
    val context = LocalContext.current

    // شناسه Bot فعال از Session یا اولین Bot ذخیره‌شده تعیین می‌شود.
    val ownerBotId = CatalogSelection.botId.ifBlank {
        remember { LocalStore(context).loadBots().firstOrNull()?.id.orEmpty() }
    }

    // اطلاعات Bot برای ساخت لینک مستقیم t.me هر Product استفاده می‌شود.
    val ownerBot = remember(ownerBotId) { LocalStore(context).loadBots().firstOrNull { it.id == ownerBotId } }

    // در نبود Bot، کاربر باید ابتدا یک Bot انتخاب یا متصل کند.
    if (ownerBotId.isBlank()) {
        EmptyState("رباتی انتخاب نشده", "ابتدا از داشبورد یک ربات را انتخاب کنید.", Icons.Filled.SmartToy)
        return
    }

    // داده صفحه فقط از Catalog متعلق به Bot فعال ساخته می‌شود.
    val visibleProducts = products.filter { it.botId == ownerBotId }
    val visibleCategories = categories.filter { it.botId == ownerBotId }

    // Category واقعی محصول با UUID پایدار پیدا می‌شود و عنوان فقط fallback داده‌های قدیمی است.
    fun categoryFor(product: StoreProduct): StoreCategory? =
        visibleCategories.firstOrNull { it.id == product.categoryId }
            ?: visibleCategories.firstOrNull {
                it.title.trim().equals(product.category.trim(), ignoreCase = true)
            }

    // تغییرات محصولات همین Bot با داده سایر Botها Merge و ارتباط Category نیز نرمال می‌شود.
    fun saveVisibleProducts(updated: List<StoreProduct>) {
        // محصولات سایر Botها بدون تغییر حفظ می‌شوند.
        val otherBotsProducts = products.filterNot { it.botId == ownerBotId }

        // categoryId منبع اصلی ارتباط است و title فعلی Category به‌عنوان داده سازگاری ذخیره می‌شود.
        val ownedUpdated = updated.map { product ->
            val category = visibleCategories.firstOrNull { it.id == product.categoryId }
                ?: visibleCategories.firstOrNull {
                    it.title.trim().equals(product.category.trim(), ignoreCase = true)
                }
            product.copy(
                botId = ownerBotId,
                categoryId = category?.id.orEmpty(),
                category = category?.title ?: product.category
            )
        }

        // لیست کامل برای ذخیره و Sync به Activity برگردانده می‌شود.
        onChange(otherBotsProducts + ownedUpdated)
    }

    // هنگام ورود به صفحه، Stock واقعی Backend روی همان UUID Product Merge می‌شود؛ نسخه Stock محلی عمداً دست‌نخورده می‌ماند.
    LaunchedEffect(ownerBotId, ownerBot?.token) {
        val token = ownerBot?.token.orEmpty()
        if (token.isBlank()) return@LaunchedEffect

        BotStoreInventoryApi.fetch(token).onSuccess { backendInventory ->
            val merged = visibleProducts.map { product ->
                backendInventory[product.id]?.let { remote ->
                    product.copy(
                        stockEnabled = remote.stockEnabled,
                        stockQuantity = remote.stockQuantity
                    )
                } ?: product
            }
            if (merged != visibleProducts) saveVisibleProducts(merged)
        }
    }

    // شناسه Category برای افزودن Product و شناسه Product برای ویرایش نگهداری می‌شوند.
    var addCategoryId by remember { mutableStateOf<String?>(null) }
    var editingProductId by remember { mutableStateOf<String?>(null) }

    // محصولی که باید حذف شود برای جلوگیری از حذف تصادفی در Dialog تأیید نگهداری می‌شود.
    var deletingProductId by remember { mutableStateOf<String?>(null) }

    // بدون Category افزودن Product مجاز نیست؛ کاربر مستقیم به صفحه ساخت دسته هدایت می‌شود.
    if (visibleCategories.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.Category, null, tint = Blue, modifier = Modifier.size(58.dp))
            Spacer(Modifier.height(14.dp))
            Text("اول دسته‌بندی بسازید", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            Spacer(Modifier.height(7.dp))
            Text(
                "محصول بدون دسته‌بندی ساخته نمی‌شود. ابتدا حداقل یک دسته ایجاد کنید و سپس محصولات را داخل همان دسته اضافه کنید.",
                color = TextMuted,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenCategories) {
                Icon(Icons.Outlined.AddBox, null)
                Spacer(Modifier.width(7.dp))
                Text("ساخت دسته‌بندی")
            }
        }
        return
    }

    // محصولات دارای Category معتبر داخل همان Category نمایش داده می‌شوند.
    val productsByCategory = visibleCategories.associate { category ->
        category.id to visibleProducts.filter { product -> categoryFor(product)?.id == category.id }
    }

    // محصولات قدیمی که هنوز Category معتبر ندارند حذف نمی‌شوند و برای اصلاح جداگانه نمایش داده می‌شوند.
    val uncategorizedProducts = visibleProducts.filter { product -> categoryFor(product) == null }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // راهنمای ساختار صفحه به کاربر نشان می‌دهد Product مستقیماً زیر Category قرار می‌گیرد.
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Blue.copy(alpha = .10f)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountTree, null, tint = Blue)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("ساختار فروشگاه", fontWeight = FontWeight.Bold)
                            Text("هر محصول باید عضو یکی از دسته‌بندی‌های زیر باشد.", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            // هر Category یک ظرف مستقل دارد و Productهای همان Category داخل آن رندر می‌شوند.
            items(visibleCategories, key = { category -> category.id }) { category ->
                val categoryProducts = productsByCategory[category.id].orEmpty()

                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji, fontSize = 25.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.title, fontWeight = FontWeight.ExtraBold)
                                Text("${categoryProducts.size.toPersian()} محصول", color = TextMuted, fontSize = 9.sp)
                            }
                            TextButton(onClick = { addCategoryId = category.id }) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("محصول")
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = .07f))

                        // Category خالی نیز نمایش داده می‌شود تا کاربر مستقیماً اولین Product را داخل آن بسازد.
                        if (categoryProducts.isEmpty()) {
                            Text(
                                "هنوز محصولی داخل این دسته نیست.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            categoryProducts.forEachIndexed { index, product ->
                                CatalogProductRow(
                                    product = product,
                                    botUsername = ownerBot?.username.orEmpty(),
                                    onShare = { link ->
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, link)
                                                },
                                                "اشتراک لینک محصول"
                                            )
                                        )
                                    },
                                    onEdit = { editingProductId = product.id },
                                    onDelete = { deletingProductId = product.id }
                                )
                                if (index != categoryProducts.lastIndex) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = .05f),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // این بخش فقط برای نجات داده legacy است؛ Product جدید هرگز بدون Category ساخته نمی‌شود.
            if (uncategorizedProducts.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = .10f)), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
                            Text(
                                "محصولات نیازمند تعیین دسته‌بندی",
                                fontWeight = FontWeight.Bold,
                                color = Danger,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Text(
                                "این موارد از نسخه قبلی باقی مانده‌اند. با ویرایش هر محصول یک دسته معتبر انتخاب کنید.",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                            uncategorizedProducts.forEach { product ->
                                CatalogProductRow(
                                    product = product,
                                    botUsername = ownerBot?.username.orEmpty(),
                                    onShare = { link ->
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, link)
                                                },
                                                "اشتراک لینک محصول"
                                            )
                                        )
                                    },
                                    onEdit = { editingProductId = product.id },
                                    onDelete = { deletingProductId = product.id }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB همیشه Product جدید را داخل یک Category واقعی آغاز می‌کند.
        FloatingActionButton(
            onClick = { addCategoryId = visibleCategories.first().id },
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            containerColor = Blue
        ) { Icon(Icons.Filled.Add, "افزودن محصول") }
    }

    // Dialog افزودن Product فقط وقتی حداقل یک Category معتبر وجود دارد باز می‌شود.
    addCategoryId?.let { initialCategoryId ->
        CatalogProductEditorDialog(
            categories = visibleCategories,
            product = null,
            botId = ownerBotId,
            initialCategoryId = initialCategoryId,
            onDismiss = { addCategoryId = null },
            onSave = { newProduct ->
                saveVisibleProducts(visibleProducts + newProduct)
                addCategoryId = null
            }
        )
    }

    // همان Editor برای ویرایش نام، قیمت، توضیح، وضعیت و Category محصول استفاده می‌شود.
    editingProductId?.let { productId ->
        visibleProducts.firstOrNull { it.id == productId }?.let { product ->
            CatalogProductEditorDialog(
                categories = visibleCategories,
                product = product,
                botId = ownerBotId,
                initialCategoryId = categoryFor(product)?.id ?: visibleCategories.first().id,
                onDismiss = { editingProductId = null },
                onSave = { updatedProduct ->
                    saveVisibleProducts(visibleProducts.map { if (it.id == updatedProduct.id) updatedProduct else it })
                    editingProductId = null
                }
            )
        }
    }

    // حذف Product با تأیید انجام می‌شود تا لمس اشتباه باعث حذف فوری نشود.
    deletingProductId?.let { productId ->
        val product = visibleProducts.firstOrNull { it.id == productId }
        if (product != null) {
            AlertDialog(
                onDismissRequest = { deletingProductId = null },
                title = { Text("حذف محصول") },
                text = { Text("«${product.title}» حذف شود؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            saveVisibleProducts(visibleProducts.filterNot { it.id == productId })
                            deletingProductId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Danger)
                    ) { Text("حذف") }
                },
                dismissButton = { TextButton(onClick = { deletingProductId = null }) { Text("انصراف") } }
            )
        }
    }
}

// ردیف Product داخل Card دسته‌بندی ساخته می‌شود و عملیات ویرایش/اشتراک/حذف را ارائه می‌کند.
@Composable
private fun CatalogProductRow(
    product: StoreProduct,
    botUsername: String,
    onShare: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(TelegramBlue.copy(alpha = .12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.ShoppingBag, null, tint = TelegramBlue) }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(product.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!product.active) StatusPill("غیرفعال", TextMuted)
            }
            Text(product.price.money() + " تومان", color = TelegramBlue, fontSize = 10.sp)
            if (product.description.isNotBlank()) {
                Text(product.description, color = TextMuted, fontSize = 9.sp, maxLines = 2)
            }
            // وضعیت موجودی داخل همان Category دیده می‌شود و برای صفرموجودی هشدار قرمز نمایش داده می‌شود.
            Text(
                if (!product.stockEnabled) "موجودی: نامحدود"
                else if (product.stockQuantity <= 0) "ناموجود"
                else "موجودی: ${product.stockQuantity.toPersian()}",
                color = if (product.stockEnabled && product.stockQuantity <= 0) Danger else TextMuted,
                fontSize = 9.sp
            )
        }

        // لینک مستقیم فقط برای Botهایی که username معتبر دارند ساخته می‌شود.
        TelegramApi.productDeepLink(botUsername, product.id)?.let { link ->
            IconButton(onClick = { onShare(link) }) {
                Icon(Icons.Outlined.Share, "اشتراک لینک مستقیم محصول", tint = Success)
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, "ویرایش محصول", tint = Blue)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "حذف محصول", tint = Danger)
        }
    }
}

// Dialog مشترک افزودن/ویرایش Product است؛ Category از لیست موجود انتخاب می‌شود و ورود متن آزاد مجاز نیست.
@Composable
private fun CatalogProductEditorDialog(
    categories: List<StoreCategory>,
    product: StoreProduct?,
    botId: String,
    initialCategoryId: String,
    onDismiss: () -> Unit,
    onSave: (StoreProduct) -> Unit
) {
    // کلید state با Product تغییر می‌کند تا ویرایش آیتم دیگر مقادیر قبلی را نگه ندارد.
    val editorKey = product?.id ?: "new-$initialCategoryId"

    // فیلدهای ویرایش از Product موجود یا مقادیر اولیه Product جدید ساخته می‌شوند.
    var title by remember(editorKey) { mutableStateOf(product?.title.orEmpty()) }
    var price by remember(editorKey) { mutableStateOf(product?.price?.toString().orEmpty()) }
    var description by remember(editorKey) { mutableStateOf(product?.description.orEmpty()) }
    var active by remember(editorKey) { mutableStateOf(product?.active ?: true) }
    // موجودی همان Product در Editor نگهداری می‌شود؛ خاموش بودن یعنی موجودی نامحدود.
    var stockEnabled by remember(editorKey) { mutableStateOf(product?.stockEnabled ?: false) }
    var stockQuantity by remember(editorKey) { mutableStateOf((product?.stockQuantity ?: 0).toString()) }

    // Category معتبر اولیه از UUID، سپس عنوان legacy و در نهایت اولین Category انتخاب می‌شود.
    val resolvedInitialCategoryId = remember(editorKey, categories) {
        product?.categoryId?.takeIf { id -> categories.any { it.id == id } }
            ?: categories.firstOrNull {
                it.title.trim().equals(product?.category?.trim().orEmpty(), ignoreCase = true)
            }?.id
            ?: initialCategoryId.takeIf { id -> categories.any { it.id == id } }
            ?: categories.firstOrNull()?.id.orEmpty()
    }
    var selectedCategoryId by remember(editorKey, resolvedInitialCategoryId) { mutableStateOf(resolvedInitialCategoryId) }
    var categoryMenuOpen by remember(editorKey) { mutableStateOf(false) }

    // Category انتخاب‌شده هم برای نمایش و هم برای ذخیره عنوان سازگاری استفاده می‌شود.
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "محصول جدید" else "ویرایش محصول") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نام محصول") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    label = { Text("قیمت (تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category به‌صورت انتخابی است تا Product هرگز به نام آزاد یا Category ناموجود وصل نشود.
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedCategory?.let { "${it.emoji} ${it.title}" } ?: "انتخاب دسته‌بندی",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Outlined.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.title}") },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryMenuOpen = false
                                },
                                leadingIcon = if (category.id == selectedCategoryId) {
                                    { Icon(Icons.Outlined.CheckCircle, null, tint = Success) }
                                } else null
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات محصول") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // کنترل Stock در همان Editor نگه داشته می‌شود تا ساختار Category-first قابلیت موجودی نسخه جدید را از بین نبرد.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ردیابی موجودی", fontWeight = FontWeight.Bold)
                        Text(
                            if (stockEnabled) "فروش بیشتر از موجودی جلوگیری می‌شود" else "موجودی نامحدود",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                    Switch(checked = stockEnabled, onCheckedChange = { stockEnabled = it })
                }

                if (stockEnabled) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it.filter(Char::isDigit) },
                        label = { Text("تعداد موجودی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // وضعیت نمایش Product در ربات نیز همزمان قابل ویرایش است.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نمایش در فروشگاه", fontWeight = FontWeight.Bold)
                        Text(if (active) "محصول فعال است" else "محصول مخفی است", color = TextMuted, fontSize = 9.sp)
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // selectedCategory باید واقعی باشد؛ بنابراین Product بدون Category ذخیره نمی‌شود.
                    val category = selectedCategory ?: return@Button
                    val nextStockQuantity = if (stockEnabled)
                        stockQuantity.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    else 0
                    val stockChanged = product == null ||
                        stockEnabled != product.stockEnabled ||
                        nextStockQuantity != product.stockQuantity

                    val base = product ?: StoreProduct(
                        title = title.trim(),
                        price = price.toLongOrNull() ?: 0L,
                        categoryId = category.id,
                        category = category.title,
                        botId = botId
                    )
                    onSave(
                        base.copy(
                            title = title.trim(),
                            price = price.toLongOrNull() ?: 0L,
                            categoryId = category.id,
                            category = category.title,
                            description = description.trim(),
                            active = active,
                            stockEnabled = stockEnabled,
                            stockQuantity = nextStockQuantity,
                            // فقط تغییر عمدی Stock نسخه را عوض می‌کند؛ تغییر Category/نام/قیمت موجودی Backend را Reset نمی‌کند.
                            stockVersion = when {
                                product == null && !stockEnabled -> ""
                                stockChanged -> java.util.UUID.randomUUID().toString()
                                else -> base.stockVersion
                            },
                            botId = botId
                        )
                    )
                },
                enabled = title.isNotBlank() && price.toLongOrNull() != null && selectedCategory != null &&
                    (!stockEnabled || stockQuantity.toIntOrNull() != null)
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// مدیریت دسته‌بندی‌ها فقط برای Bot فعال انجام می‌شود و تعداد Productهای هر Category را نیز کنترل می‌کند.
@Composable
fun V12Categories(
    categories: List<StoreCategory>,
    products: List<StoreProduct>,
    onChange: (List<StoreCategory>) -> Unit
) {
    // Context برای fallback انتخاب Bot استفاده می‌شود.
    val context = LocalContext.current

    // Bot مالک این صفحه از Session یا اولین Bot موجود تعیین می‌شود.
    val ownerBotId = CatalogSelection.botId.ifBlank {
        remember { LocalStore(context).loadBots().firstOrNull()?.id.orEmpty() }
    }

    // بدون Bot هیچ Category ساخته نمی‌شود.
    if (ownerBotId.isBlank()) {
        EmptyState("رباتی انتخاب نشده", "ابتدا از داشبورد یک ربات را انتخاب کنید.", Icons.Filled.SmartToy)
        return
    }

    // فقط Category و Productهای Bot فعلی در مدیریت این صفحه استفاده می‌شوند.
    val visibleCategories = categories.filter { it.botId == ownerBotId }
    val visibleProducts = products.filter { it.botId == ownerBotId }

    // تعداد Productهای Category بر پایه UUID و برای legacy بر پایه عنوان محاسبه می‌شود.
    fun productCount(category: StoreCategory): Int = visibleProducts.count { product ->
        product.categoryId == category.id ||
            (product.categoryId.isBlank() && product.category.trim().equals(category.title.trim(), ignoreCase = true))
    }

    // تغییرات Category همین Bot با داده سایر Botها Merge می‌شود.
    fun saveVisibleCategories(updated: List<StoreCategory>) {
        // Categoryهای سایر Botها حفظ می‌شوند.
        val otherBotsCategories = categories.filterNot { it.botId == ownerBotId }

        // botId تمام آیتم‌های فعلی تثبیت می‌شود.
        val ownedUpdated = updated.map { category -> category.copy(botId = ownerBotId) }

        // لیست کامل برای ذخیره به Activity برگردانده می‌شود.
        onChange(otherBotsCategories + ownedUpdated)
    }

    // شناسه Category درحال ویرایش و Category درخواست‌شده برای حذف نگهداری می‌شوند.
    var add by remember { mutableStateOf(false) }
    var editingCategoryId by remember { mutableStateOf<String?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (visibleCategories.isEmpty()) {
            EmptyState("هنوز دسته‌بندی ندارید", "اول دسته‌بندی بسازید؛ سپس محصولات را داخل آن قرار دهید.", Icons.Outlined.Category)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visibleCategories, key = { it.id }) { category ->
                    // Productهای همین Category با UUID و fallback داده legacy پیدا می‌شوند و داخل Card والد دیده می‌شوند.
                    val categoryProducts = visibleProducts.filter { product ->
                        product.categoryId == category.id ||
                            (product.categoryId.isBlank() && product.category.trim().equals(category.title.trim(), ignoreCase = true))
                    }
                    val count = categoryProducts.size
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(category.emoji, fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(category.title, fontWeight = FontWeight.Bold)
                                    Text("${count.toPersian()} محصول داخل این دسته", color = TextMuted, fontSize = 9.sp)
                                }
                                IconButton(onClick = { editingCategoryId = category.id }) {
                                    Icon(Icons.Outlined.Edit, "ویرایش دسته‌بندی", tint = Blue)
                                }
                                IconButton(onClick = { deletingCategoryId = category.id }) {
                                    Icon(Icons.Outlined.Delete, "حذف دسته‌بندی", tint = Danger)
                                }
                            }

                            // نام Productها زیر Category نشان داده می‌شود تا ساختار «دسته ← محصول» در همین صفحه هم قابل مشاهده باشد.
                            if (categoryProducts.isNotEmpty()) {
                                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                                categoryProducts.forEach { product ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Inventory2, null, tint = TelegramBlue, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text(product.title, modifier = Modifier.weight(1f), fontSize = 10.sp)
                                        Text(product.price.money() + " تومان", color = TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { add = true },
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            containerColor = Blue
        ) { Icon(Icons.Filled.Add, "افزودن دسته‌بندی") }
    }

    // Dialog ساخت Category جدید نمایش داده می‌شود.
    if (add) {
        CatalogCategoryEditorDialog(
            existing = visibleCategories,
            category = null,
            ownerBotId = ownerBotId,
            onDismiss = { add = false },
            onSave = { newCategory ->
                saveVisibleCategories(visibleCategories + newCategory)
                add = false
            }
        )
    }

    // ویرایش عنوان/ایموجی Category بدون تغییر UUID انجام می‌شود تا Productها متصل باقی بمانند.
    editingCategoryId?.let { categoryId ->
        visibleCategories.firstOrNull { it.id == categoryId }?.let { category ->
            CatalogCategoryEditorDialog(
                existing = visibleCategories,
                category = category,
                ownerBotId = ownerBotId,
                onDismiss = { editingCategoryId = null },
                onSave = { updatedCategory ->
                    saveVisibleCategories(visibleCategories.map { if (it.id == categoryId) updatedCategory else it })
                    editingCategoryId = null
                }
            )
        }
    }

    // Category دارای Product حذف نمی‌شود تا Productها بدون والد و خارج از ساختار فروشگاه نمانند.
    deletingCategoryId?.let { categoryId ->
        val category = visibleCategories.firstOrNull { it.id == categoryId }
        if (category != null) {
            val count = productCount(category)
            AlertDialog(
                onDismissRequest = { deletingCategoryId = null },
                title = { Text(if (count > 0) "این دسته‌بندی محصول دارد" else "حذف دسته‌بندی") },
                text = {
                    Text(
                        if (count > 0) {
                            "داخل «${category.title}» تعداد ${count.toPersian()} محصول وجود دارد. ابتدا محصولات را به دسته دیگری منتقل یا حذف کنید؛ سپس دسته‌بندی را حذف کنید."
                        } else {
                            "دسته‌بندی «${category.title}» حذف شود؟"
                        }
                    )
                },
                confirmButton = {
                    if (count > 0) {
                        Button(onClick = { deletingCategoryId = null }) { Text("متوجه شدم") }
                    } else {
                        Button(
                            onClick = {
                                saveVisibleCategories(visibleCategories.filterNot { it.id == categoryId })
                                deletingCategoryId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Danger)
                        ) { Text("حذف") }
                    }
                },
                dismissButton = {
                    if (count == 0) TextButton(onClick = { deletingCategoryId = null }) { Text("انصراف") }
                }
            )
        }
    }
}

// Dialog مشترک افزودن/ویرایش Category است و از نام تکراری در یک Bot جلوگیری می‌کند.
@Composable
private fun CatalogCategoryEditorDialog(
    existing: List<StoreCategory>,
    category: StoreCategory?,
    ownerBotId: String,
    onDismiss: () -> Unit,
    onSave: (StoreCategory) -> Unit
) {
    // کلید مستقل باعث می‌شود داده Dialog هنگام تغییر Category قبلی باقی نماند.
    val editorKey = category?.id ?: "new-category"
    var title by remember(editorKey) { mutableStateOf(category?.title.orEmpty()) }
    var emoji by remember(editorKey) { mutableStateOf(category?.emoji ?: "🛍️") }

    // عنوان مشابه در همان Bot مجاز نیست؛ UUID مستقل است ولی نام تکراری تجربه Telegram را مبهم می‌کند.
    val duplicateTitle = existing.any { item ->
        item.id != category?.id && item.title.trim().equals(title.trim(), ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "دسته‌بندی جدید" else "ویرایش دسته‌بندی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نام دسته") },
                    singleLine = true,
                    isError = duplicateTitle,
                    supportingText = if (duplicateTitle) ({ Text("این نام قبلاً استفاده شده است.") }) else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("ایموجی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val base = category ?: StoreCategory(title = title.trim(), botId = ownerBotId)
                    onSave(
                        base.copy(
                            title = title.trim(),
                            emoji = emoji.ifBlank { "🛍️" },
                            botId = ownerBotId
                        )
                    )
                },
                enabled = title.isNotBlank() && !duplicateTitle
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}


// پیش‌نمایش فقط Catalog همان Bot را نمایش می‌دهد.
@Composable
fun V12Preview(bot: ConnectedBot?, categories: List<StoreCategory>, products: List<StoreProduct>) {
    // بدون Bot انتخاب‌شده پیش‌نمایش معنی ندارد.
    if (bot == null) {
        EmptyState("رباتی انتخاب نشده", "از داشبورد ربات را انتخاب کنید.", Icons.Filled.SmartToy)
        return
    }

    // انتخاب Catalog روی Bot فعلی تثبیت می‌شود.
    SideEffect { CatalogSelection.botId = bot.id }

    // فقط داده متعلق به همین Bot برای Preview استفاده می‌شود.
    val botCategories = categories.filter { it.botId == bot.id }
    val botProducts = products.filter { it.botId == bot.id }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A26)), shape = RoundedCornerShape(27.dp)) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF17212B)).padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(41.dp).background(platformColor(bot.platform), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(platformIcon(bot.platform), null) }
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(bot.name, fontWeight = FontWeight.Bold)
                            Text(
                                if (bot.username.isBlank()) bot.platform.faName else "@${bot.username}",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Column(Modifier.padding(15.dp)) {
                        Surface(color = Color(0xFF182533), shape = RoundedCornerShape(16.dp)) {
                            Text(
                                "سلام 👋\nبه ${bot.name} خوش آمدید.\nیکی از گزینه‌ها را انتخاب کنید:",
                                modifier = Modifier.padding(13.dp),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        val buttons = if (botCategories.isEmpty()) {
                            listOf("🛍️ محصولات", "🧾 سفارش‌های من", "☎️ پشتیبانی")
                        } else {
                            botCategories.map { "${it.emoji} ${it.title}" }
                        }

                        buttons.forEach { button ->
                            Surface(
                                color = Color(0xFF23384C),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Text(
                                    button,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(9.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "${botProducts.size.toPersian()} محصول • ${botCategories.size.toPersian()} دسته‌بندی",
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// تنظیمات، حساب کاربری، ویرایش پروفایل و بررسی بروزرسانی نمایش داده می‌شود.
@Composable
fun V12Settings(
    loggedIn: Boolean,
    admin: Boolean,
    userName: String,
    userPhone: String,
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onProfileUpdate: (String, String) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf("بررسی بروزرسانی") }
    var editingProfile by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item { SectionTitle("حساب کاربری") }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccountCircle, null, tint = Blue, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (loggedIn) userName.ifBlank { "کاربر" } else "مهمان", fontWeight = FontWeight.Bold)
                        Text(
                            if (admin) "Administrator • نامحدود" else if (loggedIn) userPhone else "برای اتصال ربات وارد شوید",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (loggedIn && !admin) {
                        IconButton(onClick = { editingProfile = true }) {
                            Icon(Icons.Filled.Edit, "ویرایش اطلاعات", tint = Blue)
                        }
                    }
                    if (!loggedIn) TextButton(onClick = onLogin) { Text("ورود") }
                }
            }
        }

        item { SectionTitle("تنظیمات") }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, null, tint = Blue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("اعلان‌ها", fontWeight = FontWeight.Bold)
                        Text("وضعیت ربات و پایان اشتراک", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(notifications, onNotifications)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        update = "در حال بررسی..."
                        UpdateChecker.check().fold(
                            onSuccess = { info ->
                                if (info == null) {
                                    update = "برنامه بروز است"
                                } else {
                                    update = "نسخه ${info.latestVersion} موجود است"
                                    if (info.downloadUrl.isNotBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                                    }
                                }
                            },
                            onFailure = { update = "خطا در بررسی بروزرسانی" }
                        )
                    }
                }
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Android, null, tint = Success)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("بروزرسانی برنامه", fontWeight = FontWeight.Bold)
                        Text(update, color = TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted)
                }
            }
        }

        if (loggedIn) {
            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    border = BorderStroke(1.dp, Danger.copy(alpha = .45f))
                ) {
                    Icon(Icons.Outlined.Logout, null)
                    Spacer(Modifier.width(6.dp))
                    Text("خروج از حساب")
                }
            }
        }
    }

    if (editingProfile) {
        var newName by remember(userName) { mutableStateOf(userName) }
        var newPhone by remember(userPhone) { mutableStateOf(userPhone) }
        var profileError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingProfile = false },
            title = { Text("ویرایش اطلاعات حساب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        newName,
                        {
                            newName = it
                            profileError = false
                        },
                        label = { Text("نام و نام خانوادگی") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        newPhone,
                        {
                            newPhone = it
                            profileError = false
                        },
                        label = { Text("شماره موبایل") },
                        singleLine = true
                    )
                    if (profileError) Text("اطلاعات واردشده معتبر نیست.", color = Danger, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ok = onProfileUpdate(newName, newPhone)
                    if (ok) editingProfile = false else profileError = true
                }) { Text("ذخیره تغییرات") }
            },
            dismissButton = { TextButton(onClick = { editingProfile = false }) { Text("انصراف") } }
        )
    }
}

// صفحه قابلیت آینده همگام‌سازی انتخابی بین چند Bot نمایش داده می‌شود.
@Composable
fun V12SyncSoon() {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, RubikaPurple.copy(alpha = .35f)),
            shape = RoundedCornerShape(27.dp)
        ) {
            Column(Modifier.padding(27.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(80.dp).background(RubikaPurple.copy(alpha = .16f), RoundedCornerShape(25.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Sync, null, tint = RubikaPurple, modifier = Modifier.size(44.dp)) }
                Spacer(Modifier.height(17.dp))
                Text("همگام‌سازی ربات‌ها", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Catalog هر ربات اکنون مستقل است. در مرحله بعد می‌توانید محصولات و دسته‌بندی‌های انتخابی را بین فروشگاه‌ها کپی کنید.",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(14.dp))
                StatusPill("به‌زودی", Warning)
            }
        }
    }
}

// صفحه عمومی درباره ما، تماس و درباره نرم‌افزار نمایش داده می‌شود.
@Composable
fun V12Info(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RobotLogo(70)
                Spacer(Modifier.height(15.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Text(body, color = TextMuted, textAlign = TextAlign.Center, lineHeight = 21.sp)
            }
        }
    }
}
