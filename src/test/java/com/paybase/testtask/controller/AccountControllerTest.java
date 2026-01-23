package com.paybase.testtask.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createAccountReturnsPersistedAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                "merchant-123",
                "USD",
                new BigDecimal("125.50")
        );

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.merchantId").value("merchant-123"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(125.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void balanceEndpointReturnsCurrentBalance() throws Exception {
        AccountEntity account = new AccountEntity();
        account.setMerchantId("merchant-balance");
        account.setCurrency("EUR");
        account.setBalance(new BigDecimal("99.99"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        account = accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/{id}/balance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.balance").value(99.99))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.lastUpdated", notNullValue()));
    }

    @Test
    void statementEndpointReturnsTransactionsForAccount() throws Exception {
        AccountEntity account = new AccountEntity();
        account.setMerchantId("merchant-statement");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        account = accountRepository.save(account);

        TransactionEntity first = new TransactionEntity();
        first.setIdempotencyKey("tx-1");
        first.setType(TransactionType.DEPOSIT);
        first.setToAccountId(account.getId());
        first.setAmount(new BigDecimal("50.00"));
        first.setCurrency("USD");
        first.setStatus("COMPLETED");
        first.setCreatedAt(Instant.parse("2024-01-02T10:00:00Z"));
        transactionRepository.save(first);

        TransactionEntity second = new TransactionEntity();
        second.setIdempotencyKey("tx-2");
        second.setType(TransactionType.WITHDRAWAL);
        second.setFromAccountId(account.getId());
        second.setAmount(new BigDecimal("25.00"));
        second.setCurrency("USD");
        second.setStatus("COMPLETED");
        second.setCreatedAt(Instant.parse("2024-01-03T10:00:00Z"));
        transactionRepository.save(second);

        mockMvc.perform(get("/api/accounts/{id}/statement", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idempotencyKey").value("tx-1"))
                .andExpect(jsonPath("$[1].idempotencyKey").value("tx-2"));
    }
}