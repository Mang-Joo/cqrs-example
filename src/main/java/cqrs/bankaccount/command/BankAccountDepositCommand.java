package cqrs.bankaccount.command;

import java.math.BigDecimal;

public record BankAccountDepositCommand(String accountNumber, BigDecimal amount) {

    public BankAccountDepositCommand {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount is required and must be greater than 0");
        }
    }
}