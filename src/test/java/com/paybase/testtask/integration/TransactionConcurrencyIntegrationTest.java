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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class TransactionConcurrencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @AfterEach
    void shutdownExecutor() {
        executorService.shutdownNow();
    }

    @Test
    void concurrentWithdrawalsRespectBalanceAndAuditTrail() throws Exception {
        AccountEntity account = accountService.create(new CreateAccountRequest(
                "merchant-cc",
                "USD",
                new BigDecimal("1000.00")
        ));

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String key = "wd-" + i;
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    TransactionRequest request = new TransactionRequest(
                            key,
                            TransactionType.WITHDRAWAL,
                            account.getId(),
                            null,
                            new BigDecimal("200.00"),
                            "USD",
                            "concurrent-withdraw"
                    );
                    transactionService.create(request);
                    return true;
                } catch (InsufficientFundsException ex) {
                    return false;
                }
            }));
        }

        assertThat(ready.await(Duration.ofSeconds(5))).isTrue();
        start.countDown();

        int success = 0;
        int failures = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    success++;
                } else {
                    failures++;
                }
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof InsufficientFundsException) {
                    failures++;
                } else {
                    throw ex;
                }
            }
        }

        AccountEntity reloaded = accountRepository.findById(account.getId()).orElseThrow();
        List<TransactionEntity> transactions = transactionRepository.findAll();

        assertThat(success).isEqualTo(5);
        assertThat(failures).isEqualTo(5);
        assertThat(reloaded.getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactions).hasSize(5);
        assertThat(transactions).allSatisfy(tx -> {
            assertThat(tx.getFromBalanceBefore()).isNotNull();
            assertThat(tx.getFromBalanceAfter()).isNotNull();
            assertThat(tx.getFromBalanceAfter())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(tx.getAmount()).isEqualByComparingTo("200.00");
        });
    }
}
