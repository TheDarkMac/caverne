Now I have everything. Here is the full client purchase workflow:
                                                 
---                                                                                                                                   
Full Purchase Workflow

Phase 1 — Browse

Guest or authenticated user

GET /categories                     → product tree navigation                                                                         
GET /products?page=1&per_page=20    → catalog listing (filter by category, search, currency)                                          
GET /products/{id}                  → product detail (label, prices, stock_quantity, images)                                          
GET /delivery-costs                 → list available delivery options and their amounts                                               
GET /payment-methods                → list available providers (MANUAL, STRIPE if enabled)

No auth required. All these are public routes.
                                                                                                                                        
---                                                                                                                                   
Phase 2 — Optional: Authenticate

If the client wants their order history tracked:

POST /auth/register   → creates local user + Supabase user (if Supabase enabled)
POST /auth/login      → returns { access_token, token_type, expires_in }

From this point, every authenticated call includes:                                                                                   
Authorization: Bearer <access_token>
                                                                                                                                        
---                                                                                                                                   
Phase 3 — Create Order

POST /orders

Payload:
{
"currency_code": "MGA",
"delivery_cost_id": "<uuid>",        ← optional
"items": [                                                                                                                          
{ "product_id": "<uuid>", "quantity": 1 }                                                                                         
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

What happens inside OrderService.createOrder():

1. Input validated (currency, items, recipient all required and complete)
2. For each item:
   - Product fetched from DB (productService.findById)                                                                                 
   - Stock check: ensureSufficientStock → throws 422 if requested > stock                                                              
   - Price resolved: finds the most recent price for the given currency_code → throws 422 if none exists                               
   - Stock decremented: product.stock_quantity -= quantity                                                                             
   - Stock movement recorded: reason = ORDER_PLACED, delta = -quantity                                                                 
   - Product snapshot (label + price at time of order) serialized into order_items.product_snapshot
3. total_amount = sum of all item totals + delivery cost amount (if delivery_cost_id provided)
4. Order saved with status = PENDING

Response: 201 with full order object including id, reference (e.g. ORD-A1B2C3D4), total_amount

Access rules after creation:
- If request had no Bearer token → guest order (no user_id), publicly accessible
- If request had Bearer token → authenticated order (user_id set), owner + admin only

  ---                                                                                                                                   
Phase 4 — Initiate Payment

POST /orders/{id}/payments

Payload:        
{
"method_code": "STRIPE",
"currency_code": "MGA",
"amount": 18000        
}

The amount must equal order.total_amount exactly — otherwise 422.

What happens inside OrderService.processPayment():

1. Order access checked (owner or admin for authenticated orders, public for guest)
2. Payment provider resolved by method_code
3. If STRIPE:                                                                                                                         
   - StripePaymentProvider calls Stripe API: Session.create(...) with payment_method_types, line_items, success_url, cancel_url        
   - Returns { checkout_url, checkout_session_id, checkout_status, payment_status }
4. OrderPayment record saved with status = pending, internal_reference = Stripe session ID
5. Response includes provider_response.checkout_url

Response: 201 with payment object
                                                                                                                                        
---             
Phase 5 — Stripe Checkout (external)

Frontend redirects the browser to provider_response.checkout_url.

The client enters card details on Stripe's hosted page.

After payment:
- Stripe redirects to STRIPE_CHECKOUT_SUCCESS_URL (success) or STRIPE_CHECKOUT_CANCEL_URL (cancel)

  ---             
Phase 6 — Stripe Webhook (async, server-side)

Stripe calls your backend:

POST /payments/webhooks/stripe
Stripe-Signature: t=...,v1=...

Inside StripeWebhookService:

1. Stripe signature verified against STRIPE_WEBHOOK_SECRET — 401 if invalid
2. Session ID extracted from event payload
3. OrderPaymentRepository.findByInternalReference(sessionId) — finds the payment record
4. Payment status updated:

┌──────────────────────────────────────────┬────────────────┬────────────────────────────┐
│               Stripe event               │ Payment status │        Order status        │                                            
├──────────────────────────────────────────┼────────────────┼────────────────────────────┤                                            
│ checkout.session.completed               │ confirmed      │ CONFIRMED (if was PENDING) │
├──────────────────────────────────────────┼────────────────┼────────────────────────────┤                                            
│ checkout.session.async_payment_succeeded │ confirmed      │ CONFIRMED                  │
├──────────────────────────────────────────┼────────────────┼────────────────────────────┤                                            
│ checkout.session.async_payment_failed    │ failed         │ unchanged                  │
├──────────────────────────────────────────┼────────────────┼────────────────────────────┤                                            
│ checkout.session.expired                 │ failed         │ unchanged                  │
└──────────────────────────────────────────┴────────────────┴────────────────────────────┘

5. Returns 204 to Stripe

  ---
Phase 7 — Optional: Cancel

POST /orders/{id}/cancel

Inside OrderService.cancelOrder():

1. Access checked (owner or admin)
2. Stock restored: for each item, product.stock_quantity += quantity
3. Stock movement recorded: reason = ORDER_CANCELLED, delta = +quantity
4. Order status → CANCELLED

  ---             
Full State Machine

                    ┌─────────────────────────┐
                    │         PENDING          │ ← created by POST /orders                                                              
                    └────────────┬────────────┘                                                                                         
                                 │
                ┌────────────────┼────────────────┐                                                                                     
                │                │                │                                                                                     
      webhook completed    webhook failed    POST /cancel
                │                │                │                                                                                     
                ▼                ▼                ▼
           CONFIRMED           (PENDING)      CANCELLED                                                                                 
                            payment=failed   stock restored                                                                             
                                                                                                                                        
---                                                                                                                                   
Key Invariants

- Stock is decremented at order creation, not at payment — so a PENDING order already holds the stock
- Stock is restored only on cancel, not on payment failure — a failed payment leaves the order PENDING so the client can retry payment
- Product snapshot is frozen at order time — price changes after order creation don't affect existing orders
- Guest orders are fully functional (browse → order → pay → webhook) without any account     