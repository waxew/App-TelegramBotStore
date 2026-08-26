// این فایل تنظیمات ساخت ماژول اصلی Android و وابستگی‌های نسخه ۱.۳.۱ را تعریف می‌کند.
plugins {
    // پلاگین استاندارد ساخت اپلیکیشن Android فعال می‌شود.
    id("com.android.application")
    // پلاگین Kotlin Android برای کامپایل سورس‌های Kotlin فعال می‌شود.
    id("org.jetbrains.kotlin.android")
}

// بلوک تنظیمات Android آغاز می‌شود.
android {
    // namespace قدیمی حفظ می‌شود تا سازگاری سورس و آپدیت نسخه‌های نصب‌شده از بین نرود.
    namespace = "ir.asteam.telegrambotstore"
    // برنامه با API 35 کامپایل می‌شود.
    compileSdk = 35

    // تنظیمات پیش‌فرض تمام build variantها تعریف می‌شوند.
    defaultConfig {
        // applicationId ثابت نگه داشته می‌شود تا نسخه‌های آینده روی همین برنامه نصب شوند.
        applicationId = "ir.asteam.telegrambotstore"
        // حداقل اندروید قابل پشتیبانی API 24 است.
        minSdk = 24
        // targetSdk روی API 35 قرار می‌گیرد.
        targetSdk = 35
        // versionCode برای نسخه ۱.۳.۱ افزایش داده می‌شود تا روی ۱.۳.۰ نصب شود.
        versionCode = 15
        // versionName نسخه Patch جدید Backend واقعی است.
        versionName = "1.3.1"

        // Runner پیش‌فرض تست‌های Instrumentation تعیین می‌شود.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // پشتیبانی از VectorDrawable روی نسخه‌های قدیمی‌تر فعال می‌شود.
        vectorDrawables.useSupportLibrary = true
    }

    // انواع Build برنامه تعریف می‌شوند.
    buildTypes {
        // تنظیمات نسخه Release آغاز می‌شود.
        release {
            // برای نسخه فعلی minify غیرفعال است تا عیب‌یابی و خوانایی سورس ساده‌تر بماند.
            isMinifyEnabled = false
            // فایل‌های ProGuard استاندارد و سفارشی معرفی می‌شوند.
            proguardFiles(
                // قوانین بهینه‌سازی استاندارد Android استفاده می‌شود.
                getDefaultProguardFile("proguard-android-optimize.txt"),
                // قوانین اختصاصی پروژه نیز اضافه می‌شوند.
                "proguard-rules.pro"
            )
        }
    }

    // تنظیمات سازگاری Java تعریف می‌شود.
    compileOptions {
        // سورس Java با نسخه 17 کامپایل می‌شود.
        sourceCompatibility = JavaVersion.VERSION_17
        // bytecode مقصد Java نسخه 17 است.
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Kotlin نیز bytecode سازگار با JVM 17 تولید می‌کند.
    kotlinOptions { jvmTarget = "17" }

    // قابلیت‌های Build مورد نیاز فعال می‌شوند.
    buildFeatures {
        // Jetpack Compose برای رابط کاربری فعال می‌شود.
        compose = true
        // BuildConfig برای دسترسی به VERSION_NAME فعال می‌شود.
        buildConfig = true
    }
    // نسخه Compiler Extension سازگار با Compose تعیین می‌شود.
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    // فایل‌های لایسنس تکراری از بسته‌بندی APK حذف می‌شوند.
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

// وابستگی‌های ماژول اصلی تعریف می‌شوند.
dependencies {
    // Core KTX APIهای Kotlin-friendly اندروید را فراهم می‌کند.
    implementation("androidx.core:core-ktx:1.13.1")
    // Activity Compose اتصال Activity و Compose و BackHandler را فراهم می‌کند.
    implementation("androidx.activity:activity-compose:1.9.1")
    // Compose BOM نسخه‌های کتابخانه‌های Compose را هماهنگ می‌کند.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    // هسته رابط کاربری Compose اضافه می‌شود.
    implementation("androidx.compose.ui:ui")
    // API پیش‌نمایش UI برای توسعه اضافه می‌شود.
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material 3 برای کامپوننت‌های مدرن رابط کاربری اضافه می‌شود.
    implementation("androidx.compose.material3:material3")
    // مجموعه کامل آیکون‌های Material برای داشبورد و منو اضافه می‌شود.
    implementation("androidx.compose.material:material-icons-extended")
    // Lifecycle Runtime برای هماهنگی چرخه عمر Activity اضافه می‌شود.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    // ViewModel Compose برای توسعه‌های آینده اضافه می‌شود.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // Coroutines Android برای عملیات async و شبکه اضافه می‌شود.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // UI Tooling فقط در Build دیباگ برای ابزارهای پیش‌نمایش اضافه می‌شود.
    debugImplementation("androidx.compose.ui:ui-tooling")
}
