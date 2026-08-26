-- Indexهای تکراری که توسط Unique Constraint پوشش داده می‌شوند حذف می‌شوند.
drop index if exists public.botstore_customers_bot_user_idx;
drop index if exists public.botstore_cart_bot_user_idx;

-- Foreign Keyهای پرتکرار Index مستقل می‌گیرند تا lookup و حذف والد Full Scan نشود.
create index if not exists botstore_cart_items_product_idx
  on public.botstore_cart_items(product_id);
create index if not exists botstore_order_items_product_idx
  on public.botstore_order_items(product_id);
create index if not exists botstore_orders_customer_idx
  on public.botstore_orders(customer_id);
