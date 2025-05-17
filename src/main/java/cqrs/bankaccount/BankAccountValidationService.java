package cqrs.bankaccount;

import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.query.BankAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountValidationService implements BankAccountValidation {
    private final BankAccountQueryService queryService;

    @Override
    public boolean exists(String accountNumber) {
        return queryService.existsAccountNumber(accountNumber);
    }

    @Override
    public boolean exists(UUID aggregateId) {
        return queryService.existsAggregateId(aggregateId);
    }
}
