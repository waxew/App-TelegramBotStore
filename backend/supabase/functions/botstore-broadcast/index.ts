// این Edge Function صف ارسال همگانی App BotStore را برای هر Bot به‌صورت مستقل مدیریت می‌کند.
// احراز کنترل Bot در MVP با Token ثبت‌شده همان Bot انجام می‌شود و service_role فقط در Edge Runtime باقی می‌ماند.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// پاسخ JSON یکنواخت برای Android ساخته می‌شود.
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  })
}

// کلاینت مدیریتی Supabase فقط سمت سرور ساخته می‌شود.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // نبود ساختار Secret جدید، fallback قدیمی را مختل نمی‌کند.
    }
  }
  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
}

// توقف کوتاه برای رعایت RetryAfter تلگرام استفاده می‌شود.
function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// یک پیام Broadcast ارسال می‌شود و Rate Limit کوتاه تلگرام یک بار Retry می‌شود.
async function sendTelegramMessage(token: string, chatId: number, text: string) {
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const response = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ chat_id: chatId, text }),
      signal: AbortSignal.timeout(10_000),
    })
    const payload = await response.json().catch(() => null) as any

    if (response.ok && payload?.ok) return { ok: true, error: '' }

    const retryAfter = Number(payload?.parameters?.retry_after ?? 0)
    if (response.status === 429 && retryAfter > 0 && retryAfter <= 5 && attempt === 0) {
      await sleep((retryAfter * 1000) + 150)
      continue
    }

    const description = String(payload?.description ?? `Telegram HTTP ${response.status}`)
    return { ok: false, error: description.slice(0, 500) }
  }

  return { ok: false, error: 'Telegram send retry exhausted' }
}

// مدل خروجی Broadcast برای Android کوچک و پایدار نگه داشته می‌شود.
function broadcastView(row: any) {
  return {
    id: Number(row.id),
    message_text: String(row.message_text ?? ''),
    status: String(row.status ?? 'queued'),
    total_recipient_count: Number(row.total_recipient_count ?? 0),
    sent_count: Number(row.sent_count ?? 0),
    failed_count: Number(row.failed_count ?? 0),
    created_at: String(row.created_at ?? ''),
    started_at: row.started_at ? String(row.started_at) : '',
    completed_at: row.completed_at ? String(row.completed_at) : '',
  }
}

Deno.serve(async (req) => {
  // فقط POST برای عملیات Broadcast قابل قبول است.
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)

  try {
    const body = await req.json().catch(() => ({})) as {
      token?: string
      action?: string
      message?: string
      broadcast_id?: number
      limit?: number
    }

    const token = String(body.token ?? '').trim()
    const action = String(body.action ?? '').trim()
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'توکن ربات صحیح نیست.' }, 400)
    }

    const supabase = createAdminClient()

    // تمام Actionها فقط برای Bot فعال با Token دقیق همان Bot مجاز هستند.
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .select('id, bot_token, username, active')
      .eq('bot_token', token)
      .eq('active', true)
      .maybeSingle()

    if (botError) throw botError
    if (!bot) return json({ ok: false, error: 'BOT_NOT_REGISTERED', message: 'ربات فعال یا ثبت‌شده نیست.' }, 404)

    const botId = Number(bot.id)

    // تاریخچه Broadcastها برای نمایش و Resume در Android خوانده می‌شود.
    if (action === 'list') {
      const requestedLimit = Math.trunc(Number(body.limit ?? 20))
      const limit = Math.min(50, Math.max(1, requestedLimit))
      const { data: rows, error } = await supabase
        .from('botstore_broadcasts')
        .select('id, message_text, status, total_recipient_count, sent_count, failed_count, created_at, started_at, completed_at')
        .eq('bot_id', botId)
        .order('created_at', { ascending: false })
        .limit(limit)
      if (error) throw error
      return json({ ok: true, broadcasts: (rows ?? []).map(broadcastView) })
    }

    // یک Broadcast جدید ساخته و کاربران Blockنشده همان لحظه Snapshot می‌شوند.
    if (action === 'create') {
      const message = String(body.message ?? '').trim()
      if (message.length < 1 || message.length > 4000) {
        return json({ ok: false, error: 'INVALID_MESSAGE', message: 'متن پیام باید بین ۱ تا ۴۰۰۰ نویسه باشد.' }, 400)
      }

      const { data: createdRows, error: createError } = await supabase.rpc('botstore_create_broadcast', {
        p_bot_id: botId,
        p_message_text: message,
      })
      if (createError) throw createError

      const created = createdRows?.[0]
      if (!created?.broadcast_id) throw new Error('Broadcast creation failed')

      const { data: row, error: rowError } = await supabase
        .from('botstore_broadcasts')
        .select('id, message_text, status, total_recipient_count, sent_count, failed_count, created_at, started_at, completed_at')
        .eq('bot_id', botId)
        .eq('id', Number(created.broadcast_id))
        .single()
      if (rowError) throw rowError
      return json({ ok: true, broadcast: broadcastView(row) })
    }

    // Status بدون اجرای Worker قابل دریافت است.
    if (action === 'status') {
      const broadcastId = Math.trunc(Number(body.broadcast_id ?? 0))
      if (broadcastId <= 0) return json({ ok: false, error: 'INVALID_BROADCAST_ID' }, 400)

      const { data: row, error } = await supabase
        .from('botstore_broadcasts')
        .select('id, message_text, status, total_recipient_count, sent_count, failed_count, created_at, started_at, completed_at')
        .eq('bot_id', botId)
        .eq('id', broadcastId)
        .maybeSingle()
      if (error) throw error
      if (!row) return json({ ok: false, error: 'BROADCAST_NOT_FOUND' }, 404)
      return json({ ok: true, broadcast: broadcastView(row) })
    }

    // هر Process فقط یک Batch کوچک را می‌فرستد تا اجرای Edge کوتاه و قابل ادامه باشد.
    if (action === 'process') {
      const broadcastId = Math.trunc(Number(body.broadcast_id ?? 0))
      if (broadcastId <= 0) return json({ ok: false, error: 'INVALID_BROADCAST_ID' }, 400)

      const { data: broadcast, error: broadcastError } = await supabase
        .from('botstore_broadcasts')
        .select('id, message_text, status, total_recipient_count, sent_count, failed_count, created_at, started_at, completed_at')
        .eq('bot_id', botId)
        .eq('id', broadcastId)
        .maybeSingle()
      if (broadcastError) throw broadcastError
      if (!broadcast) return json({ ok: false, error: 'BROADCAST_NOT_FOUND' }, 404)

      if (broadcast.status === 'completed' || broadcast.status === 'partial' || broadcast.status === 'failed') {
        return json({ ok: true, broadcast: broadcastView(broadcast), processed: 0, done: true })
      }

      // اندازه Batch حداکثر 20 کاربر است.
      const requestedBatch = Math.trunc(Number(body.limit ?? 20))
      const batchSize = Math.min(20, Math.max(1, requestedBatch))
      const { data: recipients, error: recipientError } = await supabase
        .from('botstore_broadcast_recipients')
        .select('id, customer_id, telegram_user_id, attempt_count')
        .eq('broadcast_id', broadcastId)
        .eq('status', 'pending')
        .order('id')
        .limit(batchSize)
      if (recipientError) throw recipientError

      // اولین Batch زمان شروع ارسال را ثبت می‌کند.
      if (!broadcast.started_at) {
        const { error } = await supabase
          .from('botstore_broadcasts')
          .update({ status: 'sending', started_at: new Date().toISOString() })
          .eq('bot_id', botId)
          .eq('id', broadcastId)
        if (error) throw error
      } else if (broadcast.status !== 'sending') {
        const { error } = await supabase
          .from('botstore_broadcasts')
          .update({ status: 'sending' })
          .eq('bot_id', botId)
          .eq('id', broadcastId)
        if (error) throw error
      }

      // اگر کاربر بعد از Snapshot Block شده باشد، قبل از Telegram دوباره کنترل می‌شود.
      const customerIds = (recipients ?? [])
        .map((recipient: any) => Number(recipient.customer_id ?? 0))
        .filter((id: number) => id > 0)
      const blockedCustomerIds = new Set<number>()
      if (customerIds.length) {
        const { data: customers, error: customersError } = await supabase
          .from('botstore_customers')
          .select('id, blocked')
          .eq('bot_id', botId)
          .in('id', customerIds)
        if (customersError) throw customersError
        for (const customer of customers ?? []) {
          if (customer.blocked) blockedCustomerIds.add(Number(customer.id))
        }
      }

      let processed = 0
      for (const recipient of recipients ?? []) {
        const recipientId = Number(recipient.id)
        const customerId = Number(recipient.customer_id ?? 0)
        const attempts = Number(recipient.attempt_count ?? 0) + 1

        if (customerId > 0 && blockedCustomerIds.has(customerId)) {
          const { error } = await supabase
            .from('botstore_broadcast_recipients')
            .update({
              status: 'failed',
              attempt_count: attempts,
              last_error: 'CUSTOMER_BLOCKED',
              attempted_at: new Date().toISOString(),
            })
            .eq('broadcast_id', broadcastId)
            .eq('id', recipientId)
          if (error) throw error
          processed += 1
          continue
        }

        const result = await sendTelegramMessage(token, Number(recipient.telegram_user_id), String(broadcast.message_text))
        const { error } = await supabase
          .from('botstore_broadcast_recipients')
          .update({
            status: result.ok ? 'sent' : 'failed',
            attempt_count: attempts,
            last_error: result.error,
            attempted_at: new Date().toISOString(),
          })
          .eq('broadcast_id', broadcastId)
          .eq('id', recipientId)
        if (error) throw error
        processed += 1

        // فاصله کوتاه از Burst سریع روی Bot API جلوگیری می‌کند.
        await sleep(45)
      }

      // آمار از وضعیت واقعی گیرنده‌ها محاسبه می‌شود تا Retry Client دوباره شماری ایجاد نکند.
      const [pendingResult, sentResult, failedResult] = await Promise.all([
        supabase.from('botstore_broadcast_recipients').select('id', { count: 'exact', head: true }).eq('broadcast_id', broadcastId).eq('status', 'pending'),
        supabase.from('botstore_broadcast_recipients').select('id', { count: 'exact', head: true }).eq('broadcast_id', broadcastId).eq('status', 'sent'),
        supabase.from('botstore_broadcast_recipients').select('id', { count: 'exact', head: true }).eq('broadcast_id', broadcastId).eq('status', 'failed'),
      ])
      if (pendingResult.error) throw pendingResult.error
      if (sentResult.error) throw sentResult.error
      if (failedResult.error) throw failedResult.error

      const pending = pendingResult.count ?? 0
      const sent = sentResult.count ?? 0
      const failed = failedResult.count ?? 0
      const done = pending === 0
      const finalStatus = !done ? 'sending' : failed === 0 ? 'completed' : sent > 0 ? 'partial' : 'failed'

      const { data: updated, error: updateError } = await supabase
        .from('botstore_broadcasts')
        .update({
          status: finalStatus,
          sent_count: sent,
          failed_count: failed,
          completed_at: done ? new Date().toISOString() : null,
        })
        .eq('bot_id', botId)
        .eq('id', broadcastId)
        .select('id, message_text, status, total_recipient_count, sent_count, failed_count, created_at, started_at, completed_at')
        .single()
      if (updateError) throw updateError

      return json({ ok: true, broadcast: broadcastView(updated), processed, pending, done })
    }

    return json({ ok: false, error: 'UNKNOWN_ACTION' }, 400)
  } catch (error) {
    console.error('[botstore-broadcast]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'خطای داخلی در ارسال همگانی.' }, 500)
  }
})
