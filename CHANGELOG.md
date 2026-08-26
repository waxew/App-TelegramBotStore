# CHANGELOG

## v1.3.1

- افزایش `versionCode` به 15 و `versionName` به 1.3.1 با حفظ `applicationId` برای نصب روی نسخه 1.3.0
- تبدیل اتصال Telegram از اعتبارسنجی ساده `getMe` به ثبت واقعی Bot روی Backend
- افزودن Edge Function `botstore-register` برای اعتبارسنجی Token، ثبت Bot و اجرای `setWebhook`
- افزودن Edge Function چندرباته `botstore-telegram` برای دریافت Updateهای Telegram حتی هنگام بسته بودن APK
- افزودن Secret مستقل برای Webhook هر Bot و بررسی هدر `X-Telegram-Bot-Api-Secret-Token`
- افزودن پاسخ واقعی `/start` و منوی محصولات، حساب من، پشتیبانی و درباره فروشگاه
- افزودن نمایش دسته‌بندی‌ها با Inline Keyboard و نمایش محصولات همان دسته
- افزودن Edge Function `botstore-sync` برای انتقال Catalog از Android به PostgreSQL
- افزودن `CatalogSyncProvider` داخلی و `exported=false` برای Sync خودکار تغییرات ربات‌ها، محصولات و دسته‌بندی‌ها
- افزودن Debounce برای جلوگیری از درخواست‌های Sync پشت‌سرهم
- ایجاد جداول `botstore_bots`، `botstore_categories` و `botstore_products` با RLS و دسترسی server-only
- ثبت Migration رسمی `create_app_botstore_multibot` در Supabase
- اضافه شدن سورس Migration و هر سه Edge Function به پوشه `backend/supabase/` در GitHub
- اضافه شدن `BACKEND.md` برای توضیح معماری، امنیت، جریان اتصال و بدهی‌های فنی Backend
- بروزرسانی Workflow برای نام Artifactهای v1.3.1
- اجرای smoke test کامپایل روی `TelegramApi.kt` و `CatalogSyncProvider.kt`
- مشخص شدن محدودیت MVP: Catalog فعلی LocalStore هنوز per-bot نیست و فعلاً روی همه Botهای Telegram فعال یکسان Sync می‌شود

## v1.3.0

- اعمال استاندارد کامنت‌گذاری فارسی پروژه روی فایل اصلی Activity و حفظ توضیحات گسترده فایل‌های دیگر
- تکمیل منوی همبرگری و افزودن دسترسی مستقیم به ربات‌های من، محصولات، دسته‌بندی‌ها و پیش‌نمایش
- اصلاح کامل رفتار Back در صفحات داخلی با history داخلی
- بسته شدن Drawer با Back پیش از هر تغییر صفحه
- جلوگیری از برگشت ناخواسته به فرم اتصال پس از اتصال موفق ربات
- اصلاح مسیر پس از حذف ربات و پاک شدن history نامعتبر
- ساده‌سازی «درباره نرم‌افزار» و حذف اطلاعات فنی مانند package name
- نمایش توضیح کوتاه برنامه و شماره نسخه در «درباره نرم‌افزار»
- افزایش versionCode به 14 و versionName به 1.3.0
- آماده‌سازی Build Release و ZIP سورس در GitHub Actions
- تعریف کلید Release دائمی برای اولین نسخه پابلیش
- بروزرسانی README، Roadmap و فهرست پروژه‌های مشابه

## v1.2.1

- تغییر برند پروژه به App-BotStore
- آماده‌سازی URL بروزرسانی برای مخزن App-BotStore با fallback نام قبلی
- تغییر نام Gradle project و Artifactهای CI به App-BotStore
- حفظ applicationId برای امکان نصب به‌صورت Update
- طراحی لوگوی شفاف رباتی جدید
- بازطراحی کامل داشبورد به کارت تمام‌عرض مستقل برای هر پلتفرم
- افزودن دکمه اتصال، تعداد فعال و تنظیمات در هدر هر پلتفرم
- افزودن جدول ربات‌ها شامل ویرایش، نام، تاریخ خرید، تاریخ انقضا و تمدید
- بازطراحی کارت‌های اشتراک با قیمت درشت، قیمت قبلی قرمز و خط‌خورده و درصد تخفیف
- افزودن ویرایش اطلاعات حساب کاربری
- اصلاح متن درباره نرم‌افزار و حذف نمایش package name
- افزودن کامنت‌ها و توضیحات فارسی گسترده به سورس و فایل‌های تنظیمات

## v1.2

- اضافه شدن ورود، ثبت‌نام و حالت مهمان
- اضافه شدن داشبورد چندپلتفرمی
- اضافه شدن اشتراک مستقل برای هر ربات
- اضافه شدن حساب مدیر نامحدود
- اضافه شدن قابلیت آینده همگام‌سازی ربات‌ها
