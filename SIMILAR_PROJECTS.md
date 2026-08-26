# پروژه‌های مشابه برای بررسی

این پروژه‌ها فقط برای بررسی معماری، UX و قابلیت‌ها فهرست شده‌اند. استفاده از کد هر مخزن باید مطابق License همان پروژه انجام شود.

## 1) ilyarolf/AiogramShopBot

GitHub: https://github.com/ilyarolf/AiogramShopBot

نزدیک‌ترین مرجع از نظر Backend حرفه‌ای و چندرباتی است. امکانات شاخص:

- Aiogram 3 + FastAPI
- PostgreSQL + Redis
- Webhook و Docker
- Multibot mode
- دسته‌بندی، زیر‌دسته، سبد خرید و تاریخچه خرید
- پنل ادمین
- کد تخفیف، Referral، Review، Shipping و Analytics

برای App BotStore، معماری Multibot و جداسازی Handler/Service/Repository آن ارزش بررسی دارد.

## 2) JumpCodeFrog/telegram-shop-bot

GitHub: https://github.com/JumpCodeFrog/telegram-shop-bot

نمونه کامل فروشگاه تلگرام با قابلیت‌هایی مانند:

- Catalog و Cart
- Telegram Stars و USDT
- Subscription
- Promo Code
- Wishlist
- Referral و Loyalty
- Admin Panel
- Mini App
- Analytics و CSV Export
- Redis و Health Check

برای Roadmap فروشگاه، پرداخت، Mini App و گزارش‌گیری مرجع خوبی است.

## 3) interlumpen/Telegram-shop

GitHub: https://github.com/interlumpen/Telegram-shop

تمرکز اصلی روی فروش کالای دیجیتال و مدیریت فروشگاه است:

- Catalog و Stock
- Cart
- چند روش پرداخت
- پنل ادمین داخل چت و وب
- Role-based access
- جستجو
- اعلان موجود شدن کالا
- Redis اختیاری

برای طراحی سیستم موجودی، نقش‌های ادمین و Notification قابل بررسی است.

## 4) XayitovB/shop-bot

GitHub: https://github.com/XayitovB/shop-bot

یک نمونه ساده‌تر بر پایه Aiogram 3 است:

- Product Catalog
- Shopping Cart
- Checkout
- Inline Keyboard

برای فهم سریع Flow پایه فروشگاه و جدا کردن MVP از امکانات پیشرفته مناسب است.

## نتیجه برای App BotStore

هیچ‌کدام دقیقاً همان مدل «اپ اندروید فروشگاه‌ساز که توکن BotFather چند مشتری را می‌گیرد و چند ربات مستقل را مدیریت می‌کند» نیستند. بهترین مسیر این است که UI و مدیریت حساب در Android باقی بماند و Backend چندمستاجری، اجرای 24/7 ربات‌ها، دیتابیس و Webhookها به‌صورت سرویس مستقل توسعه داده شوند.
