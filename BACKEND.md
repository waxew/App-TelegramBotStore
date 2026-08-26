# Backend واقعی App BotStore

این سند معماری Backend چندرباته نسخه `1.3.1` را توضیح می‌دهد. هدف این Backend حل مشکل اصلی نسخه‌های قبلی است: در گذشته APK فقط BotFather Token را با `getMe` بررسی می‌کرد، اما خود Bot هیچ Worker/Webhook فعالی نداشت و `/start` پاسخی نمی‌گرفت.

## جریان واقعی اتصال ربات

1. کاربر در BotFather ربات خودش را می‌سازد و Token را دریافت می‌کند.
2. کاربر Token را داخل App BotStore وارد می‌کند.
3. `TelegramApi.validateToken()` برای سازگاری نام قبلی حفظ شده، ولی اکنون به `connectBot()` می‌رود.
4. Android به Edge Function `botstore-register` درخواست می‌فرستد.
5. Backend با Telegram `getMe` Token را دوباره روی سرور اعتبارسنجی می‌کند.
6. مشخصات Bot در `botstore_bots` ذخیره/به‌روز می‌شود.
7. Backend یک `webhook_secret` تصادفی می‌سازد.
8. با `setWebhook` آدرس `botstore-telegram?bot_id=...` روی همان Bot ثبت می‌شود.
9. از این لحظه Bot حتی با بسته بودن APK نیز Updateهای Telegram را از طریق Supabase Edge Function دریافت می‌کند.
10. `/start` منوی واقعی فروشگاه را نمایش می‌دهد.

## Edge Functionها

### `botstore-register`

وظیفه: ثبت Bot و فعال‌کردن Webhook.

- ورودی: Bot Token
- اعتبارسنجی فرمت Token
- اعتبارسنجی واقعی با `getMe`
- Upsert رکورد Bot
- ساخت Secret مخصوص Webhook
- ثبت `setWebhook`
- خروجی فقط شامل اطلاعات غیرحساس Bot است

### `botstore-telegram`

وظیفه: Webhook مشترک چندرباته.

هر Update با دو عامل به Bot درست نگاشت می‌شود:

- `bot_id` در URL
- هدر `X-Telegram-Bot-Api-Secret-Token`

منوی MVP فعلی:

- `🛍️ محصولات`
- `👤 حساب من`
- `☎️ پشتیبانی`
- `ℹ️ درباره فروشگاه`

دسته‌بندی‌ها با Inline Keyboard نمایش داده می‌شوند و انتخاب هر دسته، محصولات فعال همان دسته را از PostgreSQL می‌خواند.

### `botstore-sync`

وظیفه: انتقال Catalog ساخته‌شده در Android به Bot واقعی.

- Token را دوباره با `getMe` بررسی می‌کند.
- Bot باید قبلاً در `botstore-register` ثبت شده باشد.
- دسته‌بندی‌ها و محصولات Android را دریافت می‌کند.
- Catalog فعلی Bot را به‌صورت Replace-All با نسخه جدید همگام می‌کند.

## همگام‌سازی خودکار Android

فایل `CatalogSyncProvider.kt` هنگام شروع Process برنامه فعال می‌شود و تغییر این کلیدهای SharedPreferences را گوش می‌دهد:

- `connected_bots_v12`
- `products`
- `categories`

برای جلوگیری از درخواست‌های پشت‌سرهم، تغییرات با Debounce حدود 650 میلی‌ثانیه ترکیب می‌شوند. سپس Catalog فعلی روی تمام Botهای فعال Telegram که Token محلی دارند Sync می‌شود.

ساختار فعلی `LocalStore` Catalog را سراسری نگه می‌دارد؛ به همین دلیل v1.3.1 همان Catalog را روی همه Botهای فعال Sync می‌کند. در نسخه آینده باید محصولات و دسته‌بندی‌ها به `botId` وابسته شوند تا هر فروشگاه Catalog مستقل داشته باشد.

## دیتابیس

Migration نسخه‌بندی‌شده:

`backend/supabase/migrations/20260826_create_app_botstore_multibot.sql`

جداول:

- `botstore_bots`
- `botstore_categories`
- `botstore_products`

RLS روی هر سه جدول فعال است. نقش‌های `anon` و `authenticated` مجوز مستقیم ندارند و دسترسی داده فقط از Edge Functionهای Backend با نقش server-side انجام می‌شود.

## امنیت

- هیچ `service_role` یا Secret دیتابیس داخل APK قرار نگرفته است.
- Webhook هر Bot Secret تصادفی مستقل دارد.
- Token در Logcat چاپ نمی‌شود.
- Endpoint Webhook درخواست بدون Telegram Secret صحیح را نادیده می‌گیرد.
- Endpointهای register/sync در MVP از خود Bot Token به‌عنوان اثبات کنترل Bot استفاده می‌کنند.

### بدهی فنی امنیتی

در MVP، Bot Token در جدول server-only ذخیره می‌شود تا Webhook بتواند Telegram Bot API را صدا بزند. قبل از انتشار بزرگ و چندمستاجری باید Tokenها با یک لایه Encryption/Vault سمت سرور نگهداری شوند و `register/sync` نیز به حساب واقعی App BotStore و شناسه مالک Bot متصل شوند.

## وضعیت فعلی v1.3.1

عملیاتی:

- اعتبارسنجی Token
- ثبت Bot
- `setWebhook`
- اجرای واقعی `/start`
- منوی اصلی فروشگاه
- خواندن دسته‌بندی‌ها و محصولات
- Sync خودکار Catalog از Android
- اجرای چند Bot روی یک Webhook مشترک

مرحله بعد:

- Catalog مستقل برای هر Bot
- سفارش و سبد خرید
- کیف پول و تراکنش
- پنل کاربران فروشگاه
- Block/Unblock
- ارسال همگانی
- لینک مستقیم محصول
- موجودی
- کد تخفیف
- پرداخت
- پشتیبانی اختصاصی فروشنده
- اشتراک/Trial هفت‌روزه شبیه مدل Babba
