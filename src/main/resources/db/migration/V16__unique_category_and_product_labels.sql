create unique index if not exists ux_categories_label_lower on categories (lower(label));

create unique index if not exists ux_products_label_lower on products (lower(label));
