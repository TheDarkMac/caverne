# DeliveryCostsApi

All URIs are relative to *http://localhost:8080/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deliveryCostsGet**](DeliveryCostsApi.md#deliveryCostsGet) | **GET** /delivery-costs | Liste des coûts de livraison |
| [**deliveryCostsIdDelete**](DeliveryCostsApi.md#deliveryCostsIdDelete) | **DELETE** /delivery-costs/{id} | Supprimer un coût de livraison (admin) |
| [**deliveryCostsIdGet**](DeliveryCostsApi.md#deliveryCostsIdGet) | **GET** /delivery-costs/{id} | Détail d&#39;un coût de livraison |
| [**deliveryCostsIdPut**](DeliveryCostsApi.md#deliveryCostsIdPut) | **PUT** /delivery-costs/{id} | Mettre à jour un coût de livraison (admin) |
| [**deliveryCostsPost**](DeliveryCostsApi.md#deliveryCostsPost) | **POST** /delivery-costs | Créer un coût de livraison (admin) |


<a id="deliveryCostsGet"></a>
# **deliveryCostsGet**
> List&lt;DeliverCost&gt; deliveryCostsGet()

Liste des coûts de livraison

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DeliveryCostsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    DeliveryCostsApi apiInstance = new DeliveryCostsApi(defaultClient);
    try {
      List<DeliverCost> result = apiInstance.deliveryCostsGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeliveryCostsApi#deliveryCostsGet");
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

[**List&lt;DeliverCost&gt;**](DeliverCost.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="deliveryCostsIdDelete"></a>
# **deliveryCostsIdDelete**
> deliveryCostsIdDelete(id)

Supprimer un coût de livraison (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DeliveryCostsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    DeliveryCostsApi apiInstance = new DeliveryCostsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      apiInstance.deliveryCostsIdDelete(id);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeliveryCostsApi#deliveryCostsIdDelete");
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
| **id** | **UUID**|  | |

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
| **204** | Supprimé |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **404** | Ressource introuvable |  -  |

<a id="deliveryCostsIdGet"></a>
# **deliveryCostsIdGet**
> DeliverCost deliveryCostsIdGet(id)

Détail d&#39;un coût de livraison

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DeliveryCostsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    DeliveryCostsApi apiInstance = new DeliveryCostsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      DeliverCost result = apiInstance.deliveryCostsIdGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeliveryCostsApi#deliveryCostsIdGet");
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
| **id** | **UUID**|  | |

### Return type

[**DeliverCost**](DeliverCost.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **404** | Ressource introuvable |  -  |

<a id="deliveryCostsIdPut"></a>
# **deliveryCostsIdPut**
> DeliverCost deliveryCostsIdPut(id, deliverCostInput)

Mettre à jour un coût de livraison (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DeliveryCostsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    DeliveryCostsApi apiInstance = new DeliveryCostsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    DeliverCostInput deliverCostInput = new DeliverCostInput(); // DeliverCostInput | 
    try {
      DeliverCost result = apiInstance.deliveryCostsIdPut(id, deliverCostInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeliveryCostsApi#deliveryCostsIdPut");
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
| **id** | **UUID**|  | |
| **deliverCostInput** | [**DeliverCostInput**](DeliverCostInput.md)|  | |

### Return type

[**DeliverCost**](DeliverCost.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Coût de livraison mis à jour |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **404** | Ressource introuvable |  -  |
| **422** | Données invalides |  -  |

<a id="deliveryCostsPost"></a>
# **deliveryCostsPost**
> DeliverCost deliveryCostsPost(deliverCostInput)

Créer un coût de livraison (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DeliveryCostsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    DeliveryCostsApi apiInstance = new DeliveryCostsApi(defaultClient);
    DeliverCostInput deliverCostInput = new DeliverCostInput(); // DeliverCostInput | 
    try {
      DeliverCost result = apiInstance.deliveryCostsPost(deliverCostInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeliveryCostsApi#deliveryCostsPost");
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
| **deliverCostInput** | [**DeliverCostInput**](DeliverCostInput.md)|  | |

### Return type

[**DeliverCost**](DeliverCost.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Coût de livraison créé |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **422** | Données invalides |  -  |

