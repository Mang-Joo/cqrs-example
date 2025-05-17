package cqrs.bankaccount.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountViewRepository extends JpaRepository<BankAccountView, String> {
    Optional<BankAccountView> findByAccountNumber(String accountNumber);

    boolean existsByAggregateId(UUID aggregateId);

    boolean existsByAccountNumber(String accountNumber);
}