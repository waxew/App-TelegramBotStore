// این Edge Function Webhook مشترک تمام ربات‌های ساخته‌شده با App BotStore است.
// نسخه فعلی علاوه بر Catalog، مشتری، سبد خرید و سفارش واقعی را نیز برای هر Bot مستقل مدیریت می‌کند.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

type TelegramUpdate = {
  message?: {
    message_id: number
    text?: string
    chat: { id: number }
    from?: { id: number; first_name?: string; username?: string }
  }
  callback_query?: {
    id: string
    data?: string
    from: { id: number; first_name?: string; username?: string }
    message?: { message_id: number; chat: { id: number } }
  }
}

// تمام فراخوانی‌های Telegram Bot API از این تابع عبور می‌کنند تا مدیریت خطا یکدست باشد.
async function telegramCall(token: string, method: string, payload: Record<string, unknown>) {
  const response = await fetch(`https://api.telegram.org/bot${token}/${method}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(10_000),
  })
  const result = await response.json().catch(() => null) as any
  if (!response.ok || !result?.ok) {
    throw new Error(result?.description ?? `Telegram ${method} failed`)
  }
  return result.result
}

// کلاینت مدیریتی فقط سمت سرور ساخته می‌شود و هیچ Secret در APK قرار نمی‌گیرد.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
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

// قیمت‌ها با جداکننده هزارگان فارسی نمایش داده می‌شوند.
function money(value: number) {
  return new Intl.NumberFormat('fa-IR').format(Math.max(0, Math.trunc(value)))
}

// وضعیت داخلی سفارش به متن فارسی تبدیل می‌شود.
function orderStatusFa(status: string) {
  const map: Record<string, string> = {
    new: 'جدید',
    awaiting_payment: 'در انتظار پرداخت',
    paid: 'پرداخت‌شده',
    processing: 'در حال آماده‌سازی',
    shipped: 'ارسال‌شده',
    completed: 'تکمیل‌شده',
    cancelled: 'لغوشده',
  }
  return map[status] ?? status
}

// منوی اصلی فروشگاه برای /start ساخته می‌شود.
async function sendMainMenu(token: string, chatId: number, botName: string, firstName = '') {
  const hello = firstName ? `سلام ${firstName} 👋\n` : 'سلام 👋\n'
  await telegramCall(token, 'sendMessage', {
    chat_id: chatId,
    text: `${hello}به فروشگاه «${botName || 'فروشگاه'}» خوش آمدید.\nاز منوی زیر بخش موردنظر را انتخاب کنید.`,
    reply_markup: {
      keyboard: [
        [{ text: '🛍️ محصولات' }, { text: '🛒 سبد خرید' }],
        [{ text: '🧾 سفارش‌های من' }, { text: '👤 حساب من' }],
        [{ text: '☎️ پشتیبانی' }, { text: 'ℹ️ درباره فروشگاه' }],
      ],
      resize_keyboard: true,
      is_persistent: true,
    },
  })
}

// جزئیات محصول فقط در صورتی نمایش داده می‌شود که متعلق به همان Bot و فعال باشد.
async function sendProductDetail(supabase: any, token: string, chatId: number, botId: number, productId: number) {
  const { data: product, error } = await supabase
    .from('botstore_products')
    .select('id, title, price, description, active')
    .eq('id', productId)
    .eq('bot_id', botId)
    .eq('active', true)
    .maybeSingle()

  if (error) throw error
  if (!product) {
    await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'این محصول دیگر در دسترس نیست.' })
    return
  }

  await telegramCall(token, 'sendMessage', {
    chat_id: chatId,
    text: `🛍️ ${product.title}\n\n${product.description || 'بدون توضیحات'}\n\n💳 ${money(Number(product.price))} تومان`,
    reply_markup: {
      inline_keyboard: [
        [{ text: '➕ افزودن به سبد خرید', callback_data: `cart_add:${product.id}` }],
        [{ text: '🛒 مشاهده سبد خرید', callback_data: 'cart_show' }],
      ],
    },
  })
}

// سبد خرید کاربر از DB خوانده و با قیمت فعلی محصولات نمایش داده می‌شود.
async function sendCart(supabase: any, token: string, chatId: number, botId: number, telegramUserId: number) {
  const { data: cartRows, error: cartError } = await supabase
    .from('botstore_cart_items')
    .select('product_id, quantity')
    .eq('bot_id', botId)
    .eq('telegram_user_id', telegramUserId)
    .order('id')

  if (cartError) throw cartError
  if (!cartRows?.length) {
    await telegramCall(token, 'sendMessage', {
      chat_id: chatId,
      text: '🛒 سبد خرید شما خالی است.',
      reply_markup: { inline_keyboard: [[{ text: '🛍️ مشاهده محصولات', callback_data: 'products_show' }]] },
    })
    return
  }

  // Productها یکجا خوانده می‌شوند تا N+1 Query ایجاد نشود.
  const productIds = cartRows.map((row: any) => Number(row.product_id))
  const { data: products, error: productsError } = await supabase
    .from('botstore_products')
    .select('id, title, price, active')
    .eq('bot_id', botId)
    .in('id', productIds)

  if (productsError) throw productsError
  const productMap = new Map<number, any>((products ?? []).map((product: any) => [Number(product.id), product]))

  let total = 0
  const lines: string[] = []
  const buttons: any[][] = []

  cartRows.forEach((row: any, index: number) => {
    const product = productMap.get(Number(row.product_id))
    if (!product || !product.active) return
    const quantity = Number(row.quantity)
    const lineTotal = Number(product.price) * quantity
    total += lineTotal
    lines.push(`${index + 1}) ${product.title} × ${quantity}\n${money(lineTotal)} تومان`)
    buttons.push([
      { text: `➖ ${product.title}`, callback_data: `cart_dec:${product.id}` },
      { text: `➕ ${quantity}`, callback_data: `cart_add:${product.id}` },
    ])
  })

  if (!lines.length) {
    await supabase.from('botstore_cart_items').delete().eq('bot_id', botId).eq('telegram_user_id', telegramUserId)
    await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'محصولات سبد شما دیگر در دسترس نیستند و سبد پاک شد.' })
    return
  }

  buttons.push([{ text: '✅ ثبت سفارش', callback_data: 'checkout' }])
  buttons.push([{ text: '🗑 خالی کردن سبد', callback_data: 'cart_clear' }])

  await telegramCall(token, 'sendMessage', {
    chat_id: chatId,
    text: `🛒 سبد خرید شما\n\n${lines.join('\n\n')}\n\n💰 جمع کل: ${money(total)} تومان`,
    reply_markup: { inline_keyboard: buttons },
  })
}

// فهرست دسته‌ها یا در نبود دسته، محصولات فعال Bot را نمایش می‌دهد.
async function sendProducts(supabase: any, token: string, chatId: number, botId: number) {
  const { data: categories, error: categoryError } = await supabase
    .from('botstore_categories')
    .select('id, title, emoji')
    .eq('bot_id', botId)
    .order('position')
    .order('id')

  if (categoryError) throw categoryError
  if (categories?.length) {
    await telegramCall(token, 'sendMessage', {
      chat_id: chatId,
      text: 'دسته‌بندی موردنظر را انتخاب کنید:',
      reply_markup: {
        inline_keyboard: categories.map((category: any) => [{
          text: `${category.emoji || '🛍️'} ${category.title}`,
          callback_data: `category:${category.id}`,
        }]),
      },
    })
    return
  }

  const { data: products, error: productsError } = await supabase
    .from('botstore_products')
    .select('id, title, price')
    .eq('bot_id', botId)
    .eq('active', true)
    .order('position')
    .order('id')

  if (productsError) throw productsError
  if (!products?.length) {
    await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'هنوز محصولی برای این فروشگاه ثبت نشده است.' })
    return
  }

  await telegramCall(token, 'sendMessage', {
    chat_id: chatId,
    text: '🛍️ محصولات فروشگاه:',
    reply_markup: {
      inline_keyboard: products.map((product: any) => [{
        text: `${product.title} — ${money(Number(product.price))} تومان`,
        callback_data: `product:${product.id}`,
      }]),
    },
  })
}

// آخرین سفارش‌های همان User در همان Bot نمایش داده می‌شوند.
async function sendOrders(supabase: any, token: string, chatId: number, botId: number, telegramUserId: number) {
  const { data: orders, error } = await supabase
    .from('botstore_orders')
    .select('order_code, status, total_price, created_at')
    .eq('bot_id', botId)
    .eq('telegram_user_id', telegramUserId)
    .order('created_at', { ascending: false })
    .limit(8)

  if (error) throw error
  if (!orders?.length) {
    await telegramCall(token, 'sendMessage', { chat_id: chatId, text: '🧾 هنوز سفارشی ثبت نکرده‌اید.' })
    return
  }

  const lines = orders.map((order: any, index: number) =>
    `${index + 1}) ${order.order_code}\nوضعیت: ${orderStatusFa(String(order.status))}\nمبلغ: ${money(Number(order.total_price))} تومان`,
  )
  await telegramCall(token, 'sendMessage', { chat_id: chatId, text: `🧾 آخرین سفارش‌های شما\n\n${lines.join('\n\n')}` })
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') return new Response('ok', { status: 200 })

  try {
    const url = new URL(req.url)
    const botId = Number(url.searchParams.get('bot_id'))
    if (!Number.isInteger(botId) || botId <= 0) return new Response('ok', { status: 200 })

    const supabase = createAdminClient()
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .select('id, bot_token, username, first_name, webhook_secret, active')
      .eq('id', botId)
      .single()

    if (botError || !bot || !bot.active) return new Response('ok', { status: 200 })

    // Secret ثبت‌شده توسط setWebhook باید دقیقاً با Header Telegram برابر باشد.
    const suppliedSecret = req.headers.get('X-Telegram-Bot-Api-Secret-Token') ?? ''
    if (!suppliedSecret || suppliedSecret !== bot.webhook_secret) {
      console.warn(`[botstore-telegram] invalid webhook secret for bot_id=${botId}`)
      return new Response('ok', { status: 200 })
    }

    const update = await req.json() as TelegramUpdate
    const token = String(bot.bot_token)
    const actor = update.callback_query?.from ?? update.message?.from
    const chatId = update.callback_query?.message?.chat.id ?? update.message?.chat.id
    if (!actor || !chatId) return new Response('ok', { status: 200 })

    const telegramUserId = Number(actor.id)
    const firstName = String(actor.first_name ?? '')
    const username = String(actor.username ?? '')

    // هر تعامل، پروفایل مشتری همان Bot را Upsert می‌کند؛ blocked عمداً در payload نیست تا مقدار مدیریتی حفظ شود.
    const { data: customer, error: customerError } = await supabase
      .from('botstore_customers')
      .upsert({
        bot_id: botId,
        telegram_user_id: telegramUserId,
        first_name: firstName,
        username,
        updated_at: new Date().toISOString(),
      }, { onConflict: 'bot_id,telegram_user_id' })
      .select('id, blocked')
      .single()

    if (customerError) throw customerError

    if (customer?.blocked) {
      if (update.callback_query) {
        await telegramCall(token, 'answerCallbackQuery', {
          callback_query_id: update.callback_query.id,
          text: 'دسترسی شما به این فروشگاه مسدود شده است.',
          show_alert: true,
        }).catch(() => null)
      } else {
        await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'دسترسی شما به این فروشگاه مسدود شده است.' })
      }
      return new Response('ok', { status: 200 })
    }

    if (update.callback_query) {
      const callback = update.callback_query
      const data = callback.data ?? ''
      await telegramCall(token, 'answerCallbackQuery', { callback_query_id: callback.id }).catch(() => null)

      if (data === 'products_show') {
        await sendProducts(supabase, token, chatId, botId)
        return new Response('ok', { status: 200 })
      }

      if (data === 'cart_show') {
        await sendCart(supabase, token, chatId, botId, telegramUserId)
        return new Response('ok', { status: 200 })
      }

      if (data === 'cart_clear') {
        const { error } = await supabase.from('botstore_cart_items').delete().eq('bot_id', botId).eq('telegram_user_id', telegramUserId)
        if (error) throw error
        await telegramCall(token, 'sendMessage', { chat_id: chatId, text: '🗑 سبد خرید پاک شد.' })
        return new Response('ok', { status: 200 })
      }

      if (data === 'checkout') {
        const { data: checkoutRows, error } = await supabase.rpc('botstore_checkout_order', {
          p_bot_id: botId,
          p_telegram_user_id: telegramUserId,
        })

        if (error || !checkoutRows?.length) {
          await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'سبد خرید خالی است یا ثبت سفارش در حال حاضر ممکن نیست.' })
          return new Response('ok', { status: 200 })
        }

        const order = checkoutRows[0]
        await telegramCall(token, 'sendMessage', {
          chat_id: chatId,
          text: `✅ سفارش شما ثبت شد.\n\nشماره سفارش: ${order.order_code}\nتعداد اقلام: ${order.item_count}\nمبلغ کل: ${money(Number(order.total_price))} تومان\n\nوضعیت فعلی: جدید`,
          reply_markup: { inline_keyboard: [[{ text: '🧾 سفارش‌های من', callback_data: 'orders_show' }]] },
        })
        return new Response('ok', { status: 200 })
      }

      if (data === 'orders_show') {
        await sendOrders(supabase, token, chatId, botId, telegramUserId)
        return new Response('ok', { status: 200 })
      }

      const categoryMatch = /^category:(\d+)$/.exec(data)
      if (categoryMatch) {
        const categoryId = Number(categoryMatch[1])
        const { data: category, error: categoryError } = await supabase
          .from('botstore_categories')
          .select('id, title, emoji')
          .eq('bot_id', botId)
          .eq('id', categoryId)
          .maybeSingle()

        if (categoryError) throw categoryError
        if (!category) return new Response('ok', { status: 200 })

        const { data: products, error: productsError } = await supabase
          .from('botstore_products')
          .select('id, title, price')
          .eq('bot_id', botId)
          .eq('category_id', categoryId)
          .eq('active', true)
          .order('position')
          .order('id')

        if (productsError) throw productsError
        if (!products?.length) {
          await telegramCall(token, 'sendMessage', { chat_id: chatId, text: `در دسته «${category.title}» هنوز محصول فعالی ثبت نشده است.` })
        } else {
          await telegramCall(token, 'sendMessage', {
            chat_id: chatId,
            text: `${category.emoji || '🛍️'} ${category.title}\nمحصول موردنظر را انتخاب کنید:`,
            reply_markup: {
              inline_keyboard: products.map((product: any) => [{
                text: `${product.title} — ${money(Number(product.price))} تومان`,
                callback_data: `product:${product.id}`,
              }]),
            },
          })
        }
        return new Response('ok', { status: 200 })
      }

      const productMatch = /^product:(\d+)$/.exec(data)
      if (productMatch) {
        await sendProductDetail(supabase, token, chatId, botId, Number(productMatch[1]))
        return new Response('ok', { status: 200 })
      }

      const cartChangeMatch = /^cart_(add|dec):(\d+)$/.exec(data)
      if (cartChangeMatch) {
        const delta = cartChangeMatch[1] === 'add' ? 1 : -1
        const productId = Number(cartChangeMatch[2])
        const { error } = await supabase.rpc('botstore_cart_change', {
          p_bot_id: botId,
          p_telegram_user_id: telegramUserId,
          p_product_id: productId,
          p_delta: delta,
        })

        if (error) {
          await telegramCall(token, 'sendMessage', { chat_id: chatId, text: 'این محصول در حال حاضر قابل افزودن به سبد نیست.' })
        } else {
          await sendCart(supabase, token, chatId, botId, telegramUserId)
        }
        return new Response('ok', { status: 200 })
      }

      return new Response('ok', { status: 200 })
    }

    const message = update.message
    if (!message) return new Response('ok', { status: 200 })
    const text = (message.text ?? '').trim()

    // Deep-link محصول با /start p_<id> آماده است تا لینک اختصاصی محصول قابل تولید باشد.
    const productStartMatch = /^\/start\s+p_(\d+)$/.exec(text)
    if (productStartMatch) {
      await sendProductDetail(supabase, token, chatId, botId, Number(productStartMatch[1]))
      return new Response('ok', { status: 200 })
    }

    if (text === '/start' || text.startsWith('/start ')) {
      await sendMainMenu(token, chatId, String(bot.first_name ?? ''), firstName)
      return new Response('ok', { status: 200 })
    }

    if (text === '🛍️ محصولات') {
      await sendProducts(supabase, token, chatId, botId)
      return new Response('ok', { status: 200 })
    }

    if (text === '🛒 سبد خرید') {
      await sendCart(supabase, token, chatId, botId, telegramUserId)
      return new Response('ok', { status: 200 })
    }

    if (text === '🧾 سفارش‌های من') {
      await sendOrders(supabase, token, chatId, botId, telegramUserId)
      return new Response('ok', { status: 200 })
    }

    if (text === '👤 حساب من') {
      const { count } = await supabase
        .from('botstore_orders')
        .select('id', { count: 'exact', head: true })
        .eq('bot_id', botId)
        .eq('telegram_user_id', telegramUserId)

      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: `👤 حساب شما\nشناسه تلگرام: ${telegramUserId}\nنام: ${firstName || '-'}\nنام کاربری: ${username ? '@' + username : '-'}\nتعداد سفارش‌ها: ${count ?? 0}`,
      })
      return new Response('ok', { status: 200 })
    }

    if (text === '☎️ پشتیبانی') {
      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: '☎️ برای پشتیبانی با مدیر همین فروشگاه در ارتباط باشید.\nاطلاعات تماس اختصاصی در نسخه بعد از پنل فروشنده قابل تنظیم می‌شود.',
      })
      return new Response('ok', { status: 200 })
    }

    if (text === 'ℹ️ درباره فروشگاه') {
      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: `🤖 ${bot.first_name || 'فروشگاه تلگرامی'}${bot.username ? `\n@${bot.username}` : ''}\nساخته‌شده با App BotStore`,
      })
      return new Response('ok', { status: 200 })
    }

    await telegramCall(token, 'sendMessage', {
      chat_id: chatId,
      text: 'دستور شناخته نشد. لطفاً یکی از گزینه‌های منوی فروشگاه را انتخاب کنید.',
    })

    return new Response('ok', { status: 200 })
  } catch (error) {
    // پاسخ 200 مانع Retry طوفانی Telegram می‌شود؛ جزئیات فقط در Log سرور باقی می‌ماند.
    console.error('[botstore-telegram]', error)
    return new Response('ok', { status: 200 })
  }
})
