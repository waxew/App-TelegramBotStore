// این Edge Function Catalog Android را با شناسه‌های پایدار به همان Bot واقعی همگام می‌کند.
// stock_version تضمین می‌کند Sync خودکار Android موجودی مصرف‌شده در Backend را با مقدار قدیمی گوشی بازنویسی نکند.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

type InputCategory = { id?: string; source_id?: string; title?: string; emoji?: string }
type InputProduct = {
  id?: string
  source_id?: string
  title?: string
  price?: number
  category?: string
  category_source_id?: string
  description?: string
  active?: boolean
  stock_enabled?: boolean
  stock_quantity?: number
  stock_version?: string
}

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
    const body = await req.json().catch(() => ({})) as {
      token?: string
      categories?: InputCategory[]
      products?: InputProduct[]
    }
    const token = body.token?.trim() ?? ''
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'فرمت توکن صحیح نیست.' }, 400)
    }

    const getMeResponse = await fetch(`https://api.telegram.org/bot${token}/getMe`, { signal: AbortSignal.timeout(10_000) })
    const getMe = await getMeResponse.json().catch(() => null) as any
    if (!getMeResponse.ok || !getMe?.ok || !getMe?.result?.id) {
      return json({ ok: false, error: 'INVALID_BOT_TOKEN', message: 'توکن توسط تلگرام تایید نشد.' }, 400)
    }

    const supabase = createAdminClient()
    const telegramBotId = Number(getMe.result.id)
    const { data: bot, error: botError } = await supabase.from('botstore_bots')
      .select('id').eq('telegram_bot_id', telegramBotId).eq('bot_token', token).eq('active', true).maybeSingle()
    if (botError) throw botError
    if (!bot) return json({ ok: false, error: 'BOT_NOT_REGISTERED', message: 'ابتدا ربات را از صفحه اتصال فعال کنید.' }, 404)

    const categories = (body.categories ?? [])
      .map((category) => {
        const title = String(category.title ?? '').trim()
        const sourceId = String(category.source_id ?? category.id ?? `title:${title}`).trim()
        return {
          source_id: sourceId,
          title,
          emoji: String(category.emoji ?? '🛍️').trim() || '🛍️',
        }
      })
      .filter((category) => category.source_id.length > 0 && category.title.length > 0)

    const categorySourceByTitle = new Map(categories.map((category) => [category.title, category.source_id]))

    const products = (body.products ?? [])
      .map((product) => {
        const title = String(product.title ?? '').trim()
        const sourceId = String(product.source_id ?? product.id ?? `title:${title}`).trim()
        const categoryTitle = String(product.category ?? '').trim()
        const stockEnabled = product.stock_enabled === true
        return {
          source_id: sourceId,
          title,
          price: Math.max(0, Math.trunc(Number(product.price ?? 0))),
          category_source_id: String(product.category_source_id ?? categorySourceByTitle.get(categoryTitle) ?? '').trim(),
          description: String(product.description ?? '').trim(),
          active: product.active !== false,
          stock_enabled: stockEnabled,
          stock_quantity: stockEnabled ? Math.max(0, Math.trunc(Number(product.stock_quantity ?? 0))) : 0,
          stock_version: String(product.stock_version ?? '').trim().slice(0, 100),
        }
      })
      .filter((product) => product.source_id.length > 0 && product.title.length > 0)

    const { data: syncRows, error: syncError } = await supabase.rpc('botstore_sync_catalog', {
      p_bot_id: Number(bot.id),
      p_categories: categories,
      p_products: products,
    })
    if (syncError) throw syncError
    const result = syncRows?.[0] ?? { categories_synced: categories.length, products_synced: products.length }

    return json({
      ok: true,
      categories_synced: Number(result.categories_synced ?? categories.length),
      products_synced: Number(result.products_synced ?? products.length),
      stable_ids: true,
      inventory_enabled: true,
      inventory_versioned: true,
    })
  } catch (error) {
    console.error('[botstore-sync]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'همگام‌سازی فروشگاه ناموفق بود.' }, 500)
  }
})
