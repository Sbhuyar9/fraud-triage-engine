package com.fraudtriage.service;

import com.fraudtriage.dto.TransactionRequest;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PreFilterServiceTest {
    private final PreFilterService service = new PreFilterService();

    private TransactionRequest tx(double amount, String country, int velocity) {
        return new TransactionRequest("T","A",BigDecimal.valueOf(amount),"USD","M",country,"127.0.0.1",velocity,"US");
    }

    @Test void highAmountIsFlagged(){ assertTrue(service.shouldOrchestrate(tx(10000,"US",0))); }
    @Test void normalTransactionIsNotFlagged(){ assertFalse(service.shouldOrchestrate(tx(20,"US",0))); }
    @Test void velocityIsFlagged(){ assertTrue(service.shouldOrchestrate(tx(20,"US",3))); }
}
