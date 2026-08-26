-- این Migration تابع اتمیک ساخت Broadcast و Snapshot گیرنده‌های همان Bot را اضافه می‌کند.
-- تابع فقط به service_role داده می‌شود تا Android نتواند مستقیماً از Data API آن را اجرا کند.

create or replace function public.botstore_create_broadcast(
  p_bot_id bigint,
  p_message_text text
)
returns table (
  broadcast_id bigint,
  total_recipients integer
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_broadcast_id bigint;
  v_total integer;
begin
  -- شناسه Bot باید معتبر باشد.
  if p_bot_id is null or p_bot_id <= 0 then
    raise exception 'INVALID_BOT_ID';
  end if;

  -- متن Broadcast با محدودیت امن زیر سقف Telegram بررسی می‌شود.
  if p_message_text is null or char_length(trim(p_message_text)) < 1 or char_length(trim(p_message_text)) > 4000 then
    raise exception 'INVALID_BROADCAST_MESSAGE';
  end if;

  -- فقط Bot فعال اجازه ساخت صف دارد.
  if not exists (select 1 from public.botstore_bots where id = p_bot_id and active = true) then
    raise exception 'BOT_NOT_ACTIVE';
  end if;

  -- رکورد اصلی Broadcast ایجاد می‌شود.
  insert into public.botstore_broadcasts (bot_id, message_text, status)
  values (p_bot_id, trim(p_message_text), 'queued')
  returning id into v_broadcast_id;

  -- کاربران Blockنشده همان Bot در همان لحظه Snapshot می‌شوند.
  insert into public.botstore_broadcast_recipients (
    broadcast_id,
    customer_id,
    telegram_user_id,
    status
  )
  select
    v_broadcast_id,
    c.id,
    c.telegram_user_id,
    'pending'
  from public.botstore_customers c
  where c.bot_id = p_bot_id
    and c.blocked = false
  on conflict (broadcast_id, telegram_user_id) do nothing;

  -- تعداد دقیق گیرنده‌ها برای آمار صفحه Android محاسبه می‌شود.
  select count(*)::integer
  into v_total
  from public.botstore_broadcast_recipients
  where broadcast_id = v_broadcast_id;

  -- Broadcast بدون گیرنده همان لحظه کامل محسوب می‌شود.
  update public.botstore_broadcasts
  set total_recipient_count = v_total,
      status = case when v_total = 0 then 'completed' else 'queued' end,
      completed_at = case when v_total = 0 then now() else null end
  where id = v_broadcast_id;

  return query select v_broadcast_id, v_total;
end;
$$;

-- اجرای مستقیم RPC برای نقش‌های Client بسته می‌شود.
revoke all on function public.botstore_create_broadcast(bigint, text) from public, anon, authenticated;
-- Worker Backend تنها مصرف‌کننده مستقیم این RPC است.
grant execute on function public.botstore_create_broadcast(bigint, text) to service_role;
