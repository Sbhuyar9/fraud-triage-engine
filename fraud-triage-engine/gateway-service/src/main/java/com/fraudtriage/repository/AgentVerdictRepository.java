package com.fraudtriage.repository;

import com.fraudtriage.entity.AgentVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentVerdictRepository extends JpaRepository<AgentVerdict, Long> {
    List<AgentVerdict> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}
