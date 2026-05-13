

# StripeEvent

Event Stripe (https://stripe.com/docs/api/events/object). Seuls les champs exploités par le serveur sont listés ; les autres propriétés transmises par Stripe sont conservées. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **String** | Identifiant d&#39;event Stripe (utilisé pour l&#39;idempotence). |  |
|**type** | **String** | Type d&#39;event. Traités comme &#x60;confirmed&#x60; : &#x60;checkout.session.completed&#x60;, &#x60;checkout.session.async_payment_succeeded&#x60;, &#x60;payment_intent.succeeded&#x60;. Traités comme &#x60;failed&#x60; : &#x60;checkout.session.async_payment_failed&#x60;, &#x60;checkout.session.expired&#x60;, &#x60;payment_intent.payment_failed&#x60;. Les autres restent en &#x60;pending&#x60;.  |  |
|**apiVersion** | **String** |  |  [optional] |
|**created** | **Long** | Timestamp Unix de création de l&#39;event. |  [optional] |
|**livemode** | **Boolean** |  |  [optional] |
|**data** | [**StripeEventData**](StripeEventData.md) |  |  |
|**request** | **StripeEventRequest** |  |  [optional] |



