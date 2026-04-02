

# Payment


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Integer** |  |  [optional] |
|**orderId** | **Integer** |  |  [optional] |
|**methodCode** | **String** |  |  [optional] |
|**currencyCode** | **String** |  |  [optional] |
|**amount** | **Double** |  |  [optional] |
|**date** | **OffsetDateTime** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**internalReference** | **String** |  |  [optional] |
|**providerResponse** | **Object** |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| FAILED | &quot;failed&quot; |
| REFUNDED | &quot;refunded&quot; |



