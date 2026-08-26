# App BotStore

اپلیکیشن اندروید فارسی AS Team برای ساخت، اتصال و مدیریت ربات‌های فروشگاهی تلگرام در یک پنل واحد.

## نسخه فعلی: 1.4.1

نسخه `1.4.1` مدیریت Catalog را به ساختار واقعی «دسته‌بندی ← محصول» تبدیل می‌کند. ساخت محصول بدون Category مجاز نیست، هر Product با `categoryId` پایدار به Category متصل می‌شود و قابلیت موجودی/جلوگیری از Oversell نسخه فعلی نیز حفظ شده است. Runtime ربات‌ها همچنان روی Supabase و مستقل از باز بودن APK اجرا می‌شود.

## امکانات فعلی

### Android

- Kotlin + Jetpack Compose + Material 3
- رابط RTL و تم تیره
- Splash، ورود، ثبت‌نام و حالت مهمان
- منوی همبرگری از سمت راست
- Back Stack داخلی و بازگشت صحیح از صفحات
- مدیریت چند Bot و انتخاب واقعی Bot فعال
- پلن اشتراک مستقل برای هر Bot
- مدیریت Category-first؛ ابتدا دسته‌بندی ساخته می‌شود و Product فقط داخل یک Category واقعی قابل ایجاد است
- نمایش Productها به‌صورت گروه‌بندی‌شده داخل Category و امکان انتقال Product بین دسته‌ها
- ویرایش/حذف امن Category با جلوگیری از حذف دسته دارای Product
- موجودی عددی اختیاری برای Product و نمایش وضعیت موجودی
- پیش‌نمایش منوی Bot
- صفحه سفارش‌های فروشگاه
- صفحه کاربران فروشگاه و Block / Unblock
- صفحه مدیریت عمومی شامل نام فروشگاه، پیام خوش‌آمدگویی، متن پشتیبانی و درباره فروشگاه
- Share لینک مستقیم پایدار هر محصول
- صفحه ارسال همگانی با صف قابل Resume، Progress و تاریخچه
- تنظیم اعلان‌ها و بررسی بروزرسانی
- حفظ `applicationId` و داده‌های نسخه‌های قبلی برای نصب به‌صورت Update

### Telegram Runtime

- اعتبارسنجی واقعی BotFather Token
- `setWebhook` واقعی برای هر Bot
- Secret مستقل Webhook برای هر Bot
- اجرای چند Bot روی یک Runtime مشترک با جداسازی `bot_id`
- پاسخ واقعی `/start`
- منوی محصولات، سبد خرید، سفارش‌های من، حساب، پشتیبانی و درباره فروشگاه
- دسته‌بندی‌ها و محصولات با Inline Keyboard
- جزئیات محصول و افزودن به سبد
- افزایش/کاهش تعداد محصول در سبد
- محاسبه جمع کل
- Checkout اتمیک و تولید شماره سفارش `BS-...`
- نمایش تاریخچه سفارش مشتری
- جلوگیری از دو Checkout هم‌زمان برای یک Cart
- Block شدن واقعی کاربر در Runtime همان فروشگاه
- متن خوش‌آمدگویی، پشتیبانی و درباره اختصاصی هر Bot
- Deep Link پایدار محصول با `/start p_<source_id>`
- نمایش موجودی Product و جلوگیری از افزودن بیش از Stock
- Checkout با کنترل و کاهش اتمیک موجودی برای جلوگیری از Oversell

### Catalog و داده

- Catalog مستقل برای هر Bot در Android و PostgreSQL
- `source_id` پایدار برای Product و Category
- `categoryId` پایدار در Android؛ تغییر نام Category اتصال Productها را قطع نمی‌کند
- Upsert به‌جای Replace-All؛ تغییر قیمت یا عنوان، شناسه Product را عوض نمی‌کند
- حفظ Cartهای باز هنگام ویرایش Catalog
- Sync خودکار Android → Backend
- مهاجرت Catalog نسخه‌های قدیمی فاقد `botId`
- حذف واقعی Bot با `deleteWebhook` و پاک‌سازی cascade داده‌های همان Bot

### پنل فروشنده

- مشاهده سفارش‌ها
- تغییر وضعیت سفارش
- مشاهده کاربران همان فروشگاه
- Block / Unblock
- آمار سفارش/کاربر در Backend
- تنظیم متن‌های عمومی فروشگاه
- ساخت لینک مستقیم Product
- ارسال همگانی فقط به کاربران Block‌نشده همان Bot
- ساخت Snapshot گیرنده‌ها قبل از ارسال
- ارسال Batchهای کوچک و قابل ادامه
- ثبت `sent/failed/pending` برای هر گیرنده
- امکان ادامه صف پس از خروج از صفحه یا قطع درخواست
- تایید جداگانه قبل از اولین پیام؛ ساخت صف به‌تنهایی هیچ پیامی نمی‌فرستد

## Backend

Backend روی Supabase Edge Functions و PostgreSQL اجرا می‌شود. Functionهای اصلی:

- `botstore-register`: اعتبارسنجی Token، ثبت Bot، ایجاد تنظیمات پایه و `setWebhook`
- `botstore-telegram`: Runtime مشترک چندرباته، فروشگاه، Cart، Checkout و Deep Link
- `botstore-sync`: همگام‌سازی پایدار Catalog
- `botstore-disconnect`: `deleteWebhook` و حذف Runtime Bot
- `botstore-manage`: سفارش‌ها، کاربران، Block/Unblock و تنظیمات عمومی
- `botstore-broadcast`: صف ارسال همگانی قابل Resume
- `botstore-inventory`: مدیریت موجودی Product برای فروشنده

Migrationها و سورس Functionها در `backend/supabase/` نسخه‌بندی شده‌اند تا Backend از Repository قابل بازسازی باشد.

## امنیت Backend

- هیچ `service_role` یا Secret دیتابیس داخل APK قرار ندارد.
- RLS روی جداول BotStore فعال است.
- نقش‌های `anon` و `authenticated` به جداول server-only دسترسی مستقیم ندارند.
- RPCهای حساس Broadcast فقط برای `service_role` قابل اجرا هستند.
- هر Bot دارای `webhook_secret` مستقل است.
- Webhook فقط Secret معتبر Telegram را پردازش می‌کند.
- Token در Logcat چاپ نمی‌شود.
- عملیات مدیریتی در MVP با Bot Token ثبت‌شده همان Bot احراز می‌شوند.

در نسخه MVP، Bot Token برای اجرای Telegram Bot API در جدول server-only نگهداری می‌شود. پیش از عرضه در مقیاس بزرگ، مرحله امنیتی بعدی انتقال Tokenها به Encryption/Vault و اتصال مالکیت Bot به حساب واقعی App BotStore است.

## وضعیت پلتفرم‌ها

- Telegram: Runtime واقعی و قابلیت‌های فروشگاهی فعال است.
- WhatsApp، Rubika و Bale: رابط Android وجود دارد؛ Runtime فروشگاهی مستقل آن‌ها هنوز پیاده‌سازی نشده است.

## موارد باقی‌مانده برای نسخه‌های بعد

- پرداخت و درگاه ایرانی
- Telegram Stars برای سناریوهای مجاز
- کد تخفیف
- آدرس و روش ارسال
- محصول دیجیتال
- Trial واقعی ۷ روزه و توقف Runtime پس از پایان اشتراک
- Notification واقعی سفارش برای فروشنده
- آمار فروش و CRM پیشرفته
- Encryption/Vault Tokenها
- Supabase Auth / مالکیت حساب‌محور Bot
- Mini App فروشگاهی

## سازگاری بروزرسانی

شناسه نصب Android ثابت است:

`ir.asteam.telegrambotstore`

نسخه `1.4.1` دارای `versionCode = 17` است و برای نصب مستقیم روی `1.4.0` و نسخه‌های قبلی سازگار طراحی شده است. SharedPreferences و Catalog محلی حفظ می‌شوند و Productهای قدیمی در صورت امکان از عنوان Category به `categoryId` پایدار migrate می‌شوند.

نسخه `1.3.0` مبنای اولین Release Key دائمی پروژه است. نسخه `1.4.1` و تمام نسخه‌های بعدی باید با همان کلید Release امضا شوند تا Android آن‌ها را به‌عنوان Update معتبر بپذیرد.

## Build

- Java 17
- Gradle 8.7
- Workflow عمومی: `.github/workflows/build-apk.yml`
- Workflow قابل تکرار نسخه: `.github/workflows/build-v141.yml`
- Artifact Release به‌صورت unsigned در GitHub ساخته می‌شود و Signing نهایی خارج از مخزن عمومی با JKS دائمی انجام می‌شود.
- کلید خصوصی و Password هرگز داخل Repository قرار نمی‌گیرند.

## کامنت‌گذاری سورس

فایل‌های دست‌نویس Android، Backend، Migration و Workflow دارای توضیحات فارسی هستند. استاندارد پروژه در `COMMENTING_GUIDE.md` ثبت شده است.

## مستندات

- `BACKEND.md`: معماری Backend
- `CHANGELOG.md`: تاریخچه تغییرات
- `ROADMAP.md`: برنامه توسعه
- `SIMILAR_PROJECTS.md`: نمونه‌های متن‌باز مشابه
- `SIGNING.md`: اطلاعات عمومی Certificate انتشار بدون کلید خصوصی

## مخزن

GitHub: `waxew/App-TelegramBotStore`
