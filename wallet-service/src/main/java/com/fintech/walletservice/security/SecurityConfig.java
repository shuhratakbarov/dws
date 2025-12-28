package com.fintech.walletservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration for Wallet Service.
 * Registers the Gateway authentication filter.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<GatewayAuthenticationFilter> gatewayAuthFilter() {
        FilterRegistrationBean<GatewayAuthenticationFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(gatewayAuthenticationFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // Run early in the filter chain

        return registrationBean;
    }
}

