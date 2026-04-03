# CategoriesApi

All URIs are relative to *https://api.lacaverne/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**categoriesGet**](CategoriesApi.md#categoriesGet) | **GET** /categories | Liste des catégories (arbre) |
| [**categoriesIdDelete**](CategoriesApi.md#categoriesIdDelete) | **DELETE** /categories/{id} | Supprimer une catégorie (admin) |
| [**categoriesIdGet**](CategoriesApi.md#categoriesIdGet) | **GET** /categories/{id} | Détail d&#39;une catégorie |
| [**categoriesIdPut**](CategoriesApi.md#categoriesIdPut) | **PUT** /categories/{id} | Mettre à jour une catégorie par identifiant (admin) |
| [**categoriesPost**](CategoriesApi.md#categoriesPost) | **POST** /categories | Créer ou mettre à jour une catégorie (admin) |


<a id="categoriesGet"></a>
# **categoriesGet**
> List&lt;Category&gt; categoriesGet(flat)

Liste des catégories (arbre)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CategoriesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    CategoriesApi apiInstance = new CategoriesApi(defaultClient);
    Boolean flat = true; // Boolean | Si true, retourne une liste plate sans enfants
    try {
      List<Category> result = apiInstance.categoriesGet(flat);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CategoriesApi#categoriesGet");
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
| **flat** | **Boolean**| Si true, retourne une liste plate sans enfants | [optional] |

### Return type

[**List&lt;Category&gt;**](Category.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="categoriesIdDelete"></a>
# **categoriesIdDelete**
> categoriesIdDelete(id)

Supprimer une catégorie (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CategoriesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CategoriesApi apiInstance = new CategoriesApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      apiInstance.categoriesIdDelete(id);
    } catch (ApiException e) {
      System.err.println("Exception when calling CategoriesApi#categoriesIdDelete");
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
| **403** | Accès refusé (rôle insuffisant) |  -  |

<a id="categoriesIdGet"></a>
# **categoriesIdGet**
> Category categoriesIdGet(id)

Détail d&#39;une catégorie

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CategoriesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    CategoriesApi apiInstance = new CategoriesApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      Category result = apiInstance.categoriesIdGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CategoriesApi#categoriesIdGet");
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

[**Category**](Category.md)

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

<a id="categoriesIdPut"></a>
# **categoriesIdPut**
> Category categoriesIdPut(id, categoryInput)

Mettre à jour une catégorie par identifiant (admin)

Endpoint de mise à jour avec identifiant dans l&#39;URL. Le &#x60;id&#x60; du path fait foi. Si &#x60;id&#x60; est aussi fourni dans le payload, il doit correspondre à l&#39;identifiant du path. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CategoriesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CategoriesApi apiInstance = new CategoriesApi(defaultClient);
    Integer id = 56; // Integer | 
    CategoryInput categoryInput = new CategoryInput(); // CategoryInput | 
    try {
      Category result = apiInstance.categoriesIdPut(id, categoryInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CategoriesApi#categoriesIdPut");
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
| **categoryInput** | [**CategoryInput**](CategoryInput.md)|  | |

### Return type

[**Category**](Category.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |
| **404** | Ressource introuvable |  -  |

<a id="categoriesPost"></a>
# **categoriesPost**
> Category categoriesPost(categoryInput)

Créer ou mettre à jour une catégorie (admin)

Endpoint d&#39;upsert. Si &#x60;id&#x60; est absent ou null dans le payload, une nouvelle catégorie est créée. Si &#x60;id&#x60; est fourni, la catégorie existante correspondante doit être mise à jour. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CategoriesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CategoriesApi apiInstance = new CategoriesApi(defaultClient);
    CategoryInput categoryInput = new CategoryInput(); // CategoryInput | 
    try {
      Category result = apiInstance.categoriesPost(categoryInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CategoriesApi#categoriesPost");
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
| **categoryInput** | [**CategoryInput**](CategoryInput.md)|  | |

### Return type

[**Category**](Category.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Créée quand &#x60;id&#x60; est absent |  -  |
| **200** | Mise à jour quand &#x60;id&#x60; est fourni |  -  |
| **401** | Token JWT manquant ou invalide |  -  |
| **403** | Accès refusé (rôle insuffisant) |  -  |

