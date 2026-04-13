

# Order


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** |  |  [optional] |
|**userId** | **UUID** |  |  [optional] |
|**currencyCode** | **String** |  |  [optional] |
|**reference** | **String** |  |  [optional] |
|**date** | **OffsetDateTime** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**totalAmount** | **Double** |  |  [optional] |
|**deliveryCostId** | **UUID** |  |  [optional] |
|**items** | [**List&lt;OrderItem&gt;**](OrderItem.md) |  |  [optional] |
|**recipient** | [**RecipientInput**](RecipientInput.md) |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| DELIVERED | &quot;delivered&quot; |
| CANCELLED | &quot;cancelled&quot; |



