create table app_users (
  id bigserial primary key,
  firstname varchar(255) not null,
  lastname varchar(255) not null,
  email varchar(255) not null unique,
  phone varchar(255),
  password_hash varchar(255) not null,
  role varchar(255) not null,
  status varchar(255) not null
);

create table auth_sessions (
  id bigserial primary key,
  user_id bigint not null references app_users (id),
  token varchar(255) not null unique,
  expires_at timestamp(6) not null
);

create index idx_auth_sessions_user_id on auth_sessions (user_id);
