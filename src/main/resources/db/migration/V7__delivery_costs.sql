create table if not exists delivery_costs (
  id bigserial primary key,
  amount numeric(19, 4) not null,
  provider varchar(255) not null,
  response_provider text
);

create index if not exists idx_delivery_costs_provider on delivery_costs (provider);
