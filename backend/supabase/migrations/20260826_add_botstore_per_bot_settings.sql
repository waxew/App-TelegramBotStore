-- این Migration تنظیمات عمومی مستقل هر فروشگاه را به Backend App BotStore اضافه می‌کند.
-- تمام داده‌ها server-only هستند و Android فقط از طریق Edge Function مدیریت‌شده به آن‌ها دسترسی دارد.

-- هر Bot دقیقاً یک رکورد تنظیمات عمومی دارد و با حذف Bot، تنظیمات هم خودکار حذف می‌شوند.
create table if not exists public.botstore_settings (
  bot_id bigint primary key references public.botstore_bots(id) on delete cascade,
  store_name text not null default '' check (char_length(store_name) <= 80),
  welcome_text text not null default '' check (char_length(welcome_text) <= 1000),
  support_text text not null default '' check (char_length(support_text) <= 1200),
  about_text text not null default '' check (char_length(about_text) <= 1200),
  updated_at timestamptz not null default now()
);

-- دسترسی مستقیم Data API برای کاربران برنامه بسته می‌شود.
alter table public.botstore_settings enable row level security;
revoke all on table public.botstore_settings from anon, authenticated;

-- فقط Edge Functionهای دارای service_role اجازه خواندن و تغییر تنظیمات را دارند.
grant select, insert, update, delete on table public.botstore_settings to service_role;

-- برای Botهای موجود رکورد پیش‌فرض ساخته می‌شود؛ Botهای آینده در API مدیریت به‌صورت Lazy رکورد می‌گیرند.
insert into public.botstore_settings (bot_id)
select id from public.botstore_bots
on conflict (bot_id) do nothing;
