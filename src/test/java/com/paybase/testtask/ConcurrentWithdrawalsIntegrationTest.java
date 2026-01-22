package com.paybase.testtask;

import com.paybase.testtask.domain.TransactionType;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.exceptions.InsufficientFundsException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import com.paybase.testtask.service.AccountService;
import com.paybase.testtask.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConcurrentWithdrawalsIntegrationTest {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    ConcurrentWithdrawalsIntegrationTest(
            AccountService accountService,
            TransactionService transactionService,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Test
    void concurrentWithdrawalsOnlyAllowAvailableFunds() throws Exception {
        var account = accountService.create(new CreateAccountRequest(
                "merchant-001",
                "USD",
                new BigDecimal("1000.00")
        ));

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                TransactionRequest request = new TransactionRequest(
                        UUID.randomUUID().toString(),
                        TransactionType.WITHDRAWAL,
                        account.getId(),
                        null,
                        new BigDecimal("200.00"),
                        "USD",
                        "Concurrent withdrawal"
                );
                try {
                    transactionService.create(request);
                    success.incrementAndGet();
                } catch (InsufficientFundsException ex) {
                    failures.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        var refreshed = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(success.get()).isEqualTo(5);
        assertThat(failures.get()).isEqualTo(5);
        assertThat(refreshed.getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isEqualTo(5);
    }
}
