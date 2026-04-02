

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
|**items** | [**List&lt;OrderItem&gt;**](OrderItem.md) |  |  [optional] |
|**guestAddress** | [**GuestAddressInput**](GuestAddressInput.md) |  |  [optional] |
|**providerResponse** | **Object** |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;pending&quot; |
| CONFIRMED | &quot;confirmed&quot; |
| DELIVERED | &quot;delivered&quot; |
| CANCELLED | &quot;cancelled&quot; |



