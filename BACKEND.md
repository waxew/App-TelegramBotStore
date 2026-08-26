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
11. Catalog Android با `botId` همان Bot جدا شده و فقط برای همان Token Sync می‌شود.

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

وظیفه: انتقال Catalog اختصاصی یک Bot از Android به همان Bot واقعی.

- Token را دوباره با `getMe` بررسی می‌کند.
- Bot باید قبلاً در `botstore-register` ثبت شده باشد.
- دسته‌بندی‌ها و محصولات فقط همان `botId` توسط Android ارسال می‌شوند.
- Catalog فعلی Bot را به‌صورت Replace-All با نسخه جدید همگام می‌کند.
- داده Botهای دیگر دست‌نخورده می‌ماند.

### `botstore-disconnect`

وظیفه: خاموش‌کردن واقعی Bot حذف‌شده.

- ورودی: Token همان Bot
- lookup فقط با تطبیق دقیق Token در جدول server-only
- اجرای `deleteWebhook` در Telegram
- حذف رکورد `botstore_bots`
- حذف خودکار Category/Product همان Bot به‌واسطه Foreign Keyهای `ON DELETE CASCADE`
- عملیات idempotent است؛ اگر Bot قبلاً حذف شده باشد نیز موفق پاسخ می‌دهد

## همگام‌سازی و چرخه عمر Android

فایل `CatalogSyncProvider.kt` هنگام شروع Process برنامه فعال می‌شود و تغییر این کلیدهای SharedPreferences را گوش می‌دهد:

- `connected_bots_v12`
- `products`
- `categories`

برای جلوگیری از درخواست‌های پشت‌سرهم، تغییرات با Debounce حدود 650 میلی‌ثانیه ترکیب می‌شوند.

### Catalog مستقل

`StoreProduct` و `StoreCategory` اکنون `botId` دارند. Provider برای هر Bot فقط آیتم‌هایی را انتخاب می‌کند که `botId` آن‌ها با شناسه همان Bot برابر است و سپس همان زیرمجموعه را به `botstore-sync` می‌فرستد.

داده‌های نسخه‌های قدیمی که `botId` ندارند در شروع Process به Bot اصلی نسبت داده و بلافاصله دوباره ذخیره می‌شوند. این Migration قبل از ثبت Listener انجام می‌شود تا حذف Bot اول باعث انتقال ناخواسته Catalog legacy به Bot دیگری نشود.

### حذف Bot

Provider یک Snapshot از Botهای قبلی نگه می‌دارد. اگر Bot از `connected_bots_v12` حذف شود یا Token همان id تغییر کند، Provider `TelegramApi.disconnectBot()` را اجرا می‌کند. در نتیجه حذف داخل APK فقط ظاهری نیست و Runtime سرور نیز خاموش می‌شود.

اگر Disconnect در همان Process به علت شبکه شکست بخورد، Bot قدیمی در Snapshot Retry نگه داشته می‌شود تا تغییر بعدی دوباره تلاش کند.

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
- Endpointهای register/sync/disconnect در MVP از خود Bot Token به‌عنوان اثبات کنترل Bot استفاده می‌کنند.

### بدهی فنی امنیتی

در MVP، Bot Token در جدول server-only ذخیره می‌شود تا Webhook بتواند Telegram Bot API را صدا بزند. قبل از انتشار بزرگ و چندمستاجری باید Tokenها با یک لایه Encryption/Vault سمت سرور نگهداری شوند و عملیات register/sync/disconnect نیز به حساب واقعی App BotStore و شناسه مالک Bot متصل شوند.

## وضعیت فعلی v1.3.1

عملیاتی:

- اعتبارسنجی Token
- ثبت Bot
- `setWebhook`
- اجرای واقعی `/start`
- منوی اصلی فروشگاه
- خواندن دسته‌بندی‌ها و محصولات
- Catalog مستقل برای هر Bot
- Sync خودکار Catalog از Android
- اجرای چند Bot روی یک Webhook مشترک
- تشخیص حذف Bot و `deleteWebhook`
- حذف cascade داده Backend همان Bot

مرحله بعد:

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
- Encryption/Vault Tokenها
- اشتراک/Trial هفت‌روزه شبیه مدل Babba
