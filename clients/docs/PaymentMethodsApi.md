# PaymentMethodsApi

All URIs are relative to *https://api.lacaverne/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**paymentMethodsGet**](PaymentMethodsApi.md#paymentMethodsGet) | **GET** /payment-methods | Liste des méthodes de paiement actives |
| [**paymentMethodsPost**](PaymentMethodsApi.md#paymentMethodsPost) | **POST** /payment-methods | Créer une méthode de paiement (admin) |
| [**paymentMethodsProviderCodeDelete**](PaymentMethodsApi.md#paymentMethodsProviderCodeDelete) | **DELETE** /payment-methods/{provider_code} | Supprimer une méthode de paiement (admin) |
| [**paymentMethodsProviderCodePut**](PaymentMethodsApi.md#paymentMethodsProviderCodePut) | **PUT** /payment-methods/{provider_code} | Modifier une méthode de paiement (admin) |


<a id="paymentMethodsGet"></a>
# **paymentMethodsGet**
> List&lt;PaymentMethod&gt; paymentMethodsGet()

Liste des méthodes de paiement actives

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentMethodsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    PaymentMethodsApi apiInstance = new PaymentMethodsApi(defaultClient);
    try {
      List<PaymentMethod> result = apiInstance.paymentMethodsGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentMethodsApi#paymentMethodsGet");
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

[**List&lt;PaymentMethod&gt;**](PaymentMethod.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="paymentMethodsPost"></a>
# **paymentMethodsPost**
> PaymentMethod paymentMethodsPost(paymentMethodInput)

Créer une méthode de paiement (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentMethodsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    PaymentMethodsApi apiInstance = new PaymentMethodsApi(defaultClient);
    PaymentMethodInput paymentMethodInput = new PaymentMethodInput(); // PaymentMethodInput | 
    try {
      PaymentMethod result = apiInstance.paymentMethodsPost(paymentMethodInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentMethodsApi#paymentMethodsPost");
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
| **paymentMethodInput** | [**PaymentMethodInput**](PaymentMethodInput.md)|  | |

### Return type

[**PaymentMethod**](PaymentMethod.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Créée |  -  |

<a id="paymentMethodsProviderCodeDelete"></a>
# **paymentMethodsProviderCodeDelete**
> paymentMethodsProviderCodeDelete(providerCode)

Supprimer une méthode de paiement (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentMethodsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    PaymentMethodsApi apiInstance = new PaymentMethodsApi(defaultClient);
    String providerCode = "providerCode_example"; // String | 
    try {
      apiInstance.paymentMethodsProviderCodeDelete(providerCode);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentMethodsApi#paymentMethodsProviderCodeDelete");
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
| **providerCode** | **String**|  | |

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

<a id="paymentMethodsProviderCodePut"></a>
# **paymentMethodsProviderCodePut**
> PaymentMethod paymentMethodsProviderCodePut(providerCode, paymentMethodInput)

Modifier une méthode de paiement (admin)

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentMethodsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");
    
    // Configure HTTP bearer authorization: BearerAuth
    HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
    BearerAuth.setBearerToken("BEARER TOKEN");

    PaymentMethodsApi apiInstance = new PaymentMethodsApi(defaultClient);
    String providerCode = "providerCode_example"; // String | 
    PaymentMethodInput paymentMethodInput = new PaymentMethodInput(); // PaymentMethodInput | 
    try {
      PaymentMethod result = apiInstance.paymentMethodsProviderCodePut(providerCode, paymentMethodInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentMethodsApi#paymentMethodsProviderCodePut");
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
| **providerCode** | **String**|  | |
| **paymentMethodInput** | [**PaymentMethodInput**](PaymentMethodInput.md)|  | |

### Return type

[**PaymentMethod**](PaymentMethod.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

