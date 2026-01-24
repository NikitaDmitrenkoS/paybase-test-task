package com.paybase.testtask.controller;

import com.paybase.testtask.exceptions.ApiExceptionHandler;
import com.paybase.testtask.exceptions.InsufficientFundsException;
import com.paybase.testtask.exceptions.NotFoundException;
import com.paybase.testtask.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(ApiExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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

    @Test
    void getReturnsNotFoundWhenMissing() throws Exception {
        when(transactionService.get(99L))
                .thenThrow(new NotFoundException());

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found"));
    }
}
