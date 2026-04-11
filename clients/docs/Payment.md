

# Payment


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** |  |  [optional] |
|**orderId** | **UUID** |  |  [optional] |
|**methodCode** | **String** |  |  [optional] |
|**currencyCode** | **String** |  |  [optional] |
|**amount** | **Double** |  |  [optional] |
|**date** | **OffsetDateTime** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**internalReference** | **String** |  |  [optional] |
|**providerResponse** | **Object** | Sous-ensemble contrôlé de la réponse du provider. L&#39;API ne renvoie pas l&#39;objet Stripe complet. Pour Stripe Checkout, contient notamment &#x60;checkout_url&#x60;, &#x60;checkout_session_id&#x60;, &#x60;checkout_status&#x60; et &#x60;payment_status&#x60;.  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| FAILED | &quot;failed&quot; |
| REFUNDED | &quot;refunded&quot; |



