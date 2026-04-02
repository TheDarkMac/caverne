

# StockEntry


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Integer** |  |  [optional] |
|**productId** | **Integer** |  |  [optional] |
|**quantity** | **Double** |  |  [optional] |
|**movementType** | [**MovementTypeEnum**](#MovementTypeEnum) |  |  [optional] |
|**date** | **OffsetDateTime** |  |  [optional] |
|**unit** | **String** |  |  [optional] |



## Enum: MovementTypeEnum

| Name | Value |
|---- | -----|
| IN | &quot;in&quot; |
| OUT | &quot;out&quot; |
| ADJUSTMENT | &quot;adjustment&quot; |



