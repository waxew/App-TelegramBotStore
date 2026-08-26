-- Cart mutation و Checkout یک قفل تراکنشی مشترک بر اساس Bot/User می‌گیرند تا Double Checkout و Race Condition رخ ندهد.
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
begin
  if p_delta = 0 or p_delta < -99 or p_delta > 99 then
    raise exception 'invalid cart delta';
  end if;

  v_lock_key := hashtextextended(p_bot_id::text || ':' || p_telegram_user_id::text, 0);
  perform pg_advisory_xact_lock(v_lock_key);

  if not exists (
    select 1
    from public.botstore_products p
    where p.id = p_product_id
      and p.bot_id = p_bot_id
      and p.active = true
  ) then
    raise exception 'product not available';
  end if;

  if p_delta > 0 then
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

  select ci.quantity into v_current
  from public.botstore_cart_items ci
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id
    and ci.product_id = p_product_id
  for update;

  if v_current is null then
    return 0;
  end if;

  v_new := v_current + p_delta;
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
begin
  v_lock_key := hashtextextended(p_bot_id::text || ':' || p_telegram_user_id::text, 0);
  perform pg_advisory_xact_lock(v_lock_key);

  if not exists (
    select 1
    from public.botstore_bots b
    where b.id = p_bot_id
      and b.active = true
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

  delete from public.botstore_cart_items ci
  where ci.bot_id = p_bot_id
    and ci.telegram_user_id = p_telegram_user_id;

  return query select v_order_id, v_order_code, v_total, v_item_count;
end;
$$;

revoke all on function public.botstore_cart_change(bigint,bigint,bigint,integer) from public, anon, authenticated;
revoke all on function public.botstore_checkout_order(bigint,bigint) from public, anon, authenticated;
grant execute on function public.botstore_cart_change(bigint,bigint,bigint,integer) to service_role;
grant execute on function public.botstore_checkout_order(bigint,bigint) to service_role;
