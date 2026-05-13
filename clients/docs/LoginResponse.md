

# LoginResponse

Issued by `POST /auth/login` and `POST /auth/refresh`. The `access_token` is a short-lived (15 min) HS256 JWT with claims `iss`, `sub`, `jti`, `iat`, `exp`, `role`, `email`, `phone`. Send it back as `Authorization: Bearer <access_token>`.  The response also sets two cookies that the client never reads or forwards explicitly — the browser handles them:  - `refresh_token` (HttpOnly, Secure, SameSite=None, Path=/api/v1/auth) — 30 days,   rotated on every `/auth/refresh`. Stored as a SHA-256 hash server-side; reuse   of a revoked refresh triggers revocation of the entire token family. - `csrf_token` (Secure, SameSite=None, Path=/api/v1/auth, **not** HttpOnly so JS   can read it) — must be echoed in the `X-CSRF-Token` header on `/auth/refresh`   and `/auth/logout`.  The client must call `fetch(..., { credentials: 'include' })` so the cookies are sent/received. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**accessToken** | **String** | Signed HS256 JWT. Send back as &#x60;Authorization: Bearer &lt;access_token&gt;&#x60;. |  [optional] |
|**tokenType** | **String** |  |  [optional] |
|**expiresIn** | **Integer** | Access token lifetime in seconds (matches the &#x60;exp&#x60; claim minus &#x60;iat&#x60;). |  [optional] |



