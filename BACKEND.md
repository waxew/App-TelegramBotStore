# Backend واقعی App BotStore

این سند معماری Backend چندرباته نسخه `1.4.0` را توضیح می‌دهد. Android پنل مدیریت است؛ اجرای Bot روی Supabase Edge Functions و PostgreSQL انجام می‌شود تا بسته بودن APK ربات را متوقف نکند.

## جریان اتصال

1. کاربر Bot را در BotFather می‌سازد و Token را داخل App BotStore وارد می‌کند.
2. Android به `botstore-register` درخواست می‌فرستد.
3. Backend Token را با Telegram `getMe` اعتبارسنجی می‌کند.
4. Bot در `botstore_bots` Upsert می‌شود.
5. `webhook_secret` مستقل ساخته می‌شود.
6. رکورد `botstore_settings` همان Bot ایجاد می‌شود بدون بازنویسی تنظیمات اتصال مجدد.
7. `setWebhook` روی `botstore-telegram?bot_id=...` ثبت می‌شود.
8. از این لحظه Runtime بدون وابستگی به APK Updateهای Telegram را پردازش می‌کند.

## Edge Functionها

### `botstore-register`

- اعتبارسنجی Token
- Upsert Bot
- ایجاد تنظیمات پایه
- ایجاد Webhook Secret
- `setWebhook`

### `botstore-telegram`

Runtime مشترک تمام Botها است. هر Update با `bot_id` URL و هدر `X-Telegram-Bot-Api-Secret-Token` به فروشگاه صحیح نگاشت می‌شود.

قابلیت‌ها:

- `/start`
- متن خوش‌آمدگویی اختصاصی
- دسته‌بندی و Product
- جزئیات Product
- Deep Link پایدار `/start p_<source_id>`
- سبد خرید
- افزایش/کاهش Quantity
- Checkout
- سفارش‌های من
- حساب مشتری
- پشتیبانی و درباره اختصاصی
- Block واقعی مشتری

### `botstore-sync`

Catalog فقط همان Bot را Sync می‌کند.

نسخه 1.4.0 از `source_id` پایدار Android و RPC `botstore_sync_catalog` استفاده می‌کند. Sync دیگر Replace-All نیست؛ بنابراین ویرایش نام/قیمت Product شناسه DB را تغییر نمی‌دهد و Cartهای باز سالم می‌مانند.

### `botstore-disconnect`

- تطبیق Token دقیق Bot
- `deleteWebhook`
- حذف Bot از Backend
- Cascade داده‌های وابسته همان Bot
- idempotent

### `botstore-manage`

API پنل فروشنده:

- Overview
- لیست سفارش‌ها
- جزئیات سفارش و Snapshot اقلام
- تغییر وضعیت سفارش
- لیست مشتری‌ها
- Block / Unblock
- خواندن تنظیمات عمومی
- ذخیره تنظیمات عمومی

### `botstore-broadcast`

صف ارسال همگانی مستقل هر Bot:

- `list`: تاریخچه
- `create`: ساخت Broadcast و Snapshot گیرنده‌ها، بدون ارسال پیام
- `status`: خواندن وضعیت
- `process`: پردازش Batch کوچک و قابل Resume

فقط کاربران Block‌نشده همان Bot هنگام ساخت صف Snapshot می‌شوند. پیش از ارسال Batch نیز Block دوباره کنترل می‌شود. وضعیت هر گیرنده `pending/sent/failed` است؛ در نتیجه Resume باعث ارسال دوباره به کاربران `sent` نمی‌شود.

## دیتابیس

جداول اصلی:

- `botstore_bots`
- `botstore_settings`
- `botstore_categories`
- `botstore_products`
- `botstore_customers`
- `botstore_cart_items`
- `botstore_orders`
- `botstore_order_items`
- `botstore_broadcasts`
- `botstore_broadcast_recipients`

RPCهای اصلی:

- `botstore_sync_catalog`: Upsert/Delete اتمیک Catalog پایدار
- `botstore_cart_change`: تغییر اتمیک Cart
- `botstore_checkout_order`: ساخت سفارش + اقلام Snapshot + پاک‌کردن Cart در یک تراکنش
- `botstore_create_broadcast`: ساخت Broadcast و Snapshot تمام گیرنده‌های مجاز داخل PostgreSQL

Migrationها در `backend/supabase/migrations/` نسخه‌بندی شده‌اند.

## سفارش و Cart

Cart با `(bot_id, telegram_user_id, product_id)` جدا می‌شود. Checkout با Advisory Lock روی `(bot,user)` سریالی شده تا دو لمس سریع یا درخواست هم‌زمان نتواند دو سفارش از یک Cart بسازد.

`botstore_order_items` عنوان، قیمت واحد، Quantity و مبلغ خط را Snapshot می‌کند؛ بنابراین تغییر Catalog بعدی تاریخچه سفارش را خراب نمی‌کند.

## تنظیمات فروشگاه

`botstore_settings` یک رکورد برای هر Bot دارد:

- `store_name`
- `welcome_text`
- `support_text`
- `about_text`

Runtime همین مقادیر را مستقیماً در پیام‌های بعدی Telegram مصرف می‌کند.

## Deep Link محصول

Android UUID محلی Product را به‌عنوان `source_id` پایدار Sync می‌کند. لینک فروشنده:

`https://t.me/<bot_username>?start=p_<source_id>`

Runtime `source_id` را به Product فعلی همان Bot Resolve می‌کند. تغییر عنوان یا قیمت لینک را نمی‌شکند.

## Broadcast

فرآیند عمداً دو مرحله دارد:

1. Create: صف و Snapshot گیرنده‌ها ساخته می‌شوند؛ هیچ پیام Telegram ارسال نمی‌شود.
2. Process: پس از تایید فروشنده Batchهای حداکثر 20تایی ارسال می‌شوند.

اگر Android بسته شود، Loop Client لغو می‌شود ولی صف Backend و وضعیت گیرنده‌ها باقی می‌ماند. بازگشت به صفحه و Resume از اولین `pending` ادامه می‌دهد.

## امنیت

- `service_role` داخل APK وجود ندارد.
- RLS روی جداول BotStore فعال است.
- `anon/authenticated` به جداول server-only مجوز مستقیم ندارند.
- RPCهای حساس فقط برای `service_role` قابل اجرا هستند.
- Webhook Secret برای هر Bot مستقل است.
- Token در Logcat چاپ نمی‌شود.
- عملیات مدیریتی MVP با Bot Token دقیق همان Bot احراز می‌شوند.

### بدهی امنیتی بعدی

Token Bot برای نیاز Runtime فعلاً در جدول server-only نگهداری می‌شود. قبل از مقیاس عمومی باید:

- Token به Vault/Encryption منتقل شود.
- Supabase Auth یا سیستم Auth معادل، مالکیت واقعی حساب ↔ Bot را برقرار کند.
- عملیات Manage/Broadcast علاوه بر Bot Token به Session مالک وابسته شوند.
- Audit log مدیریتی اضافه شود.

## چرخه Android

`CatalogSyncProvider` با `exported=false` تغییر Bot/Catalog را گوش می‌دهد، Debounce می‌کند و فقط Catalog همان Bot را Sync می‌کند. حذف Bot باعث `disconnectBot()` و خاموش‌شدن Runtime واقعی می‌شود.

## وضعیت v1.4.0

عملیاتی:

- Backend 24/7
- Multi-Bot Webhook
- Catalog مستقل و پایدار
- Cart و Checkout
- سفارش‌ها
- مشتری‌ها و Block
- تنظیمات عمومی
- Deep Link Product
- Broadcast قابل Resume

مرحله بعد:

- موجودی و رزرو موجودی
- پرداخت
- کد تخفیف
- آدرس/ارسال
- Trial واقعی
- Notification فروشنده
- آمار/CRM پیشرفته
- Vault و Auth حساب‌محور
