

# LoginResponse

Issued by `POST /auth/login`. `access_token` is a signed HS256 JWT with claims `iss`, `sub`, `jti`, `iat`, `exp`, `role`, `email`, `phone`. The `jti` is persisted server-side so `POST /auth/logout` can revoke the token before `exp`. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**accessToken** | **String** | Signed HS256 JWT. Send back as &#x60;Authorization: Bearer &lt;access_token&gt;&#x60;. |  [optional] |
|**tokenType** | **String** |  |  [optional] |
|**expiresIn** | **Integer** | Token lifetime in seconds (matches the &#x60;exp&#x60; claim minus &#x60;iat&#x60;). |  [optional] |



