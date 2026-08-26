// این فایل صفحه «مدیریت عمومی» هر Bot را می‌سازد و متن‌های اختصاصی فروشگاه را مستقیماً با Backend همگام می‌کند.
package ir.asteam.telegrambotstore

// چیدمان‌های اصلی Compose برای فرم استفاده می‌شوند.
import androidx.compose.foundation.layout.*
// LazyColumn اجازه می‌دهد فرم در نمایشگرهای کوچک اسکرول شود.
import androidx.compose.foundation.lazy.LazyColumn
// شکل گرد کارت‌ها و دکمه‌ها تعریف می‌شود.
import androidx.compose.foundation.shape.RoundedCornerShape
// آیکون‌های مرتبط با فروشگاه و پیام استفاده می‌شوند.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
// اجزای Material 3 صفحه را می‌سازند.
import androidx.compose.material3.*
// state، Effect و Coroutine scope برای دریافت/ذخیره Backend استفاده می‌شوند.
import androidx.compose.runtime.*
// Modifier برای اندازه و فاصله‌ها استفاده می‌شود.
import androidx.compose.ui.Modifier
// Alignment برای چیدمان هدر استفاده می‌شود.
import androidx.compose.ui.Alignment
// وزن فونت عنوان‌ها را کنترل می‌کند.
import androidx.compose.ui.text.font.FontWeight
// واحد dp برای فاصله‌های رابط کاربری است.
import androidx.compose.ui.unit.dp
// واحد sp برای متن‌های کوچک توضیحی استفاده می‌شود.
import androidx.compose.ui.unit.sp
// Coroutine برای عملیات شبکه‌ای بدون مسدود کردن UI اجرا می‌شود.
import kotlinx.coroutines.launch

// این صفحه فقط برای Bot تلگرام متصل‌شده کاربرد دارد و Token را از مدل Bot موجود دریافت می‌کند.
@Composable
fun V12GeneralManagement(bot: ConnectedBot) {
    // scope صفحه عملیات ذخیره را در Coroutine اجرا می‌کند.
    val scope = rememberCoroutineScope()
    // Snackbar پیام موفقیت یا خطا را در همان صفحه نمایش می‌دهد.
    val snackbar = remember { SnackbarHostState() }

    // وضعیت بارگذاری اولیه از Backend نگهداری می‌شود.
    var loading by remember(bot.id) { mutableStateOf(true) }
    // وضعیت ذخیره‌سازی برای غیرفعال کردن دکمه هنگام درخواست شبکه استفاده می‌شود.
    var saving by remember(bot.id) { mutableStateOf(false) }
    // خطای بارگذاری در کارت قابل فهم نمایش داده می‌شود.
    var loadError by remember(bot.id) { mutableStateOf<String?>(null) }

    // چهار فیلد اصلی شخصی‌سازی هر فروشگاه state مستقل دارند.
    var storeName by remember(bot.id) { mutableStateOf("") }
    var welcomeText by remember(bot.id) { mutableStateOf("") }
    var supportText by remember(bot.id) { mutableStateOf("") }
    var aboutText by remember(bot.id) { mutableStateOf("") }
    // Username تاییدشده Backend برای نمایش لینک Bot نگهداری می‌شود.
    var verifiedUsername by remember(bot.id) { mutableStateOf(bot.username) }

    // با تغییر Bot، تنظیمات واقعی همان Token از Backend خوانده می‌شوند.
    LaunchedEffect(bot.id, bot.token) {
        loading = true
        loadError = null
        TelegramApi.fetchStoreSettings(bot.token)
            .onSuccess { settings ->
                storeName = settings.storeName
                welcomeText = settings.welcomeText
                supportText = settings.supportText
                aboutText = settings.aboutText
                verifiedUsername = settings.botUsername.ifBlank { bot.username }
            }
            .onFailure { error ->
                loadError = error.message ?: "دریافت تنظیمات فروشگاه ناموفق بود."
            }
        loading = false
    }

    // Scaffold داخلی فقط Snackbar و فرم را مدیریت می‌کند؛ AppBar اصلی در V12Activity باقی می‌ماند.
    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when {
            // در بارگذاری اولیه Progress در مرکز صفحه نمایش داده می‌شود.
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            // اگر Backend در دسترس نباشد، پیام خطا به‌جای فرم نیمه‌کاره نمایش داده می‌شود.
            loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyCard(
                    "دریافت تنظیمات ناموفق بود",
                    loadError ?: "خطای نامشخص",
                    Icons.Outlined.CloudOff
                )
            }

            // فرم اصلی مدیریت عمومی نمایش داده می‌شود.
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // کارت توضیح بالای صفحه مشخص می‌کند تغییرات روی Bot واقعی اعمال می‌شوند.
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Tune, null, tint = TelegramBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("مدیریت عمومی فروشگاه", fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (verifiedUsername.isBlank()) "تنظیمات ربات متصل‌شده"
                                    else "@${verifiedUsername.removePrefix("@")}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            StatusPill("Backend فعال", Success)
                        }
                    }
                }

                item {
                    // نام فروشگاه می‌تواند مستقل از نام فنی BotFather باشد.
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { if (it.length <= 80) storeName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام فروشگاه") },
                        leadingIcon = { Icon(Icons.Outlined.Storefront, null) },
                        supportingText = { Text("در پیام خوش‌آمدگویی و درباره فروشگاه استفاده می‌شود.") },
                        singleLine = true
                    )
                }

                item {
                    // متن خوش‌آمدگویی بعد از سلام کاربر در /start نمایش داده می‌شود.
                    OutlinedTextField(
                        value = welcomeText,
                        onValueChange = { if (it.length <= 1000) welcomeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("پیام خوش‌آمدگویی") },
                        leadingIcon = { Icon(Icons.Outlined.WavingHand, null) },
                        supportingText = { Text("خالی باشد، متن پیش‌فرض App BotStore نمایش داده می‌شود.") },
                        minLines = 4,
                        maxLines = 8
                    )
                }

                item {
                    // متن پشتیبانی با لمس گزینه پشتیبانی داخل Bot نمایش داده می‌شود.
                    OutlinedTextField(
                        value = supportText,
                        onValueChange = { if (it.length <= 1200) supportText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("متن پشتیبانی") },
                        leadingIcon = { Icon(Icons.Outlined.SupportAgent, null) },
                        supportingText = { Text("می‌توانید شماره تماس، آیدی تلگرام یا ساعت پاسخگویی را بنویسید.") },
                        minLines = 4,
                        maxLines = 8
                    )
                }

                item {
                    // متن درباره فروشگاه محتوای بخش About Bot را شخصی‌سازی می‌کند.
                    OutlinedTextField(
                        value = aboutText,
                        onValueChange = { if (it.length <= 1200) aboutText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("درباره فروشگاه") },
                        leadingIcon = { Icon(Icons.Outlined.Info, null) },
                        supportingText = { Text("معرفی کوتاه فروشگاه، خدمات یا شرایط خرید.") },
                        minLines = 4,
                        maxLines = 8
                    )
                }

                item {
                    // دکمه ذخیره چهار فیلد را در یک درخواست به Backend همان Bot می‌فرستد.
                    Button(
                        onClick = {
                            if (saving) return@Button
                            saving = true
                            scope.launch {
                                val draft = BotStoreSettings(
                                    storeName = storeName.trim(),
                                    welcomeText = welcomeText.trim(),
                                    supportText = supportText.trim(),
                                    aboutText = aboutText.trim(),
                                    botUsername = verifiedUsername
                                )

                                TelegramApi.updateStoreSettings(bot.token, draft)
                                    .onSuccess { saved ->
                                        storeName = saved.storeName
                                        welcomeText = saved.welcomeText
                                        supportText = saved.supportText
                                        aboutText = saved.aboutText
                                        snackbar.showSnackbar("تنظیمات فروشگاه ذخیره شد و روی ربات فعال است.")
                                    }
                                    .onFailure { error ->
                                        snackbar.showSnackbar(error.message ?: "ذخیره تنظیمات ناموفق بود.")
                                    }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Save, null)
                            Spacer(Modifier.width(7.dp))
                            Text("ذخیره و اعمال روی ربات")
                        }
                    }
                }
            }
        }
    }
}
