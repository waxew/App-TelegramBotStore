// این فایل صفحه «ارسال همگانی» هر Bot را می‌سازد.
// ایجاد صف و شروع ارسال عمداً دو عملیات جدا هستند تا با یک لمس اشتباه هیچ پیامی برای کاربران ارسال نشود.
package ir.asteam.telegrambotstore

// چیدمان‌ها و فاصله‌های صفحه از Compose Foundation استفاده می‌کنند.
import androidx.compose.foundation.layout.*
// LazyColumn تاریخچه Broadcastها را به‌صورت اسکرول‌پذیر نمایش می‌دهد.
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// کارت‌ها با گوشه گرد طراحی می‌شوند.
import androidx.compose.foundation.shape.RoundedCornerShape
// آیکون‌های Material برای ارسال، تاریخچه و وضعیت استفاده می‌شوند.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
// اجزای Material 3 فرم، Dialog، Progress و Snackbar را فراهم می‌کنند.
import androidx.compose.material3.*
// stateها و Effectهای Compose برای اتصال صفحه به Backend استفاده می‌شوند.
import androidx.compose.runtime.*
// Alignment و Modifier برای چیدمان استفاده می‌شوند.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// وزن فونت و Ellipsis برای کارت‌های تاریخچه استفاده می‌شوند.
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// واحدهای اندازه رابط کاربری هستند.
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Coroutine عملیات شبکه‌ای و فاصله کوتاه بین Batchها را مدیریت می‌کند.
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// متن وضعیت داخلی Broadcast به عنوان فارسی قابل فهم تبدیل می‌شود.
private fun broadcastStatusFa(status: String): String = when (status) {
    "queued" -> "در انتظار شروع"
    "sending" -> "در حال ارسال"
    "completed" -> "تکمیل‌شده"
    "partial" -> "تکمیل با خطا"
    "failed" -> "ناموفق"
    else -> status
}

// رنگ وضعیت برای Pill صفحه تعیین می‌شود.
private fun broadcastStatusColor(status: String) = when (status) {
    "completed" -> Success
    "partial" -> Warning
    "failed" -> Danger
    "sending" -> TelegramBlue
    else -> TextMuted
}

// صفحه ارسال همگانی فقط داده و Token Bot انتخاب‌شده را مصرف می‌کند.
@Composable
fun V12Broadcast(bot: ConnectedBot) {
    // CoroutineScope با Lifecycle همین صفحه لغو می‌شود؛ بنابراین خروج از صفحه حلقه Client را متوقف می‌کند ولی صف Backend باقی می‌ماند.
    val scope = rememberCoroutineScope()
    // پیام‌های موفقیت و خطا در Snackbar نمایش داده می‌شوند.
    val snackbar = remember { SnackbarHostState() }

    // متن Broadcast جدید تا قبل از ایجاد صف محلی نگهداری می‌شود.
    var message by remember(bot.id) { mutableStateOf("") }
    // تاریخچه از Backend دریافت و بعد از هر Batch با وضعیت تازه جایگزین می‌شود.
    var broadcasts by remember(bot.id) { mutableStateOf<List<BotStoreBroadcast>>(emptyList()) }
    // حالت بارگذاری اولیه صفحه را کنترل می‌کند.
    var loading by remember(bot.id) { mutableStateOf(true) }
    // هنگام ساخت صف جدید دکمه ساخت غیرفعال می‌شود.
    var creating by remember(bot.id) { mutableStateOf(false) }
    // فقط یک Broadcast در هر لحظه از همین صفحه پردازش می‌شود.
    var processingId by remember(bot.id) { mutableStateOf<Long?>(null) }
    // Broadcast انتخاب‌شده برای تایید شروع ارسال نگهداری می‌شود.
    var confirmStart by remember(bot.id) { mutableStateOf<BotStoreBroadcast?>(null) }

    // این تابع یک Broadcast تازه را در لیست جایگزین می‌کند بدون اینکه ترتیب تاریخچه به هم بخورد.
    fun replaceBroadcast(updated: BotStoreBroadcast) {
        broadcasts = broadcasts.map { old -> if (old.id == updated.id) updated else old }
    }

    // تاریخچه هنگام ورود یا تغییر Bot از Backend دریافت می‌شود.
    LaunchedEffect(bot.id, bot.token) {
        loading = true
        BotStoreBroadcastApi.list(bot.token)
            .onSuccess { broadcasts = it }
            .onFailure { error -> snackbar.showSnackbar(error.message ?: "دریافت تاریخچه ارسال ناموفق بود.") }
        loading = false
    }

    // Dialog شروع/ادامه ارسال تعداد Snapshot گیرنده‌ها را قبل از اولین پیام به فروشنده نشان می‌دهد.
    confirmStart?.let { selected ->
        AlertDialog(
            onDismissRequest = { if (processingId == null) confirmStart = null },
            icon = { Icon(Icons.Outlined.Campaign, null) },
            title = { Text(if (selected.status == "queued") "شروع ارسال همگانی؟" else "ادامه ارسال همگانی؟") },
            text = {
                Text(
                    "این پیام برای گیرنده‌های باقی‌مانده همین صف ارسال می‌شود.\n\n" +
                        "کل گیرنده‌ها: ${selected.totalRecipientCount.toPersian()}\n" +
                        "ارسال‌شده: ${selected.sentCount.toPersian()}\n" +
                        "ناموفق: ${selected.failedCount.toPersian()}"
                )
            },
            confirmButton = {
                Button(onClick = {
                    // Dialog بسته و پردازش Batchها پس از تایید صریح کاربر آغاز می‌شود.
                    confirmStart = null
                    if (processingId != null) return@Button
                    processingId = selected.id

                    scope.launch {
                        var current = selected
                        var keepRunning = true

                        // تا زمانی که صفحه باز است Batchهای 20تایی پشت سر هم پردازش می‌شوند.
                        while (keepRunning && !current.isDone) {
                            val result = BotStoreBroadcastApi.process(
                                token = bot.token,
                                broadcastId = current.id,
                                limit = 20
                            )

                            result.onSuccess { batch ->
                                current = batch.broadcast
                                replaceBroadcast(current)
                                keepRunning = !batch.done
                            }.onFailure { error ->
                                keepRunning = false
                                snackbar.showSnackbar(error.message ?: "ادامه ارسال ناموفق بود؛ صف برای ادامه بعدی حفظ شد.")
                            }

                            // فاصله کوتاه از ایجاد درخواست‌های پشت‌سرهم Edge جلوگیری می‌کند.
                            if (keepRunning) delay(120)
                        }

                        // در پایان وضعیت نهایی دوباره از Backend خوانده می‌شود تا UI با سرور یکسان باشد.
                        BotStoreBroadcastApi.status(bot.token, selected.id)
                            .onSuccess { finalStatus ->
                                replaceBroadcast(finalStatus)
                                if (finalStatus.isDone) {
                                    snackbar.showSnackbar(
                                        "ارسال پایان یافت: ${finalStatus.sentCount.toPersian()} موفق، ${finalStatus.failedCount.toPersian()} ناموفق"
                                    )
                                }
                            }
                        processingId = null
                    }
                }) {
                    Text("تایید و شروع")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStart = null }) { Text("انصراف") }
            }
        )
    }

    // Scaffold داخلی Snackbar را مدیریت می‌کند؛ AppBar اصلی توسط V12Activity ساخته می‌شود.
    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // کارت ساخت Broadcast جدید تأکید می‌کند که این مرحله هنوز پیام ارسال نمی‌کند.
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Campaign, null, tint = TelegramBlue)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ارسال همگانی", fontWeight = FontWeight.ExtraBold)
                                Text("فقط کاربران Block‌نشده همین ربات", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = message,
                            onValueChange = { if (it.length <= 4000) message = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("متن پیام") },
                            supportingText = { Text("${message.length.toPersian()} / ۴۰۰۰ • ایجاد صف هنوز هیچ پیامی ارسال نمی‌کند.") },
                            minLines = 5,
                            maxLines = 10
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (creating || message.isBlank()) return@Button
                                creating = true
                                scope.launch {
                                    BotStoreBroadcastApi.create(bot.token, message)
                                        .onSuccess { created ->
                                            broadcasts = listOf(created) + broadcasts.filterNot { it.id == created.id }
                                            message = ""
                                            snackbar.showSnackbar(
                                                "صف ساخته شد: ${created.totalRecipientCount.toPersian()} گیرنده. هنوز پیامی ارسال نشده است."
                                            )
                                        }
                                        .onFailure { error ->
                                            snackbar.showSnackbar(error.message ?: "ساخت صف ارسال ناموفق بود.")
                                        }
                                    creating = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !creating && message.isNotBlank(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (creating) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.PlaylistAdd, null)
                                Spacer(Modifier.width(7.dp))
                                Text("ایجاد صف ارسال")
                            }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("تاریخچه ارسال‌ها", Modifier.weight(1f))
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            // در نبود تاریخچه حالت خالی نمایش داده می‌شود.
            if (!loading && broadcasts.isEmpty()) {
                item {
                    EmptyCard(
                        "هنوز ارسال همگانی ندارید",
                        "پیام را بنویسید و ابتدا یک صف امن ایجاد کنید.",
                        Icons.Outlined.MarkEmailUnread
                    )
                }
            }

            items(broadcasts, key = { it.id }) { item ->
                // هر کارت آمار واقعی Backend همان Broadcast را نشان می‌دهد.
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ارسال #${item.id}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusPill(broadcastStatusFa(item.status), broadcastStatusColor(item.status))
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.messageText,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(7.dp))

                        Text(
                            "کل: ${item.totalRecipientCount.toPersian()}  •  موفق: ${item.sentCount.toPersian()}  •  ناموفق: ${item.failedCount.toPersian()}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        // Broadcastهای ناتمام دکمه Resume دارند؛ وضعیت هر گیرنده مانع ارسال دوباره می‌شود.
                        if (!item.isDone && item.totalRecipientCount > 0) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { if (processingId == null) confirmStart = item },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = processingId == null
                            ) {
                                if (processingId == item.id) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Text("در حال ارسال…")
                                } else {
                                    Icon(Icons.Outlined.Send, null)
                                    Spacer(Modifier.width(7.dp))
                                    Text(if (item.status == "queued") "شروع ارسال" else "ادامه ارسال")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
