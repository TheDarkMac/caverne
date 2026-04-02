# ProductsApi

All URIs are relative to *https://api.lacaverne/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**productsGet**](ProductsApi.md#productsGet) | **GET** /products | Catalogue produits |
| [**productsIdDelete**](ProductsApi.md#productsIdDelete) | **DELETE** /products/{id} | Supprimer un produit (admin) |
| [**productsIdGet**](ProductsApi.md#productsIdGet) | **GET** /products/{id} | Détail d&#39;un produit |
| [**productsIdImagesGet**](ProductsApi.md#productsIdImagesGet) | **GET** /products/{id}/images | Images d&#39;un produit |
| [**productsIdImagesImageIdDelete**](ProductsApi.md#productsIdImagesImageIdDelete) | **DELETE** /products/{id}/images/{imageId} | Supprimer une image |
| [**productsIdImagesImageIdPut**](ProductsApi.md#productsIdImagesImageIdPut) | **PUT** /products/{id}/images/{imageId} | Modifier une image (définir main, etc.) |
| [**productsIdImagesPost**](ProductsApi.md#productsIdImagesPost) | **POST** /products/{id}/images | Ajouter une image (admin) |
| [**productsIdPricesGet**](ProductsApi.md#productsIdPricesGet) | **GET** /products/{id}/prices | Historique des prix d&#39;un produit |
| [**productsIdPricesPost**](ProductsApi.md#productsIdPricesPost) | **POST** /products/{id}/prices | Ajouter un prix (admin) |
| [**productsIdPut**](ProductsApi.md#productsIdPut) | **PUT** /products/{id} | Modifier un produit (admin) |
| [**productsIdReviewsGet**](ProductsApi.md#productsIdReviewsGet) | **GET** /products/{id}/reviews | Avis clients d&#39;un produit |
| [**productsIdReviewsPost**](ProductsApi.md#productsIdReviewsPost) | **POST** /products/{id}/reviews | Déposer un avis (utilisateur connecté) |
| [**productsIdStockGet**](ProductsApi.md#productsIdStockGet) | **GET** /products/{id}/stock | Stock disponible d&#39;un produit |
| [**productsIdStockHistoryGet**](ProductsApi.md#productsIdStockHistoryGet) | **GET** /products/{id}/stock/history | Historique des mouvements de stock |
| [**productsIdStockPost**](ProductsApi.md#productsIdStockPost) | **POST** /products/{id}/stock | Enregistrer un mouvement de stock (admin) |
| [**productsIdTechnicalSpecsGet**](ProductsApi.md#productsIdTechnicalSpecsGet) | **GET** /products/{id}/technical-specs | Spécifications techniques d&#39;un produit |
| [**productsIdTechnicalSpecsPut**](ProductsApi.md#productsIdTechnicalSpecsPut) | **PUT** /products/{id}/technical-specs | Créer ou remplacer les specs techniques (admin) |
| [**productsPost**](ProductsApi.md#productsPost) | **POST** /products | Créer un produit (admin) |


<a id="productsGet"></a>
# **productsGet**
> ProductsGet200Response productsGet(page, perPage, categoryId, isActive, search, currency)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer page = 1; // Integer | 
    Integer perPage = 20; // Integer | 
    Integer categoryId = 56; // Integer | 
    Boolean isActive = true; // Boolean | 
    String search = "search_example"; // String | 
    String currency = "MGA"; // String | 
    try {
      ProductsGet200Response result = apiInstance.productsGet(page, perPage, categoryId, isActive, search, currency);
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
| **categoryId** | **Integer**|  | [optional] |
| **isActive** | **Boolean**|  | [optional] |
| **search** | **String**|  | [optional] |
| **currency** | **String**|  | [optional] |

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
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
| **id** | **Integer**|  | |

### Return type

null (empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Supprimé |  -  |

<a id="productsIdGet"></a>
# **productsIdGet**
> Product productsIdGet(id)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      Product result = apiInstance.productsIdGet(id);
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
| **id** | **Integer**|  | |

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

<a id="productsIdImagesGet"></a>
# **productsIdImagesGet**
> List&lt;ProductImage&gt; productsIdImagesGet(id)

Images d&#39;un produit

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      List<ProductImage> result = apiInstance.productsIdImagesGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdImagesGet");
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

[**List&lt;ProductImage&gt;**](ProductImage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsIdImagesImageIdDelete"></a>
# **productsIdImagesImageIdDelete**
> productsIdImagesImageIdDelete(id, imageId)

Supprimer une image

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    Integer imageId = 56; // Integer | 
    try {
      apiInstance.productsIdImagesImageIdDelete(id, imageId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdImagesImageIdDelete");
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
| **imageId** | **Integer**|  | |

### Return type

null (empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Supprimée |  -  |

<a id="productsIdImagesImageIdPut"></a>
# **productsIdImagesImageIdPut**
> ProductImage productsIdImagesImageIdPut(id, imageId, productImageInput)

Modifier une image (définir main, etc.)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    Integer imageId = 56; // Integer | 
    ProductImageInput productImageInput = new ProductImageInput(); // ProductImageInput | 
    try {
      ProductImage result = apiInstance.productsIdImagesImageIdPut(id, imageId, productImageInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdImagesImageIdPut");
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
| **imageId** | **Integer**|  | |
| **productImageInput** | [**ProductImageInput**](ProductImageInput.md)|  | |

### Return type

[**ProductImage**](ProductImage.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsIdImagesPost"></a>
# **productsIdImagesPost**
> ProductImage productsIdImagesPost(id, productImageInput)

Ajouter une image (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    ProductImageInput productImageInput = new ProductImageInput(); // ProductImageInput | 
    try {
      ProductImage result = apiInstance.productsIdImagesPost(id, productImageInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdImagesPost");
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
| **productImageInput** | [**ProductImageInput**](ProductImageInput.md)|  | |

### Return type

[**ProductImage**](ProductImage.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Créée |  -  |

<a id="productsIdPricesGet"></a>
# **productsIdPricesGet**
> List&lt;Price&gt; productsIdPricesGet(id)

Historique des prix d&#39;un produit

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      List<Price> result = apiInstance.productsIdPricesGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdPricesGet");
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

[**List&lt;Price&gt;**](Price.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsIdPricesPost"></a>
# **productsIdPricesPost**
> Price productsIdPricesPost(id, priceInput)

Ajouter un prix (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    PriceInput priceInput = new PriceInput(); // PriceInput | 
    try {
      Price result = apiInstance.productsIdPricesPost(id, priceInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdPricesPost");
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
| **priceInput** | [**PriceInput**](PriceInput.md)|  | |

### Return type

[**Price**](Price.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Créé |  -  |

<a id="productsIdPut"></a>
# **productsIdPut**
> Product productsIdPut(id, productInput)

Modifier un produit (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
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
| **id** | **Integer**|  | |
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
| **200** | Mis à jour |  -  |

<a id="productsIdReviewsGet"></a>
# **productsIdReviewsGet**
> ProductsIdReviewsGet200Response productsIdReviewsGet(id, page)

Avis clients d&#39;un produit

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    Integer page = 1; // Integer | 
    try {
      ProductsIdReviewsGet200Response result = apiInstance.productsIdReviewsGet(id, page);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdReviewsGet");
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
| **page** | **Integer**|  | [optional] [default to 1] |

### Return type

[**ProductsIdReviewsGet200Response**](ProductsIdReviewsGet200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsIdReviewsPost"></a>
# **productsIdReviewsPost**
> Review productsIdReviewsPost(id, reviewInput)

Déposer un avis (utilisateur connecté)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    ReviewInput reviewInput = new ReviewInput(); // ReviewInput | 
    try {
      Review result = apiInstance.productsIdReviewsPost(id, reviewInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdReviewsPost");
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
| **reviewInput** | [**ReviewInput**](ReviewInput.md)|  | |

### Return type

[**Review**](Review.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Avis enregistré |  -  |
| **422** | Données invalides |  -  |

<a id="productsIdStockGet"></a>
# **productsIdStockGet**
> StockSummary productsIdStockGet(id)

Stock disponible d&#39;un produit

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      StockSummary result = apiInstance.productsIdStockGet(id);
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
| **id** | **Integer**|  | |

### Return type

[**StockSummary**](StockSummary.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Résumé du stock |  -  |

<a id="productsIdStockHistoryGet"></a>
# **productsIdStockHistoryGet**
> ProductsIdStockHistoryGet200Response productsIdStockHistoryGet(id, page, movementType)

Historique des mouvements de stock

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    Integer page = 1; // Integer | 
    String movementType = "in"; // String | 
    try {
      ProductsIdStockHistoryGet200Response result = apiInstance.productsIdStockHistoryGet(id, page, movementType);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdStockHistoryGet");
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
| **page** | **Integer**|  | [optional] [default to 1] |
| **movementType** | **String**|  | [optional] [enum: in, out, adjustment] |

### Return type

[**ProductsIdStockHistoryGet200Response**](ProductsIdStockHistoryGet200Response.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsIdStockPost"></a>
# **productsIdStockPost**
> StockEntry productsIdStockPost(id, stockInput)

Enregistrer un mouvement de stock (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    StockInput stockInput = new StockInput(); // StockInput | 
    try {
      StockEntry result = apiInstance.productsIdStockPost(id, stockInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdStockPost");
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
| **stockInput** | [**StockInput**](StockInput.md)|  | |

### Return type

[**StockEntry**](StockEntry.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Mouvement enregistré |  -  |

<a id="productsIdTechnicalSpecsGet"></a>
# **productsIdTechnicalSpecsGet**
> TechnicalSpecs productsIdTechnicalSpecsGet(id)

Spécifications techniques d&#39;un produit

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
    defaultClient.setBasePath("https://api.lacaverne/v1");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      TechnicalSpecs result = apiInstance.productsIdTechnicalSpecsGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdTechnicalSpecsGet");
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

[**TechnicalSpecs**](TechnicalSpecs.md)

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

<a id="productsIdTechnicalSpecsPut"></a>
# **productsIdTechnicalSpecsPut**
> TechnicalSpecs productsIdTechnicalSpecsPut(id, technicalSpecsInput)

Créer ou remplacer les specs techniques (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    ProductsApi apiInstance = new ProductsApi(defaultClient);
    Integer id = 56; // Integer | 
    TechnicalSpecsInput technicalSpecsInput = new TechnicalSpecsInput(); // TechnicalSpecsInput | 
    try {
      TechnicalSpecs result = apiInstance.productsIdTechnicalSpecsPut(id, technicalSpecsInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ProductsApi#productsIdTechnicalSpecsPut");
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
| **technicalSpecsInput** | [**TechnicalSpecsInput**](TechnicalSpecsInput.md)|  | |

### Return type

[**TechnicalSpecs**](TechnicalSpecs.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="productsPost"></a>
# **productsPost**
> Product productsPost(productInput)

Créer un produit (admin)

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
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
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

