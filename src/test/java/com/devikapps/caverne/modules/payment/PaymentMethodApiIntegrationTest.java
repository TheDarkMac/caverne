package com.devikapps.caverne.modules.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.caverne.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "stripe.enabled=false")
class PaymentMethodApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldExposeManualPaymentMethod() throws Exception {
    mockMvc
        .perform(get("/payment-methods"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].provider_code").value("MANUAL"))
        .andExpect(jsonPath("$[0].requires_manual_check").value(true))
        .andExpect(jsonPath("$[0].integration_type").value("manual"));
  }
}
