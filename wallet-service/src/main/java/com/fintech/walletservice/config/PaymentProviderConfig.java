package com.fintech.walletservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for payment providers.
 * Values are loaded from application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "payment.providers")
@Data
public class PaymentProviderConfig {

    private PaymeConfig payme = new PaymeConfig();
    private ClickConfig click = new ClickConfig();
    private StripeConfig stripe = new StripeConfig();

    /**
     * Payme (Uzbekistan) payment provider configuration.
     */
    @Data
    public static class PaymeConfig {
        /**
         * Merchant ID from Payme dashboard.
         */
        private String merchantId = "mock";

        /**
         * Secret key for API authentication.
         */
        private String secretKey = "mock";

        /**
         * Payme API endpoint.
         */
        private String apiUrl = "https://checkout.paycom.uz/api";

        /**
         * Webhook secret for signature verification.
         */
        private String webhookSecret = "mock";
    }

    /**
     * Click (Uzbekistan) payment provider configuration.
     */
    @Data
    public static class ClickConfig {
        /**
         * Merchant ID from Click dashboard.
         */
        private String merchantId = "mock";

        /**
         * Service ID for this application.
         */
        private String serviceId = "mock";

        /**
         * Merchant user ID.
         */
        private String merchantUserId = "mock";

        /**
         * Secret key for API authentication.
         */
        private String secretKey = "mock";

        /**
         * Click API endpoint.
         */
        private String apiUrl = "https://api.click.uz/v2";
    }

    /**
     * Stripe (International) payment provider configuration.
     */
    @Data
    public static class StripeConfig {
        /**
         * Stripe secret key (sk_xxx).
         */
        private String secretKey = "mock";

        /**
         * Stripe publishable key (pk_xxx) - for frontend.
         */
        private String publishableKey = "mock";

        /**
         * Webhook signing secret (whsec_xxx).
         */
        private String webhookSecret = "mock";

        /**
         * Stripe API endpoint.
         */
        private String apiUrl = "https://api.stripe.com/v1";
    }
}
