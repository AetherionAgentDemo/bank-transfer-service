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
     * BUG: there's no check here that {@code amount} is actually positive.
     * A caller can pass zero or a negative number straight through to
     * withdraw/deposit below, which (combined with Account's own missing
     * balance check) lets a "transfer" invent money out of nowhere instead
     * of just moving it.
     */
    public Transaction transfer(Account from, Account to, double amount) {
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
