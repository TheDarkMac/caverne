# ProductsApi

All URIs are relative to *http://localhost:8080/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**productsGet**](ProductsApi.md#productsGet) | **GET** /products | Catalogue produits |
| [**productsIdDelete**](ProductsApi.md#productsIdDelete) | **DELETE** /products/{id} | Supprimer un produit (admin) |
| [**productsIdGet**](ProductsApi.md#productsIdGet) | **GET** /products/{id} | Détail d&#39;un produit |
| [**productsIdPut**](ProductsApi.md#productsIdPut) | **PUT** /products/{id} | Créer ou mettre à jour un produit par identifiant (admin) |
| [**productsIdStockGet**](ProductsApi.md#productsIdStockGet) | **GET** /products/{id}/stock | Consulter le stock d&#39;un produit (admin) |
| [**productsIdStockPut**](ProductsApi.md#productsIdStockPut) | **PUT** /products/{id}/stock | Mettre à jour le stock d&#39;un produit (admin) |
| [**productsPost**](ProductsApi.md#productsPost) | **POST** /products | Créer ou mettre à jour un produit (admin) |


<a id="productsGet"></a>
# **productsGet**
> ProductsGet200Response productsGet(page, perPage, categoryId, isActive, search, currency, priceDate, priceFrom, priceTo)

Catalogue produits

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer page = 1; // Integer | 
    Integer perPage = 20; // Integer | 
    UUID categoryId = UUID.randomUUID(); // UUID | 
    Boolean isActive = true; // Boolean | 
    String search = "search_example"; // String | 
    String currency = "MGA"; // String | 
    LocalDate priceDate = LocalDate.parse("Sun Jun 01 03:00:00 EAT 2025"); // LocalDate | Effective price date (ISO 8601). Returns the most recent price whose `validFrom` is on or before this date, per (currency, unit) pair. Defaults to today. 
    LocalDate priceFrom = LocalDate.parse("Wed Jan 01 03:00:00 EAT 2025"); // LocalDate | Range start for price history view (ISO 8601). When set, all price records with `validFrom >= price_from` are returned. Takes precedence over `price_date`. 
    LocalDate priceTo = LocalDate.parse("Wed Dec 31 03:00:00 EAT 2025"); // LocalDate | Range end for price history view (ISO 8601). When set, all price records with `validFrom <= price_to` are returned. Takes precedence over `price_date`. 
    try {
      ProductsGet200Response result = apiInstance.productsGet(page, perPage, categoryId, isActive, search, currency, priceDate, priceFrom, priceTo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsGet");
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
| **categoryId** | **UUID**|  | [optional] |
| **isActive** | **Boolean**|  | [optional] |
| **search** | **String**|  | [optional] |
| **currency** | **String**|  | [optional] |
| **priceDate** | **LocalDate**| Effective price date (ISO 8601). Returns the most recent price whose &#x60;validFrom&#x60; is on or before this date, per (currency, unit) pair. Defaults to today.  | [optional] |
| **priceFrom** | **LocalDate**| Range start for price history view (ISO 8601). When set, all price records with &#x60;validFrom &gt;&#x3D; price_from&#x60; are returned. Takes precedence over &#x60;price_date&#x60;.  | [optional] |
| **priceTo** | **LocalDate**| Range end for price history view (ISO 8601). When set, all price records with &#x60;validFrom &lt;&#x3D; price_to&#x60; are returned. Takes precedence over &#x60;price_date&#x60;.  | [optional] |

### Return type

[**ProductsGet200Response**](ProductsGet200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Liste paginée |  -  |

<a id="productsIdDelete"></a>
# **productsIdDelete**
> productsIdDelete(id)

Supprimer un produit (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      apiInstance.productsIdDelete(id);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdDelete");
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

<a id="productsIdGet"></a>
# **productsIdGet**
> Product productsIdGet(id, priceDate, priceFrom, priceTo)

Détail d&#39;un produit

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    LocalDate priceDate = LocalDate.parse("Sun Jun 01 03:00:00 EAT 2025"); // LocalDate | Effective price date (ISO 8601). Returns the most recent price whose `validFrom` is on or before this date, per (currency, unit) pair. Defaults to today. 
    LocalDate priceFrom = LocalDate.parse("Wed Jan 01 03:00:00 EAT 2025"); // LocalDate | Range start for price history view (ISO 8601). When set, all price records with `validFrom >= price_from` are returned. Takes precedence over `price_date`. 
    LocalDate priceTo = LocalDate.parse("Wed Dec 31 03:00:00 EAT 2025"); // LocalDate | Range end for price history view (ISO 8601). When set, all price records with `validFrom <= price_to` are returned. Takes precedence over `price_date`. 
    try {
      Product result = apiInstance.productsIdGet(id, priceDate, priceFrom, priceTo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdGet");
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
| **priceDate** | **LocalDate**| Effective price date (ISO 8601). Returns the most recent price whose &#x60;validFrom&#x60; is on or before this date, per (currency, unit) pair. Defaults to today.  | [optional] |
| **priceFrom** | **LocalDate**| Range start for price history view (ISO 8601). When set, all price records with &#x60;validFrom &gt;&#x3D; price_from&#x60; are returned. Takes precedence over &#x60;price_date&#x60;.  | [optional] |
| **priceTo** | **LocalDate**| Range end for price history view (ISO 8601). When set, all price records with &#x60;validFrom &lt;&#x3D; price_to&#x60; are returned. Takes precedence over &#x60;price_date&#x60;.  | [optional] |

### Return type

[**Product**](Product.md)

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

<a id="productsIdPut"></a>
# **productsIdPut**
> Product productsIdPut(id, productInput)

Créer ou mettre à jour un produit par identifiant (admin)

Si le produit existe, il est mis à jour. Sinon, un nouveau produit est créé et l&#39;identifiant retourné fait foi. Si &#x60;id&#x60; est aussi fourni dans le payload, il doit correspondre à l&#39;identifiant du path. Les champs &#x60;label&#x60; et &#x60;reference&#x60; doivent être uniques (insensibles à la casse) parmi tous les produits. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    ProductInput productInput = new ProductInput(); // ProductInput | 
    try {
      Product result = apiInstance.productsIdPut(id, productInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdPut");
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
| **productInput** | [**ProductInput**](ProductInput.md)|  | |

### Return type

[**Product**](Product.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Produit mis à jour |  -  |
| **201** | Produit créé |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **409** | Conflit — la ressource viole une contrainte d&#39;unicité |  -  |
| **422** | Données invalides |  -  |

<a id="productsIdStockGet"></a>
# **productsIdStockGet**
> ProductStock productsIdStockGet(id)

Consulter le stock d&#39;un produit (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    try {
      ProductStock result = apiInstance.productsIdStockGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdStockGet");
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

[**ProductStock**](ProductStock.md)

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
| **404** | Ressource introuvable |  -  |

<a id="productsIdStockPut"></a>
# **productsIdStockPut**
> ProductStock productsIdStockPut(id, productStockInput)

Mettre à jour le stock d&#39;un produit (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
    ProductStockInput productStockInput = new ProductStockInput(); // ProductStockInput | 
    try {
      ProductStock result = apiInstance.productsIdStockPut(id, productStockInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdStockPut");
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
| **productStockInput** | [**ProductStockInput**](ProductStockInput.md)|  | |

### Return type

[**ProductStock**](ProductStock.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Stock mis à jour |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **404** | Ressource introuvable |  -  |
| **422** | Données invalides |  -  |

<a id="productsPost"></a>
# **productsPost**
> Product productsPost(productInput)

Créer ou mettre à jour un produit (admin)

Endpoint d&#39;upsert. Si &#x60;id&#x60; est absent ou null dans le payload, un nouveau produit est créé. Si &#x60;id&#x60; est fourni et qu&#39;un produit existe, il est mis à jour. Sinon, un nouveau produit est créé. Les champs &#x60;label&#x60; et &#x60;reference&#x60; doivent être uniques (insensibles à la casse) parmi tous les produits. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8080/api/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    ProductInput productInput = new ProductInput(); // ProductInput | 
    try {
      Product result = apiInstance.productsPost(productInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsPost");
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
| **productInput** | [**ProductInput**](ProductInput.md)|  | |

### Return type

[**Product**](Product.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Produit créé |  -  |
| **200** | Produit mis à jour |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **409** | Conflit — la ressource viole une contrainte d&#39;unicité |  -  |

