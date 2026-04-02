

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
|**deliveryCostId** | **Integer** |  |  [optional] |
|**items** | [**List&lt;OrderItem&gt;**](OrderItem.md) |  |  [optional] |
|**recipient** | [**RecipientInput**](RecipientInput.md) |  |  [optional] |
|**providerResponse** | **Object** |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| DELIVERED | &quot;delivered&quot; |
| CANCELLED | &quot;cancelled&quot; |



