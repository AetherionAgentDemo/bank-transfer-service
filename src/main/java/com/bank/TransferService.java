package com.bank;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves money between two {@link Account}s and keeps a log of every
 * transfer made.
 */
public class TransferService {

    private static final double DAILY_LIMIT = 100000.0;
    private final Clock clock;
    private final List<Transaction> history = new ArrayList<>();

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
            double remainingAmount = DAILY_LIMIT - dailyTotal;
            throw new IllegalStateException("Daily transfer limit exceeded. Remaining amount allowed: " + remainingAmount);
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
            if (transaction.getFromAccountId().equals(account.getAccountId()) && isSameDay(transaction.getDate(), today)) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    private boolean isSameDay(LocalDate date1, LocalDate date2) {
        return date1.equals(date2);
    }
}
