alter table app_users alter column email drop not null;

alter table app_users drop constraint if exists app_users_email_key;

create unique index if not exists ux_app_users_email_ci
on app_users ((lower(email)))
where email is not null;

create unique index if not exists ux_app_users_phone
on app_users (phone)
where phone is not null;

create table user_addresses (
  id bigserial primary key,
  user_id bigint not null references app_users (id) on delete cascade,
  location varchar(255) not null,
  postal_code varchar(255) not null,
  country_code varchar(3) not null,
  is_default boolean not null default false
);

create index idx_user_addresses_user_id on user_addresses (user_id);
