package com.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private TransferService service;

    // Simple in-memory Account stub used across all tests
    private Account makeAccount(double initialBalance) {
        return new Account(initialBalance);
    }

    @BeforeEach
    void setUp() {
        service = new TransferService();
    }

    // -----------------------------------------------------------------------
    // Helper: perform a transfer and return nothing (accounts are ephemeral
    // stubs that only need to hold enough balance for the test).
    // -----------------------------------------------------------------------
    private void doTransfer(String from, String to, double amount) {
        Account sender   = makeAccount(amount + 1000.0); // always has enough
        Account receiver = makeAccount(0.0);
        service.transfer(from, to, amount, sender, receiver);
    }

    // -----------------------------------------------------------------------
    // getHistoryForReturnsSenderTransactions
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForReturnsSenderTransactions() {
        doTransfer("A1", "B1", 50.0);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getFromAccountId());
    }

    // -----------------------------------------------------------------------
    // getHistoryForReturnsReceiverTransactions
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForReturnsReceiverTransactions() {
        doTransfer("C1", "A1", 40.0);

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getToAccountId());
    }

    // -----------------------------------------------------------------------
    // getHistoryForReturnsBothDirections
    // Canonical BTS-2 scenario: T1(A1->B1), T2(C1->A1), T3(D1->E1)
    // getHistoryFor("A1") should return [T1, T2]
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForReturnsBothDirections() {
        doTransfer("A1", "B1", 30.0); // T1
        doTransfer("C1", "A1", 20.0); // T2
        doTransfer("D1", "E1", 10.0); // T3 – should be excluded

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(2, result.size());
        assertEquals("A1", result.get(0).getFromAccountId()); // T1
        assertEquals("A1", result.get(1).getToAccountId());   // T2
    }

    // -----------------------------------------------------------------------
    // getHistoryForReturnsEmptyListForAccountWithNoTransfers
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForReturnsEmptyListForAccountWithNoTransfers() {
        // Case 1: completely empty history
        assertTrue(service.getHistoryFor("Z9").isEmpty());

        // Case 2: history has transfers, but none involve Z9
        doTransfer("A1", "B1", 15.0);
        doTransfer("C1", "D1", 25.0);

        assertTrue(service.getHistoryFor("Z9").isEmpty());
    }

    // -----------------------------------------------------------------------
    // getHistoryForThrowsOnNullOrEmptyAccountId
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForThrowsOnNullOrEmptyAccountId() {
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> service.getHistoryFor(null));
        assertEquals("accountId is required", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> service.getHistoryFor(""));
        assertEquals("accountId is required", ex2.getMessage());
    }

    // -----------------------------------------------------------------------
    // getHistoryForIsCaseSensitive
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForIsCaseSensitive() {
        doTransfer("A1", "B1", 10.0);

        // lowercase "a1" must NOT match uppercase "A1"
        assertTrue(service.getHistoryFor("a1").isEmpty());

        // exact case "A1" must match
        assertEquals(1, service.getHistoryFor("A1").size());
    }

    // -----------------------------------------------------------------------
    // getHistoryForPreservesChronologicalOrder
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForPreservesChronologicalOrder() {
        doTransfer("A1", "B1", 10.0); // T1 – inserted first
        doTransfer("A1", "C1", 20.0); // T2 – inserted second

        List<Transaction> result = service.getHistoryFor("A1");

        assertEquals(2, result.size());
        assertEquals(10.0, result.get(0).getAmount(), 0.001); // T1 first
        assertEquals(20.0, result.get(1).getAmount(), 0.001); // T2 second
    }

    // -----------------------------------------------------------------------
    // getHistoryForReturnsUnmodifiableList
    // -----------------------------------------------------------------------
    @Test
    void getHistoryForReturnsUnmodifiableList() {
        doTransfer("A1", "B1", 10.0);

        List<Transaction> result = service.getHistoryFor("A1");

        assertThrows(UnsupportedOperationException.class,
                () -> result.add(null));

        // Internal history must be untouched
        assertEquals(1, service.getHistory().size());
    }

    // -----------------------------------------------------------------------
    // sameAccountTransferThrowsSameAccountException
    // -----------------------------------------------------------------------
    @Test
    void sameAccountTransferThrowsSameAccountException() {
        Account account = new Account("ACC-123", "Alice", 100.0);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.transfer("ACC-123", "ACC-123", 10.0, account, account)
        );
        assertEquals("Cannot transfer to the same account", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // sameAccountTransferLeavesBalanceAndHistoryUnchanged
    // -----------------------------------------------------------------------
    @Test
    void sameAccountTransferLeavesBalanceAndHistoryUnchanged() {
        Account account = new Account("ACC-123", "Alice", 100.0);
        int historyBefore = service.getHistory().size();
        assertThrows(IllegalArgumentException.class,
            () -> service.transfer("ACC-123", "ACC-123", 10.0, account, account));
        assertEquals(100.0, account.getBalance(), 0.001);
        assertEquals(historyBefore, service.getHistory().size());
    }

    // -----------------------------------------------------------------------
    // sameAccountTransferWithZeroAmountThrowsAmountMustBePositive
    // -----------------------------------------------------------------------
    @Test
    void sameAccountTransferWithZeroAmountThrowsAmountMustBePositive() {
        Account account = new Account("ACC-123", "Alice", 100.0);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.transfer("ACC-123", "ACC-123", 0.0, account, account)
        );
        assertEquals("Amount must be positive", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // differentAccountTransferSucceedsAfterChange
    // -----------------------------------------------------------------------
    @Test
    void differentAccountTransferSucceedsAfterChange() {
        Account from = new Account("ACC-123", "Alice", 100.0);
        Account to = new Account("ACC-456", "Bob", 50.0);
        service.transfer("ACC-123", "ACC-456", 30.0, from, to);
        assertEquals(70.0, from.getBalance(), 0.001);
        assertEquals(80.0, to.getBalance(), 0.001);
        assertEquals(1, service.getHistory().size());
    }
}
