// این فایل صفحات مدیریتی فروشنده برای سفارش‌ها و کاربران هر ربات Telegram را می‌سازد.
// تمام داده‌ها از Backend همان Bot خوانده می‌شوند و هیچ داده فروشگاه دیگری در این صفحات نمایش داده نمی‌شود.
package ir.asteam.telegrambotstore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// گزینه‌های معتبر وضعیت سفارش دقیقاً با allow-list Backend یکسان نگه داشته می‌شوند.
private val sellerOrderStatuses = listOf(
    "new" to "جدید",
    "awaiting_payment" to "در انتظار پرداخت",
    "paid" to "پرداخت‌شده",
    "processing" to "در حال آماده‌سازی",
    "shipped" to "ارسال‌شده",
    "completed" to "تکمیل‌شده",
    "cancelled" to "لغوشده"
)

// این تابع وضعیت داخلی سفارش را به عنوان فارسی تبدیل می‌کند.
private fun sellerOrderStatusLabel(status: String): String =
    sellerOrderStatuses.firstOrNull { it.first == status }?.second ?: status

// این تابع رنگ مناسب هر وضعیت سفارش را برای Badge انتخاب می‌کند.
private fun sellerOrderStatusColor(status: String): Color = when (status) {
    "paid", "completed" -> Success
    "cancelled" -> Danger
    "new", "awaiting_payment" -> Warning
    "processing", "shipped" -> TelegramBlue
    else -> TextMuted
}

// تاریخ ISO Backend به نمایش کوتاه مناسب UI تبدیل می‌شود.
private fun backendDate(value: String): String =
    value.take(10).replace('-', '/').ifBlank { "-" }.toPersian()

// این Composable صفحه سفارش‌های Bot انتخاب‌شده را نمایش می‌دهد و امکان تغییر Status را فراهم می‌کند.
@Composable
fun V12Orders(bot: ConnectedBot?) {
    // اگر Bot معتبر Telegram انتخاب نشده باشد، صفحه درخواست انتخاب Bot نشان می‌دهد.
    if (bot == null || bot.platform != BotPlatform.TELEGRAM || bot.token.isBlank()) {
        EmptyState(
            "ربات تلگرام انتخاب نشده",
            "ابتدا از داشبورد یک ربات تلگرام را انتخاب کنید.",
            Icons.Outlined.ReceiptLong
        )
        return
    }

    // Scope برای فراخوانی suspend APIهای Backend ساخته می‌شود.
    val scope = rememberCoroutineScope()

    // لیست سفارش‌های همین Bot نگهداری می‌شود.
    var orders by remember(bot.id) { mutableStateOf<List<BotStoreOrder>>(emptyList()) }

    // آمار خلاصه فروشگاه نگهداری می‌شود.
    var overview by remember(bot.id) { mutableStateOf<BotStoreOverview?>(null) }

    // وضعیت بارگذاری برای جلوگیری از چند درخواست هم‌زمان نگهداری می‌شود.
    var loading by remember(bot.id) { mutableStateOf(true) }

    // خطای شبکه یا Backend برای Retry نمایش داده می‌شود.
    var error by remember(bot.id) { mutableStateOf<String?>(null) }

    // شناسه سفارشی که Dropdown تغییر وضعیت آن باز است نگهداری می‌شود.
    var statusMenuOrderId by remember { mutableStateOf<Long?>(null) }

    // این تابع سفارش‌ها و Overview را دوباره از Backend بارگذاری می‌کند.
    fun refresh() {
        scope.launch {
            loading = true
            error = null

            val ordersResult = TelegramApi.fetchOrders(bot.token)
            val overviewResult = TelegramApi.fetchOverview(bot.token)

            ordersResult.fold(
                onSuccess = { orders = it },
                onFailure = { error = it.message ?: "دریافت سفارش‌ها ناموفق بود." }
            )
            overviewResult.onSuccess { overview = it }
            loading = false
        }
    }

    // با تغییر Bot، اطلاعات همان فروشگاه خودکار بارگذاری می‌شوند.
    LaunchedEffect(bot.id) { refresh() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // هدر صفحه نام Bot، تعداد سفارش‌ها و Refresh را نمایش می‌دهد.
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = .28f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ReceiptLong, null, tint = TelegramBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سفارش‌های ${bot.name}", fontWeight = FontWeight.ExtraBold)
                            Text(
                                "کل: ${overview?.orders ?: orders.size} • جدید: ${overview?.newOrders ?: orders.count { it.status == "new" }}".toPersian(),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(onClick = { refresh() }, enabled = !loading) {
                            Icon(Icons.Outlined.Refresh, "بروزرسانی")
                        }
                    }
                }
            }

            // خطا با دکمه Retry نمایش داده می‌شود.
            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = .08f)),
                        border = BorderStroke(1.dp, Danger.copy(alpha = .28f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error.orEmpty(), color = Danger, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { refresh() }) { Text("تلاش مجدد") }
                        }
                    }
                }
            }

            // در نبود سفارش، حالت خالی داخل لیست نمایش داده می‌شود.
            if (!loading && error == null && orders.isEmpty()) {
                item {
                    EmptyCard(
                        "هنوز سفارشی ثبت نشده",
                        "پس از خرید مشتری از ربات، سفارش اینجا نمایش داده می‌شود.",
                        Icons.Outlined.ReceiptLong
                    )
                }
            }

            // هر سفارش به‌صورت کارت مستقل نمایش داده می‌شود.
            items(orders, key = { it.id }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(order.orderCode, fontWeight = FontWeight.ExtraBold)
                                val customer = order.customerName.ifBlank {
                                    if (order.customerUsername.isNotBlank()) "@${order.customerUsername}" else "کاربر ${order.telegramUserId}"
                                }
                                Text(customer, color = TextMuted, fontSize = 10.sp)
                            }
                            StatusPill(
                                sellerOrderStatusLabel(order.status),
                                sellerOrderStatusColor(order.status)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(order.totalPrice.money() + " تومان", color = TelegramBlue, fontWeight = FontWeight.Bold)
                                Text("ثبت: ${backendDate(order.createdAt)}", color = TextMuted, fontSize = 9.sp)
                            }

                            Box {
                                OutlinedButton(onClick = { statusMenuOrderId = order.id }) {
                                    Text("تغییر وضعیت", fontSize = 10.sp)
                                }

                                DropdownMenu(
                                    expanded = statusMenuOrderId == order.id,
                                    onDismissRequest = { statusMenuOrderId = null }
                                ) {
                                    sellerOrderStatuses.forEach { (status, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                statusMenuOrderId = null
                                                scope.launch {
                                                    TelegramApi.setOrderStatus(bot.token, order.id, status).fold(
                                                        onSuccess = {
                                                            if (it) {
                                                                orders = orders.map { current ->
                                                                    if (current.id == order.id) current.copy(status = status) else current
                                                                }
                                                            }
                                                        },
                                                        onFailure = { failure ->
                                                            error = failure.message ?: "تغییر وضعیت سفارش ناموفق بود."
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // در اولین بارگذاری Progress وسط صفحه نمایش داده می‌شود.
        if (loading && orders.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

// این Composable کاربران همان فروشگاه را نمایش می‌دهد و Block/Unblock را مدیریت می‌کند.
@Composable
fun V12Customers(bot: ConnectedBot?) {
    // صفحه فقط برای Bot واقعی Telegram معنا دارد.
    if (bot == null || bot.platform != BotPlatform.TELEGRAM || bot.token.isBlank()) {
        EmptyState(
            "ربات تلگرام انتخاب نشده",
            "ابتدا از داشبورد یک ربات تلگرام را انتخاب کنید.",
            Icons.Outlined.Groups
        )
        return
    }

    // Scope برای درخواست‌های Backend ساخته می‌شود.
    val scope = rememberCoroutineScope()

    // کاربران همان Bot نگهداری می‌شوند.
    var customers by remember(bot.id) { mutableStateOf<List<BotStoreCustomer>>(emptyList()) }

    // آمار خلاصه برای هدر نگهداری می‌شود.
    var overview by remember(bot.id) { mutableStateOf<BotStoreOverview?>(null) }

    // وضعیت بارگذاری صفحه است.
    var loading by remember(bot.id) { mutableStateOf(true) }

    // خطا برای نمایش و Retry نگهداری می‌شود.
    var error by remember(bot.id) { mutableStateOf<String?>(null) }

    // این تابع لیست مشتری‌ها و Overview را از Backend تازه می‌کند.
    fun refresh() {
        scope.launch {
            loading = true
            error = null

            val customersResult = TelegramApi.fetchCustomers(bot.token)
            val overviewResult = TelegramApi.fetchOverview(bot.token)

            customersResult.fold(
                onSuccess = { customers = it },
                onFailure = { error = it.message ?: "دریافت کاربران ناموفق بود." }
            )
            overviewResult.onSuccess { overview = it }
            loading = false
        }
    }

    // با تغییر Bot لیست همان فروشگاه بارگذاری می‌شود.
    LaunchedEffect(bot.id) { refresh() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, RubikaPurple.copy(alpha = .28f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Groups, null, tint = RubikaPurple)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("کاربران ${bot.name}", fontWeight = FontWeight.ExtraBold)
                            Text(
                                "${overview?.customers ?: customers.size} کاربر • ${customers.count { it.blocked }} مسدود".toPersian(),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(onClick = { refresh() }, enabled = !loading) {
                            Icon(Icons.Outlined.Refresh, "بروزرسانی")
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = .08f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error.orEmpty(), color = Danger, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { refresh() }) { Text("تلاش مجدد") }
                        }
                    }
                }
            }

            if (!loading && error == null && customers.isEmpty()) {
                item {
                    EmptyCard(
                        "هنوز کاربری ثبت نشده",
                        "هر کاربر با اولین تعامل با ربات در این قسمت ثبت می‌شود.",
                        Icons.Outlined.Groups
                    )
                }
            }

            items(customers, key = { it.id }) { customer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = if (customer.blocked) BorderStroke(1.dp, Danger.copy(alpha = .25f)) else null,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                customer.firstName.ifBlank { "کاربر ${customer.telegramUserId}" },
                                fontWeight = FontWeight.Bold
                            )
                            if (customer.username.isNotBlank()) {
                                Text("@${customer.username}", color = TelegramBlue, fontSize = 10.sp)
                            }
                            Text(
                                "ID: ${customer.telegramUserId} • عضویت: ${backendDate(customer.createdAt)}".toPersian(),
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }

                        if (customer.blocked) {
                            StatusPill("مسدود", Danger)
                            Spacer(Modifier.width(6.dp))
                        }

                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val newBlocked = !customer.blocked
                                    TelegramApi.setCustomerBlocked(bot.token, customer.id, newBlocked).fold(
                                        onSuccess = {
                                            if (it) {
                                                customers = customers.map { current ->
                                                    if (current.id == customer.id) current.copy(blocked = newBlocked) else current
                                                }
                                            }
                                        },
                                        onFailure = { failure ->
                                            error = failure.message ?: "تغییر دسترسی کاربر ناموفق بود."
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = (if (customer.blocked) Success else Danger).copy(alpha = .13f),
                                contentColor = if (customer.blocked) Success else Danger
                            )
                        ) {
                            Icon(
                                if (customer.blocked) Icons.Outlined.Restore else Icons.Outlined.Block,
                                null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (customer.blocked) "رفع مسدودی" else "مسدود", fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        if (loading && customers.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}
