package com.fraudtriage.dto;

import java.util.List;

public record OrchestrationResponse(
    String transactionId, String status, String riskTier, String explanation,
    Double confidence, List<AgentVerdictResponse> agentVerdicts
) {}
