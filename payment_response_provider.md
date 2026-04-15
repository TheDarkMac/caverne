Here is exactly where provider_response lives and how it moves through the system:
                                                                                                                                        
---                                                                                                                                   
provider_response — Full Data Path

1. Built by the Stripe provider (StripePaymentProvider.java)

// StripePaymentProvider.buildProviderData()                                                                                          
Map<String, Object> providerData = new HashMap<>();                                                                                   
providerData.put("checkout_url",        session.getUrl());                                                                            
providerData.put("checkout_session_id", session.getId());
providerData.put("checkout_status",     session.getStatus());                                                                         
providerData.put("payment_status",      session.getPaymentStatus());                                                                  
providerData.put("payment_intent_id",   session.getPaymentIntent()); // if present

2. Serialized to JSON and stored in DB (OrderService.processPayment())

OrderPayment.builder()                                                                                                              
...                                                                                                                               
.internalReference(response.transactionId())     // Stripe session ID → used later for webhook lookup
.providerResponse(writeProviderResponse(response.providerData()))  // JSON string in order_payments.provider_response             
.build()

Column: order_payments.provider_response TEXT

3. Deserialized and returned in the API response (OrderApiMapper.toPaymentModel())

new Payment()                                                                                                                         
...         
.providerResponse(readProviderResponse(payment.getProviderResponse()))  // Map<String,Object>

4. What POST /orders/{id}/payments actually returns

{               
"id": "uuid",
"order_id": "uuid",
"method_code": "STRIPE",                                                                                                            
"currency_code": "MGA",
"amount": 18000,                                                                                                                    
"status": "pending",
"internal_reference": "cs_test_abc123",
"provider_response": {                                                                                                              
"checkout_url": "https://checkout.stripe.com/pay/cs_test_abc123",
"checkout_session_id": "cs_test_abc123",                                                                                          
"checkout_status": "open",                                                                                                        
"payment_status": "unpaid"
}                                                                                                                                   
}

The frontend reads provider_response.checkout_url from this response and redirects the browser there.
  
---                                                                                                                                   
5. Updated by the Stripe webhook (StripeWebhookService.java)

When Stripe fires the event, the backend finds the payment by internal_reference (= session ID), updates provider_response with the
new checkout_status/payment_status, and flips order.status to CONFIRMED.
  
---                                                                                                                                   
One important note

provider_response is only on Payment, never on Order. The GET /orders/{id} response does not contain it. The frontend must call GET
/orders/{id}/payments to get the checkout URL — not the order itself.