// این Edge Function API مدیریت فروشنده App BotStore است.
// مالک Bot در MVP با Token ثبت‌شده همان Bot احراز می‌شود و هیچ service_role داخل APK قرار نمی‌گیرد.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// پاسخ JSON استاندارد برای Android ساخته می‌شود.
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  })
}

// کلاینت مدیریتی فقط داخل Edge Runtime ساخته می‌شود.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // نبود ساختار جدید Secret Keys به fallback قدیمی آسیبی نمی‌زند.
    }
  }
  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
}

// Statusهایی که فروشنده اجازه ثبت آن‌ها را دارد محدود می‌شوند.
const ORDER_STATUSES = new Set([
  'new',
  'awaiting_payment',
  'paid',
  'processing',
  'shipped',
  'completed',
  'cancelled',
])

// طول متن‌های قابل تنظیم از سمت فروشنده پیش از رسیدن به Constraint دیتابیس محدود می‌شود.
function cleanText(value: unknown, maxLength: number) {
  return String(value ?? '').trim().slice(0, maxLength)
}

Deno.serve(async (req) => {
  // این API فقط درخواست POST می‌پذیرد.
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)

  try {
    // تمام فیلدهای قابل استفاده در Actionهای مدیریتی از Body خوانده می‌شوند.
    const body = await req.json().catch(() => ({})) as {
      token?: string
      action?: string
      order_id?: number
      customer_id?: number
      status?: string
      blocked?: boolean
      limit?: number
      store_name?: string
      welcome_text?: string
      support_text?: string
      about_text?: string
    }

    const token = body.token?.trim() ?? ''
    const action = body.action?.trim() ?? ''

    // Token malformed قبل از Query دیتابیس رد می‌شود.
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'توکن ربات صحیح نیست.' }, 400)
    }

    const supabase = createAdminClient()

    // هر Action فقط برای Bot فعالی که Token دقیق آن در Backend ثبت شده مجاز است.
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .select('id, telegram_bot_id, username, first_name, active')
      .eq('bot_token', token)
      .eq('active', true)
      .maybeSingle()

    if (botError) throw botError
    if (!bot) return json({ ok: false, error: 'BOT_NOT_REGISTERED', message: 'ربات فعال یا ثبت‌شده نیست.' }, 404)

    const botId = Number(bot.id)
    const requestedLimit = Math.trunc(Number(body.limit ?? 50))
    const limit = Math.min(100, Math.max(1, requestedLimit))

    // تنظیمات عمومی یک Bot خوانده می‌شود؛ اگر رکورد هنوز وجود نداشته باشد به‌صورت Lazy ساخته می‌شود.
    if (action === 'get_settings') {
      const { data: existing, error: existingError } = await supabase
        .from('botstore_settings')
        .select('bot_id, store_name, welcome_text, support_text, about_text, updated_at')
        .eq('bot_id', botId)
        .maybeSingle()
      if (existingError) throw existingError

      let settings = existing
      if (!settings) {
        const { data: created, error: createError } = await supabase
          .from('botstore_settings')
          .insert({ bot_id: botId })
          .select('bot_id, store_name, welcome_text, support_text, about_text, updated_at')
          .single()
        if (createError) throw createError
        settings = created
      }

      return json({
        ok: true,
        bot: {
          telegram_id: bot.telegram_bot_id,
          username: bot.username ?? '',
          first_name: bot.first_name ?? '',
        },
        settings,
      })
    }

    // تنظیمات عمومی با Upsert و کلید bot_id ذخیره می‌شوند تا هر فروشگاه تنظیمات مستقل داشته باشد.
    if (action === 'set_settings') {
      const payload = {
        bot_id: botId,
        store_name: cleanText(body.store_name, 80),
        welcome_text: cleanText(body.welcome_text, 1000),
        support_text: cleanText(body.support_text, 1200),
        about_text: cleanText(body.about_text, 1200),
        updated_at: new Date().toISOString(),
      }

      const { data: settings, error } = await supabase
        .from('botstore_settings')
        .upsert(payload, { onConflict: 'bot_id' })
        .select('bot_id, store_name, welcome_text, support_text, about_text, updated_at')
        .single()
      if (error) throw error

      return json({ ok: true, settings })
    }

    // Overview تعداد مشتری، سفارش و سفارش‌های جدید را برمی‌گرداند.
    if (action === 'overview') {
      const [customersResult, ordersResult, newOrdersResult] = await Promise.all([
        supabase.from('botstore_customers').select('id', { count: 'exact', head: true }).eq('bot_id', botId),
        supabase.from('botstore_orders').select('id', { count: 'exact', head: true }).eq('bot_id', botId),
        supabase.from('botstore_orders').select('id', { count: 'exact', head: true }).eq('bot_id', botId).eq('status', 'new'),
      ])
      if (customersResult.error) throw customersResult.error
      if (ordersResult.error) throw ordersResult.error
      if (newOrdersResult.error) throw newOrdersResult.error
      return json({
        ok: true,
        bot: { telegram_id: bot.telegram_bot_id, username: bot.username ?? '', first_name: bot.first_name ?? '' },
        overview: {
          customers: customersResult.count ?? 0,
          orders: ordersResult.count ?? 0,
          new_orders: newOrdersResult.count ?? 0,
        },
      })
    }

    // فهرست سفارش‌ها فقط از همان Bot خوانده می‌شود.
    if (action === 'orders') {
      const { data: orders, error } = await supabase
        .from('botstore_orders')
        .select('id, order_code, customer_id, telegram_user_id, status, total_price, created_at, updated_at')
        .eq('bot_id', botId)
        .order('created_at', { ascending: false })
        .limit(limit)
      if (error) throw error

      // مشتری‌های همان صفحه یکجا خوانده می‌شوند تا N+1 Query ایجاد نشود.
      const customerIds = Array.from(new Set((orders ?? []).map((order: any) => Number(order.customer_id)).filter((id) => id > 0)))
      let customerMap = new Map<number, any>()
      if (customerIds.length) {
        const { data: customers, error: customersError } = await supabase
          .from('botstore_customers')
          .select('id, first_name, username, telegram_user_id, blocked')
          .eq('bot_id', botId)
          .in('id', customerIds)
        if (customersError) throw customersError
        customerMap = new Map<number, any>((customers ?? []).map((customer: any) => [Number(customer.id), customer]))
      }

      return json({
        ok: true,
        orders: (orders ?? []).map((order: any) => ({ ...order, customer: customerMap.get(Number(order.customer_id)) ?? null })),
      })
    }

    // جزئیات سفارش و Snapshot اقلام همان سفارش برگردانده می‌شود.
    if (action === 'order_details') {
      const orderId = Math.trunc(Number(body.order_id ?? 0))
      if (orderId <= 0) return json({ ok: false, error: 'INVALID_ORDER_ID' }, 400)

      const { data: order, error: orderError } = await supabase
        .from('botstore_orders')
        .select('id, order_code, customer_id, telegram_user_id, status, total_price, created_at, updated_at')
        .eq('bot_id', botId)
        .eq('id', orderId)
        .maybeSingle()
      if (orderError) throw orderError
      if (!order) return json({ ok: false, error: 'ORDER_NOT_FOUND' }, 404)

      const { data: items, error: itemsError } = await supabase
        .from('botstore_order_items')
        .select('id, product_id, title_snapshot, unit_price, quantity, line_total')
        .eq('order_id', orderId)
        .order('id')
      if (itemsError) throw itemsError

      let customer = null
      if (order.customer_id) {
        const { data, error } = await supabase
          .from('botstore_customers')
          .select('id, telegram_user_id, first_name, username, blocked')
          .eq('bot_id', botId)
          .eq('id', order.customer_id)
          .maybeSingle()
        if (error) throw error
        customer = data
      }
      return json({ ok: true, order, items: items ?? [], customer })
    }

    // مشتری‌های همان فروشگاه برای CRM اولیه برگردانده می‌شوند.
    if (action === 'customers') {
      const { data: customers, error } = await supabase
        .from('botstore_customers')
        .select('id, telegram_user_id, first_name, username, blocked, created_at, updated_at')
        .eq('bot_id', botId)
        .order('created_at', { ascending: false })
        .limit(limit)
      if (error) throw error
      return json({ ok: true, customers: customers ?? [] })
    }

    // وضعیت سفارش فقط با allow-list و شرط bot_id قابل تغییر است.
    if (action === 'set_order_status') {
      const orderId = Math.trunc(Number(body.order_id ?? 0))
      const status = body.status?.trim() ?? ''
      if (orderId <= 0 || !ORDER_STATUSES.has(status)) return json({ ok: false, error: 'INVALID_ORDER_UPDATE' }, 400)

      const { data: order, error } = await supabase
        .from('botstore_orders')
        .update({ status, updated_at: new Date().toISOString() })
        .eq('bot_id', botId)
        .eq('id', orderId)
        .select('id, order_code, status, updated_at')
        .maybeSingle()
      if (error) throw error
      if (!order) return json({ ok: false, error: 'ORDER_NOT_FOUND' }, 404)
      return json({ ok: true, order })
    }

    // Block/Unblock فقط روی مشتری متعلق به همین Bot اعمال می‌شود.
    if (action === 'set_customer_blocked') {
      const customerId = Math.trunc(Number(body.customer_id ?? 0))
      if (customerId <= 0 || typeof body.blocked !== 'boolean') return json({ ok: false, error: 'INVALID_CUSTOMER_UPDATE' }, 400)

      const { data: customer, error } = await supabase
        .from('botstore_customers')
        .update({ blocked: body.blocked, updated_at: new Date().toISOString() })
        .eq('bot_id', botId)
        .eq('id', customerId)
        .select('id, telegram_user_id, first_name, username, blocked, updated_at')
        .maybeSingle()
      if (error) throw error
      if (!customer) return json({ ok: false, error: 'CUSTOMER_NOT_FOUND' }, 404)
      return json({ ok: true, customer })
    }

    return json({ ok: false, error: 'UNKNOWN_ACTION' }, 400)
  } catch (error) {
    console.error('[botstore-manage]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'خطای داخلی در مدیریت فروشگاه.' }, 500)
  }
})
