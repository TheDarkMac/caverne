alter table order_payments
  add column if not exists stripe_payment_intent_id varchar(255);

create index if not exists idx_order_payments_stripe_payment_intent_id
  on order_payments (stripe_payment_intent_id);

alter table order_payments
  alter column refunded_at type timestamp with time zone using refunded_at at time zone 'UTC';
