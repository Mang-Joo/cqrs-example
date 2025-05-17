package cqrs.bankaccount.model.event;

import cqrs.common.Event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CreateAccountEvent extends Event {
    private final String accountNumber;
    private final String accountHolder;

    public CreateAccountEvent(String accountNumber, String accountHolder, int version) {
        super(UUID.randomUUID(), LocalDateTime.now(), version);
        validation(accountNumber, accountHolder);
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

    private void validation(String accountNumber, String accountHolder) {
        if (Objects.requireNonNullElseGet(accountNumber, () -> "").isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (Objects.requireNonNullElseGet(accountHolder, () -> "").isBlank()) {
            throw new IllegalArgumentException("Account holder is required");
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

}
