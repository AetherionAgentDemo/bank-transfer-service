package com.bank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransferService {

    private final List<Transaction> history = new ArrayList<>();

    public void transfer(Account from, Account to, double amount) {
        if (from == null) {
            throw new IllegalArgumentException("from account is required");
        }
        if (to == null) {
            throw new IllegalArgumentException("to account is required");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.getBalance() < amount) {
            throw new IllegalStateException("insufficient funds");
        }
        from.debit(amount);
        to.credit(amount);
        history.add(new Transaction(from.getId(), to.getId(), amount));
    }

    public List<Transaction> getHistory() {
        return new ArrayList<>(history);
    }

    public List<Transaction> getHistoryFor(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        return history.stream()
                .filter(t -> t.getFromAccountId().equals(accountId)
                        || t.getToAccountId().equals(accountId))
                .collect(Collectors.toList());
    }

    public double getTotalSentBy(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        return history.stream()
                .filter(t -> t.getFromAccountId().equals(accountId))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
}
