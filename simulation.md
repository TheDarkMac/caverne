# Simulation

This file describes, in practical order, what a `simple_user` can do and what an `admin` user can do with the current backend.

It is based on the system as implemented now, not on future ideas.

## 0. Running The Gatling Performance Simulation Locally

The Gatling simulation lives in [`PublicApiSimulation.java`](/home/icecream/IdeaProjects/caverne/src/gatling/java/com/devikapps/caverne/performance/PublicApiSimulation.java).

**The simulation does not start the server.** The server must be running before `gatlingRun` is called.

The simplest way to run it locally:

```bash
make gatling
```

This builds the jar, starts Postgres via Docker Compose, waits for the server to be healthy, runs the simulation, and stops the server automatically.

**Prerequisites for a passing run:**

- Docker must be running (Postgres is started via `docker compose up -d`)
- The catalog must be populated — `CATALOG_NICHES_IMPORT_ENABLED=true` is set automatically by `make gatling`
- The simulation uses `currency_code: MGA` which matches the `niches.md` catalog currency

**CI:** the `CI-Gatling.yml` workflow handles all of this automatically on every push.

**Tunable load via `-D` flags or env vars** (all have safe defaults):

| Property | Env var | Default |
|---|---|---|
| `gatling.baseUrl` | `GATLING_BASE_URL` | `http://localhost:8080/api/v1` |
| `gatling.users.browse` | `GATLING_USERS_BROWSE` | 50 |
| `gatling.rate.browse` | `GATLING_RATE_BROWSE` | 60 |
| `gatling.users.auth` | `GATLING_USERS_AUTH` | 40 |
| `gatling.users.checkout` | `GATLING_USERS_CHECKOUT` | 10 |
| `gatling.rate.checkout` | `GATLING_RATE_CHECKOUT` | 10 |

## 1. Common Starting Point

Before talking about roles, these public actions are available to anyone:

### 1.1 Browse the catalog

Anyone can:

- list categories with `GET /categories`
- read one category with `GET /categories/{id}`
- list products with `GET /products`
- read one product with `GET /products/{id}`
- list payment methods with `GET /payment-methods`

### 1.2 Create a guest order

Anyone can create an order without being authenticated:

- `POST /orders`

The payload must include:

- `currency_code`
- `items`
- `recipient`

If the order is created as a guest order:

- it has no `user_id`
- it can still be read later with `GET /orders/{id}`
- it can still be cancelled with `POST /orders/{id}/cancel`
- it can still receive a payment request with `POST /orders/{id}/payments`
- its payments can still be listed with `GET /orders/{id}/payments`

So guest checkout already exists in the current system.

## 2. Simple User Simulation

A `simple_user` is the standard authenticated customer account.

### 2.1 Register

The user can create an account with:

- `POST /auth/register`

He can provide:

- `email`
- `phone`
- or both

But at least one of them is required together with:

- `firstname`
- `lastname`
- `password`

When registration succeeds:

- the created backend role is `simple_user`
- status is usually `active`

### 2.2 Log in

The user can authenticate with:

- `POST /auth/login`

He can log in using:

- `email + password`
- or `phone + password`

The response contains:

- `access_token`
- `token_type`
- `expires_in`

Then the frontend must send:

```text
Authorization: Bearer <access_token>
```

on authenticated requests.

### 2.3 Read his own profile

Once authenticated, the user can call:

- `GET /users/me`

He receives:

- `id`
- `firstname`
- `lastname`
- `email`
- `phone`
- `role`
- `status`

### 2.4 Update his own profile

The user can modify his own personal information with:

- `PUT /users/me`

Currently the editable fields are:

- `firstname`
- `lastname`
- `phone`

### 2.5 Log out

The user can invalidate the local session token with:

- `POST /auth/logout`

If the token is a local backend-issued token:

- it becomes unusable immediately

If the token comes from Supabase:

- the backend accepts the logout endpoint
- but real token revocation is still handled by Supabase itself

### 2.6 Manage his addresses

The user can manage delivery addresses under:

- `GET /users/me/addresses`
- `POST /users/me/addresses`
- `PUT /users/me/addresses/{id}`
- `DELETE /users/me/addresses/{id}`
- `PUT /users/me/addresses/{id}/default`

Typical flow:

1. create a first address
2. create another one
3. mark one as default
4. edit an address later
5. delete an address no longer needed

### 2.7 Create an authenticated order

If the user is logged in and sends the bearer token while calling:

- `POST /orders`

then the created order is attached to that user.

That changes the behavior compared to a guest order:

- the order gets a `user_id`
- it appears in the user history
- access becomes owner/admin-only

### 2.8 View his own order history

The user can list only his own orders with:

- `GET /orders`

This is the dashboard/history endpoint for authenticated customers.

### 2.9 Read one of his own orders

The user can call:

- `GET /orders/{id}`

If the order belongs to him:

- access is allowed

If the order belongs to another authenticated user:

- access is forbidden

If the order is a guest order:

- access can still be public

### 2.10 Cancel one of his own orders

The user can call:

- `POST /orders/{id}/cancel`

Rules:

- if it is his own authenticated order, it works
- if it belongs to another authenticated user, it is forbidden
- if it is a guest order, cancellation can still be public

### 2.11 Start a payment for his own order

The user can call:

- `POST /orders/{id}/payments`

If the order belongs to him:

- payment initiation is allowed

If it belongs to another authenticated user:

- payment initiation is forbidden

If it is a guest order:

- payment initiation can still be public

### 2.12 Read payments for his own order

The user can call:

- `GET /orders/{id}/payments`

If the order belongs to him:

- payment listing is allowed

If it belongs to another authenticated user:

- payment listing is forbidden

If it is a guest order:

- payment listing can still be public

### 2.13 Use Stripe checkout

If the frontend chooses `STRIPE` as payment method and Stripe is enabled, the backend can return:

- `provider_response.checkout_url`
- `provider_response.checkout_session_id`

The expected frontend behavior is:

1. create payment with `POST /orders/{id}/payments`
2. read `provider_response.checkout_url`
3. redirect the browser to Stripe Checkout

Important clarification about webhooks:

- `POST /orders/{id}/payments` is your application endpoint for starting the payment on one order
- `POST /payments/webhooks/stripe` is not called by the frontend user
- it is called later by Stripe servers, from outside, after checkout events happen

So the direction is:

1. your frontend calls `POST /orders/{id}/payments`
2. your backend creates the Stripe Checkout Session
3. the user pays on Stripe
4. Stripe calls `POST /payments/webhooks/stripe`
5. your backend verifies the Stripe signature and updates the local payment/order state

This is why the webhook route is separate from `/orders/{id}/payments`:

- Stripe does not know your internal order id
- Stripe sends provider-level events for all sessions to one backend endpoint
- your backend matches the Stripe session id back to the stored order payment

What the webhook request from Stripe actually looks like (contract published in `docs/api.yaml`):

- method and path: `POST /payments/webhooks/stripe`
- required header: `Stripe-Signature: t=<timestamp>,v1=<hmac_sha256>` — verified against `STRIPE_WEBHOOK_SECRET`
- body: a raw Stripe Event (JSON), with `id`, `type`, and `data.object` — the backend reads `data.object.id` (either a `cs_...` Checkout Session id or a `pi_...` PaymentIntent id) plus `status`, `payment_status`, `payment_intent`
- the frontend never sees this request, it only sees the redirect back from Stripe's hosted checkout page to the `success_url` / `cancel_url` configured at payment creation time
- duplicate deliveries are safe: the backend records each Stripe `event.id` and short-circuits repeat calls with `204`
- possible responses Stripe may observe: `204` processed, `404` Stripe disabled or unknown payment, `422` bad signature/payload, `500` internal failure, `503` webhook secret missing

### 2.14 What a simple user cannot do

A `simple_user` cannot:

- list all users
- create users in admin mode
- get another user by id
- delete another user
- list all orders
- update order status manually
- create/update/delete categories
- create/update/delete products

If he tries these admin routes:

- the backend returns `403`

## 3. Admin Simulation

An `admin` user can do everything a `simple_user` can do for his own account, plus the management actions below.

### 3.1 Become admin

In the current backend, an admin can exist in 3 ways:

1. created directly in database/bootstrap
2. created by another admin through `POST /users`
3. linked from an existing local admin account to an external provider such as Supabase

Important current rule:

- an external auth token does not automatically make someone admin
- admin role is managed by the backend, not granted from external token claims

### 3.2 Log in as admin

An admin uses the same login endpoint:

- `POST /auth/login`

or may authenticate through a configured external provider such as Supabase if the linked local account is already admin.

### 3.3 Read his own profile and manage his own addresses

An admin can also use:

- `GET /users/me`
- `PUT /users/me`
- `/users/me/addresses*`

because admin is still a normal authenticated user too.

### 3.4 List all users

Admin can call:

- `GET /users`

This is the user management list.

### 3.5 Create another user

Admin can call:

- `POST /users`

and create:

- another `simple_user`
- or another `admin`

depending on the payload.

### 3.6 Read one user by id

Admin can call:

- `GET /users/{id}`

### 3.7 Delete a user

Admin can call:

- `DELETE /users/{id}`

### 3.8 List all orders

Admin can access the backoffice order list with:

- `GET /orders/all`

This is different from `GET /orders`:

- `GET /orders` is only current user history
- `GET /orders/all` is the admin/global listing

Admin can also filter by:

- order status
- `user_id`

### 3.9 Read any authenticated user's order

Admin can call:

- `GET /orders/{id}`

and access:

- his own orders
- another authenticated user's order
- guest orders

### 3.10 Cancel any order allowed by business rules

Admin can call:

- `POST /orders/{id}/cancel`

and is not blocked by the owner-only restriction.

### 3.11 Access payments for any owned order

Admin can:

- list payments with `GET /orders/{id}/payments`
- initiate payments with `POST /orders/{id}/payments`

even when the order belongs to another authenticated user.

### 3.12 Update order status manually

Admin can call:

- `PUT /orders/{id}/status`

This is the management endpoint used to move an order for example to:

- `confirmed`
- `delivered`
- `cancelled`

depending on current business logic and accepted contract values.

### 3.13 Manage categories

Admin can:

- create category with `POST /categories`
- update category with `PUT /categories/{id}`
- delete category with `DELETE /categories/{id}`

The catalog write flow currently supports the upsert-style behavior already implemented in the backend.

### 3.14 Manage products

Admin can:

- create product with `POST /products`
- update product with `PUT /products/{id}`
- delete product with `DELETE /products/{id}`

### 3.15 What admin still cannot do because the backend does not implement it yet

Even an admin currently does not have complete features for:

- review management
- dashboard/business analytics

Areas that ARE implemented today (admin-only unless otherwise stated):

- payment webhook processing (`POST /payments/webhooks/stripe`, public, signature-validated)
- refunds: `POST /payments/{orderId}/refund`
- stock management: `GET /products/{id}/stock`, `PUT /products/{id}/stock`, `GET /products/{id}/stock/movements`
- product image management: `GET/POST /products/{productId}/images`, `DELETE /products/{productId}/images/{imageId}`, `PUT /products/{productId}/images/{imageId}/main`
- delivery cost management: `GET /delivery-costs` and `POST /delivery-costs` are public (guest can read and create at checkout); `PUT /delivery-costs/{id}` and `DELETE /delivery-costs/{id}` are admin-only
- inbound Supabase auth lifecycle: `POST /auth/webhooks/supabase` (shared-secret protected, not bearer auth)
- health: `GET /actuator/health` is public

Role names used above match the `UserRole` enum: `ADMIN` and `SIMPLE_USER`.

## 4. Guest vs Simple User vs Admin Summary

### 4.1 Guest

Can:

- browse catalog
- create guest order
- read/cancel/pay guest order
- list payment methods

Cannot:

- access `/users/me`
- access `/orders`
- manage addresses
- access admin routes

### 4.2 Simple user

Can:

- do everything a guest can
- manage own profile
- manage own addresses
- create authenticated orders
- list own order history
- read/cancel/pay only own authenticated orders

Cannot:

- manage users globally
- manage catalog writes
- manage all orders
- change order status globally

### 4.3 Admin

Can:

- do everything a simple user can
- manage users
- manage catalog writes (categories, products, product images with main toggle)
- manage stock and read stock movement audit log
- manage delivery-cost catalog (`PUT`/`DELETE` on `/delivery-costs/{id}`)
- list all orders
- filter orders by user/status
- update order status
- access any owned order/payment surface
- trigger Stripe refunds via `POST /payments/{orderId}/refund`

## 5. Realistic Frontend Reading

If you are building the frontend, the role behavior should be interpreted like this:

- public storefront:
  - categories
  - products
  - guest checkout
- customer dashboard:
  - `/users/me`
  - `/users/me/addresses*`
  - `/orders`
  - `/orders/{id}`
  - `/orders/{id}/payments`
- admin dashboard/backoffice:
  - `/users`
  - `/users/{id}`
  - `/orders/all`
  - `/orders/{id}/status`
  - catalog write endpoints

## 6. Important Current Note

The backend now enforces order ownership for authenticated orders.

That means:

- simply knowing an authenticated order id is not enough to read/pay/cancel it
- only the owner or an admin can access it

Guest orders remain more open because they are not linked to a user account.
