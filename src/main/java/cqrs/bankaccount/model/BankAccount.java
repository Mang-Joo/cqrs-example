package cqrs.bankaccount.model;

import cqrs.bankaccount.model.event.CreateAccountEvent;
import cqrs.bankaccount.model.event.DepositEvent;
import cqrs.bankaccount.model.event.TransferEvent;
import cqrs.bankaccount.model.event.WithdrawEvent;
import cqrs.common.Aggregate;
import cqrs.common.Event;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
public class BankAccount extends Aggregate {
    private String accountNumber;
    private String accountHolder;
    private BigDecimal balance;

    public BankAccount(UUID aggregateId, List<Event> events) {
        super(aggregateId, events);
    }

    private BankAccount() {
        super();
    }

    public static BankAccount create(CreateAccountEvent event) {
        if (event.getAccountNumber() == null || event.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (event.getAccountHolder() == null || event.getAccountHolder().isBlank()) {
            throw new IllegalArgumentException("Account holder is required");
        }
        BankAccount account = new BankAccount();
        account.applyEvent(event);
        return account;
    }

    public void deposit(DepositEvent event) {
        if (event.getAmount().compareTo(BigDecimal.TEN) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 10");
        }
        this.applyEvent(event);
    }

    public void withdraw(WithdrawEvent event) {
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (event.getAmount().compareTo(this.balance) > 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.applyEvent(event);
    }

    public void withdrawForTransfer(TransferEvent event) {
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (event.getAmount().compareTo(this.balance) > 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.applyEvent(event);
    }

    public void depositForTransfer(TransferEvent event) {
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        this.applyEvent(event);
    }

    @Override
    protected void apply(Event event) {
        switch (event) {
            case CreateAccountEvent createAccountEvent -> apply(createAccountEvent);
            case DepositEvent depositEvent -> apply(depositEvent);
            case WithdrawEvent withdrawEvent -> apply(withdrawEvent);
            case TransferEvent transferEvent -> apply(transferEvent);
            default ->
                    throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getSimpleName());
        }
    }

    private void apply(DepositEvent depositEvent) {
        this.balance = this.balance.add(depositEvent.getAmount());
    }

    private void apply(WithdrawEvent withdrawEvent) {
        this.balance = this.balance.subtract(withdrawEvent.getAmount());
    }

    private void apply(TransferEvent transferEvent) {
        if (isSender(transferEvent)) {
            this.balance = this.balance.subtract(transferEvent.getAmount());
        } else if (isReceiver(transferEvent)) {
            this.balance = this.balance.add(transferEvent.getAmount());
        } else {
            throw new IllegalArgumentException("Invalid transfer event");
        }
    }

    private void apply(CreateAccountEvent createAccountEvent) {
        this.accountNumber = createAccountEvent.getAccountNumber();
        this.accountHolder = createAccountEvent.getAccountHolder();
        this.balance = BigDecimal.ZERO;
    }

    private boolean isSender(TransferEvent transferEvent) {
        return transferEvent.getFromAccountId().equals(this.getAggregateId());
    }

    private boolean isReceiver(TransferEvent transferEvent) {
        return transferEvent.getToAccountId().equals(this.getAggregateId());
    }
}
