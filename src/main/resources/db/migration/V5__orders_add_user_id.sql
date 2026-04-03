alter table orders add column if not exists user_id bigint references app_users (id);

create index if not exists idx_orders_user_id on orders (user_id);
