# Roadmap پیشنهادی App BotStore

## v1.3.1 — Backend واقعی MVP ✅

- Backend چندرباته روی Supabase
- اعتبارسنجی BotFather Token
- `setWebhook` واقعی
- Webhook Secret مستقل
- `/start` واقعی
- Catalog مستقل هر Bot
- Sync Android → Backend
- `deleteWebhook` هنگام حذف Bot
- RLS و دسترسی server-only

## v1.4.0 — هسته فروشگاه‌ساز واقعی ✅

موارد زیر در نسخه 1.4.0 عملیاتی شده‌اند:

- سبد خرید واقعی داخل Telegram
- افزایش/کاهش Quantity و جمع کل
- Checkout اتمیک
- شماره سفارش
- سفارش‌های من
- پنل سفارش‌های فروشنده
- تغییر وضعیت سفارش
- پنل کاربران هر فروشگاه
- Block / Unblock
- اعمال Block در Runtime
- Catalog پایدار با `source_id`
- حفظ Cart هنگام ویرایش Product
- تنظیم نام فروشگاه
- پیام خوش‌آمدگویی اختصاصی
- پشتیبانی اختصاصی
- درباره فروشگاه اختصاصی
- لینک مستقیم پایدار هر Product با `/start` payload
- Share لینک Product از Android
- ارسال همگانی Bot-specific
- Snapshot گیرنده‌های Broadcast
- ارسال Batch و Resume بدون ارسال دوباره
- Progress و تاریخچه Broadcast
- بررسی دوباره Block قبل از ارسال
- اصلاح انتخاب واقعی Bot در حالت چندرباته

## v1.4.1 — Catalog دسته‌بندی‌محور و موجودی ✅

- Category-first در Android
- انتخاب اجباری Category هنگام افزودن/ویرایش Product
- اتصال پایدار Product به Category با `categoryId/source_id`
- نمایش Productها داخل Category
- انتقال Product بین Categoryها
- جلوگیری از حذف Category دارای Product
- موجودی عددی Product
- کنترل Stock در Cart و Checkout
- جلوگیری اتمیک از Oversell
- نمایش وضعیت موجودی در Telegram Runtime

## v1.5 — تکمیل قابلیت‌های فروشگاه

اولویت بعدی:

- کد تخفیف
- جستجوی Product
- تصویر و گالری Product
- محصول فیزیکی و دیجیتال
- ثبت آدرس مشتری
- روش‌های ارسال
- هزینه ارسال
- یادداشت سفارش
- اطلاعات تکمیلی مشتری
- تغییر گروهی قیمت
- Health Check و وضعیت Webhook داخل Android
- صفحه آخرین خطای Runtime
- Backup / Restore تنظیمات Bot

## v1.6 — پرداخت و اشتراک واقعی

- درگاه پرداخت ایرانی
- Telegram Stars در سناریوهای مجاز
- اتصال Payment به Order
- وضعیت `awaiting_payment / paid`
- کیف پول
- فاکتور و تاریخچه پرداخت
- Trial واقعی ۷ روزه
- توقف Runtime پس از پایان اشتراک
- تمدید و یادآوری انقضا
- کد معرف و Referral

## v1.7 — امنیت و مالکیت حساب‌محور

- Supabase Auth یا Auth معادل برای حساب App BotStore
- اتصال Bot به Owner واقعی
- حذف وابستگی عملیات مدیریتی صرفاً به Bot Token
- Encryption/Vault Tokenها
- Audit Log مدیریت
- Sessionهای قابل لغو
- نقش مدیر/اپراتور/پشتیبان
- محدودیت Rate برای APIهای مدیریتی

## v1.8 — چندپلتفرمی واقعی

- Telegram Adapter کامل
- WhatsApp Business Platform Adapter
- Rubika Adapter در صورت API رسمی/قابل اتکا
- Bale Adapter
- Interface مشترک Catalog/Order/Message بین پلتفرم‌ها

## v1.9 — Sync، Template و Clone

- کپی Catalog بین Botها
- Sync انتخابی یا کامل
- Templateهای آماده فروشگاهی
- Clone Bot
- Import / Export JSON
- تاریخچه تغییر تنظیمات
- Undo برای تنظیمات مهم

## v2.0 — Analytics، CRM و Mini App

- آمار کاربران فعال/جدید
- بازدید Product و Conversion
- فروش روزانه/هفتگی/ماهانه
- Productهای پرفروش
- پرونده مشتری و Tag
- کمپین پیشرفته
- خروجی CSV/Excel
- Telegram Mini App فروشگاهی
- پنل وب مکمل Android
- Notification واقعی سفارش و خطای Bot
- Ticket پشتیبانی
- API عمومی و Webhook خروجی

## اولویت فعلی

هسته‌ای که قبلاً مانع کارکرد واقعی ربات‌ساز بود در `v1.3.1` و `v1.4.0` بسته شده است. اولویت بعدی `v1.5` است: **موجودی + رزرو Stock + کد تخفیف + آدرس/ارسال**؛ سپس پرداخت و Trial واقعی انجام می‌شود.
