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
- افزودن `botId` به `StoreProduct` و `StoreCategory` و مستقل شدن Catalog هر Bot
- افزودن مهاجرت خودکار Catalog قدیمی فاقد `botId` به Bot اصلی و ذخیره دائمی مالکیت قبل از اجرای UI
- تغییر صفحات محصولات، دسته‌بندی‌ها و پیش‌نمایش برای نمایش/ویرایش فقط داده Bot انتخاب‌شده
- تغییر `CatalogSyncProvider` تا هر Token فقط محصولات و دسته‌بندی‌های خودش را Sync کند
- افزودن Edge Function `botstore-disconnect` برای `deleteWebhook` و حذف رکورد Backend Bot
- تشخیص Bot حذف‌شده در Android و خاموش‌کردن Runtime واقعی آن در Backend
- افزودن Retry در همان Process برای Disconnect ناموفق بدون ثبت Token در Logcat
- افزودن `CatalogSyncProvider` داخلی و `exported=false` برای Sync خودکار چرخه ربات و Catalog
- افزودن Debounce برای جلوگیری از درخواست‌های پشت‌سرهم
- ایجاد جداول `botstore_bots`، `botstore_categories` و `botstore_products` با RLS و دسترسی server-only
- ثبت Migration رسمی `create_app_botstore_multibot` در Supabase
- اضافه شدن سورس Migration و چهار Edge Function به پوشه `backend/supabase/` در GitHub
- اضافه شدن `BACKEND.md` برای توضیح معماری، امنیت، جریان اتصال و بدهی‌های فنی Backend
- بروزرسانی Workflow برای نام Artifactهای v1.3.1
- اجرای smoke test اولیه کامپایل روی `TelegramApi.kt` و نسخه اولیه `CatalogSyncProvider.kt`

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
