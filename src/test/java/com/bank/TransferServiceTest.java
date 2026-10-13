package com.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private TransferService service;

    // Helper: create an Account with a given id and balance
    private Account account(String id, double balance) {
        Account a = new Account(id);
        a.setBalance(balance);
        return a;
    }

    @BeforeEach
    void setUp() {
        service = new TransferService();
    }

    // -----------------------------------------------------------------------
    // getHistoryFor tests
    // -----------------------------------------------------------------------

    @Test
    void getHistoryForReturnsSenderTransactions() {
        Account a1 = account("A1", 100.0);
        Account b1 = account("B1", 0.0);
        service.transfer("A1", "B1", 30.0, a1, b1);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getFromAccountId());
    }

    @Test
    void getHistoryForReturnsReceiverTransactions() {
        Account c1 = account("C1", 100.0);
        Account a1 = account("A1", 0.0);
        service.transfer("C1", "A1", 20.0, c1, a1);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getToAccountId());
    }

    @Test
    void getHistoryForReturnsBothDirections() {
        // T1: A1 -> B1, 30.0
        Account a1 = account("A1", 100.0);
        Account b1 = account("B1", 0.0);
        service.transfer("A1", "B1", 30.0, a1, b1);

        // T2: C1 -> A1, 20.0
        Account c1 = account("C1", 100.0);
        service.transfer("C1", "A1", 20.0, c1, a1);

        // T3: D1 -> E1, 10.0 (unrelated)
        Account d1 = account("D1", 100.0);
        Account e1 = account("E1", 0.0);
        service.transfer("D1", "E1", 10.0, d1, e1);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(2, result.size());
        assertEquals("A1", result.get(0).getFromAccountId()); // T1
        assertEquals("A1", result.get(1).getToAccountId());   // T2
    }

    @Test
    void getHistoryForReturnsEmptyListForAccountWithNoTransfers() {
        // Test 1: empty history
        List<Transaction> result1 = service.getHistoryFor("Z9");
        assertTrue(result1.isEmpty());

        // Test 2: history with unrelated transfers
        Account d1 = account("D1", 100.0);
        Account e1 = account("E1", 0.0);
        service.transfer("D1", "E1", 10.0, d1, e1);

        List<Transaction> result2 = service.getHistoryFor("Z9");
        assertTrue(result2.isEmpty());
    }

    @Test
    void getHistoryForThrowsOnNullOrEmptyAccountId() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
            () -> service.getHistoryFor(null));
        assertEquals("accountId is required", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
            () -> service.getHistoryFor(""));
        assertEquals("accountId is required", ex2.getMessage());
    }

    @Test
    void getHistoryForIsCaseSensitive() {
        Account a1 = account("A1", 100.0);
        Account b1 = account("B1", 0.0);
        service.transfer("A1", "B1", 30.0, a1, b1);

        List<Transaction> resultLower = service.getHistoryFor("a1");
        assertTrue(resultLower.isEmpty());

        List<Transaction> resultUpper = service.getHistoryFor("A1");
        assertEquals(1, resultUpper.size());
    }

    @Test
    void getHistoryForPreservesChronologicalOrder() {
        Account a1 = account("A1", 100.0);
        Account b1 = account("B1", 0.0);
        Account c1 = account("C1", 0.0);

        // T1: A1 -> B1, 10.0
        service.transfer("A1", "B1", 10.0, a1, b1);
        // T2: A1 -> C1, 20.0
        service.transfer("A1", "C1", 20.0, a1, c1);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(2, result.size());
        assertEquals(10.0, result.get(0).getAmount(), 0.001); // T1 first
        assertEquals(20.0, result.get(1).getAmount(), 0.001); // T2 second
    }

    @Test
    void getHistoryForReturnsUnmodifiableList() {
        Account a1 = account("A1", 100.0);
        Account b1 = account("B1", 0.0);
        service.transfer("A1", "B1", 30.0, a1, b1);

        List<Transaction> result = service.getHistoryFor("A1");

        assertThrows(UnsupportedOperationException.class,
            () -> result.add(null));
    }
}
