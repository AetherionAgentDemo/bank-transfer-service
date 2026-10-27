package com.bank;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Moves money between two {@link Account}s and keeps a log of every
 * transfer made.
 */
public class TransferService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final long DAILY_LIMIT_PAISE = 20_000_000L;
    static final String DAILY_LIMIT_MESSAGE = "Daily transfer limit of \u20B92,00,000 exceeded.";

    private final List<Transaction> history = new ArrayList<>();
    private final Clock clock;

    public TransferService() {
        this(Clock.systemUTC());
    }

    TransferService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Move {@code amount} from {@code from} to {@code to}.
     *
     * Validates that {@code amount} is positive and that {@code from}
     * has sufficient balance before proceeding with the transfer.
     */
    public synchronized Transaction transfer(Account from, Account to, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (dailyOutgoingPaise(from.getAccountId()) + Math.round(amount * 100) > DAILY_LIMIT_PAISE) {
            throw new IllegalStateException(DAILY_LIMIT_MESSAGE);
        }
        if (from.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        from.withdraw(amount);
        to.deposit(amount);

        Transaction transaction = new Transaction(from.getAccountId(), to.getAccountId(), amount, Instant.now(clock));
        history.add(transaction);
        return transaction;
    }

    public List<Transaction> getHistory() {
        return history;
    }

    private long dailyOutgoingPaise(String accountId) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), IST);
        long total = 0L;
        for (Transaction t : history) {
            if (Objects.equals(t.getFromAccountId(), accountId)
                    && LocalDate.ofInstant(t.getTimestamp(), IST).equals(today)) {
                total += Math.round(t.getAmount() * 100);
            }
        }
        return total;
    }
}
