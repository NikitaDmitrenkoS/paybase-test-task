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
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // или NONE, если Testcontainers

class AccountStatementIntegrationTest {

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
    void statementIncludesAuditTrailWithBalanceBeforeAfter() {
        AccountEntity account = accountService.create(new CreateAccountRequest(
                "merchant-st",
                "USD",
                new BigDecimal("0.00")
        ));

        transactionService.create(new TransactionRequest(
                "st-dep",
                TransactionType.DEPOSIT,
                null,
                account.getId(),
                new BigDecimal("100.00"),
                "USD",
                "deposit"
        ));
        transactionService.create(new TransactionRequest(
                "st-wd",
                TransactionType.WITHDRAWAL,
                account.getId(),
                null,
                new BigDecimal("40.00"),
                "USD",
                "withdraw"
        ));

        List<TransactionEntity> statement = accountService.statement(account.getId());

        assertThat(statement).hasSize(2);
        TransactionEntity deposit = statement.get(0);
        TransactionEntity withdrawal = statement.get(1);

        assertThat(deposit.getToBalanceBefore()).isEqualByComparingTo("0.00");
        assertThat(deposit.getToBalanceAfter()).isEqualByComparingTo("100.00");
        assertThat(withdrawal.getFromBalanceBefore()).isEqualByComparingTo("100.00");
        assertThat(withdrawal.getFromBalanceAfter()).isEqualByComparingTo("60.00");
    }
}
