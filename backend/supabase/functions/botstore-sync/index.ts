// این Edge Function محصولات و دسته‌بندی‌های ساخته‌شده داخل Android را با فروشگاه واقعی همان Bot همگام می‌کند.
// مالکیت در MVP با Token معتبر BotFather کنترل می‌شود و هیچ Service Key داخل APK قرار نمی‌گیرد.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// مدل ورودی دسته‌بندی فقط فیلدهای لازم برای Catalog را می‌پذیرد.
type InputCategory = { title?: string; emoji?: string }
// مدل ورودی محصول فقط داده‌های موردنیاز ربات فروشگاهی را می‌پذیرد.
type InputProduct = { title?: string; price?: number; category?: string; description?: string }

// پاسخ JSON یکنواخت ساخته می‌شود.
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  })
}

// کلاینت مدیریتی فقط سمت سرور ساخته می‌شود.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // نبود ساختار جدید Secret Keys به fallback قدیمی آسیب نمی‌زند.
    }
  }
  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
}

Deno.serve(async (req) => {
  // فقط POST برای Sync قابل قبول است.
  if (req.method !== 'POST') return json({ ok: false, error: 'METHOD_NOT_ALLOWED' }, 405)

  try {
    // Token و Catalog از body خوانده می‌شوند.
    const body = await req.json().catch(() => ({})) as {
      token?: string
      categories?: InputCategory[]
      products?: InputProduct[]
    }
    const token = body.token?.trim() ?? ''

    // فرمت اولیه Token قبل از Telegram API کنترل می‌شود.
    if (!/^\d+:[A-Za-z0-9_-]{20,}$/.test(token)) {
      return json({ ok: false, error: 'INVALID_TOKEN_FORMAT', message: 'فرمت توکن صحیح نیست.' }, 400)
    }

    // getMe تضمین می‌کند Token هنوز معتبر است و شناسه Bot برای lookup قابل اعتماد است.
    const getMeResponse = await fetch(`https://api.telegram.org/bot${token}/getMe`, {
      signal: AbortSignal.timeout(10_000),
    })
    const getMe = await getMeResponse.json().catch(() => null) as any
    if (!getMeResponse.ok || !getMe?.ok || !getMe?.result?.id) {
      return json({ ok: false, error: 'INVALID_BOT_TOKEN', message: 'توکن توسط تلگرام تایید نشد.' }, 400)
    }

    // کلاینت سرور ساخته و شناسه Telegram Bot استخراج می‌شود.
    const supabase = createAdminClient()
    const telegramBotId = Number(getMe.result.id)

    // هم شناسه Telegram و هم مقدار Token بررسی می‌شود تا Token قدیمی یا Rotate‌شده نتواند داده را تغییر دهد.
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .select('id, bot_token')
      .eq('telegram_bot_id', telegramBotId)
      .eq('bot_token', token)
      .eq('active', true)
      .maybeSingle()

    // خطای دیتابیس متوقف‌کننده است.
    if (botError) throw botError
    // Bot باید قبل از Sync از مسیر اتصال ثبت شده باشد.
    if (!bot) {
      return json({ ok: false, error: 'BOT_NOT_REGISTERED', message: 'ابتدا ربات را از صفحه اتصال فعال کنید.' }, 404)
    }

    // ورودی Android پاک‌سازی می‌شود تا عنوان خالی یا قیمت منفی وارد دیتابیس نشود.
    const categories = (body.categories ?? [])
      .map((category, index) => ({
        title: String(category.title ?? '').trim(),
        emoji: String(category.emoji ?? '🛍️').trim() || '🛍️',
        position: index,
      }))
      .filter((category) => category.title.length > 0)

    // محصولات نیز نرمال و قیمت آن‌ها غیرمنفی می‌شود.
    const products = (body.products ?? [])
      .map((product, index) => ({
        title: String(product.title ?? '').trim(),
        price: Math.max(0, Math.trunc(Number(product.price ?? 0))),
        category: String(product.category ?? '').trim(),
        description: String(product.description ?? '').trim(),
        position: index,
      }))
      .filter((product) => product.title.length > 0)

    // فعلاً Sync به‌صورت replace-all انجام می‌شود تا Android و Bot همیشه دقیقاً یک Catalog داشته باشند.
    const { error: deleteProductsError } = await supabase.from('botstore_products').delete().eq('bot_id', bot.id)
    if (deleteProductsError) throw deleteProductsError
    const { error: deleteCategoriesError } = await supabase.from('botstore_categories').delete().eq('bot_id', bot.id)
    if (deleteCategoriesError) throw deleteCategoriesError

    // نگاشت عنوان Category به شناسه جدید دیتابیس برای اتصال محصولات ساخته می‌شود.
    const categoryIdByTitle = new Map<string, number>()
    if (categories.length) {
      const { data: insertedCategories, error: categoriesError } = await supabase
        .from('botstore_categories')
        .insert(categories.map((category) => ({ bot_id: bot.id, ...category })))
        .select('id, title')
      if (categoriesError) throw categoriesError
      for (const category of insertedCategories ?? []) {
        categoryIdByTitle.set(String(category.title), Number(category.id))
      }
    }

    // محصولات به Category متناظر متصل و درج می‌شوند.
    if (products.length) {
      const productRows = products.map((product) => ({
        bot_id: bot.id,
        category_id: categoryIdByTitle.get(product.category) ?? null,
        title: product.title,
        price: product.price,
        description: product.description,
        active: true,
        position: product.position,
      }))
      const { error: productsError } = await supabase.from('botstore_products').insert(productRows)
      if (productsError) throw productsError
    }

    // تعداد داده‌های Sync‌شده به Android برگردانده می‌شود.
    return json({
      ok: true,
      categories_synced: categories.length,
      products_synced: products.length,
    })
  } catch (error) {
    // جزئیات سروری Log و پیام عمومی به Client برگردانده می‌شود.
    console.error('[botstore-sync]', error)
    return json({ ok: false, error: 'INTERNAL_ERROR', message: 'همگام‌سازی فروشگاه ناموفق بود.' }, 500)
  }
})
