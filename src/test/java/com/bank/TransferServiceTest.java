package com.bank;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferServiceTest {

    private static final String DAILY_LIMIT_MESSAGE = "Daily transfer limit of \u20B92,00,000 exceeded.";

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

    // ---------------------------------------------------------------
    // Reference note / remark (BTS-38)
    // ---------------------------------------------------------------

    @Test
    void transferWithRemarkSavesAndShowsNote() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0, "rent October");

        assertEquals(70.0, from.getBalance(), 0.001);
        assertEquals(80.0, to.getBalance(), 0.001);
        assertEquals(1, service.getHistory().size());
        assertEquals("rent October", service.getHistory().get(0).getRemark());
        assertEquals(30.0, service.getHistory().get(0).getAmount(), 0.001);
    }

    @Test
    void transferRejectsRemarkLongerThan60WithUnchangedState() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(from, to, 30.0, "a".repeat(61)));
        assertEquals("Reference note too long (max 60)", ex.getMessage());
        assertEquals(100.0, from.getBalance(), 0.001);
        assertEquals(50.0, to.getBalance(), 0.001);
        assertEquals(0, service.getHistory().size());
    }

    @Test
    void transferAcceptsRemarkOfExactly60Characters() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);
        String note = "a".repeat(60);

        service.transfer(from, to, 30.0, note);

        assertEquals(note, service.getHistory().get(0).getRemark());
        assertEquals(70.0, from.getBalance(), 0.001);
    }

    @Test
    void transferAccepts60CharactersWithTrailingSpaces() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);
        String note = "a".repeat(60);

        service.transfer(from, to, 30.0, note + "   ");

        assertEquals(note, service.getHistory().get(0).getRemark());
    }

    @Test
    void transferWithoutNoteRecordsEmptyRemark() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0);
        service.transfer(from, to, 10.0, null);
        service.transfer(from, to, 10.0, " ");
        service.transfer(from, to, 10.0, "");

        assertEquals(4, service.getHistory().size());
        for (Transaction t : service.getHistory()) {
            assertEquals("", t.getRemark());
        }
        assertEquals(40.0, from.getBalance(), 0.001);
    }

    @Test
    void transferTrimsEdgeSpacesFromRemark() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        service.transfer(from, to, 30.0, " rent October ");

        assertEquals("rent October", service.getHistory().get(0).getRemark());
    }

    @Test
    void transferRejectsRemarkWithDisallowedCharacters() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 100.0);
        Account to = new Account("A2", "Bob", 50.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(from, to, 30.0, "rent<script>"));
        assertEquals("Remark contains characters that are not allowed.", ex.getMessage());
        assertEquals(100.0, from.getBalance(), 0.001);
        assertEquals(50.0, to.getBalance(), 0.001);
        assertEquals(0, service.getHistory().size());
    }

    @Test
    void insufficientBalanceTakesPrecedenceOverRemarkValidation() {
        TransferService service = new TransferService();
        Account from = new Account("A1", "Alice", 10.0);
        Account to = new Account("A2", "Bob", 50.0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 100.0, "a".repeat(61)));
        assertEquals("Insufficient balance", ex.getMessage());
        assertEquals(10.0, from.getBalance(), 0.001);
        assertEquals(0, service.getHistory().size());
    }

    @Test
    void transactionConstructorsRecordEmptyRemarkByDefaultAndForNull() {
        assertEquals("", new Transaction("A1", "A2", 30.0, Instant.EPOCH).getRemark());
        assertEquals("", new Transaction("A1", "A2", 30.0, Instant.EPOCH, null).getRemark());
        assertEquals("", new Transaction("A1", "A2", 30.0).getRemark());
        assertEquals("rent", new Transaction("A1", "A2", 30.0, Instant.EPOCH, "rent").getRemark());
    }

    // ---------------------------------------------------------------
    // Daily transfer limit (\u20B92,00,000 per sender per IST day)
    // ---------------------------------------------------------------

    @Test
    void dailyLimitAllowsTransfersUpToExactlyTheLimit() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);

        assertEquals(4, service.getHistory().size());
        assertEquals(800_000.0, from.getBalance(), 0.001);
        assertEquals(200_000.0, to.getBalance(), 0.001);
    }

    @Test
    void dailyLimitRejectsTransferBeyondLimitWithMessageAndUnchangedState() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        for (int i = 0; i < 4; i++) {
            service.transfer(from, to, 50_000.0);
        }

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 1.0));
        assertEquals(DAILY_LIMIT_MESSAGE, e.getMessage());

        assertEquals(800_000.0, from.getBalance(), 0.001);
        assertEquals(200_000.0, to.getBalance(), 0.001);
        assertEquals(4, service.getHistory().size());
    }

    @Test
    void dailyLimitSumsExactlyInPaise() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 49_998.75);
        // Total becomes exactly 2,00,000.00 = 20_000_000 paise, which is allowed.
        service.transfer(from, to, 1.25);
        assertEquals(5, service.getHistory().size());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 0.01));
        assertEquals(DAILY_LIMIT_MESSAGE, e.getMessage());
        assertEquals(5, service.getHistory().size());
    }

    @Test
    void dailyLimitResetsAtIstMidnight() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 9, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        // 1,90,000 transferred yesterday (IST).
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 40_000.0);

        clock.set(istInstant(2026, 1, 10, 9, 0, 0));
        service.transfer(from, to, 50_000.0);

        assertEquals(5, service.getHistory().size());
        assertEquals(760_000.0, from.getBalance(), 0.001);
    }

    @Test
    void dailyLimitExcludesLastSecondOfYesterdayAndIncludesFirstSecondOfToday() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 9, 23, 59, 59));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        // At 23:59:59 IST yesterday: not counted today.
        service.transfer(from, to, 50_000.0);

        // At 00:00:00 IST today: counted today.
        clock.set(istInstant(2026, 1, 10, 0, 0, 0));
        service.transfer(from, to, 50_000.0);

        clock.set(istInstant(2026, 1, 10, 10, 0, 0));
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 1.0));
        assertEquals(DAILY_LIMIT_MESSAGE, e.getMessage());
        assertEquals(5, service.getHistory().size());
    }

    @Test
    void transferStampsTransactionWithClockInstant() {
        Instant fixed = istInstant(2026, 1, 10, 23, 0, 0);
        MutableClock clock = new MutableClock(fixed);
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        Transaction t = service.transfer(from, to, 10.0);

        assertEquals(fixed, t.getTimestamp());
    }

    @Test
    void threeArgTransactionConstructorUsesCurrentTime() {
        Instant before = Instant.now();
        Transaction t = new Transaction("A1", "A2", 10.0);
        Instant after = Instant.now();

        assertTrue(!t.getTimestamp().isBefore(before));
        assertTrue(!t.getTimestamp().isAfter(after));
    }

    @Test
    void dailyLimitIsPerSendingAccount() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account alice = new Account("A1", "Alice", 1_000_000.0);
        Account bob = new Account("A2", "Bob", 0.0);
        Account carol = new Account("A3", "Carol", 1_000_000.0);

        for (int i = 0; i < 4; i++) {
            service.transfer(alice, bob, 50_000.0);
        }

        // Carol has her own daily allowance.
        service.transfer(carol, bob, 50_000.0);
        assertEquals(950_000.0, carol.getBalance(), 0.001);
        assertEquals(5, service.getHistory().size());
    }

    @Test
    void dailyLimitCountsOnlyOutgoingTransfers() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account alice = new Account("A1", "Alice", 1_000_000.0);
        Account bob = new Account("A2", "Bob", 0.0);

        for (int i = 0; i < 4; i++) {
            service.transfer(alice, bob, 50_000.0);
        }

        // Bob received 2,00,000 today, but incoming money does not count against him.
        service.transfer(bob, alice, 50_000.0);
        assertEquals(150_000.0, bob.getBalance(), 0.001);
        assertEquals(5, service.getHistory().size());
    }

    @Test
    void dailyLimitIsCheckedBeforeBalance() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 200_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        for (int i = 0; i < 4; i++) {
            service.transfer(from, to, 50_000.0);
        }
        assertEquals(0.0, from.getBalance(), 0.001);

        // Both the limit and the balance would fail; the limit is reported.
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 1.0));
        assertEquals(DAILY_LIMIT_MESSAGE, e.getMessage());
        assertEquals(0.0, from.getBalance(), 0.001);
        assertEquals(200_000.0, to.getBalance(), 0.001);
        assertEquals(4, service.getHistory().size());
    }

    @Test
    void insufficientBalanceStillReportedWhenWithinDailyLimit() {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 10.0);
        Account to = new Account("A2", "Bob", 0.0);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.transfer(from, to, 100.0));
        assertEquals("Insufficient balance", e.getMessage());
        assertEquals(0, service.getHistory().size());
    }

    @Test
    void concurrentTransfersCannotTogetherExceedDailyLimit() throws Exception {
        MutableClock clock = new MutableClock(istInstant(2026, 1, 10, 12, 0, 0));
        TransferService service = new TransferService(clock);
        Account from = new Account("A1", "Alice", 1_000_000.0);
        Account to = new Account("A2", "Bob", 0.0);

        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);
        service.transfer(from, to, 50_000.0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                Callable<String> task = () -> {
                    start.await();
                    try {
                        service.transfer(from, to, 50_000.0);
                        return "ok";
                    } catch (IllegalStateException ex) {
                        return ex.getMessage();
                    }
                };
                futures.add(executor.submit(task));
            }
            start.countDown();

            int successes = 0;
            int rejections = 0;
            for (Future<String> f : futures) {
                String result = f.get(10, TimeUnit.SECONDS);
                if ("ok".equals(result)) {
                    successes++;
                } else {
                    assertEquals(DAILY_LIMIT_MESSAGE, result);
                    rejections++;
                }
            }

            assertEquals(1, successes);
            assertEquals(1, rejections);
            assertEquals(4, service.getHistory().size());
            assertEquals(800_000.0, from.getBalance(), 0.001);
            assertEquals(200_000.0, to.getBalance(), 0.001);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Instant istInstant(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second)
                .atZone(ZoneId.of("Asia/Kolkata"))
                .toInstant();
    }

    /** A Clock whose instant can be changed by tests. */
    private static final class MutableClock extends Clock {
        private volatile Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
