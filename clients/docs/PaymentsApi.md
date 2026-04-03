# PaymentsApi

All URIs are relative to *https://api.lacaverne/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**ordersIdPaymentsGet**](PaymentsApi.md#ordersIdPaymentsGet) | **GET** /orders/{id}/payments | Paiements associés à une commande |
| [**ordersIdPaymentsPost**](PaymentsApi.md#ordersIdPaymentsPost) | **POST** /orders/{id}/payments | Initier un paiement |
| [**paymentMethodsGet**](PaymentsApi.md#paymentMethodsGet) | **GET** /payment-methods | Liste des moyens de paiement disponibles |


<a id="ordersIdPaymentsGet"></a>
# **ordersIdPaymentsGet**
> List&lt;Payment&gt; ordersIdPaymentsGet(id)

Paiements associés à une commande

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    Integer id = 56; // Integer | 
    try {
      List<Payment> result = apiInstance.ordersIdPaymentsGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentsApi#ordersIdPaymentsGet");
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

[**List&lt;Payment&gt;**](Payment.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="ordersIdPaymentsPost"></a>
# **ordersIdPaymentsPost**
> Payment ordersIdPaymentsPost(id, paymentInput)

Initier un paiement

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    Integer id = 56; // Integer | 
    PaymentInput paymentInput = new PaymentInput(); // PaymentInput | 
    try {
      Payment result = apiInstance.ordersIdPaymentsPost(id, paymentInput);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentsApi#ordersIdPaymentsPost");
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
| **paymentInput** | [**PaymentInput**](PaymentInput.md)|  | |

### Return type

[**Payment**](Payment.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Paiement initié |  -  |
| **422** | Données invalides |  -  |

<a id="paymentMethodsGet"></a>
# **paymentMethodsGet**
> List&lt;PaymentMethod&gt; paymentMethodsGet()

Liste des moyens de paiement disponibles

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.PaymentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.lacaverne/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    try {
      List<PaymentMethod> result = apiInstance.paymentMethodsGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentsApi#paymentMethodsGet");
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
| **200** | Moyens disponibles |  -  |

