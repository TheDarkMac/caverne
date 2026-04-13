alter table products
  add column if not exists stock_quantity numeric(19, 4) not null default 0;

alter table order_items
  add column if not exists product_snapshot text;
