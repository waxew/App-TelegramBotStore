// این فایل مخازن Gradle و نام پروژه App-BotStore را تعریف می‌کند.
pluginManagement {
    // مخازن دریافت پلاگین‌ها تعریف می‌شوند.
    repositories {
        // مخزن Google برای پلاگین Android اضافه می‌شود.
        google()
        // Maven Central اضافه می‌شود.
        mavenCentral()
        // Gradle Plugin Portal اضافه می‌شود.
        gradlePluginPortal()
    }
}

// سیاست مخازن dependencyها تعریف می‌شود.
dependencyResolutionManagement {
    // استفاده از repositoryهای داخل ماژول‌ها ممنوع می‌شود تا build قابل پیش‌بینی بماند.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // مخازن dependencyها تعریف می‌شوند.
    repositories {
        // مخزن Google اضافه می‌شود.
        google()
        // Maven Central اضافه می‌شود.
        mavenCentral()
    }
}

// نام پروژه با برند جدید هماهنگ می‌شود.
rootProject.name = "App-BotStore"
// ماژول اصلی app اضافه می‌شود.
include(":app")
