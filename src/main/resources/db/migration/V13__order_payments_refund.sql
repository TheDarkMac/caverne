alter table order_payments
  add column if not exists refund_id varchar(255);

alter table order_payments
  add column if not exists refunded_amount numeric(19, 4);

alter table order_payments
  add column if not exists refunded_at timestamp;

alter table order_payments
  add column if not exists refund_reason text;
