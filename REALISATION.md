# Realisation Notes

## Scope

This repository is a Spring Boot backend built contract-first from [`docs/api.yaml`](/home/icecream/IdeaProjects/caverne/docs/api.yaml).

The word "dashboard" should not be read here as a backend business domain.
It only refers to the frontend area where authenticated users and admins interact with the website.

The backend is currently organized by functional modules:

- catalog
- orders
- payments
- users/auth
- common API infrastructure

## Backend Structure

The main backend flow is:

- controller receives HTTP request
- controller parses/generated OpenAPI request model
- service applies business rules
- repository reads or writes database state
- mapper converts internal entities to generated OpenAPI response models

Current rule:

- controllers expose generated OpenAPI models only
- services orchestrate business logic
- repositories talk to the database through Spring Data JPA
- JPA entities stay internal

## Persistence And Repository Layer

Database access is handled by Spring Data JPA repositories such as:

- [`CategoryRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/CategoryRepository.java)
- [`ProductRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/ProductRepository.java)
- [`OrderRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/OrderRepository.java)
- [`UserRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/UserRepository.java)
- [`UserAddressRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/UserAddressRepository.java)
- [`AuthSessionRepository.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AuthSessionRepository.java)

How repositories call the database:

- each repository extends Spring Data interfaces such as `JpaRepository`
- Spring generates the implementation at runtime
- services inject repositories and call them directly
- Hibernate is the JPA provider used under Spring Boot
- SQL schema is versioned by Flyway migrations

This means the repository code itself does not manually open JDBC connections.
Spring Boot wires:

- the datasource
- transaction boundaries
- Hibernate entity manager
- repository implementations

Current migrations:

- [`V1__initial_schema.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V1__initial_schema.sql) — categories, products, prices, orders, order_items, order_payments
- [`V2__users_and_auth_sessions.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V2__users_and_auth_sessions.sql) — app_users, auth_sessions
- [`V3__orders_add_delivery_cost_id.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V3__orders_add_delivery_cost_id.sql) — delivery_cost_id bigint on orders
- [`V4__user_identities_and_addresses.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V4__user_identities_and_addresses.sql) — nullable email, unique phone index, user_addresses table
- [`V5__orders_add_user_id.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V5__orders_add_user_id.sql) — user_id on orders for authenticated order linking
- [`V6__auth_provider_identity.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V6__auth_provider_identity.sql) — auth_provider and external_auth_id on app_users for external provider linking
- [`V7__delivery_costs.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V7__delivery_costs.sql) — delivery_costs table
- [`V8__convert_ids_to_uuid.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V8__convert_ids_to_uuid.sql) — converts all bigserial primary/foreign keys to UUID across every table
- [`V9__catalog_icons_images.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V9__catalog_icons_images.sql) — icon column on categories, product_images table
- [`V10__product_stock_and_order_item_snapshot.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V10__product_stock_and_order_item_snapshot.sql) — stock_quantity on products, product_snapshot text on order_items
- [`V11__order_payments_pk.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V11__order_payments_pk.sql) — NOT NULL + primary key on order_payments.payment_id, index on internal_reference

JPA is configured in validation mode, not schema-generation mode.
So Flyway creates/updates the schema, and Hibernate validates that entity mappings match it.

## Contract Layer

The API contract lives in [`docs/api.yaml`](/home/icecream/IdeaProjects/caverne/docs/api.yaml).

Generated Java models live under:

- `org.openapitools.client.model.*`

Generated client/runtime code lives under:

- [`clients/`](/home/icecream/IdeaProjects/caverne/clients)

Why generated models are used in controllers:

- they lock request/response shapes to the OpenAPI contract
- they preserve exact JSON names such as `parent_id`, `category_id`, `delivery_cost_id`
- they reduce drift between backend and frontend

After every contract change:

- regenerate `./clients`
- keep backend mappings aligned
- update or add integration tests

## Functional State

### Catalog

Implemented:

- public category tree listing
- public product listing with filtering/pagination
- public product detail
- admin category create/update
- admin product create/update
- category write alignment with upsert-style contract handling

Main files:

- [`CategoryController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/CategoryController.java)
- [`CategoryService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/CategoryService.java)
- [`CategoryApiMapper.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/CategoryApiMapper.java)
- [`ProductController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/ProductController.java)
- [`ProductService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/ProductService.java)
- [`ProductApiMapper.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/catalog/ProductApiMapper.java)

### Orders

Implemented:

- guest order creation
- authenticated order creation linked to a real user owner
- current user order history through `GET /orders`
- admin order listing
- order detail
- order cancel
- order status update
- recipient-based checkout payload
- `delivery_cost_id` on order API/persistence
- real delivery-cost catalog with public read/admin write endpoints
- order `total_amount` computation including delivery cost when `delivery_cost_id` is provided

Main files:

- [`OrderController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/OrderController.java)
- [`OrderService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/OrderService.java)
- [`OrderApiMapper.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/OrderApiMapper.java)
- [`DeliveryCostController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/DeliveryCostController.java)
- [`DeliveryCostService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/DeliveryCostService.java)

Current limitation:

- guest and authenticated orders still coexist in the same flow
- payment endpoints now follow the same owner/admin access rules as order detail and cancel for user-owned orders, while guest orders remain accessible without authentication

### Payments

Implemented:

- manual payment initiation for an order
- payment listing for an order
- payment provider abstraction
- `/payment-methods` listing of available providers and metadata
- Stripe payment provider (conditioned on `stripe.enabled`)

Current code:

- [`PaymentProvider.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/payment/PaymentProvider.java)
- [`ManualPaymentProvider.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/payment/ManualPaymentProvider.java)
- [`PaymentMethodService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/payment/PaymentMethodService.java)
- [`PaymentMethodController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/payment/PaymentMethodController.java)
- [`StripePaymentProvider.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/payment/StripePaymentProvider.java)

Important clarification:

- there is not yet a full payment module with provider persistence, payment-method administration, or refunds implemented end-to-end
- Stripe is available once `stripe.enabled=true`, `stripe.api-key`, `stripe.checkout-success-url`, and `stripe.checkout-cancel-url` are configured; frontends can read provider metadata from `/payment-methods`
- Stripe webhook consumption is implemented on `POST /payments/webhooks/stripe`; verified Checkout webhook events update stored payment state and can confirm the related order
- `RealStripePaymentIntegrationTest` is the single real Stripe integration path now; it reads `RUN_REAL_STRIPE_TEST` and `STRIPE_API_KEY` through the same Spring property loading as the application, so values defined in `.env` control whether the test is skipped or executed

### Users And Authentication

Implemented:

- user registration
- user login
- user logout
- admin user creation
- current user profile read/update
- admin user listing
- admin user detail
- admin user deletion
- signed HS256 JWT issued at login, revoked at logout via `jti` stored in `auth_sessions`
- role handling for `simple_user` and `admin`
- current user addresses CRUD/default selection

Authentication rule currently implemented:

- signup accepts `email`, `phone`, or both
- login accepts `email` or `phone`
- provided email and phone values are kept unique

Main files:

- [`AuthController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AuthController.java)
- [`AuthService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AuthService.java)
- [`UserController.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/UserController.java)
- [`UserService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/UserService.java)
- [`UserAddressService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/UserAddressService.java)

Important clarification:

- "dashboard" here means the authenticated frontend experience
- the backend work done so far is user/auth plus address capabilities used by that frontend area

## Infrastructure And Configuration

### Database

The project uses PostgreSQL in integration tests and standard Spring datasource configuration at runtime.

Configuration notes:

- `src/main/resources/application.properties` now documents the server port, PostgreSQL connection, JPA logging/validation flags, and Stripe toggles (including API version/idempotency key behavior).
- the HTTP API base path is now centralized through `server.servlet.context-path=/api/${APP_API_VERSION}`, so controller mappings stay unchanged while the exposed route prefix remains versionable.
- `.env.example` lists the environment variables that can override those Spring properties along with placeholder values.
- `.env` is now automatically parsed and injected into the Spring environment via `DotenvEnvironmentPostProcessor`, using environment-style key resolution so entries like `STRIPE_API_KEY` and `SPRING_DATASOURCE_URL` bind the same way they would as real OS environment variables.
- Docker Compose datasource auto-wiring is now opt-in through `SPRING_DOCKER_COMPOSE_ENABLED`; local and production runs default to the datasource declared by `SPRING_DATASOURCE_*` instead of silently switching to the dev container.
- CORS is now explicitly configured through `app.cors.*` / `APP_CORS_*`, instead of relying on Spring defaults. This is the mechanism that should be used to allow local frontend origins and production frontend domains.

Relevant pieces:

- Flyway for schema migrations
- Hibernate/JPA for ORM
- Spring Data JPA for repositories
- Testcontainers PostgreSQL for integration tests

### Error Handling

Shared API error handling lives in:

- [`ApiExceptionHandler.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/common/ApiExceptionHandler.java)
- [`ApiError.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/common/ApiError.java)

### CI

Repository CI tooling now includes:

- format checks
- semgrep entrypoints
- a dedicated test workflow

One earlier repository issue was that workflows called `make` targets that did not exist.
That was fixed by adding a root `Makefile`.

## Verification

Main verification style:

- Spring Boot integration tests
- PostgreSQL Testcontainers
- contract-first request/response assertions

Implemented test coverage currently exists for:

- catalog listing and admin writes
- guest checkout and order admin flows
- user registration/login/profile/admin flows
- phone-only login/signup
- current user address management

Representative commands used:

```bash
./gradlew --no-daemon test --tests com.devikapps.caverne.modules.catalog.CatalogApiIntegrationTest --tests com.devikapps.caverne.modules.order.CheckoutApiIntegrationTest
```

```bash
./gradlew --no-daemon test --tests com.devikapps.caverne.modules.user.UserAuthIntegrationTest
```

## Mistakes To Avoid

- do not expose JPA entities directly from controllers
- do not change `docs/api.yaml` without regenerating `./clients`
- do not rely on Spring default JSON naming when the OpenAPI contract uses snake_case
- do not mix API contract mapping and persistence logic inside controllers
- do not treat the frontend "dashboard" as if it were already a defined backend domain model
- do not forget that repository implementations are generated/wired by Spring Data JPA, not handwritten JDBC code

## Completed Work

Completed on April 2, 2026:

- extracted contract/entity mapping into dedicated mapper classes for catalog and order flows
- added integration tests for category create/update, product create/update, and order status update
- replaced Hibernate schema recreation with Flyway migrations and JPA validation mode
- added a dedicated GitHub Actions test workflow
- implemented admin order listing plus order detail/cancel coverage
- implemented the first authenticated user slice with signup, login, logout, profile endpoints, admin user endpoints, and persisted bearer sessions (later replaced by signed JWTs — see entry below)
- aligned backend order/category code with the latest contract updates
- added `orders.delivery_cost_id`

Completed on April 3, 2026:

- updated auth so signup accepts `email`, `phone`, or both, and login accepts either identifier
- regenerated `./clients` for the updated auth contract
- relaxed the user schema so `email` can be nullable while preserving uniqueness on provided identity values
- implemented `/users/me/addresses` list/create/update/delete/default
- added `POST /users` for admin-created users
- added the migration for user identity constraints and the `user_addresses` table
- extended integration coverage for phone-only signup/login, admin user creation, and authenticated address management
- linked orders to authenticated users through `orders.user_id`
- implemented real current-user order history with owner/admin access rules on owned orders
- aligned `/orders/{id}/payments` with the same owner/admin rules for user-owned orders while preserving guest access for guest orders
- enabled admin order filtering by `user_id`
- added the migration for `orders.user_id`
- extended integration coverage for owned order history and owner access control
- aligned `docs/api.yaml`, regenerated `./clients`, and trimmed the contract down to the actually implemented controller surface
- normalized the implemented `POST collection` + `PUT item` pairs toward the same upsert semantics for categories, products, and user addresses
- updated the contract and generated clients so `AddressInput` and `ProductInput` can carry optional `id`
- extended integration coverage for POST-driven updates and PUT-driven creation on those implemented pairs
- exposed `/payment-methods` so the frontend can discover available providers, wired a service/controller pair that reports the manual and Stripe providers, and refreshed the OpenAPI clients/docs for the new contract surface (PaymentInput description + clients)
- adjusted the catalog mapper so `Product.size` is translated to/from the contract's numeric field while keeping the legacy string column
- introduced a mock Stripe provider during integration tests (CheckoutApiIntegrationTest.StripeTestConfig) so `/orders/{id}/payments` with `method_code=STRIPE` can be validated without calling the real API
- aligned the Stripe provider with the API v2 guidance by publishing the configured `stripe.api-version` header and supplying a request-scoped idempotency key derived from the order reference
- switched the Stripe flow from raw `PaymentIntent` creation to hosted Checkout Session creation so `/orders/{id}/payments` can now return a real Stripe checkout URL/session identifiers in `provider_response`
- corrected Stripe amount conversion for zero-decimal currencies such as `MGA`, and normalized provider-specific statuses back into the API contract statuses (`pending`, `confirmed`, `failed`, `refunded`)
- kept Stripe exposure curated: `provider_response` only stores/returns the frontend-relevant Checkout fields (`checkout_url`, `checkout_session_id`, `checkout_status`, `payment_status`) instead of the full Stripe object
- aligned catalog writes with the contract security rules by requiring admin authentication on category/product create, update, and delete operations
- extended catalog integration coverage to prove admin writes still work and anonymous catalog writes are rejected
- introduced a provider-based bearer authentication architecture instead of relying only on local sessions
- kept the local session provider working for the existing `/auth/login` flow while adding a first external provider path for Supabase bearer tokens
- added local user identity linkage for external providers through `app_users.auth_provider` and `app_users.external_auth_id`
- implemented Supabase token verification using configured backend properties and automatic local user linking/provisioning with default `simple_user` role
- added integration coverage for Supabase bearer authentication, local-user linking, external-user provisioning, and logout compatibility
- extracted a reusable external-provider path (`AbstractExternalAuthenticationProvider` + `ExternalIdentityProfile`) so future providers can follow the same verify -> link/provision flow without re-implementing local user resolution rules
- locked external-provider role assignment down so new externally provisioned users always start as `simple_user`; external token claims do not auto-elevate anyone to `admin`, while already-linked local admins keep their existing role
- introduced Spring Security as the central HTTP security layer with a stateless bearer filter and explicit public/authenticated/admin route policy
- centralized password encoding through a shared security bean instead of ad-hoc encoder creation
- reduced duplicated controller-level auth handling by introducing `SecurityActorResolver`, so authenticated/admin controllers now read the current actor from Spring Security instead of manually reparsing the `Authorization` header on every secured endpoint
- added an opt-in startup bootstrap for the first local admin user, guarded by explicit `bootstrap.admin.*` properties and skipped automatically when an admin already exists
- added integration coverage for initial admin bootstrap creation
- implemented real delivery-cost persistence/endpoints and made order `total_amount` include the selected delivery cost
- added integration coverage for delivery-cost CRUD and order total computation with `delivery_cost_id`
- trimmed the OpenAPI contract so it no longer advertises unused schema-only domains such as stock, reviews, technical specs, product images, and standalone currency resources
- implemented Stripe webhook verification/processing on `POST /payments/webhooks/stripe`, updating stored Stripe payment state from Checkout events and confirming the order when payment succeeds
- added integration coverage for Stripe webhook processing through `StripeWebhookIntegrationTest`

### Authenticated Surface Checklist

- Delivered: `/users/me`
  Controller/service/test: `UserController#getMe|updateMe`, `UserService`, `UserAuthIntegrationTest`
- Delivered: `/users/me/addresses`, `/users/me/addresses/{id}`, `/users/me/addresses/{id}/default`
  Controller/service/test: `UserController`, `UserAddressService`, `UserAuthIntegrationTest`
- Delivered: `/orders`
  Controller/service/test: `OrderController#listMyOrders`, `OrderService#findAllForUser`, `CheckoutApiIntegrationTest`
- Delivered: `/orders/{id}`
  Controller/service/test: `OrderController#getOrder`, `OrderService#getOrderForActor`, `CheckoutApiIntegrationTest`
- Delivered: `/orders/{id}/payments`
  Controller/service/test: `OrderController#listPayments|initiatePayment`, `OrderService#listPayments|processPayment`, `CheckoutApiIntegrationTest`, `RealStripePaymentIntegrationTest`
- Delivered: `/payment-methods`
  Controller/service/test: `PaymentMethodController`, `PaymentMethodService`, `PaymentMethodApiIntegrationTest`

### Authenticated Surface Audit Result

- Verified: the main dashboard-facing authenticated slices are implemented and covered: current user profile, current user addresses, current user order history, owned/admin order detail, order payments, and payment method discovery.
- Improved: Spring Security is now the enforcement entry point for public/authenticated/admin HTTP routes, and the main authenticated/admin controllers now resolve the current actor from the security context through `SecurityActorResolver` instead of duplicating header parsing in every method.
- Clarified: `/orders/{id}/payments` remains callable without authentication for guest orders, but once an order is linked to a user the backend now enforces the same owner/admin access rules used by order detail and cancellation.

### Theme

- Authenticated dashboard scope: the frontend authenticated area is currently built around profile management, address management, user-owned order history, order detail, payment discovery, and Stripe checkout redirection. Backend changes in these areas must preserve stable identifiers and field names.
- Stable user data expected by the frontend: `id`, `firstname`, `lastname`, `email`, `phone`, `role`, and `status` from `/users/me`.
- Stable address data expected by the frontend: `id`, `location`, `postal_code`, `country_code`, and `is_default` from the `/users/me/addresses*` endpoints.
- Stable order history data expected by the frontend: `id`, `reference`, `date`, `status`, `currency_code`, `delivery_cost_id`, `items`, and `recipient` from `/orders` and `/orders/{id}`.
- Payment/checkout expectation: the frontend should treat Stripe as a hosted checkout redirect flow. It should read `provider_response.checkout_url` from `/orders/{id}/payments` and redirect the browser there instead of expecting a raw Stripe object or inline card form contract from this backend.
- Payment provider metadata expectation: `/payment-methods` is the source of truth for available providers and their integration style. The frontend should not hardcode provider behavior when the API can declare it.
- Role-specific behavior: `simple_user` is the default dashboard user and only sees/acts on owned resources; `admin` can access the broader management surface. Backend role semantics must remain explicit and stable because the frontend will branch on them.
- Auth-provider expectation: bearer tokens may come from local sessions or external providers such as Supabase. The frontend authenticated experience should depend on the bearer token contract, not on which provider issued the token.

### Security Lifecycle

- HTTP security is now stateless. Spring Security does not use server-side HTTP sessions for request authentication; each authenticated request must carry a bearer token.
- The bearer token is resolved by [`AuthBearerFilter.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AuthBearerFilter.java), which reads the `Authorization: Bearer ...` header, authenticates the token through the configured providers, and stores the current `UserAccount` in the Spring Security context for the lifetime of that request.
- Local authentication issues a signed **HS256 JWT** through [`AuthService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AuthService.java) and [`LocalJwtService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/LocalJwtService.java). The JWT carries `iss`, `sub`, `jti`, `iat`, `exp`, `role`, `email`, `phone` claims; the signing key is `AUTH_LOCAL_JWT_SECRET` and the TTL defaults to `3600` seconds (`AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS`). The `jti` is persisted in `auth_sessions` so logout can revoke a still-unexpired token by deleting that row.
- Token verification happens locally on every request: signature (HS256 against `AUTH_LOCAL_JWT_SECRET`), issuer (`AUTH_LOCAL_JWT_ISSUER`, default `caverne`), expiration, and `jti` presence in `auth_sessions`. The provider matching is disambiguated by `iss` so a Supabase JWT is never picked up by the local provider and vice-versa.
- External provider authentication currently supports Supabase. Supabase bearer tokens are verified on each request and mapped to a local `UserAccount` through provider identity linkage; they are not copied into `auth_sessions`.
- Logout semantics are different by provider on purpose:
  - local JWT: logout deletes the `auth_sessions` row keyed by the token's `jti`, so the same JWT becomes unusable immediately even before `exp`
  - Supabase token: logout only verifies the token path and returns successfully; real revocation remains owned by the external provider
- Missing or malformed bearer headers return `401`. Valid authenticated users without enough privileges return `403`.

### Access Control Rules

- Public routes:
  - `POST /auth/register`
  - `POST /auth/login`
  - `POST /payments/webhooks/stripe`
  - `GET /categories`
  - `GET /categories/{id}`
  - `GET /products`
  - `GET /products/{id}`
  - `GET /delivery-costs`
  - `GET /delivery-costs/{id}`
  - `POST /orders`
  - `GET /orders/{id}`
  - `POST /orders/{id}/cancel`
  - `GET /orders/{id}/payments`
  - `POST /orders/{id}/payments`
  - `GET /payment-methods`
- Authenticated routes:
  - `POST /auth/logout`
  - `GET /users/me`
  - `PUT /users/me`
  - `/users/me/addresses*`
  - `GET /orders`
- Admin-only routes:
  - `GET /orders/all`
  - `PUT /orders/{id}/status`
  - `GET /users`
  - `POST /users`
  - `GET /users/{id}`
  - `DELETE /users/{id}`
  - `POST /categories`
  - `PUT /categories/{id}`
  - `DELETE /categories/{id}`
  - `POST /products`
  - `PUT /products/{id}`
  - `DELETE /products/{id}`
  - `POST /delivery-costs`
  - `PUT /delivery-costs/{id}`
  - `DELETE /delivery-costs/{id}`
- Order ownership rules:
  - guest orders can still be read, cancelled, and paid without authentication
  - once an order has a `user_id`, order detail/cancel/payment access is restricted to that owner or an admin
  - `GET /orders` only returns orders linked to the current authenticated user
  - `GET /orders/all` remains the admin backoffice listing surface
- Current enforcement split:
  - route category enforcement happens centrally in [`SecurityConfiguration.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/config/SecurityConfiguration.java)
  - resource-ownership enforcement for orders/payments happens in [`OrderService.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/order/OrderService.java), because Spring Security route matchers alone do not know which user owns a specific order id

## Improvements Applied — April 13, 2026

### 1. Remove `Order.provider_response` from contract

`provider_response` was declared on both `Order` and `Payment` schemas but only ever populated on `Payment`.
The field was removed from the `Order` schema in `docs/api.yaml` and clients were regenerated.
The frontend should always read `provider_response` from payment objects returned by `GET /orders/{id}/payments`.

### 2. Stripe webhook route always registered

`StripeWebhookController` had `@ConditionalOnProperty(stripe.enabled)`, meaning the route was not mapped at all when Stripe was disabled — Spring returned a generic 404 with no stable surface for smoke tests. The annotation was removed from the controller. An `Optional<StripeWebhookService>` is now injected: the route is always registered; when Stripe is disabled it explicitly throws `404 Not Found` with body `"Stripe webhook is disabled"`. `StripeWebhookService` still carries `@ConditionalOnProperty` so the handler bean only exists when Stripe is configured. In production Stripe must be enabled before pointing Stripe deliveries at the endpoint, otherwise 404s will trigger Stripe retries.

### 3. `OrderPayment` promoted from `@Embeddable` to `@Entity`

`OrderPayment` was an `@Embeddable` stored as an `@ElementCollection`, which prevented direct repository queries on payments (no `findByInternalReference`, no payment lookup without loading the whole order).

Changes:
- `V11__order_payments_pk.sql` — adds `NOT NULL` on `payment_id` and `order_id`, adds primary key constraint on `order_payments(payment_id)`, and adds an index on `internal_reference` for fast webhook lookup.
- `OrderPayment` is now a full `@Entity` with `@Id UUID paymentId` and a `@ManyToOne Order order`.
- `Order.payments` is now `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true, fetch = EAGER)`.
- `OrderPaymentRepository` added with `findByInternalReference`.
- `StripeWebhookService` now queries `OrderPaymentRepository.findByInternalReference` directly instead of loading the order first and scanning the payment list.
- `OrderService.processPayment` sets `.order(order)` on the `OrderPayment` builder so the FK is populated.

### 4. Logout no longer re-parses the Authorization header

`AuthController.logout` previously took `@RequestHeader("Authorization")` and re-parsed the raw header even though `AuthBearerFilter` had already extracted and validated the token.

Changes:
- `AuthBearerFilter` now stores the raw bearer token as the `credentials` in `UsernamePasswordAuthenticationToken` (was `null`).
- `SecurityActorResolver.requireBearerToken()` reads the token from `Authentication.getCredentials()`.
- `AuthController.logout()` no longer takes a header parameter — it calls `securityActorResolver.requireBearerToken()` and passes the raw token to `authService.logout(token)`.
- `AuthService.logout(String token)` and `AuthSessionResolver.logout(String token)` now accept the raw token directly (no `extractBearerToken` call inside `logout`).

### Note on `POST /delivery-costs`

`POST /delivery-costs` remains intentionally public. The frontend computes the delivery cost client-side based on recipient/distance data and creates the record at checkout time. This is the intended design.

## Full Contract Audit — April 13, 2026

### Coverage

All 41 endpoints defined in `docs/api.yaml` are implemented and reachable through the corresponding controllers.

| Area | Endpoints | Controller |
|---|---|---|
| Auth | `POST /auth/register`, `/login`, `/logout` | `AuthController` |
| Users | `GET/POST /users`, `GET/PUT /users/me`, `GET/DELETE /users/{id}` | `UserController` |
| Addresses | `GET/POST /users/me/addresses`, `PUT/DELETE /users/me/addresses/{id}`, `PUT .../default` | `UserController` |
| Categories | `GET/POST /categories`, `GET/PUT/DELETE /categories/{id}` | `CategoryController` |
| Products | `GET/POST /products`, `GET/PUT/DELETE /products/{id}`, `GET/PUT /products/{id}/stock` | `ProductController` |
| Delivery costs | `GET/POST /delivery-costs`, `GET/PUT/DELETE /delivery-costs/{id}` | `DeliveryCostController` |
| Orders | `GET/POST /orders`, `GET /orders/all`, `GET /orders/{id}`, `PUT /orders/{id}/status`, `POST /orders/{id}/cancel` | `OrderController` |
| Payments | `GET/POST /orders/{id}/payments`, `GET /payment-methods`, `POST /payments/webhooks/stripe` | `OrderController`, `PaymentMethodController`, `StripeWebhookController` |

### Known Gaps And Discrepancies

**1. `Order.provider_response` is never populated.**

The API schema places `provider_response` on both `Order` and `Payment`.
`OrderApiMapper.toOrderModel` does not set `provider_response`, so the field is always absent in order responses.
`OrderApiMapper.toPaymentModel` correctly maps it from `OrderPayment.providerResponse`.
The frontend should read `provider_response` from the payment objects under `/orders/{id}/payments`, not from the order itself.

**2. Migration list in this document was outdated.**

REALISATION.md previously listed only V1–V4. Actual migrations run V1–V13. The list above has been corrected.

### Recently Implemented Features

- **Stock movements (V12):** every change to `products.stock_quantity` (admin adjustment, order placement, order cancellation) writes a row in `stock_movements`. Admin-only listing at `GET /products/{id}/stock/movements`.
- **Product image admin CRUD + main toggle:** new admin endpoints `GET/POST /products/{productId}/images`, `DELETE /products/{productId}/images/{imageId}`, `PUT /products/{productId}/images/{imageId}/main`. Invariant enforced: at most one `is_main` per product.
- **Supabase Auth webhook:** `POST /auth/webhooks/supabase` accepts Supabase Auth user lifecycle events (INSERT/UPDATE/DELETE on `auth.users`). Authenticated by shared secret header `X-Webhook-Secret` (constant-time compare). Returns 503 if secret is not configured. Conditional on `auth.providers.supabase.enabled=true`.
- **Refund (V13):** admin endpoint `POST /payments/{orderId}/refund` triggers Stripe Refund API, records `refund_id`, `refunded_amount`, `refunded_at`, `refund_reason` on `order_payments`. Order moves to `CANCELLED` when fully refunded. Returns 503 when `stripe.enabled=false`.
- **Health endpoint:** Spring Boot Actuator added. `GET /actuator/health` is unauthenticated; other actuator endpoints are not exposed.

### Migration Alignment

Every JPA entity maps to columns that exist after V10:

- `categories`: `id` (uuid), `label`, `slug`, `map`, `icon` (V9), `parent_id` — matches `Category` schema
- `products`: `id`, `category_id`, `label`, `reference`, `limit_date`, `description`, `size`, `is_active`, `stock_quantity` (V10) — matches `Product` / `ProductInput` schemas
- `product_images`: `id`, `product_id`, `url`, `is_main` (V9) — matches `ProductImage` / `ProductImageInput`
- `prices`: `id`, `product_id`, `currency_code`, `value`, `valid_from`, `unit` — matches `Price` / `PriceInput`
- `orders`: `id`, `reference`, `date`, `status`, `currency_code`, `delivery_cost_id`, `user_id` (V5), `customer_name`, `customer_email`, `customer_phone`, `shipping_location`, `postal_code`, `country_code`, `total_amount` — matches `Order` and `RecipientInput`
- `order_items`: `id`, `order_id`, `product_id`, `quantity`, `unit_price`, `total_price`, `product_snapshot` (V10) — matches `OrderItem`
- `order_payments`: `payment_id`, `order_id`, `method_code`, `currency_code`, `amount`, `date`, `status`, `internal_reference`, `provider_response` — matches `Payment`
- `delivery_costs`: `id`, `amount`, `provider`, `response_provider` (V7) — matches `DeliverCost`
- `app_users`: `id`, `firstname`, `lastname`, `email`, `phone`, `password_hash`, `role`, `status`, `auth_provider` (V6), `external_auth_id` (V6) — matches `User`
- `user_addresses`: `id`, `user_id`, `location`, `postal_code`, `country_code`, `is_default` (V4) — matches `Address`
- `auth_sessions`: `id`, `user_id`, `token`, `expires_at` — internal, not in contract

No entity references a column that does not exist in the current migration state.

## Improvements Applied — April 14, 2026

### 1. Supabase registration write-through

`POST /auth/register` now creates the user in Supabase at the same time as the local record when Supabase is enabled.

How it works:

- `SupabaseAdminClient` (new, `@ConditionalOnProperty(auth.providers.supabase.enabled)`) calls `POST {url}/auth/v1/admin/users` using the service role key.
- `AuthService.register()` saves the local user, then calls `SupabaseAdminClient.createUser(...)`, then stores the returned Supabase UUID in `app_users.external_auth_id` and flips `auth_provider` to `SUPABASE`.
- If the Supabase call fails, the whole transaction rolls back — no partial state.
- When Supabase fires the resulting INSERT webhook, `SupabaseAuthWebhookService.upsertFromRecord()` finds the already-linked user by `external_auth_id` and is a no-op.

New env vars required when Supabase is enabled:

- `AUTH_PROVIDERS_SUPABASE_URL` — project URL (`https://xxxx.supabase.co`)
- `AUTH_PROVIDERS_SUPABASE_SERVICE_ROLE_KEY` — service role key from Supabase dashboard → Project Settings → API

New files:

- [`SupabaseAdminClient.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/SupabaseAdminClient.java)

### 2. Gatling performance test fixes

Two bugs prevented `create order` from ever succeeding in the Gatling simulation:

- **Empty catalog:** `CATALOG_NICHES_IMPORT_ENABLED` defaulted to `false`, so the product list was always empty. `$.data[*].id` returned nothing, session attribute `productId` was never saved, and the checkout scenario failed before sending any HTTP request. Fixed by adding `CATALOG_NICHES_IMPORT_ENABLED: "true"` to `CI-Gatling.yml`.
- **Currency mismatch:** `niches.md` catalog uses `MGA` prices; the Gatling simulation was sending `currency_code: "XAF"`. `resolveCurrentPrice` threw 422 for every order. Fixed by aligning the simulation currency to `MGA`.

### 3. `make gatling` local target

A `gatling` target was added to the `Makefile` so the full load-test sequence can be run locally with a single command instead of manually managing the server lifecycle:

```bash
make gatling
```

Steps executed:
1. `./gradlew bootJar gatlingClasses`
2. `docker compose up -d` (starts Postgres)
3. Waits for Postgres readiness
4. Starts the Spring Boot jar in background with `CATALOG_NICHES_IMPORT_ENABLED=true` and all optional integrations disabled
5. Waits for `GET /actuator/health` to return 200
6. Runs `./gradlew gatlingRun`
7. Kills the server whether tests pass or fail

`compose.yaml` was updated to expose Postgres on a fixed host port `5432:5432` (previously it used a random port only accessible to Spring Boot dev mode).

## Improvements Applied — April 16, 2026

### 1. Stripe webhook contract documented in `docs/api.yaml`

`POST /payments/webhooks/stripe` was declared with an opaque `additionalProperties: true` body and only the `204`/`422` responses. That hid the real contract and did not advertise the required `Stripe-Signature` header, so a caller reading the spec could not build a correct request or anticipate error codes.

Changes to `docs/api.yaml`:

- Declared the `Stripe-Signature` header as a required `in: header` parameter (verified against `stripe.webhook-secret` by `com.stripe.net.Webhook.constructEvent`).
- Replaced the anonymous body schema with `StripeEvent` and nested `StripeEventObject` (in `components.schemas`), documenting the subset of Stripe Event fields read by `StripeWebhookService` (`id`, `type`, `data.object.{id,status,payment_status,payment_intent,...}`) while keeping `additionalProperties: true` to accept any other fields Stripe sends.
- Listed the full set of real response codes the server can produce: `204` (processed), `404` (service disabled or no matching `OrderPayment`), `422` (bad signature or bad payload), `500` (DB/processing failure), `503` (webhook secret not configured).

`docs/openapi.json` and the generated `clients/` artifacts were regenerated from `docs/api.yaml`.

Follow-up note: `DOCUMENTATION.md` §8 still reads "400 if invalid" for signature errors; the controller actually returns `422`. That prose doc should be corrected on the next pass.

### 2. Stripe webhook idempotency is already live — removed from future-work list

Previous revisions listed "add Stripe webhook idempotency" as pending work. It is in fact implemented: `StripeWebhookEventRepository` persists a row keyed by Stripe `event.id` for each processed event, and `StripeWebhookService.handleWebhook` short-circuits on `existsById` and on `DataIntegrityViolationException` during concurrent inserts. The stale bullet was dropped from "Suggested Next Improvements" in this file and from `STEP_BY_STEP.md` §14.

## Improvements Applied — April 20, 2026

### 1. Global unique `label` on categories and products

Business rule: two categories cannot share the same label, and two products cannot share the same label (case-insensitive in both cases).

Changes:

- `V16__unique_category_and_product_labels.sql` — creates functional unique indexes `ux_categories_label_lower` and `ux_products_label_lower` on `lower(label)`.
- `CategoryRepository.findByLabelIgnoreCase(String)` and `ProductRepository.findByLabelIgnoreCase(String)` added.
- `CategoryService.createCategory` / `updateCategory` and `ProductService.createProduct` / `updateProduct` now pre-validate label uniqueness before save and throw `ResponseStatusException(CONFLICT)` (HTTP **409**) when another record already owns the label.
- `NichesCatalogImporter.findOrCreateProduct` switched from per-category lookup to global `findByLabelIgnoreCase`, so re-running the importer against a seed file that lists the same label under two categories (e.g. `tableaux peints locaux` in both `MAISON ET DÉCO` and `ART ET CULTURE`) reuses the first product instead of failing on the DB constraint.
- `docs/api.yaml` — new `Conflict` response component; `409` added on `POST /categories`, `PUT /categories/{id}`, `POST /products`, `PUT /products/{id}`; schema descriptions updated to state that `label` is case-insensitive unique.

### 2. Row-level locking on product stock

Business rule: two concurrent orders on the same product must not both pass the stock-sufficiency check and leave the balance negative.

Changes:

- `ProductRepository.findByIdForUpdate(UUID)` — JPQL method annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)`; emits `SELECT … FOR UPDATE` on PostgreSQL.
- `OrderService.createOrder` and `OrderService.restoreStockForOrder` now read the product through the locked query before decrementing / restoring `stock_quantity`.
- `ProductService.updateStock` (admin manual adjustment) also uses the locked read.
- `docs/api.yaml` — `POST /orders` description now explains that stock is locked in-transaction and that the second concurrent caller receives `422` when the post-lock stock is insufficient.

Known follow-ups (not fixed in this pass):

- `StockMovement.delta` and `StockMovement.balance_after` are `int` but `Product.stockQuantity` is `BigDecimal(19,4)`; fractional quantities get truncated when recorded. Keep this in mind before exposing fractional stock quantities via the API.
- `NichesCatalogImporter` still silently reuses a product across categories on label collision. If the business decides the same label should live under distinct categories, switch to category-suffixed labels.

## Improvements Applied — May 13, 2026

### Refresh tokens: short-lived stateless access + long-lived rotated refresh

Business rule: stay-signed-in UX without trading off security. The access JWT must be short enough that a stolen token is barely usable; the refresh path must be revocable and protected against XSS, CSRF, and replay.

Design choices:

- **Access JWT** stays HS256 but TTL drops from 60 min to **15 min** (`AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS=900`). It is now **fully stateless** — `LocalSessionAuthenticationProvider` verifies signature + `iss` + `exp` and loads the user by `sub`, with **no DB lookup per request**. The `jti` claim stays in the payload for audit but is no longer the revocation handle.
- **Refresh token** is an opaque 32-byte random value delivered as an **`HttpOnly; Secure; SameSite=None` cookie** scoped to `Path=/api/v1/auth`, valid 30 days (`AUTH_LOCAL_REFRESH_EXPIRES_IN_SECONDS=2592000`). It is stored server-side as a **SHA-256 hash** in `refresh_tokens` — the raw value never lives in the DB.
- **Rotation** on every `/auth/refresh`: the previous row is marked `revoked_at` and a new row is inserted with `family_id` shared and `parent_id` pointing to the predecessor. Front-end never sees this — it just gets a fresh cookie via `Set-Cookie`.
- **Reuse detection**: if a refresh token whose `revoked_at` is already set is presented (sign of replay after theft), `RefreshTokenRepository.revokeFamily(familyId)` runs in a `REQUIRES_NEW` transaction so the family revocation commits even though the request itself responds `401`.
- **Anti-CSRF — double-submit cookie**: login/refresh also set a `csrf_token` cookie that is **not** HttpOnly (so the SPA can read it via `document.cookie`). The SPA echoes its value in `X-CSRF-Token` on every `/auth/refresh` and `/auth/logout` call. `CsrfDoubleSubmitFilter` rejects with `403` if the header is absent or does not equal the cookie. This is necessary because `SameSite=None` is required for cross-origin (Vercel front + Render API) — and `SameSite=None` removes the browser's built-in CSRF protection.

Implementation:

- New table `refresh_tokens(id, user_id, token_hash, family_id, parent_id, expires_at, revoked_at, created_at)` via [`V18__refresh_tokens.sql`](/home/icecream/IdeaProjects/caverne/src/main/resources/db/migration/V18__refresh_tokens.sql). The migration **drops** `auth_sessions` — old opaque tokens stop working at deployment, forcing one re-login.
- New JPA entity [`RefreshToken`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/RefreshToken.java) + [`RefreshTokenRepository`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/RefreshTokenRepository.java) with `revokeFamily(@Transactional REQUIRES_NEW)`.
- New service [`RefreshTokenService`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/RefreshTokenService.java) (`issueForLogin`, `rotate`, `revoke`, `revokeAllForUser`).
- New config [`RefreshTokenProperties`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/RefreshTokenProperties.java) — cookie + CSRF tunables exposed via `AUTH_LOCAL_REFRESH_*` env vars.
- New filter [`CsrfDoubleSubmitFilter`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/CsrfDoubleSubmitFilter.java) wired in `SecurityConfiguration`.
- `AuthController` rewritten: `/auth/login` and `/auth/refresh` set both cookies via `ResponseCookie`; `/auth/logout` clears them. The previously-static `LoginResponse` body remains the JSON contract (only the access JWT travels in the body).
- `AuthService` returns an `AuthIssued(body, rawRefreshToken, refreshExpiresAt, csrfToken)` record so the controller can wire cookies without business-logic knowledge.
- `LocalSessionAuthenticationProvider` no longer talks to the DB on the auth path beyond loading the user; `AuthSession` / `AuthSessionRepository` deleted.
- New `AUTH_LOCAL_REFRESH_*` env vars in `.env.example`, `.env.prod`, `.env`. `application.properties` adds `X-CSRF-Token` to the CORS `allowed-headers`.
- OpenAPI: `LoginResponse` description expanded; `POST /auth/refresh` added; the `X-CSRF-Token` header documented on `/auth/refresh` and `/auth/logout`.
- Integration tests: new [`RefreshTokenIntegrationTest`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/RefreshTokenIntegrationTest.java) covering the happy path, CSRF rejection, mismatched CSRF, reuse detection, and post-login cookie inspection. `UserAuthIntegrationTest.shouldLogoutInvalidateCurrentToken` rewritten as `shouldLogoutClearCookies` (the access JWT can no longer be revoked mid-life — that is the intentional tradeoff for statelessness).

Migration notes:

- `AUTH_LOCAL_REFRESH_COOKIE_SAME_SITE` defaults to `None` for cross-domain production deployments. Local dev (HTTP) must set `AUTH_LOCAL_REFRESH_COOKIE_SECURE=false` and may want `SAME_SITE=Lax` (the test profile already does both).
- The Supabase logout path stays as before — `auth/logout` accepts the call, CSRF is skipped when no refresh cookie is present, and revocation is delegated to Supabase.

## Improvements Applied — May 12, 2026

### Local auth: opaque session ID replaced with a signed JWT

Business rule: the token returned by `POST /auth/login` must be tamper-proof, self-describing, and verifiable without a per-request lookup when possible — while keeping instant revocation on logout.

Changes:

- `AuthService.login` now signs a real HS256 JWT instead of returning `UUID.randomUUID() + "." + UUID.randomUUID()`. Claims: `iss`, `sub` (user UUID), `jti`, `iat`, `exp`, `role`, `email`, `phone`. The `jti` is the only thing persisted in `auth_sessions` so logout can revoke a still-unexpired token by deleting that row.
- `LocalJwtService` (new) — sign/verify HS256, base64url encoding, constant-time signature comparison; fails fast at startup if `AUTH_LOCAL_JWT_SECRET` is missing.
- `LocalJwtProperties` (new) — `auth.local.jwt.secret`, `.issuer` (default `caverne`), `.expires-in-seconds` (default `3600`).
- `LocalSessionAuthenticationProvider.supportsToken` peeks at the `iss` claim so it only claims JWTs minted with the local issuer; `authenticate` verifies signature + `iss` + `exp`, then confirms the `jti` still exists in `auth_sessions`.
- `SupabaseAuthenticationProvider.supportsToken` now also filters by `iss` when configured, so a local JWT is never picked up by Supabase verification. `@Order(0)` / `@Order(10)` makes the resolution order explicit.
- `DOCUMENTATION.md`, `REALISATION.md`, `STEP_BY_STEP.md`, `DOCUMENTATION_EN.md`, `DOCUMENTATION_FR.md`, `customer_workflow.md` updated. `docs/api.yaml` `LoginResponse.access_token` description clarified.
- `.env`, `.env.example`, `.env.prod` updated with `AUTH_LOCAL_JWT_SECRET` (+ optional `AUTH_LOCAL_JWT_ISSUER` / `AUTH_LOCAL_JWT_EXPIRES_IN_SECONDS`).

Migration notes:

- Existing rows in `auth_sessions` from the previous opaque-token scheme become useless after deployment because the `token` column now holds a `jti`, not the full bearer string. Operationally: drain old sessions (`DELETE FROM auth_sessions`) or accept that logged-in clients must re-login once.
- `AUTH_LOCAL_JWT_SECRET` is required: the app refuses to start if missing. Generate with `openssl rand -base64 48` and store in the platform secret manager — never commit it.

## Next Priorities

Open items to consider next:
- Expose refund fields (`refund_id`, `refunded_amount`, `refunded_at`) in the OpenAPI `Payment` schema and regenerate clients.
- Add OpenAPI definitions for `/products/{id}/stock/movements`, `/products/{productId}/images`, `/auth/webhooks/supabase`, `/payments/{orderId}/refund`, `/actuator/health`.
- JWKS-based Supabase verification when supabase project upgrades signing model.
- Add integration test coverage for Supabase write-through registration (`SupabaseAdminClient` path).
- Align `StockMovement` quantity types with `BigDecimal` to stop truncating fractional stock changes.

## Suggested Next Improvements

- Add a real health/readiness surface for deployment environments, for example a small public health endpoint or Spring Boot actuator health exposure.
- Upgrade Supabase verification from the current shared-secret-compatible flow to JWKS/asymmetric verification when the production Supabase project uses the modern signing model.
- Add an admin password-change flow so the bootstrap admin process does not depend only on operational manual rotation.
- Improve observability with structured logs, request correlation identifiers, and clearer audit logs for admin actions and payment lifecycle events.
- Tighten payment lifecycle rules further, especially around repeated payment attempts, expired sessions, and future refund handling if that becomes part of the contract.
- Add deployment automation such as `render.yaml` if deployment on Render should become repeatable with less manual setup.

## Authentication Provider Rules

- Provider onboarding rule: every external authentication provider must verify its bearer token first, extract a stable provider-specific subject identifier, and convert the verified identity into the shared `ExternalIdentityProfile` shape before any local user is resolved or created.
- Local linkage rule: local user linkage is always based on `(auth_provider, external_auth_id)` first, then by matching unique email/phone when no provider link exists yet. If email and phone point to different local users, authentication is rejected.
- Role rule: external providers do not grant backend roles from token claims. A newly provisioned external user is always created as `simple_user`. Admin role must come from an existing local/admin assignment managed by the backend.
- Preservation rule: when an external identity links to an already existing local user, that local user's current role is preserved. This allows a pre-created admin account to authenticate through an external provider later without granting admin from the provider itself.
- Verification rule for future providers: provider-specific token verification stays inside the provider implementation; common local-account linking/provisioning stays in `UserIdentityService`.

## Production Bootstrap Admin

- The first admin bootstrap is opt-in. It only runs when `bootstrap.admin.enabled=true`.
- Required configuration when enabled:
  - `bootstrap.admin.password`
  - at least one of `bootstrap.admin.email` or `bootstrap.admin.phone`
  - non-blank `bootstrap.admin.firstname` and `bootstrap.admin.lastname`
- Current defaults and environment bindings are exposed through:
  - [`AdminBootstrapProperties.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AdminBootstrapProperties.java)
  - [`AdminBootstrapRunner.java`](/home/icecream/IdeaProjects/caverne/src/main/java/com/devikapps/caverne/modules/user/AdminBootstrapRunner.java)
  - [`src/main/resources/application.properties`](/home/icecream/IdeaProjects/caverne/src/main/resources/application.properties)
  - [`.env.example`](/home/icecream/IdeaProjects/caverne/.env.example)
- Bootstrap behavior:
  - on application startup, the runner checks whether bootstrap is enabled
  - if enabled and no `admin` user exists yet, it creates one local user with role `admin` and status `active`
  - if any admin already exists, bootstrap does nothing and does not create a second admin
- Operational expectation:
  - the configured bootstrap password is only for first access
  - after the initial production login, that password should be changed immediately through the operational/admin flow once such a flow exists
  - the bootstrap flag and default password should not remain enabled permanently in production

## Testing Guardrails

- Guardrail rule: every time a new contract path becomes real in backend code, at least one Spring Boot integration test must cover the real HTTP request/response path before the work is considered complete.
- Coverage must be recorded in this document by naming the controller/service area and the integration test class that proves it.
- Preferred scope:
  - contract shape assertions with MockMvc
  - PostgreSQL-backed persistence behavior through Testcontainers
  - security/role behavior when the endpoint is authenticated or admin-only
  - provider-specific behavior when an external integration is involved
- Current integration-test map:
  - catalog contract, admin writes, stock endpoints, and public-endpoint invalid-bearer fallback: [`CatalogApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/catalog/CatalogApiIntegrationTest.java)
  - checkout, order history, order ownership, payment access rules, and order total calculation: [`CheckoutApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/order/CheckoutApiIntegrationTest.java)
  - delivery-cost public create/read and admin update flows: [`DeliveryCostApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/order/DeliveryCostApiIntegrationTest.java)
  - local auth, admin user management, and current-user addresses: [`UserAuthIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/UserAuthIntegrationTest.java)
  - external auth provider behavior and role-linking rules: [`SupabaseAuthIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/SupabaseAuthIntegrationTest.java)
  - initial admin bootstrap behavior: [`AdminBootstrapIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/AdminBootstrapIntegrationTest.java)
  - payment method discovery: [`PaymentMethodApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/payment/PaymentMethodApiIntegrationTest.java)
  - real Stripe checkout session path when explicitly enabled: [`RealStripePaymentIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/payment/RealStripePaymentIntegrationTest.java)
  - Stripe webhook verification and payment/order state updates: [`StripeWebhookIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/payment/StripeWebhookIntegrationTest.java)
  - Spring Boot Actuator health endpoint unauthenticated access: [`HealthEndpointIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/HealthEndpointIntegrationTest.java)
  - admin product image CRUD and main-toggle invariant: [`ProductImageApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/catalog/ProductImageApiIntegrationTest.java)
  - stock movement audit log admin listing: [`StockMovementApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/catalog/StockMovementApiIntegrationTest.java)
  - admin Stripe refund endpoint: [`RefundApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/payment/RefundApiIntegrationTest.java)
  - Supabase Auth webhook shared-secret ingress: [`SupabaseAuthWebhookIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/SupabaseAuthWebhookIntegrationTest.java)
  - Stock movement journal across admin adjustment and order lifecycle: [`StockMovementApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/catalog/StockMovementApiIntegrationTest.java)
  - Product image CRUD and is_main invariant: [`ProductImageApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/catalog/ProductImageApiIntegrationTest.java)
  - Supabase auth webhook lifecycle and shared-secret rejection: [`SupabaseAuthWebhookIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/user/SupabaseAuthWebhookIntegrationTest.java)
  - Stripe refund admin endpoint with mocked refund client: [`RefundApiIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/modules/payment/RefundApiIntegrationTest.java)
  - Actuator health unauthenticated reachability: [`HealthEndpointIntegrationTest.java`](/home/icecream/IdeaProjects/caverne/src/test/java/com/devikapps/caverne/HealthEndpointIntegrationTest.java)
- Review rule: if a future change only updates documentation or generated clients without matching integration coverage for new runtime behavior, the work is incomplete.

## Latest Contract Alignment

- `POST /delivery-costs` is now public. The intended use is checkout-time delivery-cost creation from frontend-computed recipient/distance data, while `PUT` and `DELETE` on delivery costs remain admin-only.
- Product stock now has explicit admin endpoints:
  - `GET /products/{id}/stock`
  - `PUT /products/{id}/stock`
- Public endpoints now tolerate invalid bearer headers by falling back to anonymous access. Protected endpoints still return `401` or `403` through Spring Security when authentication is actually required.

## Rules To Remember

- update the OpenAPI contract first when business rules change
- regenerate `./clients` immediately after spec changes
- keep controllers bound to generated request/response models only
- keep persistence concerns in services/repositories
- keep Flyway as the source of truth for schema evolution
