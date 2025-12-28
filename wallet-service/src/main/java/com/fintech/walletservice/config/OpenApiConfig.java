package com.fintech.walletservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Wallet Service.
 *
 * Access the API documentation at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * - OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:wallet-service}")
    private String applicationName;

    @Bean
    public OpenAPI walletServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway (Recommended)"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("Direct Access (Dev Only)")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Wallets")
                                .description("Wallet management operations - create, view, deposit, withdraw, transfer"),
                        new Tag()
                                .name("Admin")
                                .description("Administrative operations - reconciliation, system health")
                ))
                // Prepare for future authentication
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token authentication (coming soon)")
                        )
                );
                // Uncomment when auth is added:
                // .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Digital Wallet Service API")
                .description("""
                        ## Overview
                        The Wallet Service manages digital wallets for users. Each user can have multiple wallets 
                        in different currencies (USD, EUR, UZS).
                        
                        ## Key Features
                        - **Multi-currency support**: USD, EUR, UZS
                        - **Atomic transfers**: Money never gets lost during transfers
                        - **Idempotency**: Safe to retry failed requests
                        - **Audit trail**: All transactions are recorded in an immutable ledger
                        
                        ## Money Format
                        All amounts are in **minor units** (cents/tiyin):
                        - `1050` = $10.50 USD
                        - `100` = €1.00 EUR
                        - `1000` = 10 UZS
                        
                        ## Error Codes
                        | HTTP Status | Meaning |
                        |-------------|---------|
                        | 400 | Bad request (validation error) |
                        | 404 | Wallet not found |
                        | 409 | Conflict (duplicate wallet, insufficient funds) |
                        | 422 | Business rule violation |
                        | 500 | Internal server error |
                        
                        ## Idempotency
                        Use the `idempotencyKey` field to safely retry requests. 
                        The same key will return the original response without re-executing.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Fintech Team")
                        .email("api@fintech.example.com")
                        .url("https://github.com/fintech/wallet-service")
                )
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT")
                );
    }
}

