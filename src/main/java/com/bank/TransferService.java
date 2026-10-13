package com.bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TransferService {

    private final List<Transaction> history = new ArrayList<>();

    public void transfer(String fromAccountId, String toAccountId, double amount, Account from, Account to) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (from.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        history.add(new Transaction(fromAccountId, toAccountId, amount));
    }

    public List<Transaction> getHistory() {
        return history;
    }

    public List<Transaction> getHistoryFor(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        List<Transaction> filtered = history.stream()
            .filter(t -> t.getFromAccountId().equals(accountId)
                      || t.getToAccountId().equals(accountId))
            .collect(Collectors.toList());
        return Collections.unmodifiableList(filtered);
    }
}
