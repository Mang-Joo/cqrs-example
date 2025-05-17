package cqrs.bankaccount.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountQueryService {
    private final BankAccountViewRepository repository;

    public UUID getAggregateIdByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"))
                .getAggregateId();
    }

    public boolean existsAggregateId(UUID aggregateId) {
        return repository.existsByAggregateId(aggregateId);
    }

    public boolean existsAccountNumber(String accountNumber) {
        return repository.existsByAccountNumber(accountNumber);
    }
}