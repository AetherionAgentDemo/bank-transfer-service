package com.bank;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransferServiceTest {

    @Test
    void transferMovesMoneyBetweenAccounts() {
        Clock clock = Clock.systemUTC();
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(70.0, from.getBalance(), 0.001);
        assertEquals(80.0, to.getBalance(), 0.001);
    }

    @Test
    void transferRecordsTransactionHistory() {
        Clock clock = Clock.systemUTC();
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0);

        assertEquals(1, service.getHistory().size());
        assertEquals(30.0, service.getHistory().get(0).getAmount(), 0.001);
    }

    @Test
    void transferRejectsWhenInsufficientBalance() {
        Clock clock = Clock.systemUTC();
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 10.0);
        Account to = new Account("A2", "Bob", 50.0);

        assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 100.0));

        // Balances should be untouched by a rejected transfer.
        assertEquals(10.0, from.getBalance(), 0.001);
        assertEquals(50.0, to.getBalance(), 0.001);
    }

    @Test
    void transferRejectsNonPositiveAmount() {
        Clock clock = Clock.systemUTC();
        TransferService service = new TransferService(clock);
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
        Clock clock = Clock.systemUTC();
        TransferService service = new TransferService(clock);
        Account from = new Account("A4", "David", 200000.0);
        Account to = new Account("A5", "Eve", 0.0);

        // Perform some transfers that do not exceed the limit
        service.transfer(from, to, 50000.0);
        service.transfer(from, to, 40000.0);

        // Try a transfer that would exceed the daily limit
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 20000.0));
        assertEquals("Daily transfer limit of 100000 exceeded. Remaining transferable amount: 10000.0", exception.getMessage());

        // Ensure balances were not changed by the rejected transfer
        assertEquals(110000.0, from.getBalance(), 0.001);
        assertEquals(90000.0, to.getBalance(), 0.001);

        // Try another transfer to confirm service is still operational
        service.transfer(from, to, 10000.0);
        assertEquals(100000.0, from.getBalance(), 0.001);
        assertEquals(100000.0, to.getBalance(), 0.001);
    }

    @Test
    void testDailyLimitWithClock() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
        TransferService service = new TransferService(fixedClock);
        Account from = new Account("A6", "Tom", 200000.0);
        Account to = new Account("A7", "Jerry", 0.0);

        // case 1: two transfers under limit
        assertDoesNotThrow(() -> service.transfer(from, to, 60000.0));
        assertDoesNotThrow(() -> service.transfer(from, to, 40000.0));
        assertEquals(100000.0, to.getBalance(), 0.001);

        // case 2: transfer exceeding limit
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 1));
        assertEquals("Daily transfer limit of 100000 exceeded. Remaining transferable amount: 0.0", exception.getMessage());

        // Advance clock to next day
        fixedClock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC);
        service = new TransferService(fixedClock);

        // reset daily total
        assertDoesNotThrow(() -> service.transfer(from, to, 50000.0));
        assertEquals(150000.0, to.getBalance(), 0.001);
    }
}
