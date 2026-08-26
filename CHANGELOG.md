# CHANGELOG

## v1.4.1

- افزایش `versionCode` به 17 و `versionName` به `1.4.1` با حفظ `applicationId = ir.asteam.telegrambotstore`
- تغییر کامل مدیریت Catalog به جریان Category-first؛ بدون Category امکان ساخت Product وجود ندارد
- افزودن `categoryId` پایدار به مدل محلی Product و مهاجرت خودکار داده‌های قدیمی بر اساس Category همان Bot
- حذف ورودی متن آزاد Category از فرم Product و جایگزینی با انتخاب اجباری از Categoryهای واقعی
- نمایش Productها به‌صورت گروه‌بندی‌شده داخل Card هر Category در صفحه محصولات
- نمایش Productهای هر Category داخل صفحه دسته‌بندی‌ها و نمایش تعداد دقیق Productها
- افزودن امکان انتقال Product بین Categoryها هنگام ویرایش بدون تغییر UUID Product
- جلوگیری از حذف Category دارای Product برای جلوگیری از داده یتیم
- جلوگیری از نام تکراری Category در یک Bot
- افزودن تأیید حذف Product برای جلوگیری از حذف تصادفی
- Sync Catalog بر اساس `categoryId/source_id` به‌جای وابستگی اصلی به عنوان Category
- حفظ کامل قابلیت موجودی، `stockVersion`، موجودی Backend و جلوگیری از Oversell هنگام تغییر ساختار Catalog
- تغییر Workflow نسخه به `.github/workflows/build-v141.yml` و Artifactهای `App-BotStore-v1.4.1-*`

## v1.4.0

- افزایش `versionCode` به 16 و `versionName` به `1.4.0` با حفظ `applicationId = ir.asteam.telegrambotstore`
- تکمیل سبد خرید واقعی Telegram با افزودن، کاهش تعداد، خالی‌کردن سبد و جمع کل
- افزودن Checkout اتمیک و ثبت سفارش با شماره `BS-...`
- افزودن جدول‌های مشتری، سبد، سفارش و اقلام سفارش با RLS و دسترسی server-only
- افزودن Snapshot عنوان/قیمت اقلام سفارش برای حفظ تاریخچه پس از تغییر Catalog
- افزودن قفل تراکنشی برای جلوگیری از Checkout دوباره و Race Condition یک Cart
- افزودن «سفارش‌های من» برای مشتری داخل Telegram
- افزودن صفحه سفارش‌ها در Android و امکان تغییر وضعیت سفارش
- افزودن صفحه کاربران فروشگاه در Android
- افزودن Block / Unblock مشتری و اعمال Block در Runtime همان Bot
- اصلاح انتخاب چند Bot در داشبورد و تغییر واقعی `selectedBotId`
- افزودن source ID پایدار برای Product و Category
- تبدیل Sync Catalog از Replace-All به Upsert پایدار با RPC `botstore_sync_catalog`
- جلوگیری از خراب‌شدن Cartهای باز هنگام ویرایش نام، قیمت یا توضیح Product
- حذف وابستگی هویت Category به عنوان و استفاده از `source_id` پایدار
- افزودن صفحه «مدیریت عمومی» برای هر Bot
- افزودن `botstore_settings` برای نام فروشگاه، متن خوش‌آمدگویی، پشتیبانی و درباره فروشگاه
- ایجاد خودکار تنظیمات پایه هنگام اولین اتصال Bot بدون بازنویسی تنظیمات اتصال مجدد
- استفاده Runtime از متن‌های اختصاصی هر فروشگاه
- افزودن لینک مستقیم پایدار هر Product با `/start p_<source_id>`
- افزودن Share Sheet لینک مستقیم محصول در Android
- افزودن API فروشنده `botstore-manage` برای سفارش‌ها، کاربران، وضعیت‌ها و تنظیمات عمومی
- افزودن صفحه «ارسال همگانی» در Android
- افزودن Edge Function `botstore-broadcast`
- افزودن جدول‌های `botstore_broadcasts` و `botstore_broadcast_recipients`
- Snapshot فقط کاربران Block‌نشده همان Bot هنگام ساخت Broadcast
- جدا کردن «ایجاد صف» از «شروع ارسال» برای جلوگیری از ارسال ناخواسته
- پردازش Broadcast در Batchهای کوچک و قابل Resume
- ثبت وضعیت `pending/sent/failed` برای هر گیرنده و جلوگیری از ارسال دوباره هنگام Resume
- بررسی مجدد Block کاربر پیش از ارسال هر Batch
- مدیریت کوتاه Telegram `retry_after` برای Rate Limit
- نمایش Progress، آمار موفق/ناموفق و تاریخچه Broadcast در Android
- نسخه‌بندی Migrationها و سورس تمام Functionهای جدید داخل `backend/supabase/`
- افزودن Workflow اختصاصی `.github/workflows/build-v140.yml`
- تغییر نام Artifactهای Release و Source به `App-BotStore-v1.4.0-*`

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
- اضافه شدن سورس Migration و Edge Functionها به پوشه `backend/supabase/` در GitHub
- اضافه شدن `BACKEND.md` برای توضیح معماری، امنیت، جریان اتصال و بدهی‌های فنی Backend
- بروزرسانی Workflow برای نام Artifactهای v1.3.1

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
