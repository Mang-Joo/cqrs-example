package cqrs.bankaccount.command;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.model.event.CreateAccountEvent;
import cqrs.bankaccount.model.event.DepositEvent;
import cqrs.bankaccount.model.event.TransferEvent;
import cqrs.bankaccount.model.event.WithdrawEvent;
import cqrs.bankaccount.query.BankAccountQueryService;
import cqrs.common.Event;
import cqrs.common.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountCommandService {
    private final EventStore eventStore;
    private final BankAccountValidation validation;
    private final BankAccountQueryService queryService;

    public BankAccount createAccount(BankAccountCreateCommand command) {
        if (validation.exists(command.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists");
        }

        CreateAccountEvent event = new CreateAccountEvent(command.accountNumber(), command.accountHolder(), 0);
        BankAccount account = BankAccount.create(event);
        log.info("Account created. accountNumber={}, accountHolder={}", command.accountNumber(), command.accountHolder());
        return account;
    }

    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        UUID aggregateId = queryService.getAggregateIdByAccountNumber(accountNumber);
        List<Event> events = eventStore.load(aggregateId);
        BankAccount account = new BankAccount(aggregateId, events);

        DepositEvent event = new DepositEvent(amount, account.nextVersion());
        account.deposit(event);

        eventStore.save(aggregateId, event);
        log.info("Deposit completed. accountNumber={}, amount={}", accountNumber, amount);
        return account;
    }

    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        UUID aggregateId = queryService.getAggregateIdByAccountNumber(accountNumber);
        List<Event> events = eventStore.load(aggregateId);
        BankAccount account = new BankAccount(aggregateId, events);

        WithdrawEvent event = new WithdrawEvent(amount, account.nextVersion());
        account.withdraw(event);

        eventStore.save(aggregateId, event);
        log.info("Withdraw completed. accountNumber={}, amount={}", accountNumber, amount);
        return account;
    }

    public BankAccount transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        UUID fromAggregateId = queryService.getAggregateIdByAccountNumber(fromAccountNumber);
        UUID toAggregateId = queryService.getAggregateIdByAccountNumber(toAccountNumber);

        List<Event> fromEvents = eventStore.load(fromAggregateId);
        List<Event> toEvents = eventStore.load(toAggregateId);

        BankAccount fromAccount = new BankAccount(fromAggregateId, fromEvents);
        BankAccount toAccount = new BankAccount(toAggregateId, toEvents);

        TransferEvent event = new TransferEvent(fromAggregateId, toAggregateId, amount, fromAccount.nextVersion());
        fromAccount.withdrawForTransfer(event);
        toAccount.depositForTransfer(event);

        eventStore.save(fromAggregateId, event);
        eventStore.save(toAggregateId, event);

        log.info("Transfer completed. fromAccountNumber={}, toAccountNumber={}, amount={}", fromAccountNumber, toAccountNumber, amount);
        return fromAccount;
    }
}