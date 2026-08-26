-- این Migration موجودی اختیاری Product و کنترل ضد Oversell را به App BotStore اضافه می‌کند.
-- Productهای قدیمی stock_enabled=false می‌مانند و برای سازگاری، موجودی آن‌ها نامحدود است.

-- دو فیلد موجودی به Product اضافه می‌شوند.
alter table public.botstore_products
  add column if not exists stock_enabled boolean not null default false,
  add column if not exists stock_quantity integer not null default 0;

-- Stock هیچ‌وقت نباید منفی ذخیره شود.
alter table public.botstore_products
  drop constraint if exists botstore_products_stock_quantity_check;
alter table public.botstore_products
  add constraint botstore_products_stock_quantity_check check (stock_quantity >= 0);

-- Queryهای Product فعال دارای ردیابی موجودی با Index جزئی سریع‌تر می‌شوند.
create index if not exists botstore_products_bot_stock_idx
  on public.botstore_products(bot_id, stock_enabled, stock_quantity)
  where active = true;

-- Catalog Sync علاوه بر اطلاعات قبلی، تنظیم موجودی Product را هم Upsert می‌کند.
create or replace function public.botstore_sync_catalog(
  p_bot_id bigint,
  p_categories jsonb,
  p_products jsonb
)
returns table(categories_synced integer, products_synced integer)
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
    bot_id, source_id, category_id, title, price, description, active, position,
    stock_enabled, stock_quantity
  )
  select
    p_bot_id,
    btrim(item.value->>'source_id'),
    category.id,
    btrim(item.value->>'title'),
    greatest(0, coalesce((item.value->>'price')::bigint, 0)),
    coalesce(item.value->>'description', ''),
    coalesce((item.value->>'active')::boolean, true),
    (item.ordinality - 1)::integer,
    coalesce((item.value->>'stock_enabled')::boolean, false),
    greatest(0, coalesce((item.value->>'stock_quantity')::integer, 0))
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
    position = excluded.position,
    stock_enabled = excluded.stock_enabled,
    stock_quantity = excluded.stock_quantity;

  get diagnostics v_products = row_count;

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

-- Cart هنگام افزایش Quantity Product را Row Lock می‌کند و مقدار بیشتر از Stock را نمی‌پذیرد.
create or replace function public.botstore_cart_change(
  p_bot_id bigint,
  p_telegram_user_id bigint,
  p_product_id bigint,
  p_delta integer
)
returns integer
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_current integer;
  v_new integer;
  v_lock_key bigint;
  v_stock_enabled boolean;
  v_stock_quantity integer;
begin
  if p_delta = 0 or p_delta < -99 or p_delta > 99 then
    raise exception 'invalid cart delta';
  end if;

  v_lock_key := hashtextextended(p_bot_id::text || ':' || p_telegram_user_id::text, 0);
  perform pg_advisory_xact_lock(v_lock_key);

  select p.stock_enabled, p.stock_quantity
  into v_stock_enabled, v_stock_quantity
  from public.botstore_products p
  where p.id = p_product_id
    and p.bot_id = p_bot_id
    and p.active = true
  for update;

  if not found then
    raise exception 'product not available';
  end if;

  select ci.quantity
  into v_current
  from public.botstore_cart_items ci
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and ci.product_id = p_product_id
  for update;

  v_current := coalesce(v_current, 0);
  v_new := v_current + p_delta;

  if p_delta > 0 then
    v_new := least(999, v_new);
    if v_stock_enabled and v_new > v_stock_quantity then
      raise exception 'insufficient stock';
    end if;

    insert into public.botstore_cart_items (
      bot_id, telegram_user_id, product_id, quantity, updated_at
    ) values (
      p_bot_id, p_telegram_user_id, p_product_id, p_delta, now()
    )
    on conflict (bot_id, telegram_user_id, product_id)
    do update set
      quantity = least(999, public.botstore_cart_items.quantity + excluded.quantity),
      updated_at = now()
    returning quantity into v_new;
    return v_new;
  end if;

  if v_current <= 0 then
    return 0;
  end if;

  if v_new <= 0 then
    delete from public.botstore_cart_items ci
    where ci.bot_id = p_bot_id
      and ci.telegram_user_id = p_telegram_user_id
      and ci.product_id = p_product_id;
    return 0;
  end if;

  update public.botstore_cart_items ci
  set quantity = v_new,
      updated_at = now()
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and ci.product_id = p_product_id;

  return v_new;
end;
$$;

-- Checkout همه Productهای Cart را Row Lock می‌کند، موجودی را کنترل می‌کند و Stock را در همان تراکنش کم می‌کند.
create or replace function public.botstore_checkout_order(
  p_bot_id bigint,
  p_telegram_user_id bigint
)
returns table (
  order_id bigint,
  order_code text,
  total_price bigint,
  item_count integer
)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_customer_id bigint;
  v_total bigint;
  v_item_count integer;
  v_order_id bigint;
  v_order_code text;
  v_lock_key bigint;
  v_invalid_count integer;
  v_shortage_count integer;
begin
  v_lock_key := hashtextextended(p_bot_id::text || ':' || p_telegram_user_id::text, 0);
  perform pg_advisory_xact_lock(v_lock_key);

  if not exists (
    select 1 from public.botstore_bots b
    where b.id = p_bot_id and b.active = true
  ) then
    raise exception 'bot not active';
  end if;

  insert into public.botstore_customers (
    bot_id, telegram_user_id, first_name, username, updated_at
  ) values (
    p_bot_id, p_telegram_user_id, '', '', now()
  )
  on conflict (bot_id, telegram_user_id)
  do update set updated_at = now();

  select c.id into v_customer_id
  from public.botstore_customers c
  where c.bot_id = p_bot_id
    and c.telegram_user_id = p_telegram_user_id
    and c.blocked = false;

  if v_customer_id is null then
    raise exception 'customer blocked';
  end if;

  if not exists (
    select 1 from public.botstore_cart_items ci
    where ci.bot_id = p_bot_id
      and ci.telegram_user_id = p_telegram_user_id
  ) then
    raise exception 'cart is empty';
  end if;

  -- ترتیب id ثابت احتمال Deadlock را هنگام Checkout چند Product کاهش می‌دهد.
  perform p.id
  from public.botstore_products p
  join public.botstore_cart_items ci
    on ci.product_id = p.id
   and ci.bot_id = p_bot_id
   and ci.telegram_user_id = p_telegram_user_id
  where p.bot_id = p_bot_id
  order by p.id
  for update;

  select count(*)::integer
  into v_invalid_count
  from public.botstore_cart_items ci
  left join public.botstore_products p
    on p.id = ci.product_id
   and p.bot_id = p_bot_id
   and p.active = true
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and p.id is null;

  if v_invalid_count > 0 then
    raise exception 'product not available';
  end if;

  select count(*)::integer
  into v_shortage_count
  from public.botstore_cart_items ci
  join public.botstore_products p
    on p.id = ci.product_id
   and p.bot_id = p_bot_id
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and p.stock_enabled = true
    and p.stock_quantity < ci.quantity;

  if v_shortage_count > 0 then
    raise exception 'insufficient stock';
  end if;

  select
    coalesce(sum(p.price * ci.quantity), 0)::bigint,
    count(*)::integer
  into v_total, v_item_count
  from public.botstore_cart_items ci
  join public.botstore_products p
    on p.id = ci.product_id
   and p.bot_id = p_bot_id
   and p.active = true
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id;

  if v_item_count = 0 or v_total <= 0 then
    raise exception 'cart is empty';
  end if;

  v_order_code := 'BS-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));

  insert into public.botstore_orders (
    order_code, bot_id, customer_id, telegram_user_id, status, total_price
  ) values (
    v_order_code, p_bot_id, v_customer_id, p_telegram_user_id, 'new', v_total
  )
  returning id into v_order_id;

  insert into public.botstore_order_items (
    order_id, product_id, title_snapshot, unit_price, quantity, line_total
  )
  select
    v_order_id,
    p.id,
    p.title,
    p.price,
    ci.quantity,
    p.price * ci.quantity
  from public.botstore_cart_items ci
  join public.botstore_products p
    on p.id = ci.product_id
   and p.bot_id = p_bot_id
   and p.active = true
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id;

  update public.botstore_products p
  set stock_quantity = p.stock_quantity - ci.quantity
  from public.botstore_cart_items ci
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and p.id = ci.product_id
    and p.bot_id = p_bot_id
    and p.stock_enabled = true;

  delete from public.botstore_cart_items ci
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id;

  return query select v_order_id, v_order_code, v_total, v_item_count;
end;
$$;

-- RPCها فقط از Backend service_role قابل اجرا هستند.
revoke all on function public.botstore_sync_catalog(bigint,jsonb,jsonb) from public, anon, authenticated;
revoke all on function public.botstore_cart_change(bigint,bigint,bigint,integer) from public, anon, authenticated;
revoke all on function public.botstore_checkout_order(bigint,bigint) from public, anon, authenticated;
grant execute on function public.botstore_sync_catalog(bigint,jsonb,jsonb) to service_role;
grant execute on function public.botstore_cart_change(bigint,bigint,bigint,integer) to service_role;
grant execute on function public.botstore_checkout_order(bigint,bigint) to service_role;
