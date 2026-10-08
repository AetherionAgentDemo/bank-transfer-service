package com.bank;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void transferRejectsExceedingDailyLimit() {
        Clock fixedClock = Clock.fixed(Instant.parse("2023-10-15T00:00:00Z"), ZoneOffset.UTC);
        TransferService service = new TransferService(fixedClock);
        Account from = new Account("A4", "David", 200000.0);
        Account to = new Account("A5", "Eve", 0.0);

        // Perform some transfers that do not exceed the limit
        service.transfer(from, to, 50000.0);
        service.transfer(from, to, 40000.0);

        // Try a transfer that would exceed the daily limit
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 20000.0));
        assertEquals("Daily transfer limit of 100000 exceeded (remaining today: 10000.0)", exception.getMessage());

        // Ensure balances were not changed by the rejected transfer
        assertEquals(110000.0, from.getBalance(), 0.001);
        assertEquals(90000.0, to.getBalance(), 0.001);

        // Try another transfer to confirm service is still operational
        service.transfer(from, to, 10000.0);
        assertEquals(100000.0, from.getBalance(), 0.001);
        assertEquals(100000.0, to.getBalance(), 0.001);
    }

    @Test
    void transferResetsDailyTotalNextDay() {
        Clock initialClock = Clock.fixed(Instant.parse("2023-10-15T00:00:00Z"), ZoneOffset.UTC);
        TransferService service = new TransferService(initialClock);
        Account from = new Account("A6", "Frank", 150000.0);
        Account to = new Account("A7", "Grace", 0.0);

        // Perform transfers to reach the daily limit
        service.transfer(from, to, 60000.0);
        service.transfer(from, to, 40000.0);
        assertThrows(IllegalStateException.class, () -> service.transfer(from, to, 1.0));

        // Advance the clock to the next UTC day
        Clock nextDayClock = Clock.fixed(Instant.parse("2023-10-16T00:00:01Z"), ZoneOffset.UTC);
        service = new TransferService(nextDayClock);

        // Verify transfer is successful on the next day
        service.transfer(from, to, 1000.0);
        assertEquals(49000.0, from.getBalance(), 0.001);
        assertEquals(101000.0, to.getBalance(), 0.001);
    }
}
