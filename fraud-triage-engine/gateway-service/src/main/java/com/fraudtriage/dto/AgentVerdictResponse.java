package com.fraudtriage.dto;

public record AgentVerdictResponse(String agentName, String reasoning, String riskSignal, Double confidence) {}
