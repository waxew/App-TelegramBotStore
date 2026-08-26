// این Edge Function موجودی واقعی Backend را برای Productهای همان Bot به Android برمی‌گرداند.
// API فقط خواندنی است؛ service_role داخل Runtime باقی می‌ماند و مالکیت در MVP با Token همان Bot کنترل می‌شود.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json; charset=utf-8' } })
}

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

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)
  try {
    const body = await req.json().catch(() => ({})) as { token?: string }
    const token = String(body.token ?? '').trim()
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'توکن ربات صحیح نیست.' }, 400)
    }

    const supabase = createAdminClient()
    const { data: bot, error: botError } = await supabase.from('botstore_bots')
      .select('id').eq('bot_token', token).eq('active', true).maybeSingle()
    if (botError) throw botError
    if (!bot) return json({ ok: false, error: 'BOT_NOT_REGISTERED', message: 'ربات فعال یا ثبت‌شده نیست.' }, 404)

    // source_id کلید اتصال به UUID محلی Android است و موجودی Backend را بدون وابستگی به PK داخلی برمی‌گرداند.
    const { data: products, error } = await supabase.from('botstore_products')
      .select('source_id, stock_enabled, stock_quantity')
      .eq('bot_id', Number(bot.id))
      .order('position')
      .order('id')
    if (error) throw error

    return json({
      ok: true,
      inventory: (products ?? []).map((product: any) => ({
        source_id: String(product.source_id ?? ''),
        stock_enabled: Boolean(product.stock_enabled),
        stock_quantity: Math.max(0, Number(product.stock_quantity ?? 0)),
      })),
    })
  } catch (error) {
    console.error('[botstore-inventory]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'دریافت موجودی فروشگاه ناموفق بود.' }, 500)
  }
})
