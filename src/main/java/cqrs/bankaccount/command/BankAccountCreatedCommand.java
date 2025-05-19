package cqrs.bankaccount.command;

import java.util.UUID;

public record BankAccountCreatedCommand(String accountNumber, String accountHolder, UUID userId) {

    public BankAccountCreatedCommand {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        if (accountHolder == null || accountHolder.isEmpty()) {
            throw new IllegalArgumentException("accountHolder is required");
        }

        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}