# AuthApi

All URIs are relative to *http://localhost:8080/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**authLoginPost**](AuthApi.md#authLoginPost) | **POST** /auth/login | Connexion — obtenir un JWT et poser les cookies refresh/csrf |
| [**authLogoutPost**](AuthApi.md#authLogoutPost) | **POST** /auth/logout | Révocation du refresh token courant et nettoyage des cookies |
| [**authRefreshPost**](AuthApi.md#authRefreshPost) | **POST** /auth/refresh | Rotation du refresh token et émission d&#39;un nouvel access JWT |
| [**authRegisterPost**](AuthApi.md#authRegisterPost) | **POST** /auth/register | Inscription d&#39;un nouvel utilisateur |


<a id="authLoginPost"></a>
# **authLoginPost**
> LoginResponse authLoginPost(loginRequest)

Connexion — obtenir un JWT et poser les cookies refresh/csrf

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AuthApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    AuthApi apiInstance = new AuthApi(defaultClient);
    LoginRequest loginRequest = new LoginRequest(); // LoginRequest | 
    try {
      LoginResponse result = apiInstance.authLoginPost(loginRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AuthApi#authLoginPost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **loginRequest** | [**LoginRequest**](LoginRequest.md)|  | |

### Return type

[**LoginResponse**](LoginResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Access JWT retourné dans le corps. Les cookies &#x60;refresh_token&#x60; (HttpOnly) et &#x60;csrf_token&#x60; sont posés via &#x60;Set-Cookie&#x60;.  |  * Set-Cookie - Deux cookies sont émis : &#x60;refresh_token&#x3D;&lt;opaque&gt;; HttpOnly; Secure; SameSite&#x3D;None; Path&#x3D;/api/v1/auth; Max-Age&#x3D;2592000&#x60; et &#x60;csrf_token&#x3D;&lt;value&gt;; Secure; SameSite&#x3D;None; Path&#x3D;/api/v1/auth; Max-Age&#x3D;2592000&#x60;.  <br>  |
| **401** | Token JWT manquant ou invalide |  -  |

<a id="authLogoutPost"></a>
# **authLogoutPost**
> authLogoutPost(xCSRFToken)

Révocation du refresh token courant et nettoyage des cookies

Révoque le refresh token courant côté serveur et efface les cookies &#x60;refresh_token&#x60; et &#x60;csrf_token&#x60; (Max-Age&#x3D;0). Le header &#x60;X-CSRF-Token&#x60; est requis. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AuthApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AuthApi apiInstance = new AuthApi(defaultClient);
    String xCSRFToken = "xCSRFToken_example"; // String | 
    try {
      apiInstance.authLogoutPost(xCSRFToken);
    } catch (ApiException e) {
      System.err.println("Exception when calling AuthApi#authLogoutPost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xCSRFToken** | **String**|  | |

### Return type

null (empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Déconnexion réussie |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | CSRF token manquant ou invalide. |  -  |

<a id="authRefreshPost"></a>
# **authRefreshPost**
> LoginResponse authRefreshPost(xCSRFToken)

Rotation du refresh token et émission d&#39;un nouvel access JWT

Lit le cookie &#x60;refresh_token&#x60; (envoyé automatiquement par le navigateur avec &#x60;credentials: &#39;include&#39;&#x60;). Vérifie la signature CSRF via header &#x60;X-CSRF-Token&#x60; (doit valoir la même chose que le cookie &#x60;csrf_token&#x60;). Révoque le refresh précédent et en émet un nouveau (rotation). Si un refresh déjà révoqué est présenté → 401 et toute la famille de tokens est invalidée (reuse detection). 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AuthApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    AuthApi apiInstance = new AuthApi(defaultClient);
    String xCSRFToken = "xCSRFToken_example"; // String | Doit être égal au cookie `csrf_token`.
    try {
      LoginResponse result = apiInstance.authRefreshPost(xCSRFToken);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AuthApi#authRefreshPost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xCSRFToken** | **String**| Doit être égal au cookie &#x60;csrf_token&#x60;. | |

### Return type

[**LoginResponse**](LoginResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Nouvel access JWT + cookies refresh/csrf rotés. |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | CSRF token manquant ou invalide. |  -  |

<a id="authRegisterPost"></a>
# **authRegisterPost**
> User authRegisterPost(registerRequest)

Inscription d&#39;un nouvel utilisateur

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AuthApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    AuthApi apiInstance = new AuthApi(defaultClient);
    RegisterRequest registerRequest = new RegisterRequest(); // RegisterRequest | 
    try {
      User result = apiInstance.authRegisterPost(registerRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AuthApi#authRegisterPost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **registerRequest** | [**RegisterRequest**](RegisterRequest.md)|  | |

### Return type

[**User**](User.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Utilisateur créé |  -  |
| **422** | Données invalides |  -  |

