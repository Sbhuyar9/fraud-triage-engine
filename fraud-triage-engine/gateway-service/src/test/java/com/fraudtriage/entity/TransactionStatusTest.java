package com.fraudtriage.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStatusTest {

    @Test
    void mapsAllAgentActionsTheOrchestratorCanReturn() {
        // The agent-orchestrator (Python) returns APPROVE / FREEZE / MANUAL_REVIEW, which are
        // NOT the same strings as this enum's constant names. A plain valueOf() on those values
        // throws IllegalArgumentException for APPROVE and FREEZE, which used to crash every
        // transaction submission that resulted in an approval or a freeze.
        assertEquals(TransactionStatus.APPROVED, TransactionStatus.fromAgentAction("APPROVE"));
        assertEquals(TransactionStatus.FROZEN, TransactionStatus.fromAgentAction("FREEZE"));
        assertEquals(TransactionStatus.MANUAL_REVIEW, TransactionStatus.fromAgentAction("MANUAL_REVIEW"));
    }

    @Test
    void rejectsUnknownAction() {
        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.fromAgentAction("SOMETHING_ELSE"));
    }
}
