package com.devikapps.caverne.config;

import com.devikapps.caverne.modules.common.logging.RequestTraceFilter;
import com.devikapps.caverne.modules.user.AuthBearerFilter;
import com.devikapps.caverne.modules.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

  private final ObjectMapper objectMapper;
  private final CorsProperties corsProperties;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AuthBearerFilter authBearerFilter)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(accessDeniedHandler()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/orders/all")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.PUT, "/orders/*/status")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.GET, "/categories", "/categories/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/products", "/products/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/delivery-costs", "/delivery-costs/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/delivery-costs")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/payments/webhooks/stripe")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/orders")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/orders/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/orders/*/cancel")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/orders/*/payments")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/orders/*/payments")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/payment-methods")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/logout")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/users/me")
                    .authenticated()
                    .requestMatchers(HttpMethod.PUT, "/users/me")
                    .authenticated()
                    .requestMatchers("/users/me/addresses/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/orders")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/users")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/users")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.GET, "/users/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.DELETE, "/users/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/categories")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.PUT, "/categories/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.DELETE, "/categories/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/products")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.PUT, "/products/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.DELETE, "/products/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.GET, "/products/*/stock")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.PUT, "/products/*/stock")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.GET, "/products/*/stock/movements")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.GET, "/products/*/images")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/products/*/images")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.DELETE, "/products/*/images/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.PUT, "/products/*/images/*/main")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/payments/*/refund")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.POST, "/auth/webhooks/supabase")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, "/delivery-costs/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .requestMatchers(HttpMethod.DELETE, "/delivery-costs/*")
                    .hasAuthority(roleAuthority(UserRole.ADMIN))
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(authBearerFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(corsProperties.getAllowedMethods());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
    configuration.setExposedHeaders(corsProperties.getExposedHeaders());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getWriter(),
          new com.devikapps.caverne.modules.common.ApiError(
              HttpStatus.FORBIDDEN.value(),
              "Access is forbidden",
              MDC.get(RequestTraceFilter.MDC_TRACE_ID),
              Instant.now()));
    };
  }

  private static String roleAuthority(UserRole role) {
    return "ROLE_" + role.name();
  }
}
