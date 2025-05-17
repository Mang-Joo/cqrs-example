package cqrs.bankaccount.command;

import java.math.BigDecimal;

public record BankAccountTransferCommand(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {

    public BankAccountTransferCommand {
        if (fromAccountNumber == null || fromAccountNumber.isEmpty()) {
            throw new IllegalArgumentException("fromAccountNumber is required");
        }
        if (toAccountNumber == null || toAccountNumber.isEmpty()) {
            throw new IllegalArgumentException("toAccountNumber is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount is required and must be greater than 0");
        }
    }
}