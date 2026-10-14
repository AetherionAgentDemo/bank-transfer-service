package com.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferServiceTest {

    @Test
    void transferMovesMoneyBetweenAccounts() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(70.0, from.getBalance(), 0.001);
        assertEquals(80.0, to.getBalance(), 0.001);
    }

    @Test
    void transferRecordsTransactionHistory() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(1, service.getHistory().size());
        assertEquals(30.0, service.getHistory().get(0).getAmount(), 0.001);
    }

    @Test
    void transferRejectsWhenInsufficientBalance() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 10.0);
        Account to = new Account("A2", "Bob", 50.0);

        assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 100.0));

        // Balances should be untouched by a rejected transfer.
        assertEquals(10.0, from.getBalance(), 0.001);
        assertEquals(50.0, to.getBalance(), 0.001);
    }

    @Test
    void transferRejectsNonPositiveAmount() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        assertThrows(IllegalArgumentException.class, () -> service.transfer(from, to, -20.0));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(from, to, 0.0));

        // A rejected transfer shouldn't have moved anything.
        assertEquals(100.0, from.getBalance(), 0.001);
        assertEquals(50.0, to.getBalance(), 0.001);
    }

    @Test
    void accountWithdrawRejectsInsufficientBalanceDirectly() {
        // Bypasses TransferService entirely and calls Account directly, so
        // this only passes if the "can't withdraw more than you have"
        // invariant is enforced inside Account itself, not just inside
        // TransferService.
        Account account = new Account("A3", "Carol", 20.0);

        assertThrows(IllegalStateException.class, () -> withdrawDirectly(account, 50.0));
        assertEquals(20.0, account.getBalance(), 0.001);
    }

    private static void withdrawDirectly(Account account, double amount) {
        // Account.withdraw is package-private; this test lives in the same
        // package (com.bank), so it can call it directly.
        account.withdraw(amount);
    }

    @Test
    void countTransfersForRejectsNullAccountId() {
        TransferService service = new TransferService();
        assertThrows(IllegalArgumentException.class, () -> service.countTransfersFor(null), "accountId is required");
    }

    @Test
    void countTransfersForRejectsBlankAccountId() {
        TransferService service = new TransferService();
        assertThrows(IllegalArgumentException.class, () -> service.countTransfersFor(""), "accountId is required");
        assertThrows(IllegalArgumentException.class, () -> service.countTransfersFor(" "), "accountId is required");
    }

    @Test
    void countTransfersForSenderOnly() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("B1", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(1, service.countTransfersFor("A1"));
    }

    @Test
    void countTransfersForRecipientOnly() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("B1", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(1, service.countTransfersFor("B1"));
    }

    @Test
    void countTransfersForBothRolesAcrossDistinctTransfers() {
        TransferService service = new TransferService();
        Account account1 = new Account("A1", "Alice", 100.0);
        Account account2 = new Account("B1", "Bob", 50.0);

        service.transfer(account1, account2, 30.0);
        service.transfer(account2, account1, 10.0);

        assertEquals(2, service.countTransfersFor("A1"));
    }

    @Test
    void countTransfersForAbsentAccount() {
        TransferService service = new TransferService();
        Account account1 = new Account("A1", "Alice", 100.0);
        Account account2 = new Account("B1", "Bob", 50.0);

        service.transfer(account1, account2, 30.0);

        assertEquals(0, service.countTransfersFor("C1"));
    }

    @Test
    void countTransfersForSameAccountTransfer() {
        TransferService service = new TransferService();
        Account account = new Account("A1", "Alice", 100.0);

        service.transfer(account, account, 10.0);

        assertEquals(1, service.countTransfersFor("A1"));
    }
}
