package cqrs.bankaccount.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountSnapshot;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.query.BankAccountQueryService;
import cqrs.common.Event;
import cqrs.common.EventStore;
import cqrs.common.snapshot.SnapshotStore;
import cqrs.common.snapshot.SnapshotStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountCommandService {
    private final EventStore eventStore;
    private final BankAccountValidation validation;
    private final BankAccountQueryService queryService;
    private final SnapshotStore<BankAccountSnapshot> snapshotStore;
    private final SnapshotStrategy snapshotStrategy;

    @Transactional
    public BankAccount createAccount(BankAccountCreatedCommand command) {
        if (validation.exists(command.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists");
        }

        BankAccount account = new BankAccount(command.accountNumber(), command.accountHolder(), command.userId());
        saveEvents(account);

        log.info("Account created. accountNumber={}, accountHolder={}", command.accountNumber(), command.accountHolder());
        return account;
    }

    @Transactional
    public BankAccount deposit(BankAccountDepositCommand command) {
        BankAccount account = loadAccount(queryService.getAggregateIdByAccountNumber(command.accountNumber()));

        account.deposit(command.amount());

        saveEvents(account);

        log.info("Deposit completed. accountNumber={}, amount={}", command.accountNumber(), command.amount());
        return account;
    }

    @Transactional
    public BankAccount withdraw(BankAccountWithdrawCommand command) {
        BankAccount account = loadAccount(queryService.getAggregateIdByAccountNumber(command.accountNumber()));

        account.withdraw(command.amount());

        saveEvents(account);

        log.info("Withdrawal completed. accountNumber={}, amount={}", command.accountNumber(), command.amount());
        return account;
    }

    @Transactional
    public BankAccount transfer(BankAccountTransferCommand command) {
        UUID fromAggregateId = queryService.getAggregateIdByAccountNumber(command.fromAccountNumber());
        UUID toAggregateId = queryService.getAggregateIdByAccountNumber(command.toAccountNumber());

        BankAccount fromAccount = loadAccount(fromAggregateId);
        BankAccount toAccount = loadAccount(toAggregateId);

        fromAccount.transferTo(command.toAccountNumber(), command.amount());
        toAccount.transferFrom(command.fromAccountNumber(), command.amount());

        saveEvents(fromAccount);
        saveEvents(toAccount);

        log.info("Transfer completed. fromAccount={}, toAccount={}, amount={}", command.fromAccountNumber(), command.toAccountNumber(), command.amount());
        return fromAccount;
    }

    private void saveEvents(BankAccount account) {
        snapshotIfNeeded(account);
        List<Event> events = account.getUncommittedEvents();
        if (!events.isEmpty()) {
            for (Event event : events) {
                eventStore.save(account.getAggregateId(), event);
            }
            account.clearUncommittedEvents();
        }
    }

    private void snapshotIfNeeded(BankAccount account) {
        if (snapshotStrategy.shouldCreateSnapshot(account.getCurrentVersion())) {
            BankAccountSnapshot snapshot = account.createSnapshot();
            snapshotStore.save(snapshot);
            log.debug("Snapshot created for aggregateId: {}, version: {}", account.getAggregateId(), account.getCurrentVersion());
        }
    }

    private BankAccount loadAccount(UUID aggregateId) {
        Optional<BankAccountSnapshot> snapshot = snapshotStore.findLatest(aggregateId, BankAccountSnapshot.class);

        if (snapshot.isPresent()) {
            BankAccountSnapshot snap = snapshot.get();
            BankAccount account = BankAccount.loadFromSnapshot(
                snap.getAggregateId(),
                snap.getAccountNumber(),
                snap.getAccountHolder(),
                snap.getUserId(),
                snap.getBalance(),
                snap.getVersion()
            );
            account.replayEventsAfterSnapshot(eventStore.load(snap.getAggregateId(), snap.getVersion()));
            log.debug("Account loaded from snapshot. aggregateId: {}, version: {}", aggregateId, snap.getVersion());
            return account;
        }

        List<Event> events = eventStore.load(aggregateId);
        if (events.isEmpty()) {
            throw new IllegalStateException("Account not found or no events for aggregateId: " + aggregateId);
        }
        log.debug("Account loaded from event history. aggregateId: {}, eventCount: {}", aggregateId, events.size());
        return BankAccount.loadFromHistory(aggregateId, events);
    }
}