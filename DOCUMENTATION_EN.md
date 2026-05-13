# Caverne — Capability Summary (EN)

Caverne is a Spring Boot (Java 21) backend exposing an e-commerce REST API under the base path
`/api/v1`. The contract is defined by `docs/api.yaml` (OpenAPI 3) and the generated clients live
in `clients/`.

## 1. Authentication and users

- Local registration (`POST /auth/register`), login at `POST /auth/login` issuing a short-lived
  (15 min) HS256 access JWT in the body plus a long-lived (30 days) rotated `refresh_token`
  cookie (HttpOnly, Secure, SameSite=None) and a `csrf_token` cookie for double-submit anti-CSRF.
- `POST /auth/refresh` rotates the refresh cookie and emits a fresh access JWT; the caller must
  echo the `csrf_token` cookie value in `X-CSRF-Token`. Reused (already revoked) refresh tokens
  trigger family-wide revocation (theft detection).
- `POST /auth/logout` revokes the current refresh token server-side and clears both cookies.
  The access JWT itself is stateless and expires naturally within 15 min.
- Optional write-through provisioning in Supabase at registration time when
  `auth.providers.supabase.enabled=true`; inbound Supabase Auth Hooks accepted on
  `POST /auth/webhooks/supabase` (shared-secret protected).
- Two roles: `ADMIN` and `SIMPLE_USER`. External-provider logins always land as `SIMPLE_USER`;
  admin elevation must come from a pre-existing local admin mapping.
- Profile endpoints for the connected user (`GET/PUT /users/me`) and address book
  (`GET/POST /users/me/addresses`, `PUT/DELETE /users/me/addresses/{id}`, set-default).
- Admin user management: `GET/POST /users`, `GET/DELETE /users/{id}`.
- Optional initial-admin bootstrap controlled by `bootstrap.admin.*` properties; runs only once.

## 2. Catalog — categories, products, prices, images

- Public tree listing `GET /categories` (flat listing available via `?flat=true`) and detail
  `GET /categories/{id}`.
- Admin upserts: `POST /categories` / `PUT /categories/{id}`, delete via `DELETE`.
- **Category `label` is globally unique (case-insensitive).** Duplicate → `409 Conflict`.
- Public paginated catalogue `GET /products` with filters `category_id`, `is_active`, `search`,
  `currency`, `price_date`, `price_from`, `price_to`. Public detail `GET /products/{id}` with the
  same date/range price selectors.
- Admin upserts: `POST /products` / `PUT /products/{id}`, delete via `DELETE`.
- **Product `label` and `reference` are globally unique (case-insensitive).** Duplicate →
  `409 Conflict`.
- Prices are historical — each product carries a list of `(currency, unit, valid_from, value)`
  rows. The public catalogue returns the most-recent applicable row per `(currency, unit)` by
  default, or the full range when `price_from`/`price_to` are supplied.
- Product images: admin CRUD `GET/POST /products/{id}/images`,
  `DELETE /products/{id}/images/{imageId}`, and `PUT .../images/{imageId}/main` which enforces
  at-most-one `is_main` per product.

## 3. Stock and stock-movement audit log

- Stock is kept on `products.stock_quantity`.
- Admin endpoints: `GET /products/{id}/stock` to read, `PUT /products/{id}/stock` to set the new
  balance (validated non-negative), `GET /products/{id}/stock/movements` to read the audit log.
- Every change is journalled in `stock_movements` with a reason:
  - `ORDER_PLACED` — decrement caused by a successful checkout.
  - `ORDER_CANCELLED` — restoration caused by an order cancellation.
  - `MANUAL_ADJUSTMENT` — admin-driven change through `PUT /products/{id}/stock`.
  - `RESTOCK` — reserved for future inbound stock operations.
- **Row-level locking:** `POST /orders` and `POST /orders/{id}/cancel` and the admin stock update
  read the product with `SELECT … FOR UPDATE`. Concurrent buyers on the same product serialize on
  the row lock, so two orders cannot both pass the sufficient-stock check. Insufficient stock
  produces `422`.

## 4. Orders and checkout

- Guests can place orders (`POST /orders`); authenticated users can also list their own orders
  via `GET /orders` and access `GET /orders/{id}` (guests can read guest orders without a
  bearer).
- Admin-wide listing through `GET /orders/all`; status transition through
  `PUT /orders/{id}/status` (admin) and cancellation through `POST /orders/{id}/cancel`
  (owner or admin). Cancellation restores stock.
- Each order line freezes a product snapshot (label and price at the time of purchase) in
  `order_items.product_snapshot`, so changes to the product later do not rewrite past orders.
- Delivery cost can be attached via `delivery_cost_id`.

## 5. Payments

- Payment providers discovered through `GET /payment-methods` (`manual`, `stripe`, etc.).
- Checkout payment initiated via `POST /orders/{id}/payments`; full payment history at
  `GET /orders/{id}/payments`.
- Stripe inbound webhook at `POST /payments/webhooks/stripe` with `Stripe-Signature` header,
  idempotent against Stripe `event.id`.
- Admin refund at `POST /payments/{orderId}/refund` (Stripe-backed). When fully refunded, the
  order moves to `CANCELLED`.

## 6. Delivery costs

- Public read (`GET /delivery-costs`) and create (`POST /delivery-costs`) so checkout can capture
  the computed cost from the frontend.
- Admin update/delete: `PUT /delivery-costs/{id}`, `DELETE /delivery-costs/{id}`.

## 7. Operational surfaces

- `GET /actuator/health` is public (readiness/liveness).
- Structured access logging configurable via `application.properties`; CORS fully externalised
  through `app.cors.*` / `APP_CORS_*`.
- Optional seed loader `NichesCatalogImporter` — controlled by `catalog.niches-import.*`.
  Idempotent against repeated labels in the source file (the second occurrence reuses the first
  product instead of failing the global unique index on `products.label`).

## 8. What the app does NOT do (explicit out-of-scope)

- No review/rating system.
- No analytics dashboard.
- No multi-tenant scoping (`products`, `categories`, `orders` are global).
- No webhook fan-out to third-party integrations beyond Stripe + Supabase Auth Hooks.
- No recurring billing or subscriptions.

## 9. Where to look

| Concern                      | Reference                                              |
|------------------------------|--------------------------------------------------------|
| REST contract                | `docs/api.yaml`                                        |
| Generated client             | `clients/`                                             |
| Detailed endpoint docs       | `DOCUMENTATION.md`                                     |
| Deployment walkthrough       | `STEP_BY_STEP.md`                                      |
| Feature history              | `REALISATION.md`                                       |
| User-flow simulation         | `simulation.md`                                        |
| Catalogue seed source        | `niches.md`                                            |
| Flyway migrations            | `src/main/resources/db/migration/`                     |

Last reviewed: 2026-04-20.
