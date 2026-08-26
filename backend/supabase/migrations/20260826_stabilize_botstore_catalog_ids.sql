-- UUID محلی Android به رکورد Backend متصل می‌شود تا Sync بعدی PK محصول/دسته را عوض نکند.
alter table public.botstore_categories add column if not exists source_id text;
alter table public.botstore_products add column if not exists source_id text;

-- رکوردهای قدیمی یک source_id یکتای legacy می‌گیرند تا Migration بدون حذف داده قابل اجرا باشد.
update public.botstore_categories
set source_id = 'legacy-category-' || id::text
where source_id is null or btrim(source_id) = '';

update public.botstore_products
set source_id = 'legacy-product-' || id::text
where source_id is null or btrim(source_id) = '';

alter table public.botstore_categories alter column source_id set not null;
alter table public.botstore_products alter column source_id set not null;

create unique index if not exists botstore_categories_bot_source_uidx
  on public.botstore_categories(bot_id, source_id);
create unique index if not exists botstore_products_bot_source_uidx
  on public.botstore_products(bot_id, source_id);

-- تمام Upsert/Deleteهای Catalog داخل یک تراکنش انجام می‌شوند تا Sync نیمه‌کاره ایجاد نشود.
create or replace function public.botstore_sync_catalog(
  p_bot_id bigint,
  p_categories jsonb,
  p_products jsonb
)
returns table (categories_synced integer, products_synced integer)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_categories integer := 0;
  v_products integer := 0;
begin
  if jsonb_typeof(coalesce(p_categories, '[]'::jsonb)) <> 'array'
     or jsonb_typeof(coalesce(p_products, '[]'::jsonb)) <> 'array' then
    raise exception 'catalog payload must contain arrays';
  end if;

  if not exists (
    select 1 from public.botstore_bots b
    where b.id = p_bot_id and b.active = true
  ) then
    raise exception 'bot not active';
  end if;

  insert into public.botstore_categories (bot_id, source_id, title, emoji, position)
  select
    p_bot_id,
    btrim(item.value->>'source_id'),
    btrim(item.value->>'title'),
    coalesce(nullif(btrim(item.value->>'emoji'), ''), '🛍️'),
    (item.ordinality - 1)::integer
  from jsonb_array_elements(coalesce(p_categories, '[]'::jsonb)) with ordinality as item(value, ordinality)
  where btrim(coalesce(item.value->>'source_id', '')) <> ''
    and btrim(coalesce(item.value->>'title', '')) <> ''
  on conflict (bot_id, source_id)
  do update set
    title = excluded.title,
    emoji = excluded.emoji,
    position = excluded.position;

  get diagnostics v_categories = row_count;

  insert into public.botstore_products (
    bot_id, source_id, category_id, title, price, description, active, position
  )
  select
    p_bot_id,
    btrim(item.value->>'source_id'),
    category.id,
    btrim(item.value->>'title'),
    greatest(0, coalesce((item.value->>'price')::bigint, 0)),
    coalesce(item.value->>'description', ''),
    coalesce((item.value->>'active')::boolean, true),
    (item.ordinality - 1)::integer
  from jsonb_array_elements(coalesce(p_products, '[]'::jsonb)) with ordinality as item(value, ordinality)
  left join public.botstore_categories category
    on category.bot_id = p_bot_id
   and category.source_id = nullif(btrim(item.value->>'category_source_id'), '')
  where btrim(coalesce(item.value->>'source_id', '')) <> ''
    and btrim(coalesce(item.value->>'title', '')) <> ''
  on conflict (bot_id, source_id)
  do update set
    category_id = excluded.category_id,
    title = excluded.title,
    price = excluded.price,
    description = excluded.description,
    active = excluded.active,
    position = excluded.position;

  get diagnostics v_products = row_count;

  -- فقط Product واقعاً حذف‌شده از Android پاک می‌شود؛ Cart همان Product در این حالت با Cascade حذف می‌شود.
  delete from public.botstore_products p
  where p.bot_id = p_bot_id
    and not exists (
      select 1
      from jsonb_array_elements(coalesce(p_products, '[]'::jsonb)) as item(value)
      where btrim(coalesce(item.value->>'source_id', '')) = p.source_id
    );

  delete from public.botstore_categories c
  where c.bot_id = p_bot_id
    and not exists (
      select 1
      from jsonb_array_elements(coalesce(p_categories, '[]'::jsonb)) as item(value)
      where btrim(coalesce(item.value->>'source_id', '')) = c.source_id
    );

  return query select v_categories, v_products;
end;
$$;

revoke all on function public.botstore_sync_catalog(bigint,jsonb,jsonb) from public, anon, authenticated;
grant execute on function public.botstore_sync_catalog(bigint,jsonb,jsonb) to service_role;
