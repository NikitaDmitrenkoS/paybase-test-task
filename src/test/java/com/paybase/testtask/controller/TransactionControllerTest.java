package com.paybase.testtask.controller;

import com.paybase.testtask.exceptions.ApiExceptionHandler;
import com.paybase.testtask.exceptions.InsufficientFundsException;
import com.paybase.testtask.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(ApiExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void createReturnsValidationErrorForNegativeAmount() throws Exception {
        String body = """
                {
                  "idempotencyKey": "bad-amount",
                  "type": "DEPOSIT",
                  "toAccountId": 1,
                  "amount": 0,
                  "currency": "USD",
                  "reference": "invalid"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsConflictOnInsufficientFunds() throws Exception {
        when(transactionService.create(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InsufficientFundsException());

        String body = """
                {
                  "idempotencyKey": "key-1",
                  "type": "WITHDRAWAL",
                  "fromAccountId": 1,
                  "amount": 10.00,
                  "currency": "USD",
                  "reference": "withdraw"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().string("Insufficient funds"));
    }
}
