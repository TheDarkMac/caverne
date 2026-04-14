create table if not exists stripe_webhook_events (
  event_id varchar(255) primary key,
  received_at timestamp with time zone not null default now()
);
