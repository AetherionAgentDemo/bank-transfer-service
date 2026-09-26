# bank-transfer-service

A small, in-memory bank account and money-transfer service written in
plain Java (no frameworks). Small enough to read end-to-end in a few
minutes, realistic enough to have logic worth testing.

## What it does

- `Account` — a bank account with a running balance.
- `Transaction` — a record of one completed transfer (from, to, amount,
  when).
- `TransferService` — moves money between two accounts and keeps a log of
  every transfer made:
  - `transfer(from, to, amount)` — should refuse the transfer if `amount`
    isn't positive, or if `from` doesn't have enough balance to cover it.
    On success, both accounts' balances update and a `Transaction` is
    added to the history.

## Requirements

Just a JDK (17+) and Maven. No database, no network calls, no external
services.

## Running the tests

```bash
mvn test
```

## Project layout

```
pom.xml
src/main/java/com/bank/
    Account.java
    Transaction.java
    TransferService.java
src/test/java/com/bank/
    TransferServiceTest.java
```
