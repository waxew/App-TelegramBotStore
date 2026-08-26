// این فایل اجزای مشترک رابط کاربری، رنگ‌ها، صفحات، پلن‌ها و ابزارهای کمکی نسخه ۱.۲.۱ را نگه می‌دارد.
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

// رنگ اصلی آبی رابط کاربری و دکمه‌های عمومی برنامه است.
val Blue = Color(0xFF5B8CFF)
// رنگ آبی اختصاصی بخش تلگرام است.
val TelegramBlue = Color(0xFF229ED9)
// رنگ سبز اختصاصی بخش واتساپ است.
val WhatsAppGreen = Color(0xFF25D366)
// رنگ بنفش اختصاصی بخش روبیکا است.
val RubikaPurple = Color(0xFF8B5CF6)
// رنگ سبز-فیروزه‌ای اختصاصی بخش بله است.
val BaleGreen = Color(0xFF14B8A6)
// رنگ پس‌زمینه‌ی اصلی برنامه است.
val Bg = Color(0xFF07111F)
// رنگ سطح اول کارت‌ها و پنل‌ها است.
val Surface = Color(0xFF0E1B2E)
// رنگ سطح دوم برای نوارهای داخلی و جدول‌ها است.
val Surface2 = Color(0xFF172941)
// رنگ متن‌های توضیحی و کم‌اهمیت‌تر است.
val TextMuted = Color(0xFF9BA9BD)
// رنگ وضعیت موفق و فعال است.
val Success = Color(0xFF2DD4BF)
// رنگ وضعیت هشدار و «به‌زودی» است.
val Warning = Color(0xFFFFB84D)
// رنگ خطا، حذف و قیمت قبلی است.
val Danger = Color(0xFFFF5D73)

// این enum تمام صفحات قابل نمایش در نسخه‌ی ۱.۲.۱ را تعریف می‌کند.
enum class V12Page(val title: String) {
    DASHBOARD("داشبورد"), SUBSCRIPTIONS("اشتراک ربات‌ها"), CONNECT("اتصال ربات"), BOT_MANAGER("مدیریت ربات"), PRODUCTS("محصولات"), CATEGORIES("دسته‌بندی‌ها"), PREVIEW("پیش‌نمایش ربات"), SETTINGS("تنظیمات"), SYNC("همگام‌سازی ربات‌ها"), ABOUT("درباره ما"), CONTACT("تماس با ما"), APP_INFO("درباره نرم‌افزار")
}

// این data class مشخصات هر پلن اشتراک را نگه می‌دارد.
data class SubscriptionPlan(val title: String, val days: Int, val price: Long, val oldPrice: Long? = null, val discount: Int? = null, val badge: String? = null)

// این لیست قیمت‌های تاییدشده‌ی نسخه‌ی ۱.۲.۱ را به ترتیب زمانی نگه می‌دارد.
val subscriptionPlans = listOf(
    SubscriptionPlan("هفتگی", 7, 50_000),
    SubscriptionPlan("یک ماهه", 30, 180_000, 200_000, 10),
    SubscriptionPlan("سه ماهه", 90, 486_000, 600_000, 19, "محبوب"),
    SubscriptionPlan("شش ماهه", 180, 874_000, 1_200_000, 27),
    SubscriptionPlan("یک ساله", 365, 1_584_000, 2_400_000, 34, "به‌صرفه‌ترین")
)

// این Composable سربرگ حساب کاربر در منوی همبرگری را می‌سازد.
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

// این Composable یک گزینه‌ی استاندارد منوی همبرگری را می‌سازد.
@Composable
fun DrawerItem(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(text) }, selected = selected, onClick = onClick, icon = { Icon(icon, null) }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Blue.copy(alpha = .16f)))
}

// این Composable یک گزینه‌ی نوار پایین برنامه را می‌سازد.
@Composable
fun RowScope.BottomNavItem(target: V12Page, current: V12Page, icon: ImageVector, onPage: (V12Page) -> Unit) {
    NavigationBarItem(selected = current == target, onClick = { onPage(target) }, icon = { Icon(icon, null) }, label = { Text(target.title, fontSize = 10.sp) })
}

// این Composable نشان رباتی داخل برنامه را با ظاهر شفاف و مدرن می‌سازد.
@Composable
fun RobotLogo(size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size((size * .86f).dp).background(Brush.radialGradient(listOf(Blue.copy(alpha = .24f), Color.Transparent)), CircleShape))
        Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size((size * .64f).dp))
        Box(Modifier.align(Alignment.TopCenter).size((size * .10f).dp).background(Success, CircleShape))
    }
}

// این Composable عنوان استاندارد هر بخش را نمایش می‌دهد.
@Composable fun SectionTitle(text: String, modifier: Modifier = Modifier) { Text(text, modifier = modifier, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }

// این Composable یک برچسب کوچک وضعیت مانند فعال، تخفیف یا به‌زودی می‌سازد.
@Composable
fun StatusPill(text: String, accent: Color) { Surface(color = accent.copy(alpha = .15f), shape = RoundedCornerShape(50)) { Text(text, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) } }

// این Composable یک چیپ اطلاعاتی ساده در هدر ربات می‌سازد.
@Composable fun InfoChip(text: String) { Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(50)) { Text(text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) } }

// این Composable یک نوار اطلاعاتی برای پلن یا دسترسی مدیر می‌سازد.
@Composable
fun InfoStrip(title: String, subtitle: String, accent: Color) { Surface(color = accent.copy(alpha = .10f), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.WorkspacePremium, null, tint = accent); Spacer(Modifier.width(9.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 10.sp) } } } }

// این Composable کارت عملیاتی برای مدیریت محصولات، دسته‌بندی یا پیش‌نمایش می‌سازد.
@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).background(accent.copy(alpha = .15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = TextMuted, fontSize = 11.sp) }; Icon(Icons.Outlined.ChevronLeft, null, tint = TextMuted) } } }

// این Composable کارت حالت خالی را برای نبود اطلاعات می‌سازد.
@Composable
fun EmptyCard(title: String, subtitle: String, icon: ImageVector) { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(64.dp).background(Blue.copy(alpha = .12f), RoundedCornerShape(21.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue, modifier = Modifier.size(34.dp)) }; Spacer(Modifier.height(13.dp)); Text(title, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(5.dp)); Text(subtitle, color = TextMuted, textAlign = TextAlign.Center, fontSize = 11.sp) } } }

// این Composable کارت حالت خالی را در مرکز کل صفحه قرار می‌دهد.
@Composable fun EmptyState(title: String, subtitle: String, icon: ImageVector) { Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { EmptyCard(title, subtitle, icon) } }

// این تابع رنگ اختصاصی هر پلتفرم را برمی‌گرداند.
fun platformColor(platform: BotPlatform): Color = when (platform) { BotPlatform.TELEGRAM -> TelegramBlue; BotPlatform.WHATSAPP -> WhatsAppGreen; BotPlatform.RUBIKA -> RubikaPurple; BotPlatform.BALE -> BaleGreen }
// این تابع آیکون اختصاصی هر پلتفرم را برمی‌گرداند.
fun platformIcon(platform: BotPlatform): ImageVector = when (platform) { BotPlatform.TELEGRAM -> Icons.Filled.Send; BotPlatform.WHATSAPP -> Icons.Filled.PhoneInTalk; BotPlatform.RUBIKA -> Icons.Outlined.Language; BotPlatform.BALE -> Icons.Filled.Chat }
// این تابع عدد Long را با جداکننده‌ی هزارگان و ارقام فارسی نمایش می‌دهد.
fun Long.money(): String = String.format(Locale.US, "%,d", this).toPersian()
// این تابع عدد Int را به رشته‌ی دارای ارقام فارسی تبدیل می‌کند.
fun Int.toPersian(): String = toString().toPersian()
// این تابع تمام ارقام انگلیسی داخل رشته را به معادل فارسی تبدیل می‌کند.
fun String.toPersian(): String = map { ch -> when (ch) { '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'; '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'; else -> ch } }.joinToString("")
