

# StripeEventObject

Ressource Stripe imbriquée dans `data.object`. Selon `type` de l'event, il s'agit d'une `checkout.session` (id préfixé `cs_`) ou d'un `payment_intent` (id préfixé `pi_`). Le serveur utilise `id` pour retrouver l'`OrderPayment` (via `stripe_payment_intent_id` pour les events `payment_intent.*`, sinon via `internal_reference` pour les events `checkout.session.*`). 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **String** |  |  |
|**_object** | [**ObjectEnum**](#ObjectEnum) |  |  [optional] |
|**status** | **String** | Statut Stripe. Pour &#x60;checkout.session&#x60; : &#x60;open&#x60;, &#x60;complete&#x60;, &#x60;expired&#x60;. Pour &#x60;payment_intent&#x60; : &#x60;succeeded&#x60;, &#x60;processing&#x60;, &#x60;requires_payment_method&#x60;, etc.  |  [optional] |
|**paymentStatus** | **String** | Présent sur &#x60;checkout.session&#x60; (ex. &#x60;paid&#x60;, &#x60;unpaid&#x60;, &#x60;no_payment_required&#x60;). |  [optional] |
|**paymentIntent** | **String** | Présent sur &#x60;checkout.session&#x60; (id du PaymentIntent associé). |  [optional] |
|**amountTotal** | **Integer** | Montant total en plus petite unité (centimes pour EUR). |  [optional] |
|**amount** | **Integer** | Montant sur &#x60;payment_intent&#x60;, en plus petite unité. |  [optional] |
|**currency** | **String** |  |  [optional] |
|**customer** | **String** |  |  [optional] |
|**mode** | [**ModeEnum**](#ModeEnum) |  |  [optional] |



## Enum: ObjectEnum

| Name | Value |
|---- | -----|
| CHECKOUT_SESSION | &quot;checkout.session&quot; |
| PAYMENT_INTENT | &quot;payment_intent&quot; |



## Enum: ModeEnum

| Name | Value |
|---- | -----|
| PAYMENT | &quot;payment&quot; |
| SETUP | &quot;setup&quot; |
| SUBSCRIPTION | &quot;subscription&quot; |



