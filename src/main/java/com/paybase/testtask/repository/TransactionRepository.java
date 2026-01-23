package com.paybase.testtask.repository;

import com.paybase.testtask.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByIdempotencyKey(String key);

    @Query("""
            select t from TransactionEntity t
            where (t.fromAccountId = :accountId or t.toAccountId = :accountId)
              and (:from is null or t.createdAt >= :from)
              and (:to is null or t.createdAt < :to)
            order by t.createdAt
            """)
    List<TransactionEntity> findStatement(
            Long accountId,
            Instant from,
            Instant to
    );

}
