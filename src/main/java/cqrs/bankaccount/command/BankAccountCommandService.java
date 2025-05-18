package cqrs.bankaccount.command;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.query.BankAccountQueryService;
import cqrs.common.Event;
import cqrs.common.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountCommandService {
    private final EventStore eventStore;
    private final BankAccountValidation validation;
    private final BankAccountQueryService queryService;

    public BankAccount createAccount(BankAccountCreatedCommand command) {
        if (validation.exists(command.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists");
        }

        BankAccount account = new BankAccount(command.accountNumber(), command.accountHolder());

        saveEvents(account);
        log.info("Account created. accountNumber={}, accountHolder={}", command.accountNumber(), command.accountHolder());
        return account;
    }

    public BankAccount deposit(BankAccountDepositCommand command) {
        BankAccount account = loadAccount(queryService.getAggregateIdByAccountNumber(command.accountNumber()));

        account.deposit(command.amount());

        saveEvents(account);

        log.info("Deposit completed. accountNumber={}, amount={}", command.accountNumber(), command.amount());
        return account;
    }

    public BankAccount withdraw(BankAccountWithdrawCommand command) {
        BankAccount account = loadAccount(queryService.getAggregateIdByAccountNumber(command.accountNumber()));

        account.withdraw(command.amount());

        saveEvents(account);

        log.info("Withdrawal completed. accountNumber={}, amount={}", command.accountNumber(), command.amount());
        return account;
    }

    public BankAccount transfer(BankAccountTransferCommand command) {
        BankAccount fromAccount = loadAccount(queryService.getAggregateIdByAccountNumber(command.fromAccountNumber()));
        BankAccount toAccount = loadAccount(queryService.getAggregateIdByAccountNumber(command.toAccountNumber()));

        fromAccount.transferTo(command.toAccountNumber(), command.amount());
        toAccount.transferFrom(command.fromAccountNumber(), command.amount());

        saveEvents(fromAccount);
        saveEvents(toAccount);

        log.info("Transfer completed. fromAccount={}, toAccount={}, amount={}", command.fromAccountNumber(), command.toAccountNumber(), command.amount());
        return fromAccount;
    }

    private void saveEvents(BankAccount account) {
        account.getUncommittedEvents()
            .forEach(event -> eventStore.save(account.getAggregateId(), event));
        account.clearUncommittedEvents();
    }

    private BankAccount loadAccount(UUID aggregateId) {
        List<Event> events = eventStore.load(aggregateId);
        return BankAccount.load(aggregateId, events);
    }
}