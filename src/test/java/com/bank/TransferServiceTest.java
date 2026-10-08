package com.bank;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2023-10-01T00:00:00.00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
    private static final Clock NEXT_DAY_CLOCK = Clock.fixed(FIXED_INSTANT.plusSeconds(86400), ZoneId.of("UTC")); // Add 1 day

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
        Account account = new Account("A3", "Carol", 20.0);

        assertThrows(IllegalStateException.class, () -> withdrawDirectly(account, 50.0));
        assertEquals(20.0, account.getBalance(), 0.001);
    }

    private static void withdrawDirectly(Account account, double amount) {
        account.withdraw(amount);
    }

    @Test
    void transferRejectsExceedingDailyLimit() {
        TransferService service = new TransferService(FIXED_CLOCK);
        Account from = new Account("A4", "David", 200000.0);
        Account to = new Account("A5", "Eve", 0.0);

        service.transfer(from, to, 60000.0);
        service.transfer(from, to, 40000.0);

        Exception exception = assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 1.0));
        assertEquals("Daily transfer limit exceeded. Remaining amount allowed: 0.0", exception.getMessage());

        assertEquals(140000.0, from.getBalance(), 0.001);
        assertEquals(100000.0, to.getBalance(), 0.001);

        TransferService nextDayService = new TransferService(NEXT_DAY_CLOCK);

        nextDayService.transfer(from, to, 60000.0);
        assertEquals(80000.0, from.getBalance(), 0.001);
        assertEquals(160000.0, to.getBalance(), 0.001);
    }
}
