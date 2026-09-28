package com.bank;

/**
 * A bank account with a running balance. {@link #withdraw} and
 * {@link #deposit} are package-private on purpose — all money movement is
 * meant to go through {@link TransferService}, but that doesn't mean this
 * class should trust whatever it's handed; it should enforce its own
 * invariants regardless of who's calling.
 */
public class Account {

    private final String accountId;
    private final String ownerName;
    private double balance;

    public Account(String accountId, String ownerName, double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative");
        }
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = initialBalance;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getBalance() {
        return balance;
    }

    /**
     * Withdraws an amount from the account balance only if the current balance
     * is sufficient to cover the withdrawal, otherwise throws an exception.
     */
    void withdraw(double amount) {
        if (amount > balance) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
    }

    void deposit(double amount) {
        balance += amount;
    }
}
