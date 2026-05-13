# API Documentation

Base URL: `http(s)://<host>/api/v1`

All request and response bodies use `Content-Type: application/json`.

---

## Authentication

All secured endpoints require a bearer token in the `Authorization` header:

```
Authorization: Bearer <token>
```

Two token types are accepted, both **HS256 JWTs** disambiguated by their `iss` claim:

| Type | Issued by | Verified by |
|---|---|---|
| Local access JWT | `POST /auth/login` and `POST /auth/refresh` | HS256 signature against `AUTH_LOCAL_JWT_SECRET` — **stateless**, no DB lookup |
| Supabase JWT | Supabase Auth | HS256 signature against `AUTH_PROVIDERS_SUPABASE_JWT_SECRET` |

Local access JWT claims:

| Claim | RFC 7519 | Description |
|---|---|---|
| `iss` | §4.1.1 | Issuer — configured via `AUTH_LOCAL_JWT_ISSUER` (default `caverne`). Used to disambiguate local vs Supabase tokens on the server side. |
| `sub` | §4.1.2 | Subject — the **user UUID**. This is the JWT-standard place to put the user id; front-end libraries (`jwt-decode`, Supabase SDK, Auth0 SDK…) read it from `sub` by convention. No separate `user_id` field is emitted to avoid duplication. |
| `jti` | §4.1.7 | JWT ID — random UUID per login. Kept in the payload for audit/log correlation but **not** tracked server-side anymore (the refresh token table is the source of truth for revocation). |
| `iat` | §4.1.6 | Issued-at (Unix seconds, UTC) |
| `exp` | §4.1.4 | Expiration (Unix seconds, UTC). Default TTL **15 min** — short on purpose, see below. |
| `role` | custom | `simple_user` or `admin`. **Convenience only** — the server re-fetches the role from the DB on every request; never trust the claim alone for authorization. |
| `email`, `phone` | custom | User contact info, for client display. Same caveat: not authoritative. |

### Refresh token flow

The access JWT is short-lived (15 min). To stay signed in, the client uses a long-lived (30 days) **refresh token** delivered as an `HttpOnly` cookie. Login and refresh also set a `csrf_token` cookie that the client must echo back in an `X-CSRF-Token` header on every `/auth/refresh` and `/auth/logout` call (double-submit pattern — protects against cross-origin forgery when the cookie's `SameSite=None`).

```
POST /auth/login (credentials in body)
  ← 200 { access_token, token_type, expires_in: 900 }
  ← Set-Cookie: refresh_token=<opaque>; HttpOnly; Secure; SameSite=None; Path=/api/v1/auth; Max-Age=2592000
  ← Set-Cookie: csrf_token=<value>;   Secure; SameSite=None; Path=/api/v1/auth; Max-Age=2592000

GET /any/protected/route
  Authorization: Bearer <access_token>
  ← 200 ...                          (access JWT verified statelessly)

POST /auth/refresh                   (cookies sent by the browser automatically)
  X-CSRF-Token: <value of csrf_token cookie>
  ← 200 { access_token, expires_in: 900 }
  ← Set-Cookie: refresh_token=<new opaque>; ...        (rotated)
  ← Set-Cookie: csrf_token=<new value>; ...

POST /auth/logout
  Authorization: Bearer <access_token>
  X-CSRF-Token: <value of csrf_token cookie>
  ← 204
  ← Set-Cookie: refresh_token=; Max-Age=0; ...   (cleared)
  ← Set-Cookie: csrf_token=; Max-Age=0; ...
```

Server-side, refresh tokens are stored as **SHA-256 hashes** in `refresh_tokens` with a `family_id` chaining the rotations from a single login. Each refresh call revokes the previous token (`revoked_at` set) and inserts a new one with `parent_id` pointing to it. If a **revoked** refresh is ever presented again — sign of token theft replay — the entire family is revoked (`revokeFamily` in a `REQUIRES_NEW` transaction so the revocation commits even though the request fails with 401).

| Layer | TTL | Where it lives client-side | Revocable mid-life? |
|---|---|---|---|
| Access JWT | 15 min (`AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS`) | client memory (or stored by JS) | **No** — stateless, expires naturally |
| Refresh cookie | 30 days (`AUTH_LOCAL_REFRESH_EXPIRES_IN_SECONDS`) | browser cookie, HttpOnly so JS can't read it | Yes — `POST /auth/logout` or family-wide revocation on reuse |

### Front-end integration

```js
// Login — credentials must be 'include' so the cookies are accepted.
const res = await fetch('/api/v1/auth/login', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
});
const { access_token } = await res.json();

// Refresh — the CSRF cookie is JS-readable; echo its value in the header.
const csrf = document.cookie
  .split('; ').find(c => c.startsWith('csrf_token='))?.split('=')[1];

const refreshed = await fetch('/api/v1/auth/refresh', {
  method: 'POST',
  credentials: 'include',
  headers: { 'X-CSRF-Token': csrf },
});
const { access_token: newToken } = await refreshed.json();
```

Recommended: an HTTP interceptor that catches 401 from protected routes, calls `/auth/refresh`, retries the original request once. If `/auth/refresh` itself returns 401 → redirect to login.

Missing access token → `401`. Expired/invalid signature → `401`. Missing/invalid CSRF on `/auth/refresh` or `/auth/logout` (when refresh cookie is present) → `403`. Refresh token reused after revocation → `401` + family revoked. Insufficient role → `403`.

---

## Error Format

All errors follow the same JSON structure:

```json
{
  "code": 422,
  "message": "email is already registered",
  "traceId": "9f1c3c3e-6b7a-4e3b-9a5e-7f0e3c2c1d4a",
  "timestamp": "2026-04-21T08:12:34.567Z"
}
```

- `code` — HTTP status code
- `message` — human-readable summary
- `traceId` — correlation ID that ties this response to server logs (same value returned in the `X-Trace-Id` response header)
- `timestamp` — instant the error was produced (UTC, ISO-8601)

When something goes wrong, the client can log/report the `traceId`; operations can grep the server logs for that exact id to see everything that happened during the request.

---

## Request/Response Headers

| Header | Direction | Purpose |
|---|---|---|
| `Authorization: Bearer <token>` | Request | Authentication (local access JWT or Supabase JWT) |
| `X-CSRF-Token` | Request (`POST /auth/refresh`, `POST /auth/logout` when refresh cookie is present) | Double-submit value — must equal the `csrf_token` cookie |
| `Set-Cookie: refresh_token` | Response (login/refresh) | Long-lived refresh token (HttpOnly, Secure, SameSite=None, Path=/api/v1/auth) |
| `Set-Cookie: csrf_token` | Response (login/refresh) | Anti-CSRF value (Secure, SameSite=None, Path=/api/v1/auth, **not** HttpOnly) |
| `Stripe-Signature` | Request (`POST /payments/webhooks/stripe` only) | Stripe signature verification |
| `X-Webhook-Secret` | Request (`POST /auth/webhooks/supabase` only) | Shared-secret auth for Supabase Auth Hooks |
| `X-Trace-Id` | Request/Response | Correlation ID — generated server-side if absent; reused if the client sends a safe value (`[A-Za-z0-9_-]{1,64}`). Always echoed back on the response. |

---

## Access Levels

| Symbol | Meaning |
|---|---|
| 🌐 Public | No token required |
| 🔐 Authenticated | Any valid bearer token |
| 🛡 Admin | Token must belong to a user with role `ADMIN` |

---

## 1. Auth

### `POST /auth/register` 🌐

Creates a new user account with role `simple_user`.

When `AUTH_PROVIDERS_SUPABASE_ENABLED=true`, also creates the user in Supabase via the Admin API and links the returned Supabase UUID (`external_auth_id`). If the Supabase call fails, the whole operation is rolled back.

**Request body:**

```json
{
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@example.com",
  "phone": "+261300000000",
  "password": "secret123"
}
```

- `email` or `phone` is required (both accepted simultaneously)
- `firstname`, `lastname`, `password` are always required

**Validation:**
- `email` must not already exist (case-insensitive) → 422 `email is already registered`
- `phone` must not already exist → 422 `phone is already registered`
- missing required fields → 422

**Response `201`:**

```json
{
  "id": "uuid",
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@example.com",
  "phone": "+261300000000",
  "role": "simple_user",
  "status": "active"
}
```

---

### `POST /auth/login` 🌐

Authenticates a user, issues a short-lived access JWT in the response body, and sets the `refresh_token` + `csrf_token` cookies. Access JWT TTL: `AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS` (default `900` = 15 min). Refresh TTL: `AUTH_LOCAL_REFRESH_EXPIRES_IN_SECONDS` (default `2592000` = 30 days).

**Request body:**

```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

Or with phone:

```json
{
  "phone": "+261300000000",
  "password": "secret123"
}
```

**Validation:**
- `password` + at least `email` or `phone` required → 422
- user not found → 401 `Invalid credentials`
- wrong password → 401 `Invalid credentials`

**Response `200`:**

```http
HTTP/1.1 200 OK
Set-Cookie: refresh_token=AbCdEf...; HttpOnly; Secure; SameSite=None; Path=/api/v1/auth; Max-Age=2592000
Set-Cookie: csrf_token=Xyz123...;   Secure; SameSite=None; Path=/api/v1/auth; Max-Age=2592000
Content-Type: application/json

{
  "access_token": "eyJhbGciOiJIUzI1NiIs...<payload>.<signature>",
  "token_type": "Bearer",
  "expires_in": 900
}
```

The access JWT is signed with `AUTH_LOCAL_JWT_SECRET` (HS256) and verified statelessly on every request. The refresh cookie is the only revocable layer — its SHA-256 hash is stored in `refresh_tokens` and rotated on every `/auth/refresh`. The browser must be given `credentials: 'include'` so it accepts and replays both cookies.

---

### `POST /auth/refresh` 🌐

Rotates the refresh token and issues a fresh access JWT. The browser sends the `refresh_token` cookie automatically; the caller must echo the `csrf_token` cookie value in `X-CSRF-Token`.

**Request headers:**

```
X-CSRF-Token: <value of csrf_token cookie>
```

**Request body:** none.

**What happens internally:**

1. CSRF filter compares the header to the cookie (constant time). Mismatch → `403`.
2. `RefreshTokenService.rotate(raw)` SHA-256-hashes the cookie and looks up the row.
3. If the row is already `revoked_at != null` → **reuse detected** → revoke the entire `family_id` in a `REQUIRES_NEW` transaction and respond `401`.
4. Otherwise mark the current row revoked, insert a new row (same `family_id`, `parent_id = old.id`), and issue a new pair of cookies + a new access JWT.

**Response `200`:** identical shape to `POST /auth/login`. Two new `Set-Cookie` headers carry the rotated `refresh_token` and `csrf_token`.

**Errors:** `401` if cookie missing, expired, or unknown. `401` + family revocation if the cookie matches a `revoked_at != null` row. `403` if CSRF check fails.

---

### `POST /auth/logout` 🔐

Revokes the refresh token referenced by the cookie (if any) and clears both cookies (`Max-Age=0`). The access JWT itself is stateless — it stays cryptographically valid until `exp`, but the client loses the ability to refresh once it expires (at most 15 min later).

If the bearer is a Supabase JWT and no refresh cookie is present, the endpoint succeeds without enforcing CSRF (nothing to protect). Real Supabase revocation stays on Supabase's side.

**Request headers (when called with a local-session refresh cookie):**

```
X-CSRF-Token: <value of csrf_token cookie>
```

**No request body.**

**Response `204` — no content.** Two `Set-Cookie: Max-Age=0` headers clear `refresh_token` and `csrf_token`.

---

## 2. Users

### `GET /users/me` 🔐

Returns the profile of the currently authenticated user.

**Response `200`:**

```json
{
  "id": "uuid",
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@example.com",
  "phone": "+261300000000",
  "role": "simple_user",
  "status": "active"
}
```

---

### `PUT /users/me` 🔐

Updates the current user's profile. Only `firstname`, `lastname`, and `phone` can be changed. `email` and `role` are not modifiable through this endpoint.

**Request body:**

```json
{
  "firstname": "Jane",
  "lastname": "Doe",
  "phone": "+261300000001"
}
```

**Response `200`:** updated user object (same shape as `GET /users/me`).

---

### `GET /users/me/addresses` 🔐

Lists all delivery addresses for the current user.

**Response `200`:**

```json
[
  {
    "id": "uuid",
    "location": "Antananarivo",
    "postal_code": "101",
    "country_code": "MDG",
    "is_default": true
  }
]
```

---

### `POST /users/me/addresses` 🔐

Creates a new address for the current user. If the body includes an `id` of an existing address owned by the user, it updates that address instead (upsert).

**Request body:**

```json
{
  "location": "Antananarivo",
  "postal_code": "101",
  "country_code": "MDG"
}
```

**Response `201`** (new) or `200`** (updated): address object.

---

### `PUT /users/me/addresses/{id}` 🔐

Updates a specific address owned by the current user.

**Request body:** same shape as `POST /users/me/addresses`.

**Response `200`** (existing updated) or `201`** (if id not found, creates): address object.

---

### `DELETE /users/me/addresses/{id}` 🔐

Deletes an address owned by the current user.

**Response `204` — no content.**

---

### `PUT /users/me/addresses/{id}/default` 🔐

Marks one address as default. Clears `is_default` on all other addresses of that user.

**No request body.**

**Response `200`:** updated address object with `is_default: true`.

---

### `GET /users` 🛡

Lists all registered users with pagination. Optionally filter by role.

**Query params:**

| Param | Type | Default | Description |
|---|---|---|---|
| `role` | string | — | Filter by role: `simple_user` or `admin` |
| `page` | int | 1 | Page number |
| `per_page` | int | 20 | Page size |

**Response `200`:**

```json
{
  "data": [ { "id": "uuid", "firstname": "...", "role": "simple_user", ... } ],
  "meta": { "total": 42, "page": 1, "per_page": 20, "last_page": 3 }
}
```

---

### `POST /users` 🛡

Admin creates a new user. Can set any role including `admin`.

**Request body:**

```json
{
  "firstname": "Jane",
  "lastname": "Smith",
  "email": "jane@example.com",
  "phone": "+261300000002",
  "password": "temp-password",
  "role": "admin"
}
```

**Response `201`:** created user object.

---

### `GET /users/{id}` 🛡

Returns a user by ID.

**Response `200`:** user object. `404` if not found.

---

### `DELETE /users/{id}` 🛡

Deletes a user by ID.

**Response `204` — no content.** `404` if not found.

---

## 3. Categories

### `GET /categories` 🌐

Returns the category tree (nested children) by default.

**Query params:**

| Param | Type | Default | Description |
|---|---|---|---|
| `flat` | boolean | false | If `true`, returns a flat list instead of nested tree |

**Response `200` (tree):**

```json
[
  {
    "id": "uuid",
    "label": "Épicerie fine",
    "slug": "epicerie-fine",
    "icon": null,
    "parent_id": null,
    "children": [
      { "id": "uuid", "label": "Épices", "slug": "epices", "children": [] }
    ]
  }
]
```

---

### `GET /categories/{id}` 🌐

Returns one category with its children tree.

**Response `200`:** category object. `404` if not found.

---

### `POST /categories` 🛡

Creates a new category. If the body includes an `id` of an existing category, it updates that category instead (upsert by id).

**Unicity:** `label` is globally unique (case-insensitive). Attempting to create or rename a category with a label already used by another category returns `409 Conflict`.

**Request body:**

```json
{
  "label": "Vanille",
  "slug": "vanille",
  "icon": null,
  "parent_id": null
}
```

**Response `201`** (created) or `200`** (updated if id matched existing): category object.
**`409`** if the label is already used by another category.

---

### `PUT /categories/{id}` 🛡

Updates an existing category. If `id` in body conflicts with path `id` → 422. If no category with that path `id` exists, creates it.

**Request body:** same shape as `POST /categories`, `id` in body is optional.

**Response `200`** (updated) or `201`** (created): category object.
**`409`** if the new label would duplicate another existing category's label.

---

### `DELETE /categories/{id}` 🛡

Deletes a category. Cascades to children depending on database constraints.

**Response `204` — no content.**

---

## 4. Products

### `GET /products` 🌐

Lists products with optional filtering and pagination.

**Query params:**

| Param | Type | Default | Description |
|---|---|---|---|
| `category_id` | uuid | — | Filter by category |
| `is_active` | boolean | — | Filter by active flag |
| `search` | string | — | Full-text search on label/description |
| `currency` | string | — | Filter to only products with a price in that currency |
| `page` | int | 1 | Page number |
| `per_page` | int | 20 | Page size |

**Response `200`:**

```json
{
  "data": [
    {
      "id": "uuid",
      "label": "Poivre noir",
      "reference": "epicerie-fine-poivre-noir-001",
      "description": null,
      "size": null,
      "is_active": true,
      "stock_quantity": 100,
      "category": { "id": "uuid", "label": "Épicerie fine", ... },
      "prices": [
        { "currency_code": "MGA", "value": 18000, "unit": "sachet", "valid_from": "2025-04-14" }
      ],
      "images": []
    }
  ],
  "meta": { "total": 42, "page": 1, "per_page": 20, "last_page": 3 }
}
```

---

### `GET /products/{id}` 🌐

Returns full product detail including all prices and images.

**Response `200`:** product object. `404` if not found.

---

### `POST /products` 🛡

Creates a new product. If the body includes an `id` of an existing product, updates it (upsert).

**Unicity:** both `label` and `reference` are globally unique (case-insensitive). Reuse triggers `409 Conflict`.

**Request body:**

```json
{
  "label": "Poivre noir",
  "reference": "poivre-noir-001",
  "category_id": "uuid",
  "description": "Poivre local de Madagascar",
  "size": null,
  "is_active": true,
  "stock_quantity": 100,
  "prices": [
    { "currency_code": "MGA", "value": 18000, "unit": "sachet" }
  ]
}
```

**Response `201`** (created) or `200`** (updated): product object.
**`409`** if the label or reference is already used by another product.

---

### `PUT /products/{id}` 🛡

Updates a product. If no product with that `id` exists, creates it.

**Request body:** same shape as `POST /products`.

**Response `200`** (updated) or `201`** (created): product object.
**`409`** if the label or reference would duplicate another product.

---

### `DELETE /products/{id}` 🛡

Deletes a product.

**Response `204` — no content.**

---

### `GET /products/{id}/stock` 🛡

Returns only the stock quantity for a product.

**Response `200`:**

```json
{
  "id": "uuid",
  "stock_quantity": 100
}
```

---

### `PUT /products/{id}/stock` 🛡

Sets the stock quantity for a product. Records a stock movement with reason `ADMIN_ADJUSTMENT`.

**Request body:**

```json
{
  "stock_quantity": 150
}
```

**Validation:** `stock_quantity` required and cannot be negative → 422.

**Response `200`:** product object with updated stock.

---

### `GET /products/{id}/stock/movements` 🛡

Returns the paginated stock movement audit log for a product.

Each entry records every change: order placement, order cancellation, or admin adjustment.

**Query params:** `page` (default 1), `per_page` (default 20).

**Response `200`:**

```json
{
  "data": [
    {
      "id": "uuid",
      "product_id": "uuid",
      "delta": -1,
      "reason": "ORDER_PLACED",
      "balance_after": 99,
      "created_at": "2026-04-14T10:00:00",
      "created_by": "user-uuid-or-null",
      "note": "Order ORD-A1B2C3D4"
    }
  ],
  "meta": { "total": 5, "page": 1, "per_page": 20, "last_page": 1 }
}
```

`reason` values: `ORDER_PLACED`, `ORDER_CANCELLED`, `ADMIN_ADJUSTMENT`.

---

### `GET /products/{productId}/images` 🛡

Lists images for a product.

**Response `200`:**

```json
[
  { "id": "uuid", "product_id": "uuid", "url": "https://...", "main": false }
]
```

---

### `POST /products/{productId}/images` 🛡

Adds an image to a product.

**Request body:**

```json
{
  "url": "https://cdn.example.com/image.jpg",
  "main": false
}
```

If `main: true`, all other images for this product are set to `main: false` (at most one main per product).

**Response `201`:** image object.

---

### `DELETE /products/{productId}/images/{imageId}` 🛡

Deletes a product image.

**Response `204` — no content.**

---

### `PUT /products/{productId}/images/{imageId}/main` 🛡

Marks one image as the main image. Clears `main` on all other images of that product.

**No request body.**

**Response `200`:** updated image object with `main: true`.

---

## 5. Delivery Costs

### `GET /delivery-costs` 🌐

Lists all delivery cost options.

**Response `200`:**

```json
[
  {
    "id": "uuid",
    "amount": 2000,
    "provider": "Standard",
    "response_provider": null
  }
]
```

---

### `GET /delivery-costs/{id}` 🌐

Returns one delivery cost option.

**Response `200`:** delivery cost object. `404` if not found.

---

### `POST /delivery-costs` 🌐

Creates a new delivery cost entry. This is intentionally public — the frontend can compute and register delivery costs at checkout time based on recipient location.

**Request body:**

```json
{
  "amount": 2000,
  "provider": "Standard"
}
```

**Response `201`:** created delivery cost object.

---

### `PUT /delivery-costs/{id}` 🛡

Updates an existing delivery cost entry.

**Request body:** same shape as `POST /delivery-costs`.

**Response `200`:** updated delivery cost object.

---

### `DELETE /delivery-costs/{id}` 🛡

Deletes a delivery cost entry.

**Response `204` — no content.**

---

## 6. Orders

### `POST /orders` 🌐

Creates a new order. Works for both guest (no token) and authenticated users (with token).

**Request body:**

```json
{
  "currency_code": "MGA",
  "delivery_cost_id": "uuid",
  "items": [
    { "product_id": "uuid", "quantity": 1 }
  ],
  "recipient": {
    "recipient_name": "John Doe",
    "recipient_email": "john@example.com",
    "recipient_phone": "+261300000000",
    "location": "Antananarivo",
    "postal_code": "101",
    "country_code": "MDG"
  }
}
```

- `delivery_cost_id` is optional
- All recipient fields are required
- `quantity` must be > 0

**What happens internally:**

1. Input fully validated
2. For each item:
   - Product fetched with a row-level write lock (`SELECT … FOR UPDATE`) → 404 if not found
   - Stock checked under the lock: `requested > available` → 422 `Requested quantity exceeds available stock`
   - Price resolved for `currency_code`: none found → 422 `No price available for product X in currency Y`
   - Stock decremented immediately
   - Stock movement recorded (reason: `ORDER_PLACED`)
   - Product snapshot (label + price at order time) frozen in `order_items.product_snapshot`
   - Concurrent orders on the same product serialize on the row lock; the second caller re-reads fresh stock and gets `422` when insufficient
3. `total_amount` = sum of all `(unit_price × quantity)` + delivery cost amount if `delivery_cost_id` is provided
4. Order saved with status `PENDING`
5. If authenticated: `user_id` is set → order becomes owner-restricted

**Response `201`:**

```json
{
  "id": "uuid",
  "reference": "ORD-A1B2C3D4",
  "date": "2026-04-14T10:00:00Z",
  "status": "PENDING",
  "currency_code": "MGA",
  "total_amount": 20000,
  "delivery_cost_id": "uuid",
  "user_id": "uuid-or-null",
  "items": [
    {
      "product_id": "uuid",
      "product_label": "Poivre noir",
      "quantity": 1,
      "unit_price": 18000,
      "total_price": 18000,
      "product": { ... }
    }
  ],
  "recipient": {
    "recipient_name": "John Doe",
    "recipient_email": "john@example.com",
    "recipient_phone": "+261300000000",
    "location": "Antananarivo",
    "postal_code": "101",
    "country_code": "MDG"
  }
}
```

**Access rules after creation:**

| Order type | Who can read/cancel/pay |
|---|---|
| Guest order (`user_id` = null) | Anyone (public) |
| Authenticated order (`user_id` set) | Owner or admin only |

---

### `GET /orders` 🔐

Returns the paginated order history of the currently authenticated user. Only returns orders where `user_id` matches the caller.

**Query params:**

| Param | Type | Default | Description |
|---|---|---|---|
| `status` | string | — | Filter by order status |
| `page` | int | 1 | Page number |
| `per_page` | int | 20 | Page size |

**Response `200`:**

```json
{
  "data": [ { "id": "uuid", "reference": "ORD-...", "status": "PENDING", ... } ],
  "meta": { "total": 5, "page": 1, "per_page": 20, "last_page": 1 }
}
```

---

### `GET /orders/all` 🛡

Returns all orders across all users with pagination. Admin backoffice listing.

**Query params:**

| Param | Type | Default | Description |
|---|---|---|---|
| `status` | string | — | Filter by order status |
| `user_id` | uuid | — | Filter by user |
| `page` | int | 1 | Page number |
| `per_page` | int | 20 | Page size |

**Response `200`:** same paginated shape as `GET /orders`.

---

### `GET /orders/{id}` 🌐

Returns full order detail.

**Access rules:**
- Guest orders: accessible to anyone
- Authenticated orders: only owner or admin → 403 otherwise

**Response `200`:** full order object. `404` if not found.

---

### `PUT /orders/{id}/status` 🛡

Manually updates the status of an order. Admin-only backoffice operation.

**Request body:**

```json
{
  "status": "CONFIRMED"
}
```

Valid statuses: `PENDING`, `CONFIRMED`, `DELIVERED`, `CANCELLED`.

**Response `200`:** updated order object.

---

### `POST /orders/{id}/cancel` 🌐

Cancels an order.

**Access rules:**
- Guest orders: anyone can cancel
- Authenticated orders: only owner or admin

**What happens internally:**

1. Order access checked
2. If order is not already `CANCELLED`:
   - For each item: stock restored (`product.stock_quantity += quantity`)
   - Stock movement recorded (reason: `ORDER_CANCELLED`)
3. Order status set to `CANCELLED`

**Response `200`:** updated order object with `status: CANCELLED`.

---

### `GET /orders/{id}/payments` 🌐

Lists all payment attempts for an order.

**Access rules:** same as `GET /orders/{id}`.

**Response `200`:**

```json
[
  {
    "id": "uuid",
    "order_id": "uuid",
    "method_code": "STRIPE",
    "currency_code": "MGA",
    "amount": 20000,
    "date": "2026-04-14T10:05:00Z",
    "status": "pending",
    "internal_reference": "cs_test_abc123",
    "provider_response": {
      "checkout_url": "https://checkout.stripe.com/pay/cs_test_abc123",
      "checkout_session_id": "cs_test_abc123",
      "checkout_status": "open",
      "payment_status": "unpaid"
    }
  }
]
```

---

### `POST /orders/{id}/payments` 🌐

Initiates a payment for an order.

**Access rules:** same as `GET /orders/{id}`.

**Request body:**

```json
{
  "method_code": "STRIPE",
  "currency_code": "MGA",
  "amount": 20000
}
```

- `amount` must exactly equal `order.total_amount` → 422 if it differs
- `method_code` must match an available provider → 422 if not found

**What happens internally (Stripe path):**

1. Stripe `Session.create(...)` called with `line_items`, `success_url`, `cancel_url`, configured API version
2. `OrderPayment` record saved:
   - `status`: `pending`
   - `internal_reference`: Stripe Checkout Session ID (used later for webhook matching)
   - `provider_response`: `{ checkout_url, checkout_session_id, checkout_status, payment_status }`

**Response `201`:** payment object with `provider_response.checkout_url` to redirect the client to Stripe.

**What happens internally (MANUAL path):**

A payment record is saved with `status: pending` and no provider data. Status must be updated manually.

---

## 7. Payment Methods

### `GET /payment-methods` 🌐

Lists all available payment providers and their metadata.

**Response `200`:**

```json
[
  {
    "provider_code": "MANUAL",
    "label": "Manual Payment",
    "description": "Pay manually"
  },
  {
    "provider_code": "STRIPE",
    "label": "Stripe",
    "description": "Pay with Stripe Checkout"
  }
]
```

Stripe only appears when `STRIPE_ENABLED=true`.

---

## 8. Stripe Webhook

### `POST /payments/webhooks/stripe` 🌐

Receives lifecycle events from Stripe. Always registered (even when Stripe is disabled — returns 404 in that case to avoid Stripe retry storms).

**Headers required:**

```
Stripe-Signature: t=...,v1=...
```

Stripe signature verified against `STRIPE_WEBHOOK_SECRET` → 400 if invalid.

**Supported events:**

| Stripe event | Payment status | Order status |
|---|---|---|
| `checkout.session.completed` | `confirmed` | `CONFIRMED` (if was PENDING) |
| `checkout.session.async_payment_succeeded` | `confirmed` | `CONFIRMED` (if was PENDING) |
| `checkout.session.async_payment_failed` | `failed` | unchanged |
| `checkout.session.expired` | `failed` | unchanged |

**What happens internally:**

1. Stripe signature verified
2. Session ID extracted from event payload
3. `OrderPayment` found by `internal_reference` = session ID
4. Payment `status` and `provider_response` updated
5. If event confirms payment and order is still `PENDING` → order set to `CONFIRMED`

**Response `204` — no content.**

---

## 9. Refund

### `POST /payments/{orderId}/refund` 🛡

Triggers a Stripe refund for a confirmed payment on an order.

Returns `503` when `STRIPE_ENABLED=false`.

**Request body (optional):**

```json
{
  "amount": 18000,
  "reason": "requested_by_customer"
}
```

- If `amount` is omitted, refunds the full remaining capturable amount
- `amount` must be ≤ remaining unrefunded amount → 422 otherwise
- Must be a STRIPE payment with `status: confirmed` to be refundable

**What happens internally:**

1. Order fetched → 404 if not found
2. Most recent confirmed Stripe payment selected
3. Remaining refundable amount computed: `captured - already_refunded`
4. Stripe Refund API called
5. `order_payments.refund_id`, `refunded_amount`, `refunded_at`, `refund_reason` stored
6. If fully refunded → order status set to `CANCELLED`

**Response `200`:**

```json
{
  "order_id": "uuid",
  "payment_id": "uuid",
  "refund_id": "re_abc123",
  "refunded_amount": 18000,
  "refunded_at": "2026-04-14T11:00:00"
}
```

---

## 10. Supabase Auth Webhook

### `POST /auth/webhooks/supabase` 🌐

Receives user lifecycle events from Supabase Auth Hooks. Only registered when `AUTH_PROVIDERS_SUPABASE_ENABLED=true`.

**Headers required:**

```
X-Webhook-Secret: <AUTH_PROVIDERS_SUPABASE_WEBHOOK_SECRET>
```

Uses constant-time comparison. Returns `503` if secret is not configured, `401` if wrong.

**Supported events** (on `auth.users` table):

| Event type | Action |
|---|---|
| `INSERT` | Create or link local user. If a local user with the same email exists, links it to the Supabase ID. Otherwise creates a new `UserAccount` with `auth_provider=SUPABASE` and role `simple_user`. |
| `UPDATE` | Updates email, phone, or status (`banned_until` → `banned`) on the linked local user. |
| `DELETE` | Deletes the linked local user and all their auth sessions. |

**Note on write-through registration:** when `POST /auth/register` already created the user in Supabase, the resulting INSERT webhook is a no-op — the backend finds the already-linked user by `external_auth_id` and skips creation.

**Response `204` — no content.**

---

## 11. Health

### `GET /actuator/health` 🌐

Returns application readiness and liveness status.

**Response `200`:**

```json
{
  "status": "UP"
}
```

Used by Render, Docker health checks, and the CI Gatling workflow to confirm the application is ready before starting load tests.

---

## Order Status State Machine

```
           POST /orders
                │
                ▼
            PENDING ──────────────────────────────► CANCELLED
                │            POST /orders/{id}/cancel  ▲
                │            (stock restored)           │
      POST /orders/{id}/payments                        │
      Stripe webhook: completed/succeeded               │
                │                                       │
                ▼                                       │
           CONFIRMED ──────────── POST /payments/{id}/refund ──► CANCELLED
                │                 (full refund)
      PUT /orders/{id}/status
                │
                ▼
           DELIVERED
```

---

## Stock Movement Reasons

| Reason | Triggered by |
|---|---|
| `ORDER_PLACED` | `POST /orders` — stock decremented per item |
| `ORDER_CANCELLED` | `POST /orders/{id}/cancel` — stock restored per item |
| `ADMIN_ADJUSTMENT` | `PUT /products/{id}/stock` — admin sets new quantity |

---

## Pagination Shape

All paginated endpoints return:

```json
{
  "data": [ ... ],
  "meta": {
    "total": 42,
    "page": 1,
    "per_page": 20,
    "last_page": 3
  }
}
```
