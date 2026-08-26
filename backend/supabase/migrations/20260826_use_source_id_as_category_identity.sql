-- عنوان Category فقط داده نمایشی است و نباید هویت رکورد باشد؛ UUID محلی Android در source_id هویت پایدار را تعیین می‌کند.
alter table public.botstore_categories
  drop constraint if exists botstore_categories_bot_id_title_key;

-- source_id یکتا هویت Category را تضمین می‌کند؛ عنوان برای جستجو Index غیر یکتا دارد.
create index if not exists botstore_categories_bot_title_idx
  on public.botstore_categories(bot_id, title);
