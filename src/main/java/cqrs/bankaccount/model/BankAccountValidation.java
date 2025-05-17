package cqrs.bankaccount.model;

import java.util.UUID;

public interface BankAccountValidation {
    boolean exists(String accountNumber);

    boolean exists(UUID aggregateId);
}
