drop table if exists auth_sessions;

create table refresh_tokens (
  id uuid primary key,
  user_id uuid not null references app_users (id) on delete cascade,
  token_hash varchar(64) not null unique,
  family_id uuid not null,
  parent_id uuid references refresh_tokens (id),
  expires_at timestamp(6) not null,
  revoked_at timestamp(6),
  created_at timestamp(6) not null default now()
);

create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index idx_refresh_tokens_family_id on refresh_tokens (family_id);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);
