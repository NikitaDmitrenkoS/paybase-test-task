package com.paybase.testtask.repository;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByIdempotencyKeyReturnsStoredTransaction() {
        AccountEntity account = new AccountEntity();
        account.setMerchantId("merchant-1");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("100.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        AccountEntity savedAccount = accountRepository.save(account);

        TransactionEntity tx = new TransactionEntity();
        tx.setIdempotencyKey("key-1");
        tx.setType(TransactionType.DEPOSIT);
        tx.setToAccountId(savedAccount.getId());
        tx.setAmount(new BigDecimal("25.00"));
        tx.setCurrency("USD");
        tx.setStatus("COMPLETED");
        tx.setToBalanceBefore(new BigDecimal("100.00"));
        tx.setToBalanceAfter(new BigDecimal("125.00"));
        tx.setCreatedAt(Instant.now());

        transactionRepository.save(tx);

        assertThat(transactionRepository.findByIdempotencyKey("key-1"))
                .isPresent()
                .get()
                .extracting(TransactionEntity::getStatus)
                .isEqualTo("COMPLETED");
    }

    @Test
    void findAllByFromOrToAccountReturnsOrderedTransactions() {
        AccountEntity account = new AccountEntity();
        account.setMerchantId("merchant-2");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("200.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        AccountEntity savedAccount = accountRepository.save(account);

        TransactionEntity first = new TransactionEntity();
        first.setIdempotencyKey("key-2");
        first.setType(TransactionType.WITHDRAWAL);
        first.setFromAccountId(savedAccount.getId());
        first.setAmount(new BigDecimal("10.00"));
        first.setCurrency("USD");
        first.setStatus("COMPLETED");
        first.setFromBalanceBefore(new BigDecimal("200.00"));
        first.setFromBalanceAfter(new BigDecimal("190.00"));
        first.setCreatedAt(Instant.now().minusSeconds(60));

        TransactionEntity second = new TransactionEntity();
        second.setIdempotencyKey("key-3");
        second.setType(TransactionType.WITHDRAWAL);
        second.setFromAccountId(savedAccount.getId());
        second.setAmount(new BigDecimal("5.00"));
        second.setCurrency("USD");
        second.setStatus("COMPLETED");
        second.setFromBalanceBefore(new BigDecimal("190.00"));
        second.setFromBalanceAfter(new BigDecimal("185.00"));
        second.setCreatedAt(Instant.now());

        transactionRepository.saveAll(List.of(second, first));

        List<TransactionEntity> results =
                transactionRepository.findAllByFromAccountIdOrToAccountIdOrderByCreatedAt(
                        savedAccount.getId(),
                        savedAccount.getId()
                );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getIdempotencyKey()).isEqualTo("key-2");
        assertThat(results.get(1).getIdempotencyKey()).isEqualTo("key-3");
    }
}
