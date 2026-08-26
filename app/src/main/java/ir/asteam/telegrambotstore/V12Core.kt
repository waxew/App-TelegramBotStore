package ir.asteam.telegrambotstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

val Blue = Color(0xFF4C8DFF)
val TelegramBlue = Color(0xFF229ED9)
val WhatsAppGreen = Color(0xFF25D366)
val RubikaPurple = Color(0xFF7C5CFC)
val BaleGreen = Color(0xFF19A974)
val Bg = Color(0xFF07111F)
val Surface = Color(0xFF0F1B2D)
val Surface2 = Color(0xFF16253A)
val TextMuted = Color(0xFF91A0B5)
val Success = Color(0xFF2DD4BF)
val Warning = Color(0xFFFFB84D)
val Danger = Color(0xFFFF6B7A)

enum class V12Page(val title: String) {
    DASHBOARD("داشبورد"),
    SUBSCRIPTIONS("اشتراک ربات‌ها"),
    CONNECT("اتصال ربات"),
    BOT_MANAGER("ویرایش ربات"),
    PRODUCTS("محصولات"),
    CATEGORIES("دسته‌بندی‌ها"),
    PREVIEW("پیش‌نمایش ربات"),
    SETTINGS("تنظیمات"),
    SYNC("همگام‌سازی ربات‌ها"),
    ABOUT("درباره ما"),
    CONTACT("تماس با ما"),
    APP_INFO("درباره نرم‌افزار")
}

data class SubscriptionPlan(
    val title: String,
    val days: Int,
    val price: Long,
    val oldPrice: Long? = null,
    val discount: Int? = null,
    val badge: String? = null
)

val subscriptionPlans = listOf(
    SubscriptionPlan("هفتگی", 7, 50_000),
    SubscriptionPlan("یک ماهه", 30, 180_000, 200_000, 10),
    SubscriptionPlan("سه ماهه", 90, 486_000, 600_000, 19, "محبوب"),
    SubscriptionPlan("شش ماهه", 180, 874_000, 1_200_000, 27),
    SubscriptionPlan("یک ساله", 365, 1_584_000, 2_400_000, 34, "به‌صرفه‌ترین")
)

@Composable
fun DrawerHeader(name: String, phone: String, loggedIn: Boolean, admin: Boolean) {
    Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        RobotLogo(54)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (loggedIn) name.ifBlank { "کاربر" } else "حالت مهمان", fontWeight = FontWeight.ExtraBold)
            Text(if (admin) "Administrator • نامحدود" else if (loggedIn) phone else "ورود برای اتصال ربات", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(text) }, selected = selected, onClick = onClick, icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Blue.copy(alpha = .16f))
    )
}

@Composable
fun RowScope.BottomNavItem(target: V12Page, current: V12Page, icon: ImageVector, onPage: (V12Page) -> Unit) {
    NavigationBarItem(selected = current == target, onClick = { onPage(target) }, icon = { Icon(icon, null) }, label = { Text(target.title, fontSize = 10.sp) })
}

@Composable
fun RobotLogo(size: Int) {
    Box(Modifier.size(size.dp).background(Brush.linearGradient(listOf(Blue, RubikaPurple)), RoundedCornerShape((size * 0.30f).dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size((size * 0.58f).dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) { Text(text, modifier = modifier, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }

@Composable
fun StatusPill(text: String, accent: Color) {
    Surface(color = accent.copy(alpha = .15f), shape = RoundedCornerShape(50)) {
        Text(text, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(50)) { Text(text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) }
}

@Composable
fun InfoStrip(title: String, subtitle: String, accent: Color) {
    Surface(color = accent.copy(alpha = .10f), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = accent); Spacer(Modifier.width(9.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 10.sp) }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(accent.copy(alpha = .15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 11.sp) }; Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted)
        }
    }
}

@Composable
fun EmptyCard(title: String, subtitle: String, icon: ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).background(Blue.copy(alpha = .12f), RoundedCornerShape(21.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(13.dp)); Text(title, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(5.dp)); Text(subtitle, color = TextMuted, textAlign = TextAlign.Center, fontSize = 11.sp)
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: ImageVector) { Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { EmptyCard(title, subtitle, icon) } }

fun platformColor(platform: BotPlatform): Color = when (platform) { BotPlatform.TELEGRAM -> TelegramBlue; BotPlatform.WHATSAPP -> WhatsAppGreen; BotPlatform.RUBIKA -> RubikaPurple; BotPlatform.BALE -> BaleGreen }
fun platformIcon(platform: BotPlatform): ImageVector = when (platform) { BotPlatform.TELEGRAM -> Icons.Filled.Send; BotPlatform.WHATSAPP -> Icons.Filled.PhoneInTalk; BotPlatform.RUBIKA -> Icons.Outlined.Language; BotPlatform.BALE -> Icons.Filled.Chat }
fun Long.money(): String = String.format(Locale.US, "%,d", this).toPersian()
fun Int.toPersian(): String = toString().toPersian()
fun String.toPersian(): String = map { ch -> when (ch) { '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'; '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'; else -> ch } }.joinToString("")
