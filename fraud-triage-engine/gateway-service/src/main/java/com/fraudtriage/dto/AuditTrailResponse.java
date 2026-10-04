package com.fraudtriage.dto;

import java.time.Instant;
import java.util.List;

public record AuditTrailResponse(
    Long transactionId, String status, String riskTier, String explanation,
    List<AgentVerdictResponse> agentVerdicts, Instant auditedAt, Long latencyMs
) {}
