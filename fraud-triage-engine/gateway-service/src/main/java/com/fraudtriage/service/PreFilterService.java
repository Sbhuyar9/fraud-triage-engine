package com.fraudtriage.service;

import com.fraudtriage.dto.TransactionRequest;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class PreFilterService {
    private static final Set<String> HIGH_RISK = Set.of("AE","IR","KP","SY");

    public boolean shouldOrchestrate(TransactionRequest tx) {
        if (tx.amount().doubleValue() >= 10000) return true;
        if (tx.recentTransactionCount10m() != null && tx.recentTransactionCount10m() >= 3) return true;
        return tx.country() != null && HIGH_RISK.contains(tx.country().toUpperCase());
    }
}
