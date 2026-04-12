alter table categories add column if not exists icon varchar(2048);

create table if not exists product_images (
  id uuid primary key,
  product_id uuid not null references products (id) on delete cascade,
  url varchar(2048) not null,
  is_main boolean not null default false
);

create index if not exists idx_product_images_product_id on product_images (product_id);
