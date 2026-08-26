// این فایل یک Compatibility Shim بسیار کوچک برای دو فراخوانی fully-qualified قدیمی در V12Activity است.
// API استاندارد Compose از `6.dp` استفاده می‌کند؛ اما نسخه فعلی Activity دو بار `androidx.compose.ui.unit.dp(6f)` دارد.
// این تابع فقط تا زمان پاک‌سازی مستقیم همان دو خط نگهداری می‌شود و هیچ رفتار UI مستقلی ایجاد نمی‌کند.
package androidx.compose.ui.unit

// مقدار Float را دقیقاً به همان Dp استاندارد Compose تبدیل می‌کند.
fun dp(value: Float): Dp = Dp(value)
