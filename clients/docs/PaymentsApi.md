# PaymentsApi

All URIs are relative to *http://localhost:8080/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**ordersIdPaymentsGet**](PaymentsApi.md#ordersIdPaymentsGet) | **GET** /orders/{id}/payments | Paiements associés à une commande |
| [**ordersIdPaymentsPost**](PaymentsApi.md#ordersIdPaymentsPost) | **POST** /orders/{id}/payments | Initier un paiement |
| [**paymentMethodsGet**](PaymentsApi.md#paymentMethodsGet) | **GET** /payment-methods | Liste des moyens de paiement disponibles |
| [**paymentsWebhooksStripePost**](PaymentsApi.md#paymentsWebhooksStripePost) | **POST** /payments/webhooks/stripe | Webhook Stripe Checkout |


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
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
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
| **id** | **UUID**|  | |

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
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    UUID id = UUID.randomUUID(); // UUID | 
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
| **id** | **UUID**|  | |
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
    defaultClient.setBasePath("http://localhost:8080/api/v1");

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

<a id="paymentsWebhooksStripePost"></a>
# **paymentsWebhooksStripePost**
> paymentsWebhooksStripePost(stripeSignature, stripeEvent)

Webhook Stripe Checkout

Endpoint invoqué par Stripe. Le corps est un objet Event Stripe brut (https://stripe.com/docs/api/events/object). Les requêtes doivent porter l&#39;en-tête &#x60;Stripe-Signature&#x60; ; il est vérifié contre &#x60;stripe.webhook-secret&#x60;. 

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
    defaultClient.setBasePath("http://localhost:8080/api/v1");

    PaymentsApi apiInstance = new PaymentsApi(defaultClient);
    String stripeSignature = "stripeSignature_example"; // String | Signature Stripe au format `t=<timestamp>,v1=<hmac_sha256>`. Générée par Stripe à partir du secret de webhook configuré. 
    StripeEvent stripeEvent = new StripeEvent(); // StripeEvent | 
    try {
      apiInstance.paymentsWebhooksStripePost(stripeSignature, stripeEvent);
    } catch (ApiException e) {
      System.err.println("Exception when calling PaymentsApi#paymentsWebhooksStripePost");
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
| **stripeSignature** | **String**| Signature Stripe au format &#x60;t&#x3D;&lt;timestamp&gt;,v1&#x3D;&lt;hmac_sha256&gt;&#x60;. Générée par Stripe à partir du secret de webhook configuré.  | |
| **stripeEvent** | [**StripeEvent**](StripeEvent.md)|  | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Webhook traité |  -  |
| **404** | Ressource introuvable |  -  |
| **422** | Données invalides |  -  |
| **500** | Erreur interne lors du traitement |  -  |
| **503** | Webhook Stripe non configuré |  -  |

