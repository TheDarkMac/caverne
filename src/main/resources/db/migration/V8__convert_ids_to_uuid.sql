create extension if not exists pgcrypto;

alter table app_users add column id_uuid uuid;
update app_users set id_uuid = gen_random_uuid() where id_uuid is null;
alter table auth_sessions add column id_uuid uuid;
update auth_sessions set id_uuid = gen_random_uuid() where id_uuid is null;
alter table auth_sessions add column user_id_uuid uuid;
update auth_sessions a
set user_id_uuid = u.id_uuid
from app_users u
where a.user_id = u.id;
alter table user_addresses add column id_uuid uuid;
update user_addresses set id_uuid = gen_random_uuid() where id_uuid is null;
alter table user_addresses add column user_id_uuid uuid;
update user_addresses a
set user_id_uuid = u.id_uuid
from app_users u
where a.user_id = u.id;
alter table categories add column id_uuid uuid;
update categories set id_uuid = gen_random_uuid() where id_uuid is null;
alter table categories add column parent_id_uuid uuid;
update categories c
set parent_id_uuid = p.id_uuid
from categories p
where c.parent_id = p.id;
alter table products add column id_uuid uuid;
update products set id_uuid = gen_random_uuid() where id_uuid is null;
alter table products add column category_id_uuid uuid;
update products p
set category_id_uuid = c.id_uuid
from categories c
where p.category_id = c.id;
alter table prices add column id_uuid uuid;
update prices set id_uuid = gen_random_uuid() where id_uuid is null;
alter table prices add column product_id_uuid uuid;
update prices p
set product_id_uuid = pr.id_uuid
from products pr
where p.product_id = pr.id;
alter table delivery_costs add column id_uuid uuid;
update delivery_costs set id_uuid = gen_random_uuid() where id_uuid is null;
alter table orders add column id_uuid uuid;
update orders set id_uuid = gen_random_uuid() where id_uuid is null;
alter table orders add column user_id_uuid uuid;
update orders o
set user_id_uuid = u.id_uuid
from app_users u
where o.user_id = u.id;
alter table orders add column delivery_cost_id_uuid uuid;
update orders o
set delivery_cost_id_uuid = d.id_uuid
from delivery_costs d
where o.delivery_cost_id = d.id;
alter table order_items add column id_uuid uuid;
update order_items set id_uuid = gen_random_uuid() where id_uuid is null;
alter table order_items add column order_id_uuid uuid;
update order_items i
set order_id_uuid = o.id_uuid
from orders o
where i.order_id = o.id;
alter table order_items add column product_id_uuid uuid;
update order_items i
set product_id_uuid = p.id_uuid
from products p
where i.product_id = p.id;
alter table order_payments add column order_id_uuid uuid;
update order_payments p
set order_id_uuid = o.id_uuid
from orders o
where p.order_id = o.id;
alter table order_payments add column payment_id_uuid uuid;
update order_payments set payment_id_uuid = gen_random_uuid() where payment_id_uuid is null;

drop index if exists idx_auth_sessions_user_id;
drop index if exists idx_user_addresses_user_id;
drop index if exists idx_categories_parent_id;
drop index if exists idx_products_category_id;
drop index if exists idx_prices_product_id;
drop index if exists idx_order_items_order_id;
drop index if exists idx_order_payments_order_id;
drop index if exists idx_orders_user_id;

alter table auth_sessions drop constraint if exists auth_sessions_user_id_fkey;
alter table user_addresses drop constraint if exists user_addresses_user_id_fkey;
alter table categories drop constraint if exists categories_parent_id_fkey;
alter table products drop constraint if exists products_category_id_fkey;
alter table prices drop constraint if exists prices_product_id_fkey;
alter table orders drop constraint if exists orders_user_id_fkey;
alter table order_items drop constraint if exists order_items_order_id_fkey;
alter table order_payments drop constraint if exists order_payments_order_id_fkey;

alter table auth_sessions drop constraint if exists auth_sessions_pkey;
alter table user_addresses drop constraint if exists user_addresses_pkey;
alter table categories drop constraint if exists categories_pkey;
alter table products drop constraint if exists products_pkey;
alter table prices drop constraint if exists prices_pkey;
alter table app_users drop constraint if exists app_users_pkey;
alter table orders drop constraint if exists orders_pkey;
alter table order_items drop constraint if exists order_items_pkey;
alter table delivery_costs drop constraint if exists delivery_costs_pkey;

alter table auth_sessions drop column id;
alter table auth_sessions drop column user_id;
alter table user_addresses drop column id;
alter table user_addresses drop column user_id;
alter table categories drop column id;
alter table categories drop column parent_id;
alter table products drop column id;
alter table products drop column category_id;
alter table prices drop column id;
alter table prices drop column product_id;
alter table app_users drop column id;
alter table orders drop column id;
alter table orders drop column user_id;
alter table orders drop column delivery_cost_id;
alter table order_items drop column id;
alter table order_items drop column order_id;
alter table order_items drop column product_id;
alter table order_payments drop column order_id;
alter table order_payments drop column payment_id;
alter table delivery_costs drop column id;

alter table app_users rename column id_uuid to id;
alter table auth_sessions rename column id_uuid to id;
alter table auth_sessions rename column user_id_uuid to user_id;
alter table user_addresses rename column id_uuid to id;
alter table user_addresses rename column user_id_uuid to user_id;
alter table categories rename column id_uuid to id;
alter table categories rename column parent_id_uuid to parent_id;
alter table products rename column id_uuid to id;
alter table products rename column category_id_uuid to category_id;
alter table prices rename column id_uuid to id;
alter table prices rename column product_id_uuid to product_id;
alter table delivery_costs rename column id_uuid to id;
alter table orders rename column id_uuid to id;
alter table orders rename column user_id_uuid to user_id;
alter table orders rename column delivery_cost_id_uuid to delivery_cost_id;
alter table order_items rename column id_uuid to id;
alter table order_items rename column order_id_uuid to order_id;
alter table order_items rename column product_id_uuid to product_id;
alter table order_payments rename column order_id_uuid to order_id;
alter table order_payments rename column payment_id_uuid to payment_id;

alter table app_users alter column id set not null;
alter table auth_sessions alter column id set not null;
alter table auth_sessions alter column user_id set not null;
alter table user_addresses alter column id set not null;
alter table user_addresses alter column user_id set not null;
alter table categories alter column id set not null;
alter table products alter column id set not null;
alter table prices alter column id set not null;
alter table delivery_costs alter column id set not null;
alter table orders alter column id set not null;
alter table order_items alter column id set not null;

alter table app_users add constraint app_users_pkey primary key (id);
alter table auth_sessions add constraint auth_sessions_pkey primary key (id);
alter table user_addresses add constraint user_addresses_pkey primary key (id);
alter table categories add constraint categories_pkey primary key (id);
alter table products add constraint products_pkey primary key (id);
alter table prices add constraint prices_pkey primary key (id);
alter table delivery_costs add constraint delivery_costs_pkey primary key (id);
alter table orders add constraint orders_pkey primary key (id);
alter table order_items add constraint order_items_pkey primary key (id);

alter table auth_sessions
  add constraint auth_sessions_user_id_fkey foreign key (user_id) references app_users (id);
alter table user_addresses
  add constraint user_addresses_user_id_fkey foreign key (user_id) references app_users (id) on delete cascade;
alter table categories
  add constraint categories_parent_id_fkey foreign key (parent_id) references categories (id);
alter table products
  add constraint products_category_id_fkey foreign key (category_id) references categories (id);
alter table prices
  add constraint prices_product_id_fkey foreign key (product_id) references products (id);
alter table orders
  add constraint orders_user_id_fkey foreign key (user_id) references app_users (id);
alter table order_items
  add constraint order_items_order_id_fkey foreign key (order_id) references orders (id);
alter table order_payments
  add constraint order_payments_order_id_fkey foreign key (order_id) references orders (id);

create index idx_auth_sessions_user_id on auth_sessions (user_id);
create index idx_user_addresses_user_id on user_addresses (user_id);
create index idx_categories_parent_id on categories (parent_id);
create index idx_products_category_id on products (category_id);
create index idx_prices_product_id on prices (product_id);
create index idx_order_items_order_id on order_items (order_id);
create index idx_order_payments_order_id on order_payments (order_id);
create index idx_orders_user_id on orders (user_id);
