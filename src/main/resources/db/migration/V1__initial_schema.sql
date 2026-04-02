create table categories (
  id bigserial primary key,
  label varchar(255),
  slug varchar(255),
  map varchar(255),
  parent_id bigint references categories (id)
);

create table products (
  id bigserial primary key,
  category_id bigint references categories (id),
  label varchar(255) not null,
  reference varchar(255) not null unique,
  limit_date date,
  description text,
  size varchar(255),
  is_active boolean not null
);

create table prices (
  id bigserial primary key,
  product_id bigint references products (id),
  currency_code varchar(3),
  value numeric(19, 4),
  valid_from date,
  unit varchar(255)
);

create table orders (
  id bigserial primary key,
  reference varchar(255),
  date timestamp(6),
  status varchar(255),
  currency_code varchar(255),
  customer_name varchar(255),
  customer_email varchar(255),
  customer_phone varchar(255),
  shipping_location varchar(255),
  postal_code varchar(255),
  country_code varchar(255),
  total_amount numeric(38, 2)
);

create table order_items (
  id bigserial primary key,
  order_id bigint references orders (id),
  product_id bigint,
  product_label varchar(255),
  quantity numeric(38, 2),
  unit_price numeric(38, 2),
  total_price numeric(38, 2)
);

create table order_payments (
  order_id bigint not null references orders (id),
  payment_id integer,
  method_code varchar(255),
  currency_code varchar(255),
  amount numeric(38, 2),
  date timestamp(6),
  status varchar(255),
  internal_reference varchar(255),
  provider_response text
);

create index idx_categories_parent_id on categories (parent_id);
create index idx_products_category_id on products (category_id);
create index idx_prices_product_id on prices (product_id);
create index idx_order_items_order_id on order_items (order_id);
create index idx_order_payments_order_id on order_payments (order_id);
