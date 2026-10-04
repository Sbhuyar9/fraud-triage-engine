package com.fraudtriage.entity;

public enum TransactionStatus {
    PENDING, APPROVED, FROZEN, MANUAL_REVIEW;

    /**
     * The agent-orchestrator (Python) returns action codes APPROVE / FREEZE / MANUAL_REVIEW.
     * Those are not the same strings as this enum's constant names (APPROVED / FROZEN /
     * MANUAL_REVIEW), so a plain valueOf() on the raw agent response throws for two of the
     * three possible outcomes. This translates the agent's action vocabulary into our
     * persistence vocabulary.
     */
    public static TransactionStatus fromAgentAction(String action) {
        if (action == null) {
            throw new IllegalArgumentException("Agent action was null");
        }
        return switch (action) {
            case "APPROVE" -> APPROVED;
            case "FREEZE" -> FROZEN;
            case "MANUAL_REVIEW" -> MANUAL_REVIEW;
            default -> throw new IllegalArgumentException("Unknown agent action: " + action);
        };
    }
}
