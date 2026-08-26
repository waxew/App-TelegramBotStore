// این Edge Function Webhook مشترک تمام ربات‌های ساخته‌شده با App BotStore است.
// هر درخواست با bot_id و Telegram secret_token به ربات درست نگاشت می‌شود تا چندین فروشگاه هم‌زمان روی یک Backend اجرا شوند.
import 'jsr:@supabase/functions-js/edge-runtime.d.ts'
import { createClient } from 'npm:@supabase/supabase-js@2.95.0'

// مدل حداقلی Update تلگرام برای پیام و Callback Query تعریف می‌شود.
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

// کلاینت مدیریتی فقط سمت سرور ساخته می‌شود؛ کلید مدیریتی هرگز در APK قرار نمی‌گیرد.
function createAdminClient() {
  const url = Deno.env.get('SUPABASE_URL') ?? ''
  let key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  if (!key) {
    try {
      const secretKeys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') ?? '{}') as Record<string, string>
      const defaultSecretEnvName = secretKeys.default
      if (defaultSecretEnvName) key = Deno.env.get(defaultSecretEnvName) ?? ''
    } catch {
      // نبود متغیر جدید مشکلی ایجاد نمی‌کند و fallback قدیمی بررسی شده است.
    }
  }
  if (!url || !key) throw new Error('Supabase server credentials are not available')
  return createClient(url, key, { auth: { persistSession: false } })
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

// اعداد قیمت برای خوانایی با جداکننده هزارگان نمایش داده می‌شوند.
function money(value: number) {
  return new Intl.NumberFormat('fa-IR').format(value)
}

// منوی اصلی فروشگاه برای /start و بازگشت به خانه ساخته می‌شود.
async function sendMainMenu(token: string, chatId: number, botName: string, firstName = '') {
  const hello = firstName ? `سلام ${firstName} 👋\n` : 'سلام 👋\n'
  await telegramCall(token, 'sendMessage', {
    chat_id: chatId,
    text: `${hello}به فروشگاه «${botName || 'فروشگاه'}» خوش آمدید.\nاز منوی زیر بخش موردنظر را انتخاب کنید.`,
    reply_markup: {
      keyboard: [
        [{ text: '🛍️ محصولات' }, { text: '👤 حساب من' }],
        [{ text: '☎️ پشتیبانی' }, { text: 'ℹ️ درباره فروشگاه' }],
      ],
      resize_keyboard: true,
      is_persistent: true,
    },
  })
}

Deno.serve(async (req) => {
  // Telegram Webhook فقط POST ارسال می‌کند.
  if (req.method !== 'POST') return new Response('ok', { status: 200 })

  try {
    // bot_id از Query String خوانده می‌شود تا Update به فروشگاه درست برسد.
    const url = new URL(req.url)
    const botId = Number(url.searchParams.get('bot_id'))
    if (!Number.isInteger(botId) || botId <= 0) return new Response('ok', { status: 200 })

    // رکورد Bot و Secret همان Webhook از دیتابیس server-only خوانده می‌شود.
    const supabase = createAdminClient()
    const { data: bot, error: botError } = await supabase
      .from('botstore_bots')
      .select('id, bot_token, username, first_name, webhook_secret, active')
      .eq('id', botId)
      .single()

    // برای جلوگیری از Retry بی‌پایان Telegram، Bot ناشناخته یا غیرفعال با 200 نادیده گرفته می‌شود.
    if (botError || !bot || !bot.active) return new Response('ok', { status: 200 })

    // هدر Secret که Telegram هنگام setWebhook ثبت کرده بود باید دقیقاً با Secret دیتابیس برابر باشد.
    const suppliedSecret = req.headers.get('X-Telegram-Bot-Api-Secret-Token') ?? ''
    if (!suppliedSecret || suppliedSecret !== bot.webhook_secret) {
      console.warn(`[botstore-telegram] invalid webhook secret for bot_id=${botId}`)
      return new Response('ok', { status: 200 })
    }

    // Update معتبر Telegram parse و Token فقط در حافظه همین درخواست استفاده می‌شود.
    const update = await req.json() as TelegramUpdate
    const token = String(bot.bot_token)

    // Callback دسته‌بندی محصولات را پردازش می‌کند.
    if (update.callback_query) {
      const callback = update.callback_query
      await telegramCall(token, 'answerCallbackQuery', { callback_query_id: callback.id })

      // Callback Data باید دقیقاً الگوی category:<id> داشته باشد.
      const data = callback.data ?? ''
      const categoryMatch = /^category:(\d+)$/.exec(data)
      if (categoryMatch && callback.message) {
        const categoryId = Number(categoryMatch[1])

        // Category باید متعلق به همین Bot باشد تا امکان دسترسی متقاطع بین فروشگاه‌ها وجود نداشته باشد.
        const { data: category } = await supabase
          .from('botstore_categories')
          .select('id, title, emoji')
          .eq('bot_id', botId)
          .eq('id', categoryId)
          .maybeSingle()

        if (!category) return new Response('ok', { status: 200 })

        // محصولات فعال همان Category خوانده و بر اساس position مرتب می‌شوند.
        const { data: products } = await supabase
          .from('botstore_products')
          .select('id, title, price, description')
          .eq('bot_id', botId)
          .eq('category_id', categoryId)
          .eq('active', true)
          .order('position')
          .order('id')

        // متن خوانای محصولات برای Telegram ساخته می‌شود.
        const lines = (products ?? []).map((product: any, index: number) =>
          `${index + 1}) ${product.title}\n💳 ${money(Number(product.price))} تومان${product.description ? `\n${product.description}` : ''}`
        )

        // نتیجه Category برای کاربر ارسال می‌شود.
        await telegramCall(token, 'sendMessage', {
          chat_id: callback.message.chat.id,
          text: lines.length
            ? `${category.emoji} ${category.title}\n\n${lines.join('\n\n')}`
            : `در دسته «${category.title}» هنوز محصول فعالی ثبت نشده است.`,
        })
      }

      return new Response('ok', { status: 200 })
    }

    // Updateهای غیر Message در MVP نادیده گرفته می‌شوند.
    const message = update.message
    if (!message) return new Response('ok', { status: 200 })

    // اطلاعات لازم پیام استخراج می‌شود.
    const chatId = message.chat.id
    const text = (message.text ?? '').trim()
    const userFirstName = message.from?.first_name ?? ''

    // /start نقطه ورود استاندارد هر فروشگاه ساخته‌شده است.
    if (text === '/start' || text.startsWith('/start ')) {
      await sendMainMenu(token, chatId, String(bot.first_name ?? ''), userFirstName)
      return new Response('ok', { status: 200 })
    }

    // فهرست دسته‌بندی‌ها از دیتابیس همان Bot خوانده می‌شود.
    if (text === '🛍️ محصولات') {
      const { data: categories } = await supabase
        .from('botstore_categories')
        .select('id, title, emoji')
        .eq('bot_id', botId)
        .order('position')
        .order('id')

      // اگر Category وجود ندارد، محصولات بدون دسته نیز برای MVP قابل نمایش‌اند.
      if (!categories?.length) {
        const { data: products } = await supabase
          .from('botstore_products')
          .select('title, price, description')
          .eq('bot_id', botId)
          .eq('active', true)
          .order('position')
          .order('id')

        const lines = (products ?? []).map((product: any, index: number) =>
          `${index + 1}) ${product.title} — ${money(Number(product.price))} تومان`
        )

        await telegramCall(token, 'sendMessage', {
          chat_id: chatId,
          text: lines.length ? `🛍️ محصولات فروشگاه\n\n${lines.join('\n')}` : 'هنوز محصولی برای این فروشگاه ثبت نشده است.',
        })
      } else {
        // Categoryها به‌صورت Inline Keyboard برای کاربر ارسال می‌شوند.
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
      }
      return new Response('ok', { status: 200 })
    }

    // صفحه حساب فعلاً اطلاعات پایه Telegram را نشان می‌دهد و بعداً به کیف پول/سفارش‌ها متصل می‌شود.
    if (text === '👤 حساب من') {
      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: `👤 حساب شما\nشناسه تلگرام: ${message.from?.id ?? '-'}\nنام: ${userFirstName || '-'}\nنام کاربری: ${message.from?.username ? '@' + message.from.username : '-'}`,
      })
      return new Response('ok', { status: 200 })
    }

    // پشتیبانی در MVP پیام عمومی دارد و بعداً از پنل فروشنده قابل تنظیم خواهد شد.
    if (text === '☎️ پشتیبانی') {
      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: '☎️ برای پشتیبانی با مدیر همین فروشگاه در ارتباط باشید.\nاطلاعات تماس اختصاصی در نسخه بعد از پنل فروشنده قابل تنظیم می‌شود.',
      })
      return new Response('ok', { status: 200 })
    }

    // درباره فروشگاه نام و Username واقعی Bot را نمایش می‌دهد.
    if (text === 'ℹ️ درباره فروشگاه') {
      await telegramCall(token, 'sendMessage', {
        chat_id: chatId,
        text: `🤖 ${bot.first_name || 'فروشگاه تلگرامی'}${bot.username ? `\n@${bot.username}` : ''}\nساخته‌شده با App BotStore`,
      })
      return new Response('ok', { status: 200 })
    }

    // پیام‌های ناشناخته کاربر را به منوی اصلی هدایت می‌کنند تا Bot بی‌پاسخ نماند.
    await telegramCall(token, 'sendMessage', {
      chat_id: chatId,
      text: 'دستور شناخته نشد. لطفاً یکی از گزینه‌های منوی فروشگاه را انتخاب کنید.',
    })

    return new Response('ok', { status: 200 })
  } catch (error) {
    // پاسخ 200 مانع Retry طوفانی Telegram می‌شود؛ جزئیات برای عیب‌یابی داخل Log باقی می‌ماند.
    console.error('[botstore-telegram]', error)
    return new Response('ok', { status: 200 })
  }
})
