package com.bank;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Moves money between two {@link Account}s and keeps a log of every
 * transfer made.
 */
public class TransferService {

    private static final double DAILY_LIMIT = 100000.0;
    private final List<Transaction> history = new ArrayList<>();
    private final Clock clock;

    /**
     * Constructs a TransferService with a given clock.
     *
     * @param clock a clock to determine the current UTC day
     */
    public TransferService(Clock clock) {
        this.clock = clock;
    }

    public TransferService() {
        this(Clock.systemUTC());
    }

    /**
     * Move {@code amount} from {@code from} to {@code to}.
     *
     * Validates that {@code amount} is positive and that {@code from}
     * has sufficient balance before proceeding with the transfer.
     */
    public Transaction transfer(Account from, Account to, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (from.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        double dailyTotal = calculateDailyTotal(from);
        if (dailyTotal + amount > DAILY_LIMIT) {
            double remaining = DAILY_LIMIT - dailyTotal;
            throw new IllegalStateException("Daily transfer limit of 100000 exceeded (remaining today: " + remaining + ")");
        }
        from.withdraw(amount);
        to.deposit(amount);

        Transaction transaction = new Transaction(from.getAccountId(), to.getAccountId(), amount);
        history.add(transaction);
        return transaction;
    }

    public List<Transaction> getHistory() {
        return history;
    }

    private double calculateDailyTotal(Account account) {
        double total = 0.0;
        LocalDate today = LocalDate.now(clock);
        for (Transaction transaction : history) {
            if (transaction.getFromAccountId().equals(account.getAccountId()) && isSameDay(transaction.getTimestamp(), today)) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    private boolean isSameDay(Date timestamp, LocalDate today) {
        LocalDate transactionDate = timestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        return transactionDate.equals(today);
    }
}