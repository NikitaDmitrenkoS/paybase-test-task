package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.exceptions.InsufficientFundsException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createReturnsExistingTransactionForIdempotencyKey() {
        TransactionEntity existing = new TransactionEntity();
        existing.setId(42L);

        TransactionRequest request = new TransactionRequest(
                "idempotent-1",
                TransactionType.DEPOSIT,
                null,
                10L,
                new BigDecimal("100.00"),
                "USD",
                "deposit"
        );

        when(transactionRepository.findByIdempotencyKey("idempotent-1"))
                .thenReturn(Optional.of(existing));

        TransactionEntity result = transactionService.create(request);

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).lockById(org.mockito.ArgumentMatchers.any());
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void depositUpdatesBalanceAndPersistsTransaction() {
        AccountEntity account = new AccountEntity();
        account.setId(1L);
        account.setBalance(new BigDecimal("50.00"));

        TransactionRequest request = new TransactionRequest(
                "dep-1",
                TransactionType.DEPOSIT,
                null,
                1L,
                new BigDecimal("25.00"),
                "USD",
                "deposit"
        );

        when(transactionRepository.findByIdempotencyKey("dep-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.lockById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionEntity result = transactionService.create(request);

        assertThat(account.getBalance()).isEqualByComparingTo("75.00");
        assertThat(result.getToAccountId()).isEqualTo(1L);
        assertThat(result.getToBalanceBefore()).isEqualByComparingTo("50.00");
        assertThat(result.getToBalanceAfter()).isEqualByComparingTo("75.00");
        assertThat(result.getFromBalanceBefore()).isNull();
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        AccountEntity account = new AccountEntity();
        account.setId(2L);
        account.setBalance(new BigDecimal("10.00"));

        TransactionRequest request = new TransactionRequest(
                "wd-1",
                TransactionType.WITHDRAWAL,
                2L,
                null,
                new BigDecimal("25.00"),
                "USD",
                "withdraw"
        );

        when(transactionRepository.findByIdempotencyKey("wd-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void transferUpdatesBothAccountsAndPersistsTransaction() {
        AccountEntity from = new AccountEntity();
        from.setId(1L);
        from.setBalance(new BigDecimal("100.00"));

        AccountEntity to = new AccountEntity();
        to.setId(2L);
        to.setBalance(new BigDecimal("50.00"));

        TransactionRequest request = new TransactionRequest(
                "tr-1",
                TransactionType.TRANSFER,
                1L,
                2L,
                new BigDecimal("30.00"),
                "USD",
                "transfer"
        );

        when(transactionRepository.findByIdempotencyKey("tr-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.lockById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(to));
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionEntity result = transactionService.create(request);

        assertThat(from.getBalance()).isEqualByComparingTo("70.00");
        assertThat(to.getBalance()).isEqualByComparingTo("80.00");

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());
        TransactionEntity saved = captor.getValue();

        assertThat(saved.getFromBalanceBefore()).isEqualByComparingTo("100.00");
        assertThat(saved.getFromBalanceAfter()).isEqualByComparingTo("70.00");
        assertThat(saved.getToBalanceBefore()).isEqualByComparingTo("50.00");
        assertThat(saved.getToBalanceAfter()).isEqualByComparingTo("80.00");
        assertThat(result).isSameAs(saved);
    }
}
