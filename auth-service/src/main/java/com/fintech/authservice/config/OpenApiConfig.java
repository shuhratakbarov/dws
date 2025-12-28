package com.fintech.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Auth Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("""
                                ## Authentication Service
                                
                                Handles user registration, login, and JWT token management.
                                
                                ## Endpoints
                                - **POST /api/v1/auth/register** - Create new user account
                                - **POST /api/v1/auth/login** - Authenticate and get tokens
                                - **POST /api/v1/auth/refresh** - Refresh access token
                                - **POST /api/v1/auth/logout** - Revoke tokens
                                - **GET /api/v1/auth/validate** - Validate access token
                                
                                ## Token Usage
                                Include the access token in the Authorization header:
                                ```
                                Authorization: Bearer <access_token>
                                ```
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Team")
                                .email("api@fintech.example.com")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Auth Service")
                ));
    }
}

