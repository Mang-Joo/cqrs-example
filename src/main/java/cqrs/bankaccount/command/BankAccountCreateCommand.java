package cqrs.bankaccount.command;

public record BankAccountCreateCommand(String accountNumber, String accountHolder) {

    public BankAccountCreateCommand {
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        if (accountHolder == null || accountHolder.isEmpty()) {
            throw new IllegalArgumentException("accountHolder is required");
        }
    }
}