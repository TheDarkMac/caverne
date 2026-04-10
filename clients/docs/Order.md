

# Order


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Integer** |  |  [optional] |
|**userId** | **Integer** |  |  [optional] |
|**addressId** | **Integer** |  |  [optional] |
|**currencyCode** | **String** |  |  [optional] |
|**reference** | **String** |  |  [optional] |
|**date** | **OffsetDateTime** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**totalAmount** | **Double** |  |  [optional] |
|**deliveryCostId** | **Integer** |  |  [optional] |
|**items** | [**List&lt;OrderItem&gt;**](OrderItem.md) |  |  [optional] |
|**recipient** | [**RecipientInput**](RecipientInput.md) |  |  [optional] |
|**providerResponse** | **Object** | Sous-ensemble contrôlé de la réponse du provider. L&#39;API ne renvoie pas l&#39;objet Stripe complet. Pour Stripe Checkout, contient notamment &#x60;checkout_url&#x60;, &#x60;checkout_session_id&#x60;, &#x60;checkout_status&#x60; et &#x60;payment_status&#x60;.  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| DELIVERED | &quot;delivered&quot; |
| CANCELLED | &quot;cancelled&quot; |



