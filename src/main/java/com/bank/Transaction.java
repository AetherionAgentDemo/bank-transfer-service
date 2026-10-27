package com.bank;

import java.time.Instant;

/** A record of a completed transfer between two accounts. */
public class Transaction {

    private final String fromAccountId;
    private final String toAccountId;
    private final double amount;
    private final Instant timestamp;

    public Transaction(String fromAccountId, String toAccountId, double amount) {
        this(fromAccountId, toAccountId, amount, Instant.now());
    }

    public Transaction(String fromAccountId, String toAccountId, double amount, Instant timestamp) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
