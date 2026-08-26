// این Edge Function مسئول ثبت واقعی ربات کاربر در Backend و فعال‌کردن Webhook تلگرام است.
// احراز مالکیت در نسخه MVP با خود Bot Token انجام می‌شود؛ توکن ابتدا مستقیماً با getMe تلگرام اعتبارسنجی می‌شود.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// پاسخ JSON یکنواخت برای Android و ابزارهای تست ساخته می‌شود.
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  })
}

// کلید مدیریتی Supabase فقط داخل محیط امن Edge Function خوانده می‌شود و هرگز به Android ارسال نمی‌شود.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''

  // پروژه‌های جدید Supabase ممکن است نام Secret Key پیش‌فرض را داخل SUPABASE_SECRET_KEYS معرفی کنند.
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // اگر متغیر جدید وجود نداشت، fallback بالا استفاده می‌شود.
    }
  }

  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
}

// Secret مخصوص Webhook تنها از کاراکترهای مجاز Telegram ساخته می‌شود.
function makeWebhookSecret() {
  const bytes = crypto.getRandomValues(new Uint8Array(32))
  return Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)

  try {
    const body = await req.json().catch(() => ({})) as { token?: string }
    const token = body.token?.trim() ?? ''

    // قبل از هر عملیات دیتابیس، فرمت اولیه توکن کنترل می‌شود.
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'فرمت توکن ربات صحیح نیست.' }, 400)
    }

    // getMe مالکیت عملی توکن و معتبر بودن آن را مستقیماً از Telegram Bot API بررسی می‌کند.
    const getMeResponse = await fetch(`https://api.telegram.org/bot${token}/getMe`, {
      signal: AbortSignal.timeout(10_000),
    })
    const getMe = await getMeResponse.json().catch(() => null) as any

    if (!getMeResponse.ok || !getMe?.ok || !getMe?.result?.id) {
      return json({ ok: false, error: 'INVALID_BOT_TOKEN', message: getMe?.description ?? 'توکن توسط تلگرام تایید نشد.' }, 400)
    }

    const telegramBotId = Number(getMe.result.id)
    const username = String(getMe.result.username ?? '')
    const firstName = String(getMe.result.first_name ?? '')
    const webhookSecret = makeWebhookSecret()
    const supabase = createAdminClient()

    // هر BotFather Bot فقط یک رکورد دارد؛ در صورت Rotate شدن Token همان رکورد به‌روز می‌شود.
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .upsert({
        telegram_bot_id: telegramBotId,
        bot_token: token,
        username,
        first_name: firstName,
        webhook_secret: webhookSecret,
        active: true,
        updated_at: new Date().toISOString(),
      }, { onConflict: 'telegram_bot_id' })
      .select('id, telegram_bot_id, username, first_name')
      .single()

    if (botError || !bot) throw botError ?? new Error('Bot registration failed')

    // یک Webhook مشترک چندرباته استفاده می‌شود و bot_id مشخص می‌کند Update متعلق به کدام فروشگاه است.
    const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? ''
    const webhookUrl = `${supabaseUrl}/functions/v1/botstore-telegram?bot_id=${bot.id}`

    // secret_token باعث می‌شود Endpoint فقط Update واقعی همان Bot را بپذیرد.
    const setWebhookResponse = await fetch(`https://api.telegram.org/bot${token}/setWebhook`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url: webhookUrl,
        secret_token: webhookSecret,
        allowed_updates: ['message', 'callback_query'],
        drop_pending_updates: false,
      }),
      signal: AbortSignal.timeout(10_000),
    })
    const setWebhook = await setWebhookResponse.json().catch(() => null) as any

    if (!setWebhookResponse.ok || !setWebhook?.ok) {
      return json({
        ok: false,
        error: 'WEBHOOK_SETUP_FAILED',
        message: setWebhook?.description ?? 'ثبت Webhook در تلگرام ناموفق بود.',
      }, 502)
    }

    // Android فقط اطلاعات غیرحساس ربات و وضعیت Webhook را دریافت می‌کند.
    return json({
      ok: true,
      bot: {
        id: telegramBotId,
        username,
        first_name: firstName,
      },
      webhook_active: true,
    })
  } catch (error) {
    console.error('[botstore-register]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'خطای داخلی هنگام فعال‌سازی ربات.' }, 500)
  }
})
