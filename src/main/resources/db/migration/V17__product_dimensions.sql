alter table products drop column if exists size;

alter table products
  add column weight numeric(19, 4),
  add column length numeric(19, 4),
  add column width numeric(19, 4),
  add column height numeric(19, 4);
