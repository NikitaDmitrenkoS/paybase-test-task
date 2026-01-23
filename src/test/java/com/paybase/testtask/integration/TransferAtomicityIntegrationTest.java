package com.paybase.testtask.integration;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.exceptions.InsufficientFundsException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class TransferAtomicityIntegrationTest {

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
    void transferUpdatesBothAccountsAtomically() {
        AccountEntity from = accountService.create(new CreateAccountRequest(
                "merchant-a",
                "USD",
                new BigDecimal("300.00")
        ));
        AccountEntity to = accountService.create(new CreateAccountRequest(
                "merchant-b",
                "USD",
                new BigDecimal("0.00")
        ));

        TransactionRequest request = new TransactionRequest(
                "transfer-1",
                TransactionType.TRANSFER,
                from.getId(),
                to.getId(),
                new BigDecimal("300.00"),
                "USD",
                "transfer"
        );

        TransactionEntity tx = transactionService.create(request);

        AccountEntity reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        AccountEntity reloadedTo = accountRepository.findById(to.getId()).orElseThrow();

        assertThat(reloadedFrom.getBalance()).isEqualByComparingTo("0.00");
        assertThat(reloadedTo.getBalance()).isEqualByComparingTo("300.00");
        assertThat(tx.getFromBalanceAfter()).isEqualByComparingTo("0.00");
        assertThat(tx.getToBalanceAfter()).isEqualByComparingTo("300.00");
    }

    @Test
    void transferRollsBackWhenInsufficientFunds() {
        AccountEntity from = accountService.create(new CreateAccountRequest(
                "merchant-c",
                "USD",
                new BigDecimal("100.00")
        ));
        AccountEntity to = accountService.create(new CreateAccountRequest(
                "merchant-d",
                "USD",
                new BigDecimal("50.00")
        ));

        TransactionRequest request = new TransactionRequest(
                "transfer-2",
                TransactionType.TRANSFER,
                from.getId(),
                to.getId(),
                new BigDecimal("300.00"),
                "USD",
                "transfer"
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InsufficientFundsException.class);

        AccountEntity reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        AccountEntity reloadedTo = accountRepository.findById(to.getId()).orElseThrow();
        List<TransactionEntity> transactions = transactionRepository.findAll();

        assertThat(reloadedFrom.getBalance()).isEqualByComparingTo("100.00");
        assertThat(reloadedTo.getBalance()).isEqualByComparingTo("50.00");
        assertThat(transactions).isEmpty();
    }
}
