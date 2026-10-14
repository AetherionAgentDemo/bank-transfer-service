package com.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService();
    }

    private void doTransfer(String fromId, String toId, double amount) {
        Account from = new Account(fromId, amount + 1000.0);
        Account to = new Account(toId, 0.0);
        service.transfer(from, to, amount);
    }

    @Test
    void getTotalSentBySumsOnlySenderTransfers() {
        doTransfer("A", "B", 10.0);
        doTransfer("A", "C", 5.0);
        doTransfer("D", "A", 3.0);
        assertEquals(15.0, service.getTotalSentBy("A"), 0.001);
        assertEquals(3.0, service.getTotalSentBy("D"), 0.001);
    }

    @Test
    void getTotalSentByExcludesReceiverOnlyTransfers() {
        doTransfer("A", "B", 10.0);
        doTransfer("A", "C", 5.0);
        assertEquals(0.0, service.getTotalSentBy("B"), 0.001);
    }

    @Test
    void getTotalSentByReturnsZeroForAccountWithNoOutgoingTransfers() {
        // empty history
        assertEquals(0.0, service.getTotalSentBy("UNKNOWN"), 0.001);
        // non-empty history with no UNKNOWN sender
        doTransfer("A", "B", 10.0);
        assertEquals(0.0, service.getTotalSentBy("UNKNOWN"), 0.001);
    }

    @Test
    void getTotalSentByThrowsOnNullOrBlankAccountId() {
        // AC4: null
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalSentBy(null));
        assertEquals("accountId is required", ex1.getMessage());

        // AC4: empty string
        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalSentBy(""));
        assertEquals("accountId is required", ex2.getMessage());

        // AC4: whitespace-only (blank)
        IllegalArgumentException ex3 = assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalSentBy(" "));
        assertEquals("accountId is required", ex3.getMessage());
    }
}
