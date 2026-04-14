create table if not exists stock_movements (
  id uuid primary key,
  product_id uuid not null references products (id) on delete cascade,
  delta integer not null,
  reason varchar(64) not null,
  balance_after integer not null,
  created_at timestamp not null default now(),
  created_by uuid references app_users (id) on delete set null,
  note text
);

create index if not exists idx_stock_movements_product_id on stock_movements (product_id);
create index if not exists idx_stock_movements_created_at on stock_movements (created_at);
