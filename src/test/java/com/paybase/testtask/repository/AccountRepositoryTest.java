package com.paybase.testtask.repository;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void lockByIdReturnsAccountWithPessimisticLock() {
        AccountEntity account = new AccountEntity();
        account.setMerchantId("merchant-1");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("100.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());

        AccountEntity saved = accountRepository.save(account);

        assertThat(accountRepository.lockById(saved.getId()))
                .isPresent()
                .get()
                .extracting(AccountEntity::getMerchantId)
                .isEqualTo("merchant-1");
    }
}
