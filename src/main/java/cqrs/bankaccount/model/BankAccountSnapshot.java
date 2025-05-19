package cqrs.bankaccount.model;

import java.math.BigDecimal;
import java.util.UUID;

import cqrs.common.snapshot.IsSnapshotData;


public class BankAccountSnapshot implements IsSnapshotData {
    private final UUID aggregateId;
    private final String accountNumber;
    private final String accountHolder;
    private final BigDecimal balance;
    private final int version;
    private final UUID userId;

    public BankAccountSnapshot(UUID aggregateId, String accountNumber, String accountHolder, BigDecimal balance, int version, UUID userId) {
        this.aggregateId = aggregateId;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.version = version;
        this.userId = userId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public int getVersion() {
        return version;
    }

    public UUID getUserId() {
        return userId;
    }

    public BankAccount toBankAccount() {
        return BankAccount.loadFromSnapshot(aggregateId, accountNumber, accountHolder, userId, balance, version);
    }
}