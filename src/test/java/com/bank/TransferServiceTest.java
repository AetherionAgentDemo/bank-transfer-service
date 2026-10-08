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
    void transferRejectsExceedingDailyLimit() {
        TransferService service = new TransferService();
        Account from = new Account("A4", "David", 200000.0);
        Account to = new Account("A5", "Eve", 0.0);

        // Perform some transfers that do not exceed the limit
        service.transfer(from, to, 50000.0);
        service.transfer(from, to, 40000.0);

        // Try a transfer that would exceed the daily limit
        assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 20000.0));

        // Ensure balances were not changed by the rejected transfer
        assertEquals(110000.0, from.getBalance(), 0.001);
        assertEquals(90000.0, to.getBalance(), 0.001);

        // Try another transfer to confirm service is still operational
        service.transfer(from, to, 10000.0);
        assertEquals(100000.0, from.getBalance(), 0.001);
        assertEquals(100000.0, to.getBalance(), 0.001);
    }
}
