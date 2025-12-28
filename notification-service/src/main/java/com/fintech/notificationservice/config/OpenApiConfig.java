package com.fintech.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("""
                                ## Multi-Channel Notification Service
                                
                                Sends notifications through multiple channels:
                                - **EMAIL**: Transaction receipts, statements
                                - **SMS**: OTP codes, urgent alerts
                                - **PUSH**: Real-time mobile notifications
                                
                                ### Features:
                                - Template-based content
                                - User preferences (opt-in/out)
                                - Delivery tracking and retry
                                - Notification history
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Team")
                                .email("api@fintech.example.com")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway"),
                        new Server().url("http://localhost:8085").description("Direct Access")
                ));
    }
}

