# CurrenciesApi

All URIs are relative to *https://api.lacaverne/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**currenciesCodeIsoDelete**](CurrenciesApi.md#currenciesCodeIsoDelete) | **DELETE** /currencies/{code_iso} | Supprimer une devise (admin) |
| [**currenciesCodeIsoPut**](CurrenciesApi.md#currenciesCodeIsoPut) | **PUT** /currencies/{code_iso} | Modifier une devise (admin) |
| [**currenciesGet**](CurrenciesApi.md#currenciesGet) | **GET** /currencies | Liste des devises supportées |
| [**currenciesPost**](CurrenciesApi.md#currenciesPost) | **POST** /currencies | Ajouter une devise (admin) |


<a id="currenciesCodeIsoDelete"></a>
# **currenciesCodeIsoDelete**
> currenciesCodeIsoDelete(codeIso)

Supprimer une devise (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CurrenciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CurrenciesApi apiInstance = new CurrenciesApi(defaultClient);
    String codeIso = "codeIso_example"; // String | 
    try {
      apiInstance.currenciesCodeIsoDelete(codeIso);
    } catch (ApiException e) {
      System.err.println("Exception when calling CurrenciesApi#currenciesCodeIsoDelete");
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
| **codeIso** | **String**|  | |

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

<a id="currenciesCodeIsoPut"></a>
# **currenciesCodeIsoPut**
> Currency currenciesCodeIsoPut(codeIso, currency)

Modifier une devise (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CurrenciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CurrenciesApi apiInstance = new CurrenciesApi(defaultClient);
    String codeIso = "codeIso_example"; // String | 
    Currency currency = new Currency(); // Currency | 
    try {
      Currency result = apiInstance.currenciesCodeIsoPut(codeIso, currency);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CurrenciesApi#currenciesCodeIsoPut");
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
| **codeIso** | **String**|  | |
| **currency** | [**Currency**](Currency.md)|  | |

### Return type

[**Currency**](Currency.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="currenciesGet"></a>
# **currenciesGet**
> List&lt;Currency&gt; currenciesGet()

Liste des devises supportées

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CurrenciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    CurrenciesApi apiInstance = new CurrenciesApi(defaultClient);
    try {
      List<Currency> result = apiInstance.currenciesGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CurrenciesApi#currenciesGet");
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

[**List&lt;Currency&gt;**](Currency.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="currenciesPost"></a>
# **currenciesPost**
> Currency currenciesPost(currency)

Ajouter une devise (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.CurrenciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    CurrenciesApi apiInstance = new CurrenciesApi(defaultClient);
    Currency currency = new Currency(); // Currency | 
    try {
      Currency result = apiInstance.currenciesPost(currency);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CurrenciesApi#currenciesPost");
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
| **currency** | [**Currency**](Currency.md)|  | |

### Return type

[**Currency**](Currency.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Créée |  -  |

