package com.bank;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves money between two {@link Account}s and keeps a log of every
 * transfer made.
 */
public class TransferService {

    private final List<Transaction> history = new ArrayList<>();

    /**
     * Move {@code amount} from {@code from} to {@code to}.
     *
     * Validates that {@code amount} is positive and that {@code from}
     * has sufficient balance before proceeding with the transfer.
     */
    public Transaction transfer(Account from, Account to, double amount) {
        return transfer(from, to, amount, "");
    }

    /**
     * Move {@code amount} from {@code from} to {@code to} with an optional reference.
     *
     * Validates that {@code amount} is positive, {@code from} has sufficient balance, and
     * {@code reference} does not exceed 140 characters before proceeding with the transfer.
     */
    public Transaction transfer(Account from, Account to, double amount, String reference) {
        if (reference != null && reference.length() > 140) {
            throw new IllegalArgumentException("Reference too long");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (from.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        from.withdraw(amount);
        to.deposit(amount);

        Transaction transaction = new Transaction(from.getAccountId(), to.getAccountId(), amount, reference == null ? "" : reference);
        history.add(transaction);
        return transaction;
    }

    public List<Transaction> getHistory() {
        return history;
    }
}
