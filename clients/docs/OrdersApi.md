# OrdersApi

All URIs are relative to *http://localhost:8080/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**ordersAllGet**](OrdersApi.md#ordersAllGet) | **GET** /orders/all | Toutes les commandes (admin) |
| [**ordersGet**](OrdersApi.md#ordersGet) | **GET** /orders | Mes commandes (utilisateur connecté) |
| [**ordersIdCancelPost**](OrdersApi.md#ordersIdCancelPost) | **POST** /orders/{id}/cancel | Annuler une commande (owner ou admin) |
| [**ordersIdGet**](OrdersApi.md#ordersIdGet) | **GET** /orders/{id} | Détail d&#39;une commande |
| [**ordersIdStatusPut**](OrdersApi.md#ordersIdStatusPut) | **PUT** /orders/{id}/status | Changer le statut d&#39;une commande (admin) |
| [**ordersPost**](OrdersApi.md#ordersPost) | **POST** /orders | Passer une commande |


<a id="ordersAllGet"></a>
# **ordersAllGet**
> OrdersGet200Response ordersAllGet(page, perPage, status, userId)

Toutes les commandes (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    Integer page = 1; // Integer | 
    Integer perPage = 20; // Integer | 
    String status = "pending"; // String | 
    UUID userId = UUID.randomUUID(); // UUID | 
    try {
      OrdersGet200Response result = apiInstance.ordersAllGet(page, perPage, status, userId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersAllGet");
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
| **page** | **Integer**|  | [optional] [default to 1] |
| **perPage** | **Integer**|  | [optional] [default to 20] |
| **status** | **String**|  | [optional] [enum: pending, confirmed, delivered, cancelled] |
| **userId** | **UUID**|  | [optional] |

### Return type

[**OrdersGet200Response**](OrdersGet200Response.md)

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
| **403** | Accès refusé (rôle insuffisant) |  -  |

<a id="ordersGet"></a>
# **ordersGet**
> OrdersGet200Response ordersGet(page, status)

Mes commandes (utilisateur connecté)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    Integer page = 1; // Integer | 
    String status = "pending"; // String | 
    try {
      OrdersGet200Response result = apiInstance.ordersGet(page, status);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersGet");
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
| **page** | **Integer**|  | [optional] [default to 1] |
| **status** | **String**|  | [optional] [enum: pending, confirmed, delivered, cancelled] |

### Return type

[**OrdersGet200Response**](OrdersGet200Response.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="ordersIdCancelPost"></a>
# **ordersIdCancelPost**
> Order ordersIdCancelPost(id)

Annuler une commande (owner ou admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      Order result = apiInstance.ordersIdCancelPost(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersIdCancelPost");
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

[**Order**](Order.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Commande annulée |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |

<a id="ordersIdGet"></a>
# **ordersIdGet**
> Order ordersIdGet(id)

Détail d&#39;une commande

Les commandes invitées peuvent être consultées sans authentification. Les commandes liées à un utilisateur nécessitent que l&#39;acteur soit le propriétaire ou un admin. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      Order result = apiInstance.ordersIdGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersIdGet");
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

[**Order**](Order.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **404** | Ressource introuvable |  -  |

<a id="ordersIdStatusPut"></a>
# **ordersIdStatusPut**
> Order ordersIdStatusPut(id, orderStatusUpdate)

Changer le statut d&#39;une commande (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    OrderStatusUpdate orderStatusUpdate = new OrderStatusUpdate(); // OrderStatusUpdate | 
    try {
      Order result = apiInstance.ordersIdStatusPut(id, orderStatusUpdate);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersIdStatusPut");
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
| **orderStatusUpdate** | [**OrderStatusUpdate**](OrderStatusUpdate.md)|  | |

### Return type

[**Order**](Order.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Statut mis à jour |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |

<a id="ordersPost"></a>
# **ordersPost**
> Order ordersPost(orderInput)

Passer une commande

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.OrdersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    OrdersApi apiInstance = new OrdersApi(defaultClient);
    OrderInput orderInput = new OrderInput(); // OrderInput | 
    try {
      Order result = apiInstance.ordersPost(orderInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrdersApi#ordersPost");
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
| **orderInput** | [**OrderInput**](OrderInput.md)|  | |

### Return type

[**Order**](Order.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Commande créée |  -  |
| **422** | Données invalides |  -  |

