// این فایل اجزای مشترک رابط کاربری، رنگ‌ها، صفحات، پلن‌ها و ابزارهای کمکی نسخه ۱.۳.۱ را نگه می‌دارد.
package ir.asteam.telegrambotstore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// رنگ‌های ثابت Theme و پلتفرم‌ها در این بخش تعریف می‌شوند.
val Blue = Color(0xFF5B8CFF)
val TelegramBlue = Color(0xFF229ED9)
val WhatsAppGreen = Color(0xFF25D366)
val RubikaPurple = Color(0xFF8B5CF6)
val BaleGreen = Color(0xFF14B8A6)
val Bg = Color(0xFF07111F)
val Surface = Color(0xFF0E1B2E)
val Surface2 = Color(0xFF172941)
val TextMuted = Color(0xFF9BA9BD)
val Success = Color(0xFF2DD4BF)
val Warning = Color(0xFFFFB84D)
val Danger = Color(0xFFFF5D73)

// این enum تمام صفحات route شده داخل V12Activity را تعریف می‌کند تا when اصلی همیشه exhaustive بماند.
enum class V12Page(val title: String) {
    DASHBOARD("داشبورد"),
    SUBSCRIPTIONS("اشتراک ربات‌ها"),
    CONNECT("اتصال ربات"),
    BOT_MANAGER("مدیریت ربات"),
    PRODUCTS("محصولات"),
    CATEGORIES("دسته‌بندی‌ها"),
    PREVIEW("پیش‌نمایش ربات"),
    ORDERS("سفارش‌ها"),
    CUSTOMERS("کاربران فروشگاه"),
    SETTINGS("تنظیمات"),
    SYNC("همگام‌سازی ربات‌ها"),
    ABOUT("درباره ما"),
    CONTACT("تماس با ما"),
    APP_INFO("درباره نرم‌افزار")
}

// مدل پلن اشتراک هر Bot است.
data class SubscriptionPlan(
    val title: String,
    val days: Int,
    val price: Long,
    val oldPrice: Long? = null,
    val discount: Int? = null,
    val badge: String? = null
)

// پلن‌های فعلی برنامه به ترتیب مدت نگهداری می‌شوند.
val subscriptionPlans = listOf(
    SubscriptionPlan("هفتگی", 7, 50_000),
    SubscriptionPlan("یک ماهه", 30, 180_000, 200_000, 10),
    SubscriptionPlan("سه ماهه", 90, 486_000, 600_000, 19, "محبوب"),
    SubscriptionPlan("شش ماهه", 180, 874_000, 1_200_000, 27),
    SubscriptionPlan("یک ساله", 365, 1_584_000, 2_400_000, 34, "به‌صرفه‌ترین")
)

// هدر حساب در Drawer ساخته می‌شود.
@Composable
fun DrawerHeader(name: String, phone: String, loggedIn: Boolean, admin: Boolean) {
    Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        RobotLogo(54)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (loggedIn) name.ifBlank { "کاربر" } else "حالت مهمان", fontWeight = FontWeight.ExtraBold)
            Text(
                if (admin) "Administrator • نامحدود" else if (loggedIn) phone else "ورود برای اتصال ربات",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// گزینه استاندارد Drawer را نمایش می‌دهد.
@Composable
fun DrawerItem(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(text) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Blue.copy(alpha = .16f))
    )
}

// گزینه استاندارد NavigationBar پایین را نمایش می‌دهد.
@Composable
fun RowScope.BottomNavItem(target: V12Page, current: V12Page, icon: ImageVector, onPage: (V12Page) -> Unit) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onPage(target) },
        icon = { Icon(icon, null) },
        label = { Text(target.title, fontSize = 10.sp) }
    )
}

// لوگوی ربات داخل برنامه ساخته می‌شود.
@Composable
fun RobotLogo(size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size((size * .86f).dp)
                .background(Brush.radialGradient(listOf(Blue.copy(alpha = .24f), Color.Transparent)), CircleShape)
        )
        Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size((size * .64f).dp))
        Box(Modifier.align(Alignment.TopCenter).size((size * .10f).dp).background(Success, CircleShape))
    }
}

// عنوان استاندارد بخش‌ها است.
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
}

// Badge کوچک وضعیت را می‌سازد.
@Composable
fun StatusPill(text: String, accent: Color) {
    androidx.compose.material3.Surface(color = accent.copy(alpha = .15f), shape = RoundedCornerShape(50)) {
        Text(text, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

// Chip کوچک هدر Bot را می‌سازد.
@Composable
fun InfoChip(text: String) {
    androidx.compose.material3.Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(50)) {
        Text(text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

// نوار اطلاعات پلن/دسترسی را می‌سازد.
@Composable
fun InfoStrip(title: String, subtitle: String, accent: Color) {
    androidx.compose.material3.Surface(
        color = accent.copy(alpha = .10f),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .25f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = accent)
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

// کارت عملیاتی قابل لمس برای صفحات مدیریت ساخته می‌شود.
@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(accent.copy(alpha = .15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted)
        }
    }
}

// کارت حالت خالی را می‌سازد.
@Composable
fun EmptyCard(title: String, subtitle: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).background(Blue.copy(alpha = .12f), RoundedCornerShape(21.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Blue, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(13.dp))
            Text(title, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = TextMuted, textAlign = TextAlign.Center, fontSize = 11.sp)
        }
    }
}

// EmptyCard را در مرکز کل صفحه قرار می‌دهد.
@Composable
fun EmptyState(title: String, subtitle: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        EmptyCard(title, subtitle, icon)
    }
}

// رنگ هر پلتفرم را برمی‌گرداند.
fun platformColor(platform: BotPlatform): Color = when (platform) {
    BotPlatform.TELEGRAM -> TelegramBlue
    BotPlatform.WHATSAPP -> WhatsAppGreen
    BotPlatform.RUBIKA -> RubikaPurple
    BotPlatform.BALE -> BaleGreen
}

// آیکون هر پلتفرم را برمی‌گرداند.
fun platformIcon(platform: BotPlatform): ImageVector = when (platform) {
    BotPlatform.TELEGRAM -> Icons.Filled.Send
    BotPlatform.WHATSAPP -> Icons.Filled.PhoneInTalk
    BotPlatform.RUBIKA -> Icons.Outlined.Language
    BotPlatform.BALE -> Icons.Filled.Chat
}

// مبلغ Long را با جداکننده هزارگان و ارقام فارسی نمایش می‌دهد.
fun Long.money(): String = String.format(Locale.US, "%,d", this).toPersian()

// Int را به ارقام فارسی تبدیل می‌کند.
fun Int.toPersian(): String = toString().toPersian()

// تمام ارقام انگلیسی یک رشته را به فارسی تبدیل می‌کند.
fun String.toPersian(): String = map { ch ->
    when (ch) {
        '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
        '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
        else -> ch
    }
}.joinToString("")
