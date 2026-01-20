package com.paybase.testtask.repository;

import com.paybase.testtask.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByIdempotencyKey(String key);

    List<TransactionEntity> findAllByFromAccountIdOrToAccountIdOrderByCreatedAt(
            Long fromId,
            Long toId
    );

}
