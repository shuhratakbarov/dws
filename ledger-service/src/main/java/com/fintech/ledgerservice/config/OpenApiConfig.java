package com.fintech.ledgerservice.config;

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
    public OpenAPI ledgerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ledger Service API")
                        .description("""
                                ## Immutable Transaction Ledger
                                
                                This service maintains an **append-only** record of all financial transactions.
                                
                                ### Key Principles:
                                - Ledger entries are **NEVER modified or deleted**
                                - Every transaction creates immutable audit trail
                                - Double-entry: transfers create TWO entries (DEBIT + CREDIT)
                                - Balance can be recalculated from ledger at any time
                                
                                ### Entry Types:
                                - **CREDIT**: Money coming into wallet
                                - **DEBIT**: Money going out of wallet
                                
                                ### Transaction Types:
                                - DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, FEE, REFUND, ADJUSTMENT
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Team")
                                .email("api@fintech.example.com")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway"),
                        new Server().url("http://localhost:8084").description("Direct Access")
                ));
    }
}

