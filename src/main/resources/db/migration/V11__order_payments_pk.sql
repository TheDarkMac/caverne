alter table order_payments alter column payment_id set not null;
alter table order_payments alter column order_id set not null;
alter table order_payments add constraint order_payments_pkey primary key (payment_id);
create index if not exists idx_order_payments_internal_reference on order_payments (internal_reference);
