// این فایل پلاگین‌های سراسری پروژه Android را بدون اعمال مستقیم روی ریشه تعریف می‌کند.
plugins {
    // پلاگین Android Application با نسخه 8.5.2 برای ماژول app آماده می‌شود.
    id("com.android.application") version "8.5.2" apply false
    // پلاگین Kotlin Android با نسخه 1.9.24 برای ماژول app آماده می‌شود.
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
