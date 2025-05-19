package cqrs.bankaccount.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import cqrs.bankaccount.model.event.AccountCreatedEvent;
import cqrs.bankaccount.model.event.MoneyDepositedEvent;
import cqrs.bankaccount.model.event.MoneyTransferEvent;
import cqrs.bankaccount.model.event.MoneyWithdrawnEvent;
import cqrs.common.AggregateRoot;
import cqrs.common.Event;

public class BankAccount {
    private final AggregateRoot aggregateRoot;
    private UUID userId;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal balance;

    public BankAccount(String accountNumber, String accountHolder, UUID userId) {
        validation(accountNumber, accountHolder, userId);
        this.aggregateRoot = new AggregateRoot(this::handleEvent);

        AccountCreatedEvent event = new AccountCreatedEvent(
            UUID.randomUUID(),
            aggregateRoot.getAggregateId(),
            accountNumber,
            accountHolder,
            userId,
            LocalDateTime.now(),
            aggregateRoot.getCurrentVersion() + 1
        );
        aggregateRoot.recordAndApplyEvent(event);
    }

    private BankAccount(UUID aggregateId, List<Event> events) {
        this.aggregateRoot = new AggregateRoot(aggregateId, events, this::handleEvent);
    }

    private BankAccount(UUID aggregateId, String accountNumber, String accountHolder, UUID userId, BigDecimal balance, int snapshotVersion) {
        this.aggregateRoot = new AggregateRoot(aggregateId, snapshotVersion, this::handleEvent);
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.userId = userId;
        this.balance = balance;
    }

    public void replayEventsAfterSnapshot(List<Event> events) {
        for (Event event : events) {
            this.aggregateRoot.replayEvent(event);
        }
    }

    public static BankAccount loadFromHistory(UUID aggregateId, List<Event> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Cannot reconstitute BankAccount from empty event list.");
        }
        return new BankAccount(aggregateId, events);
    }

    public static BankAccount loadFromSnapshot(UUID aggregateId, String accountNumber, String accountHolder, UUID userId, BigDecimal balance, int version) {
        return new BankAccount(aggregateId, accountNumber, accountHolder, userId, balance, version);
    }

    private void handleEvent(Event event) {
        switch (event) {
            case AccountCreatedEvent e -> apply(e);
            case MoneyDepositedEvent e -> apply(e);
            case MoneyWithdrawnEvent e -> apply(e);
            case MoneyTransferEvent e -> apply(e);
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
        }
    }

    private void apply(AccountCreatedEvent event) {
        this.accountNumber = event.accountNumber();
        this.accountHolder = event.accountHolder();
        this.userId = event.userId();
        this.balance = BigDecimal.ZERO;
    }

    private void apply(MoneyDepositedEvent event) {
        this.balance = this.balance.add(event.amount());
    }

    private void apply(MoneyWithdrawnEvent event) {
        this.balance = this.balance.subtract(event.amount());
    }

    private void apply(MoneyTransferEvent event) {
        if (event.fromAccountNumber().equals(accountNumber)) {
            this.balance = this.balance.subtract(event.amount());
        } else if (event.toAccountNumber().equals(accountNumber)) {
            this.balance = this.balance.add(event.amount());
        } else {
            throw new IllegalArgumentException("MoneyTransferEvent is not related to this account: " + accountNumber);
        }
    }

    private void validation(String accountNumber, String accountHolder, UUID userId) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (accountHolder == null || accountHolder.isEmpty()) {
            throw new IllegalArgumentException("Account holder is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(10)) < 0) {
            throw new IllegalArgumentException("Amount must be greater than 10");
        }

        MoneyDepositedEvent event = new MoneyDepositedEvent(
            UUID.randomUUID(),
            aggregateRoot.getAggregateId(),
            amount,
            LocalDateTime.now(),
            aggregateRoot.getCurrentVersion() + 1
        );
        aggregateRoot.recordAndApplyEvent(event);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(
            UUID.randomUUID(),
            aggregateRoot.getAggregateId(),
            amount,
            LocalDateTime.now(),
            aggregateRoot.getCurrentVersion() + 1
        );
        aggregateRoot.recordAndApplyEvent(event);
    }

    public void transferTo(String toAccountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        MoneyTransferEvent event = new MoneyTransferEvent(
            UUID.randomUUID(),
            aggregateRoot.getAggregateId(),
            accountNumber,
            toAccountNumber,
            amount,
            LocalDateTime.now(),
            aggregateRoot.getCurrentVersion() + 1
        );
        aggregateRoot.recordAndApplyEvent(event);
    }

    public void transferFrom(String fromAccountNumber, BigDecimal amount) {
        MoneyTransferEvent event = new MoneyTransferEvent(
            UUID.randomUUID(),
            aggregateRoot.getAggregateId(),
            fromAccountNumber,
            accountNumber,
            amount,
            LocalDateTime.now(),
            aggregateRoot.getCurrentVersion() + 1
        );
        aggregateRoot.recordAndApplyEvent(event);
    }

    public BankAccountSnapshot createSnapshot() {
        return new BankAccountSnapshot(
                this.aggregateRoot.getAggregateId(),
                this.accountNumber,
                this.accountHolder,
                this.balance,
                this.aggregateRoot.getCurrentVersion(),
                this.userId
        );
    }

    public UUID getAggregateId() {
        return aggregateRoot.getAggregateId();
    }

    public List<Event> getUncommittedEvents() {
        return aggregateRoot.getUncommittedEvents();
    }

    public void clearUncommittedEvents() {
        aggregateRoot.clearUncommittedEvents();
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

    public int getCurrentVersion() {
        return aggregateRoot.getCurrentVersion();
    }

    public UUID getUserId() {
        return userId;
    }
}
