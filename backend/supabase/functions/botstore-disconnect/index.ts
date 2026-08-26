// این Edge Function اتصال یک Bot را به‌صورت واقعی از Backend حذف می‌کند و Webhook تلگرام را خاموش می‌کند.
// Token خود Bot در MVP نقش اثبات کنترل Bot را دارد و هیچ Secret مدیریتی به Android داده نمی‌شود.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// پاسخ JSON یکنواخت برای Android ساخته می‌شود.
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  })
}

// کلاینت مدیریتی فقط در محیط امن Edge Function ساخته می‌شود.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''

  // پروژه‌های جدید Supabase ممکن است Secret پیش‌فرض را با نام پویا معرفی کنند.
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // نبود ساختار جدید به fallback قدیمی آسیب نمی‌زند.
    }
  }

  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
}

Deno.serve(async (req) => {
  // فقط درخواست POST پذیرفته می‌شود.
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)

  try {
    // Token از body خوانده و پاک‌سازی می‌شود.
    const body = await req.json().catch(() => ({})) as { token?: string }
    const token = body.token?.trim() ?? ''

    // فرمت اولیه Token بررسی می‌شود.
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'فرمت توکن صحیح نیست.' }, 400)
    }

    // رکورد Bot فقط با تطبیق دقیق Token در جدول server-only پیدا می‌شود.
    const supabase = createAdminClient()
    const { data: bot, error: findError } = await supabase
      .from('botstore_bots')
      .select('id, telegram_bot_id, username')
      .eq('bot_token', token)
      .maybeSingle()

    if (findError) throw findError

    // اگر Bot قبلاً حذف شده باشد عملیات idempotent موفق محسوب می‌شود.
    if (!bot) {
      return json({ ok: true, disconnected: false, already_removed: true })
    }

    // ابتدا Webhook سمت Telegram حذف می‌شود تا Bot بلافاصله Update جدید دریافت نکند.
    const deleteWebhookResponse = await fetch(`https://api.telegram.org/bot${token}/deleteWebhook`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ drop_pending_updates: false }),
      signal: AbortSignal.timeout(10_000),
    })

    // پاسخ Telegram خوانده می‌شود؛ Token باطل مانع پاک‌سازی رکورد داخلی نمی‌شود.
    const deleteWebhook = await deleteWebhookResponse.json().catch(() => null) as any
    const telegramWebhookRemoved = Boolean(deleteWebhookResponse.ok && deleteWebhook?.ok)

    // رکورد Bot حذف می‌شود و Foreign Keyهای cascade، Catalog همان Bot را نیز پاک می‌کنند.
    const { error: deleteError } = await supabase
      .from('botstore_bots')
      .delete()
      .eq('id', bot.id)
      .eq('bot_token', token)

    if (deleteError) throw deleteError

    // نتیجه برای Log و UI برگردانده می‌شود ولی Token هرگز در پاسخ نیست.
    return json({
      ok: true,
      disconnected: true,
      telegram_webhook_removed: telegramWebhookRemoved,
      bot: {
        id: bot.telegram_bot_id,
        username: bot.username ?? '',
      },
    })
  } catch (error) {
    // جزئیات فقط سمت سرور Log و پیام عمومی به Android برگردانده می‌شود.
    console.error('[botstore-disconnect]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'حذف اتصال ربات از Backend ناموفق بود.' }, 500)
  }
})
