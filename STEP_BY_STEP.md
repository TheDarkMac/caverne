# Deployment Step By Step

This file explains how to deploy this backend on Render, what external accounts you need, which environment variables to set, and which current limitations you must know before going to production.

This guide is written for the current state of this repository as of April 14, 2026.

> Note: the Dockerfile sets `SPRING_PROFILES_ACTIVE=prod`. Both `application-prod.properties` and the `prod` branch of `logback-spring.xml` are shipped — prod boots with JSON stdout logs, `root=INFO`, and masked sensitive data. See §7.7 and `LOGGING.md` for details.

## 1. What You Need Before Starting

You need these accounts/services:

- a GitHub account, because the easiest Render deployment flow is Git-backed
- a Render account, to host:
  - the web service
  - the PostgreSQL database
- optionally a Supabase account, only if you want external authentication through Supabase
- optionally a Stripe account, only if you want Stripe checkout payments
- optionally a frontend deployment target/domain, because Stripe Checkout must redirect users back to a frontend URL after payment

Useful official docs:

- Render web services: https://render.com/docs/web-services
- Render Docker deploys: https://render.com/docs/docker
- Render Postgres: https://render.com/docs/databases
- Supabase API keys: https://supabase.com/docs/guides/api/api-keys
- Supabase JWT signing keys: https://supabase.com/docs/guides/auth/signing-keys
- Stripe Checkout Sessions: https://docs.stripe.com/api/checkout/sessions/create

## 2. Important Things Already Prepared In This Project

These deployment-related points are already handled in the codebase:

- the app can now read Render's `PORT` environment variable
- the HTTP API is exposed under `/api/{version}` through `server.servlet.context-path`, with `APP_API_VERSION=v1` by default
- Stripe runtime settings are now environment-driven:
  - `STRIPE_ENABLED`
  - `STRIPE_API_KEY`
  - `STRIPE_API_VERSION`
  - `STRIPE_CHECKOUT_SUCCESS_URL`
  - `STRIPE_CHECKOUT_CANCEL_URL`
- the first admin can be auto-created on startup with:
  - `BOOTSTRAP_ADMIN_ENABLED`
  - `BOOTSTRAP_ADMIN_*`
- the project already includes a `Dockerfile`
- the app is configured for PostgreSQL, Flyway migrations, and stateless bearer auth

So you do not need to change the code just to make Render pass the port or enable Stripe.

## 3. Current Production Limitations You Must Know

Before deploying, understand these backend limits:

### 3.1 Stripe limitation

Stripe Checkout Session creation and webhook processing are implemented.

What is already handled:

- the backend can create a Stripe Checkout URL
- the user can be redirected to Stripe
- the backend can consume Stripe webhooks on `POST /payments/webhooks/stripe`
- Stripe checkout completion can update payment state and confirm the related order

Current remaining caution:

- the implemented flow covers the Stripe Checkout session lifecycle currently used by this backend
- if later you add refunds, disputes, or more advanced reconciliation rules, the webhook handling must be extended accordingly

### 3.2 Supabase limitation

The current Supabase backend integration verifies HS256 JWTs using:

- `AUTH_PROVIDERS_SUPABASE_JWT_SECRET`

This matters because Supabase now also supports newer signing-key modes. If your Supabase project is using a newer asymmetric signing-key setup instead of the legacy shared JWT secret, this backend will not verify those tokens correctly yet.

So with the current code, production Supabase auth is safe only if:

- your backend is configured with a compatible shared JWT secret / HS256 signing flow

If your Supabase project is already fully moved to asymmetric signing/JWKS-only verification, the backend auth code must be upgraded before relying on it in production.

### 3.3 Bootstrap admin limitation

The bootstrap admin is only for first access.

You should:

- enable it once
- log in
- create/adjust the real admin situation you want
- then disable bootstrap again

Do not leave a default bootstrap password active permanently.

## 4. Prepare Your Repository

### 4.1 Push the project to GitHub

Make sure this repository is pushed to GitHub on the branch you want Render to deploy.

Example:

```bash
git add .
git commit -m "prepare render deployment"
git push origin main
```

### 4.2 Check that these files are present

Render will rely on:

- [`Dockerfile`](/home/icecream/IdeaProjects/caverne/Dockerfile)
- [`build.gradle.kts`](/home/icecream/IdeaProjects/caverne/build.gradle.kts)
- [`src/main/resources/application.properties`](/home/icecream/IdeaProjects/caverne/src/main/resources/application.properties)

## 5. Create The PostgreSQL Database On Render

### 5.1 Create the database

In Render:

1. Open the dashboard.
2. Click `New`.
3. Choose `Postgres`.
4. Pick:
   - a database name
   - a user
   - a region
   - an instance type

Use the same region as your future web service.

### 5.2 Collect the database connection details

After the database is created, open the database page and collect:

- host
- port
- database name
- username
- password

Render exposes internal and external connection details.

For a Render web service in the same region, prefer the internal connection details.

### 5.3 Build the Spring JDBC URL

This Spring app expects a JDBC URL, not just a raw Postgres URL.

Set:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<HOST>:5432/<DATABASE>
SPRING_DATASOURCE_USERNAME=<USERNAME>
SPRING_DATASOURCE_PASSWORD=<PASSWORD>
```

Do not use `postgresql://...` directly unless you convert it to `jdbc:postgresql://...`.

## 6. Create The Render Web Service

### 6.1 Create the service

In Render:

1. Click `New`.
2. Choose `Web Service`.
3. Connect your GitHub repository.
4. Select the branch.
5. For runtime/deploy method, choose Docker.

This project already includes a `Dockerfile`, so Docker is the cleanest path.

### 6.2 Basic Render settings

Recommended values:

- Name: whatever you want, for example `caverne-api`
- Region: the same as the Postgres database
- Branch: `main` or your deployment branch
- Auto deploy: enabled

### 6.3 Port behavior

Render expects the app to bind to `0.0.0.0` and usually provides `PORT`.

This project now supports:

```text
server.port=${PORT:${SERVER_PORT:8080}}
```

So no extra code change is needed for the port.

## 7. Set The Required Environment Variables On Render

In your Render web service, open `Environment` and add these values.

### 7.1 Always required

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<INTERNAL_HOST>:5432/<DATABASE>
SPRING_DATASOURCE_USERNAME=<DB_USERNAME>
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DOCKER_COMPOSE_ENABLED=false

# Local JWT signing — the app refuses to start without this.
# Generate with: openssl rand -base64 48 — must NOT be the same secret as dev.
AUTH_LOCAL_JWT_SECRET=<48+ bytes random base64>
# Optional overrides:
# AUTH_LOCAL_JWT_ISSUER=caverne
# AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS=900     # access JWT TTL (default 15 min)

# Refresh token cookie — long-lived rotated refresh, HttpOnly + Secure
# AUTH_LOCAL_REFRESH_EXPIRES_IN_SECONDS=2592000   # default 30 days
AUTH_LOCAL_REFRESH_COOKIE_SAME_SITE=None           # use None for cross-domain front/api, Lax otherwise
AUTH_LOCAL_REFRESH_COOKIE_SECURE=true              # must be true in prod (HTTPS only)
# AUTH_LOCAL_REFRESH_COOKIE_DOMAIN=                # leave blank to bind to the API hostname
```

`SPRING_DOCKER_COMPOSE_ENABLED` must stay `false` on Render.

`AUTH_LOCAL_JWT_SECRET` signs every access JWT issued by `POST /auth/login` and `POST /auth/refresh`. Treat it like a password: store it in the Render env-var panel (or your platform's secret manager), never in git. Rotating it invalidates all currently issued access tokens (forces a refresh on every connected client; if their refresh cookie is still valid they get a fresh access token automatically).

The refresh cookie is signed only by being a SHA-256 hash kept server-side. Rotating `AUTH_LOCAL_JWT_SECRET` does **not** invalidate refresh tokens — only manual `DELETE FROM refresh_tokens` or per-user logout does.

### 7.2 Usually recommended

```text
SERVER_PORT=8080
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL=false
```

You can leave SQL logging on, but for production it is usually better to disable it.

### 7.3 Frontend / CORS configuration

If your frontend is hosted on another domain, configure CORS explicitly:

```text
APP_CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>
APP_CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
APP_CORS_ALLOWED_HEADERS=Authorization,Content-Type,Accept,Origin
APP_CORS_EXPOSED_HEADERS=Location
APP_CORS_ALLOW_CREDENTIALS=true
APP_CORS_MAX_AGE=3600
```

For local development, the default already allows:

- `http://localhost:3000`
- `http://127.0.0.1:3000`

For production:

- replace that with your real frontend domain
- if you have multiple frontend origins, separate them with commas

### 7.4 First admin bootstrap

Only if you want the backend to auto-create the first admin on first deploy:

```text
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_FIRSTNAME=Initial
BOOTSTRAP_ADMIN_LASTNAME=Admin
BOOTSTRAP_ADMIN_EMAIL=admin@yourdomain.com
BOOTSTRAP_ADMIN_PHONE=
BOOTSTRAP_ADMIN_PASSWORD=<strong_temporary_password>
```

Important:

- use a strong temporary password
- after first successful admin access, disable `BOOTSTRAP_ADMIN_ENABLED`

### 7.5 If you want Supabase authentication

```text
AUTH_PROVIDERS_SUPABASE_ENABLED=true
AUTH_PROVIDERS_SUPABASE_URL=https://<your-project-ref>.supabase.co
AUTH_PROVIDERS_SUPABASE_SERVICE_ROLE_KEY=<your_supabase_service_role_key>
AUTH_PROVIDERS_SUPABASE_JWT_SECRET=<your_supabase_jwt_secret_if_compatible>
AUTH_PROVIDERS_SUPABASE_ISSUER=https://<your-project-ref>.supabase.co/auth/v1
AUTH_PROVIDERS_SUPABASE_AUDIENCE=authenticated
AUTH_PROVIDERS_SUPABASE_WEBHOOK_SECRET=<shared secret used by the Supabase Auth Hook>
```

Where to get them:

- `URL`: your Supabase project URL (e.g. `https://xxxx.supabase.co`)
- `SERVICE_ROLE_KEY`: from Supabase dashboard → Project Settings → API → `service_role` key. **Keep this secret — it has full database access.**
- `ISSUER`: your Supabase project URL + `/auth/v1`
- `JWT secret`: from Supabase auth/signing configuration, only if you are using a compatible shared secret flow
- `WEBHOOK_SECRET`: a shared secret you define and configure in Supabase Auth Hooks

**Registration write-through:**

When `AUTH_PROVIDERS_SUPABASE_ENABLED=true` and `AUTH_PROVIDERS_SUPABASE_URL` + `AUTH_PROVIDERS_SUPABASE_SERVICE_ROLE_KEY` are set, `POST /auth/register` now writes the user to both your local database and Supabase simultaneously using the Admin API (`POST {url}/auth/v1/admin/users`). The Supabase UUID is stored in `app_users.external_auth_id`. If the Supabase call fails, the local registration is rolled back.

When Supabase then fires its INSERT webhook back, the backend finds the already-linked user by `external_auth_id` and does nothing — no duplicate is created.

**Inbound webhook:**

Register the inbound webhook URL `https://<your-backend-domain>/api/v1/auth/webhooks/supabase` in the Supabase dashboard (Auth > Hooks). Supabase must send the configured shared secret in the `X-Webhook-Secret` header. The endpoint:
- requires `AUTH_PROVIDERS_SUPABASE_ENABLED=true` to be registered
- returns 503 when `AUTH_PROVIDERS_SUPABASE_WEBHOOK_SECRET` is not set
- returns 401 on missing or wrong secret
- handles `INSERT`, `UPDATE`, `DELETE` events on `auth.users`

Warning:

- if your Supabase project is no longer using a compatible HS256 shared secret for user tokens, this backend auth implementation needs to be upgraded before production use

### 7.6 If you want Stripe payments

```text
STRIPE_ENABLED=true
STRIPE_API_KEY=<your_stripe_secret_key>
STRIPE_API_VERSION=2025-03-31.acacia
STRIPE_CHECKOUT_SUCCESS_URL=https://<your-frontend-domain>/checkout/success?session_id={CHECKOUT_SESSION_ID}
STRIPE_CHECKOUT_CANCEL_URL=https://<your-frontend-domain>/checkout/cancel
STRIPE_WEBHOOK_SECRET=<your_stripe_webhook_signing_secret>
```

Where to get them:

- `STRIPE_API_KEY`: from the Stripe Dashboard
  - Developers
  - API keys
  - use the secret key
- `STRIPE_WEBHOOK_SECRET`: from the Stripe Dashboard
  - Developers
  - Webhooks
  - open the webhook endpoint
  - reveal the signing secret
- success/cancel URLs:
  - these are not given by Stripe
  - they are frontend URLs you choose
  - Stripe redirects the user to them after checkout

Important:

- do not leave `http://localhost:3000/...` in production
- replace it with your real frontend domain

### 7.7 Logging / observability

The backend ships a structured logging pipeline — nothing extra is required to deploy, but a few environment knobs let you tune it on Render without redeploying. The full guide lives in [`LOGGING.md`](/home/icecream/IdeaProjects/caverne/LOGGING.md).

**What runs out of the box in `prod`:**

- JSON logs on stdout — Render captures them automatically in the dashboard
- One line per HTTP request (method, URI, status, duration, IP, user-agent, userId)
- Correlation id (`traceId`) propagated in every log line and returned to clients via `X-Trace-Id`
- Password/token/Bearer values auto-masked before serialization

**Recommended defaults (already applied — override only if you need to):**

```text
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_COM_DEVIKAPPS_CAVERNE=INFO
LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN
LOGGING_LEVEL_HTTP_ACCESS=INFO
```

To diagnose an incident without redeploying, bump a specific logger:

```text
LOGGING_LEVEL_COM_DEVIKAPPS_CAVERNE=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
```

Render restarts the service automatically when env vars change.

**Persist logs outside Render (recommended)** — Render wipes the filesystem on every redeploy, so you want an external destination if you need history.

*Recommended path — Render Log Streams:*

1. Render dashboard → your service → *Settings* → *Log Streams* → *Add Log Stream*
2. Pick a destination (Better Stack, Papertrail, Datadog, New Relic, or a syslog endpoint)
3. Paste the destination's token — Render forwards stdout in real time, zero code change

The JSON already emitted by the app (with `timestamp`, `level`, `logger`, `message`, `mdc.traceId`, `mdc.userId`, `stackTrace`) is auto-parsed by most destinations, so you get searchable logs out of the box.

*Alternative — push directly from the app to Grafana Loki:*

```text
SPRING_PROFILES_ACTIVE=prod,loki
LOKI_URL=https://logs-prod-xxx.grafana.net/loki/api/v1/push
LOKI_USER=<tenant-id>
LOKI_PASSWORD=<api-key>
LOKI_ENV=prod
```

The Loki appender is wired via a dedicated Spring profile — no code change required. Stdout still gets JSON logs even when Loki is active, so you keep a Render-side fallback. Full details in [`LOGGING.md`](/home/icecream/IdeaProjects/caverne/LOGGING.md) §9.

## 8. If You Use Supabase, What Exactly Do You Need To Collect

From Supabase you need:

- project URL / project ref
- service role key (for Admin API write-through on registration)
- auth issuer URL
- a compatible JWT secret, if your current backend verification strategy still uses it

What to collect in Supabase:

1. Open your project.
2. Open Project Settings → API.
3. Copy the **Project URL** → `AUTH_PROVIDERS_SUPABASE_URL`
4. Copy the **`service_role`** key → `AUTH_PROVIDERS_SUPABASE_SERVICE_ROLE_KEY`. Never expose this on the frontend.
5. Check Auth → Signing Keys for the JWT secret.

If the JWT secret is compatible HS256:

- set `AUTH_PROVIDERS_SUPABASE_JWT_SECRET`

If not:

- do not enable Supabase auth in this backend yet
- first upgrade the backend to support asymmetric/JWKS verification

## 9. If You Use Stripe, What Exactly Do You Need To Collect

From Stripe you need:

- a secret API key for the correct mode:
  - test mode for testing
  - live mode for production
- the frontend URLs that Stripe should redirect to after payment success/cancel

How to get the key:

1. Open Stripe Dashboard.
2. Go to `Developers`.
3. Go to `API keys`.
4. Copy the secret key.

Use:

- `sk_test_...` for testing
- `sk_live_...` only when you are actually ready for production

Current backend note:

- this app creates Stripe Checkout Sessions
- it verifies Stripe webhook signatures with `STRIPE_WEBHOOK_SECRET`
- it expects Stripe Checkout webhooks on `POST /payments/webhooks/stripe`

How the two Stripe routes differ:

- `POST /orders/{id}/payments` is your own API route used by the frontend to start payment for one order
- `POST /payments/webhooks/stripe` is the callback route used by Stripe itself to notify your backend about checkout events later
- the webhook route is therefore public on purpose, but protected by Stripe signature verification instead of bearer auth

Recommended Stripe webhook events for this backend:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`

`payment_intent.*` events are needed for asynchronous payment methods where Stripe confirms the PaymentIntent after the Checkout Session closes; the backend matches them to the stored `OrderPayment` via `stripe_payment_intent_id`.

The exact webhook contract consumed by the backend is published in `docs/api.yaml` at `POST /payments/webhooks/stripe`:

- required `Stripe-Signature: t=<timestamp>,v1=<hmac_sha256>` header (verified against `STRIPE_WEBHOOK_SECRET`)
- request body: a raw Stripe Event object (`StripeEvent` schema) — the backend reads `id`, `type`, and `data.object.{id,status,payment_status,payment_intent}`
- responses:
  - `204` — event processed (or already seen; idempotency is keyed on Stripe `event.id`)
  - `404` — Stripe is disabled in this environment, or no matching `OrderPayment` was found for the session/PaymentIntent id
  - `422` — invalid signature or malformed payload
  - `500` — internal processing failure
  - `503` — `STRIPE_WEBHOOK_SECRET` is not configured

Quickest way to check the contract locally:

```bash
stripe listen --forward-to localhost:8080/payments/webhooks/stripe
stripe trigger checkout.session.completed
```

The Stripe CLI signs the forwarded request with your dev webhook secret, so the `Stripe-Signature` verification path exercises the real production code path.

## 10. First Deployment

Once the environment variables are set:

1. Trigger the first Render deploy.
2. Watch the build logs.
3. Watch the startup logs.

What should happen:

- Docker image builds
- app starts
- Flyway runs migrations automatically
- app connects to Render Postgres
- if bootstrap admin is enabled and no admin exists, one admin user is created

What to verify in logs:

- no datasource/auth/Stripe startup failure
- Flyway migration success
- app started successfully

## 11. After Deploy, What You Must Verify

### 11.1 Health/basic startup

Check:

- the Render service is marked healthy
- the service URL responds

### 11.2 Database

Confirm:

- tables were created/migrated by Flyway
- the app is connected to the correct Render database

### 11.3 Admin bootstrap

If bootstrap was enabled:

1. log in with the bootstrap admin
2. verify the admin can access admin-only endpoints
3. disable `BOOTSTRAP_ADMIN_ENABLED`
4. redeploy

### 11.4 Supabase

If Supabase is enabled:

- test a real bearer token coming from Supabase
- verify `/users/me` works
- verify linked/provisioned users behave as expected
- verify admin users are not accidentally created from token claims

### 11.5 Stripe

If Stripe is enabled:

- create a real test order
- call `/orders/{id}/payments`
- verify the response contains:
  - `provider_response.checkout_url`
  - `provider_response.checkout_session_id`
- open the checkout URL
- verify redirect goes back to your frontend success/cancel URLs
- configure Stripe to send webhooks to:
  - `https://<your-backend-domain>/payments/webhooks/stripe`
- verify `STRIPE_WEBHOOK_SECRET` matches the Stripe endpoint signing secret
- verify a completed checkout updates the stored payment status and order status

## 12. Recommended First Production Sequence

Use this order:

1. Deploy without Supabase and without Stripe first.
2. Confirm:
   - Render service boots
   - Postgres connection works
   - admin bootstrap works
3. Disable bootstrap admin after first access.
4. Enable Supabase only after you confirm token-signing compatibility.
5. Enable Stripe only after your frontend success/cancel URLs are ready.
6. Configure Stripe webhooks and verify the signing secret before calling the Stripe flow fully production-ready.

## 13. Minimal Render Environment Example

If you want the simplest production-like deploy first:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<internal-host>:5432/<db>
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DOCKER_COMPOSE_ENABLED=false

BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_FIRSTNAME=Initial
BOOTSTRAP_ADMIN_LASTNAME=Admin
BOOTSTRAP_ADMIN_EMAIL=admin@yourdomain.com
BOOTSTRAP_ADMIN_PASSWORD=<temporary-strong-password>

AUTH_PROVIDERS_SUPABASE_ENABLED=false
STRIPE_ENABLED=false
```

Then later:

- disable bootstrap
- enable Supabase if compatible
- enable Stripe when frontend redirect URLs and webhook secret are ready

## 14. Concrete Modifications Still Worth Doing Later

You asked what modifications are still needed. For deployment, the app is now close enough to run, but these are still important future improvements:

- upgrade Supabase verification to support modern asymmetric/JWKS verification if that is your project setup
- add a real admin password-change flow after bootstrap
- expose refund metadata fields (`refund_id`, `refunded_amount`, `refunded_at`) in the OpenAPI Payment schema once the contract is regenerated
- add `render.yaml` if you want a more reproducible Render deployment flow
- add alerting on top of the existing structured logs (Better Stack / Grafana / Datadog rules)

Structured logs, correlation ids (`traceId` propagated end-to-end), per-request access logs, sensitive-data masking, and Micrometer Tracing are **already in place** — see [`LOGGING.md`](/home/icecream/IdeaProjects/caverne/LOGGING.md).

### Database changes since the previous revision

- V12: `stock_movements` table — every change of `products.stock_quantity` is journalled with reason, delta, balance_after, and optional actor.
- V13: `order_payments` adds `refund_id`, `refunded_amount`, `refunded_at`, `refund_reason` to support the new admin refund endpoint.
- V14 (planned slot): `product_images.is_main` — column already introduced in V9; reserved for future backfill/constraints if tightened.
- V16: case-insensitive unique indexes `ux_categories_label_lower` and `ux_products_label_lower` on `lower(label)`. Existing duplicate labels must be cleaned up before the migration runs.

### New backend endpoints since the previous revision

- `GET /products/{id}/stock/movements` (admin) — paginated stock-movement log.
- `GET/POST /products/{productId}/images`, `DELETE /products/{productId}/images/{imageId}`, `PUT /products/{productId}/images/{imageId}/main` (admin).
- `POST /auth/webhooks/supabase` — Supabase Auth Hooks ingress, shared-secret protected.
- `POST /payments/{orderId}/refund` (admin) — Stripe-backed refund.
- `GET /actuator/health` — public readiness/liveness via Spring Boot Actuator.

### Behavioral changes since the previous revision

- Category and product labels are now globally unique (case-insensitive). Admin upsert endpoints return `409 Conflict` on a duplicate label.
- `POST /orders` now reads each ordered product with `SELECT … FOR UPDATE`, so concurrent checkouts on the same product serialize at the row level and cannot both pass the sufficient-stock check. Insufficient-stock response remains `422`.
- The niches importer (`CATALOG_NICHES_IMPORT_ENABLED=true`) is now idempotent against labels repeated across categories: the second occurrence reuses the first product instead of failing the unique index.

## 15. Short Deployment Checklist

- GitHub repo pushed
- Render Postgres created
- JDBC URL prepared
- Render web service created from Docker
- datasource env vars set
- bootstrap admin env vars set
- Supabase env vars set only if compatible (`URL` + `SERVICE_ROLE_KEY` required for registration write-through)
- Stripe env vars set only when frontend URLs and webhook secret are ready
- first deploy successful
- first admin login successful
- bootstrap disabled after first access
- Render logs show JSON lines with `traceId`/`userId` fields (proves the logging pipeline is live)
- optional: Loki push or Better Stack/Datadog integration configured
