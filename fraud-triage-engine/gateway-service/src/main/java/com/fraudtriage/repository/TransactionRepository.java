package com.fraudtriage.repository;

import com.fraudtriage.entity.Transaction;
import com.fraudtriage.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByExternalRef(String externalRef);
    List<Transaction> findByStatus(TransactionStatus status);
}
