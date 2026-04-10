# AddressesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**usersMeAddressesGet**](AddressesApi.md#usersMeAddressesGet) | **GET** /users/me/addresses | Liste des adresses de l&#39;utilisateur connecté |
| [**usersMeAddressesIdDefaultPut**](AddressesApi.md#usersMeAddressesIdDefaultPut) | **PUT** /users/me/addresses/{id}/default | Définir comme adresse par défaut |
| [**usersMeAddressesIdDelete**](AddressesApi.md#usersMeAddressesIdDelete) | **DELETE** /users/me/addresses/{id} | Supprimer une adresse |
| [**usersMeAddressesIdPut**](AddressesApi.md#usersMeAddressesIdPut) | **PUT** /users/me/addresses/{id} | Créer ou mettre à jour une adresse par identifiant |
| [**usersMeAddressesPost**](AddressesApi.md#usersMeAddressesPost) | **POST** /users/me/addresses | Créer ou mettre à jour une adresse |


<a id="usersMeAddressesGet"></a>
# **usersMeAddressesGet**
> List&lt;Address&gt; usersMeAddressesGet()

Liste des adresses de l&#39;utilisateur connecté

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AddressesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AddressesApi apiInstance = new AddressesApi(defaultClient);
    try {
      List<Address> result = apiInstance.usersMeAddressesGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressesApi#usersMeAddressesGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;Address&gt;**](Address.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **401** | Token JWT manquant ou invalide |  -  |

<a id="usersMeAddressesIdDefaultPut"></a>
# **usersMeAddressesIdDefaultPut**
> Address usersMeAddressesIdDefaultPut(id)

Définir comme adresse par défaut

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AddressesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AddressesApi apiInstance = new AddressesApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      Address result = apiInstance.usersMeAddressesIdDefaultPut(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressesApi#usersMeAddressesIdDefaultPut");
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
| **id** | **Integer**|  | |

### Return type

[**Address**](Address.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Adresse définie par défaut |  -  |
| **401** | Token JWT manquant ou invalide |  -  |

<a id="usersMeAddressesIdDelete"></a>
# **usersMeAddressesIdDelete**
> usersMeAddressesIdDelete(id)

Supprimer une adresse

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AddressesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AddressesApi apiInstance = new AddressesApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      apiInstance.usersMeAddressesIdDelete(id);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressesApi#usersMeAddressesIdDelete");
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
| **id** | **Integer**|  | |

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
| **204** | Supprimée |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **404** | Ressource introuvable |  -  |

<a id="usersMeAddressesIdPut"></a>
# **usersMeAddressesIdPut**
> Address usersMeAddressesIdPut(id, addressInput)

Créer ou mettre à jour une adresse par identifiant

Si l&#39;adresse existe pour l&#39;utilisateur connecté, elle est mise à jour. Sinon, une nouvelle adresse est créée et l&#39;identifiant retourné fait foi. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AddressesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AddressesApi apiInstance = new AddressesApi(defaultClient);
    Integer id = 56; // Integer | 
    AddressInput addressInput = new AddressInput(); // AddressInput | 
    try {
      Address result = apiInstance.usersMeAddressesIdPut(id, addressInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressesApi#usersMeAddressesIdPut");
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
| **id** | **Integer**|  | |
| **addressInput** | [**AddressInput**](AddressInput.md)|  | |

### Return type

[**Address**](Address.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Adresse mise à jour |  -  |
| **201** | Adresse créée |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **404** | Ressource introuvable |  -  |

<a id="usersMeAddressesPost"></a>
# **usersMeAddressesPost**
> Address usersMeAddressesPost(addressInput)

Créer ou mettre à jour une adresse

Endpoint d&#39;upsert. Si &#x60;id&#x60; est absent ou null dans le payload, une nouvelle adresse est créée. Si &#x60;id&#x60; est fourni et qu&#39;une adresse appartenant à l&#39;utilisateur existe, elle est mise à jour. Sinon, une nouvelle adresse est créée. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.AddressesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    AddressesApi apiInstance = new AddressesApi(defaultClient);
    AddressInput addressInput = new AddressInput(); // AddressInput | 
    try {
      Address result = apiInstance.usersMeAddressesPost(addressInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressesApi#usersMeAddressesPost");
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
| **addressInput** | [**AddressInput**](AddressInput.md)|  | |

### Return type

[**Address**](Address.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Adresse créée |  -  |
| **200** | Adresse mise à jour |  -  |
| **401** | Token JWT manquant ou invalide |  -  |

