package com.fraudtriage.dto;

public record TransactionResponse(
    Long transactionId, String status, String riskTier, String explanation, String auditTrailUrl
) {}
