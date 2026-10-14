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
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (from.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
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
}
