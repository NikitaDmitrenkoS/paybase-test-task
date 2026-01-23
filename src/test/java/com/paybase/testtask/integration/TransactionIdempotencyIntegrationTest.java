package com.paybase.testtask.integration;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import com.paybase.testtask.service.AccountService;
import com.paybase.testtask.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class TransactionIdempotencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void idempotentDepositReturnsSameTransactionAndBalance() {
        AccountEntity account = accountService.create(new CreateAccountRequest(
                "merchant-ido",
                "USD",
                new BigDecimal("0.00")
        ));

        TransactionRequest request = new TransactionRequest(
                "key-123",
                TransactionType.DEPOSIT,
                null,
                account.getId(),
                new BigDecimal("500.00"),
                "USD",
                "deposit"
        );

        TransactionEntity first = transactionService.create(request);
        TransactionEntity second = transactionService.create(request);

        AccountEntity reloaded = accountRepository.findById(account.getId()).orElseThrow();
        List<TransactionEntity> transactions = transactionRepository.findAll();

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(reloaded.getBalance()).isEqualByComparingTo("500.00");
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getToBalanceAfter())
                .isEqualByComparingTo("500.00");
    }
}
