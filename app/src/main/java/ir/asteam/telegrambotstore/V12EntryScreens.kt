// این فایل صفحات ورودی App BotStore را جدا از Activity نگه می‌دارد تا تغییرات ناوبری باعث حذف ناخواسته Splash یا Auth نشوند.
package ir.asteam.telegrambotstore

// چیدمان‌های پایه Compose برای ساخت صفحه‌های تمام‌صفحه و فرم استفاده می‌شوند.
import androidx.compose.foundation.layout.*
// شکل گوشه‌های کارت ورود را تعریف می‌کند.
import androidx.compose.foundation.shape.RoundedCornerShape
// آیکون‌های Material برای فرم ورود و ثبت‌نام استفاده می‌شوند.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhoneAndroid
// اجزای Material 3 فرم و Splash را فراهم می‌کنند.
import androidx.compose.material3.*
// state و LaunchedEffect برای ورودی‌ها و زمان Splash استفاده می‌شوند.
import androidx.compose.runtime.*
// Alignment برای مرکزچین کردن محتوای صفحات استفاده می‌شود.
import androidx.compose.ui.Alignment
// Modifier برای اندازه، padding و چیدمان استفاده می‌شود.
import androidx.compose.ui.Modifier
// FontWeight برای تاکید عنوان‌ها استفاده می‌شود.
import androidx.compose.ui.text.font.FontWeight
// رمز عبور را در TextField مخفی می‌کند.
import androidx.compose.ui.text.input.PasswordVisualTransformation
// متن توضیحی را وسط‌چین می‌کند.
import androidx.compose.ui.text.style.TextAlign
// واحدهای اندازه Compose هستند.
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// delay زمان نمایش Splash را بدون مسدود کردن Thread اصلی مدیریت می‌کند.
import kotlinx.coroutines.delay

// Splash مستقل برنامه پس از مدت کوتاه callback پایان را اجرا می‌کند.
@Composable
fun V12Splash(onFinished: () -> Unit) {
    // این Effect فقط یک بار هنگام ورود به Splash اجرا می‌شود.
    LaunchedEffect(Unit) {
        // زمان کوتاه برای نمایش برند و آماده‌شدن stateهای اولیه در نظر گرفته شده است.
        delay(1_600)
        // پس از پایان زمان، Root اجازه نمایش صفحه بعدی را دریافت می‌کند.
        onFinished()
    }

    // Surface تمام صفحه با رنگ اصلی برنامه ساخته می‌شود.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        // محتوا در مرکز صفحه قرار می‌گیرد.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // لوگو، عنوان و Progress به صورت عمودی نمایش داده می‌شوند.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp)
            ) {
                // لوگوی مشترک برنامه نمایش داده می‌شود.
                RobotLogo(122)
                // فاصله بصری زیر لوگو ایجاد می‌شود.
                Spacer(Modifier.height(24.dp))
                // نام برنامه نمایش داده می‌شود.
                Text(
                    text = "App BotStore",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                // فاصله کوتاه بین عنوان و توضیح ایجاد می‌شود.
                Spacer(Modifier.height(8.dp))
                // توضیح کوتاه محصول نمایش داده می‌شود.
                Text(
                    text = "ساخت و مدیریت ربات فروشگاهی شما در یک پنل",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
                // فاصله تا نشانگر بارگذاری ایجاد می‌شود.
                Spacer(Modifier.height(26.dp))
                // نشانگر کوچک بارگذاری وضعیت انتقال از Splash را نمایش می‌دهد.
                CircularProgressIndicator(
                    modifier = Modifier.size(25.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// صفحه Auth بر اساس state کنترل‌شده Root بین ورود و ثبت‌نام تغییر می‌کند.
@Composable
fun V12Auth(
    login: Boolean,
    error: String?,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    // نام کاربر فقط برای حالت ثبت‌نام نگهداری می‌شود.
    var name by remember { mutableStateOf("") }
    // identifier در ورود می‌تواند شماره/نام کاربری و در ثبت‌نام شماره موبایل باشد.
    var identifier by remember { mutableStateOf("") }
    // رمز عبور به صورت state محلی نگهداری می‌شود و در UI مخفی نمایش داده می‌شود.
    var password by remember { mutableStateOf("") }

    // ریشه صفحه Auth تمام فضای قابل‌نمایش را می‌گیرد.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Bg
    ) {
        // فرم در مرکز صفحه قرار می‌گیرد و برای نمایشگرهای کوچک padding امن دارد.
        Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // ستون اصلی صفحه ساخته می‌شود.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // لوگوی برند بالای فرم قرار می‌گیرد.
                RobotLogo(82)
                // فاصله زیر لوگو ایجاد می‌شود.
                Spacer(Modifier.height(15.dp))
                // عنوان با توجه به حالت ورود یا ثبت‌نام تغییر می‌کند.
                Text(
                    text = if (login) "ورود به App BotStore" else "ساخت حساب App BotStore",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                // توضیح هدف حساب کاربری نمایش داده می‌شود.
                Text(
                    text = "برای اتصال و مدیریت ربات‌های شخصی از حساب خود استفاده کنید.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                )

                // کارت فرم ورود/ثبت‌نام را از پس‌زمینه جدا می‌کند.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    // تمام فیلدها و دکمه‌ها داخل کارت با فاصله استاندارد چیده می‌شوند.
                    Column(Modifier.padding(18.dp)) {
                        // در حالت ثبت‌نام فیلد نام نمایش داده می‌شود.
                        if (!login) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("نام") },
                                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                singleLine = true
                            )
                            Spacer(Modifier.height(9.dp))
                        }

                        // شماره موبایل یا شناسه ورود دریافت می‌شود.
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (login) "شماره موبایل / نام کاربری" else "شماره موبایل") },
                            leadingIcon = { Icon(Icons.Outlined.PhoneAndroid, null) },
                            singleLine = true
                        )
                        // فاصله بین فیلدها ایجاد می‌شود.
                        Spacer(Modifier.height(9.dp))

                        // رمز عبور با ماسک دریافت می‌شود.
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("رمز عبور") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )

                        // خطای اعتبارسنجی LocalStore در همان کارت نمایش داده می‌شود.
                        if (!error.isNullOrBlank()) {
                            Text(
                                text = error,
                                color = Danger,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        // فاصله تا دکمه اصلی ایجاد می‌شود.
                        Spacer(Modifier.height(15.dp))

                        // دکمه اصلی callback متناسب با حالت فعلی را فراخوانی می‌کند.
                        Button(
                            onClick = {
                                if (login) {
                                    onLogin(identifier.trim(), password)
                                } else {
                                    onRegister(name.trim(), identifier.trim(), password)
                                }
                            },
                            enabled = if (login) {
                                identifier.isNotBlank() && password.isNotBlank()
                            } else {
                                name.isNotBlank() && identifier.isNotBlank() && password.isNotBlank()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            // آیکون عملیات متناسب با حالت فرم نمایش داده می‌شود.
                            Icon(if (login) Icons.Outlined.Login else Icons.Outlined.PersonAdd, null)
                            // فاصله بین آیکون و عنوان دکمه ایجاد می‌شود.
                            Spacer(Modifier.width(7.dp))
                            // عنوان عملیات اصلی نمایش داده می‌شود.
                            Text(if (login) "ورود" else "ساخت حساب")
                        }

                        // تغییر بین ورود و ثبت‌نام بدون ساخت صفحه جدید انجام می‌شود.
                        TextButton(
                            onClick = onToggle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (login) "حساب ندارم؛ ثبت‌نام" else "حساب دارم؛ ورود")
                        }
                    }
                }

                // حالت مهمان برای مشاهده رابط بدون اتصال Bot در دسترس باقی می‌ماند.
                TextButton(onClick = onSkip) {
                    Text("فعلاً به صورت مهمان ادامه می‌دهم", color = TextMuted)
                }
                // محدودیت حالت مهمان شفاف به کاربر گفته می‌شود.
                Text(
                    text = "در حالت مهمان امکان اتصال و مدیریت ربات وجود ندارد.",
                    color = TextMuted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
