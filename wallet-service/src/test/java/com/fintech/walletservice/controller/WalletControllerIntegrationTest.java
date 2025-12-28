package com.fintech.walletservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.walletservice.config.TestcontainersConfiguration;
import com.fintech.walletservice.domain.Wallet;
import com.fintech.walletservice.dto.request.CreateWalletRequest;
import com.fintech.walletservice.dto.request.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateWalletViaAPI() throws Exception {
        var request = new CreateWalletRequest(
                UUID.randomUUID(),
                Wallet.Currency.USD
        );

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void shouldDepositViaAPI() throws Exception {
        // Create wallet first
        var createRequest = new CreateWalletRequest(
                UUID.randomUUID(),
                Wallet.Currency.EUR
        );

        String walletResponse = mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID walletId = UUID.fromString(objectMapper.readTree(walletResponse).get("id").asText());

        // Deposit
        var depositRequest = new TransactionRequest(
                50000L,
                "deposit-api-test-" + UUID.randomUUID(),
                "Test deposit"
        );

        mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.balanceAfter").value(50000));
    }
}
