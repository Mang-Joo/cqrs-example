package cqrs.bankaccount.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountSnapshot;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.model.event.AccountCreatedEvent;
import cqrs.bankaccount.model.event.MoneyDepositedEvent;
import cqrs.bankaccount.model.event.MoneyTransferEvent;
import cqrs.bankaccount.model.event.MoneyWithdrawnEvent;
import cqrs.bankaccount.query.BankAccountQueryService;
import cqrs.common.Event;
import cqrs.common.EventStore;
import cqrs.common.snapshot.SnapshotStore;
import cqrs.common.snapshot.SnapshotStrategy;

class BankAccountCommandServiceTest {

    private EventStore eventStore;
    private BankAccountValidation validation;
    private BankAccountQueryService queryService;
    private SnapshotStore<BankAccountSnapshot> snapshotStore;
    private SnapshotStrategy snapshotStrategy;
    private BankAccountCommandService commandService;

    private final String TEST_ACCOUNT_NUMBER = "1234567890";
    private final UUID TEST_USER_ID = UUID.randomUUID();
    private final String TEST_ACCOUNT_HOLDER = "John Doe";
    private final UUID TEST_AGGREGATE_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        eventStore = mock(EventStore.class);
        validation = mock(BankAccountValidation.class);
        queryService = mock(BankAccountQueryService.class);
        snapshotStore = mock(SnapshotStore.class);
        snapshotStrategy = mock(SnapshotStrategy.class);
        commandService = new BankAccountCommandService(eventStore, validation, queryService, snapshotStore, snapshotStrategy);

        given(queryService.getAggregateIdByAccountNumber(TEST_ACCOUNT_NUMBER)).willReturn(TEST_AGGREGATE_ID);
    }

    @Test
    @DisplayName("Create account success - no snapshot")
    void create_account_success_no_snapshot() {
        given(validation.exists(TEST_ACCOUNT_NUMBER)).willReturn(false);
        BankAccountCreatedCommand command = new BankAccountCreatedCommand(TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID);
        given(snapshotStrategy.shouldCreateSnapshot(eq(0))).willReturn(false);

        BankAccount account = commandService.createAccount(command);

        assertThat(account.getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER);
        verify(eventStore, times(1)).save(any(UUID.class), any(AccountCreatedEvent.class));
        verify(snapshotStore, never()).save(any(BankAccountSnapshot.class));
    }

    @Nested
    @DisplayName("Deposit Snapshot Creation Tests")
    class DepositSnapshotCreationTests {

        private BankAccount performDepositAndSetupSnapshotStrategy(List<Event> initialEvents, int expectedVersionAfterDeposit, boolean shouldCreateSnapshot) {
            given(eventStore.load(TEST_AGGREGATE_ID)).willReturn(initialEvents);
            given(snapshotStrategy.shouldCreateSnapshot(eq(expectedVersionAfterDeposit))).willReturn(shouldCreateSnapshot);
            return commandService.deposit(new BankAccountDepositCommand(TEST_ACCOUNT_NUMBER, BigDecimal.valueOf(100)));
        }

        @Test
        @DisplayName("Deposit at version 1 - no snapshot")
        void deposit_version1_no_snapshot() {
            List<Event> initialEvents = List.of(
                    new AccountCreatedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID, LocalDateTime.now(), 0)
            );
            BankAccount account = performDepositAndSetupSnapshotStrategy(new ArrayList<>(initialEvents), 1, false);
            assertThat(account.getCurrentVersion()).isEqualTo(1);
            verify(snapshotStore, never()).save(any(BankAccountSnapshot.class));
        }

        @Test
        @DisplayName("Deposit at version 2 - creates snapshot")
        void deposit_version2_creates_snapshot() {
            List<Event> initialEvents = List.of(
                    new AccountCreatedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID, LocalDateTime.now(), 0),
                    new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(50), LocalDateTime.now(), 1)
            );
            BankAccount account = performDepositAndSetupSnapshotStrategy(new ArrayList<>(initialEvents), 2, true);
            assertThat(account.getCurrentVersion()).isEqualTo(2);
            ArgumentCaptor<BankAccountSnapshot> snapshotCaptor = ArgumentCaptor.forClass(BankAccountSnapshot.class);
            verify(snapshotStore, times(1)).save(snapshotCaptor.capture());
            assertThat(snapshotCaptor.getValue().getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deposit at version 3 - no snapshot")
        void deposit_version3_no_snapshot() {
            List<Event> initialEvents = List.of(
                    new AccountCreatedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID, LocalDateTime.now(), 0),
                    new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(50), LocalDateTime.now(), 1),
                    new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(30), LocalDateTime.now(), 2)
            );
            BankAccount account = performDepositAndSetupSnapshotStrategy(new ArrayList<>(initialEvents), 3, false);
            assertThat(account.getCurrentVersion()).isEqualTo(3);
            verify(snapshotStore, never()).save(any(BankAccountSnapshot.class));
        }
    }

    @Nested
    @DisplayName("Account Loading Snapshot Tests")
    class AccountLoadingSnapshotTests {

        @Test
        @DisplayName("Load account with snapshot")
        void loadAccount_with_snapshot() {
            int snapshotVersion = 1;
            BankAccountSnapshot snapshot = new BankAccountSnapshot(TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, BigDecimal.valueOf(100), snapshotVersion, TEST_USER_ID);
            given(snapshotStore.findLatest(TEST_AGGREGATE_ID, BankAccountSnapshot.class)).willReturn(Optional.of(snapshot));

            MoneyDepositedEvent eventAfterSnapshot = new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(50), LocalDateTime.now(), 2);
            given(eventStore.load(TEST_AGGREGATE_ID, snapshotVersion)).willReturn(List.of(eventAfterSnapshot));

            given(snapshotStrategy.shouldCreateSnapshot(any(Integer.class))).willReturn(false);
            BankAccount loadedAccount = commandService.deposit(new BankAccountDepositCommand(TEST_ACCOUNT_NUMBER, BigDecimal.valueOf(30)));

            verify(snapshotStore, times(1)).findLatest(TEST_AGGREGATE_ID, BankAccountSnapshot.class);
            verify(eventStore, times(1)).load(TEST_AGGREGATE_ID, snapshotVersion);
            verify(eventStore, never()).load(TEST_AGGREGATE_ID);

            assertThat(loadedAccount.getAggregateId()).isEqualTo(TEST_AGGREGATE_ID);
            assertThat(loadedAccount.getAccountNumber()).isEqualTo(TEST_ACCOUNT_NUMBER);
            assertThat(loadedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(180));
            assertThat(loadedAccount.getCurrentVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("Load account without snapshot")
        void loadAccount_without_snapshot() {
            given(snapshotStore.findLatest(TEST_AGGREGATE_ID, BankAccountSnapshot.class)).willReturn(Optional.empty());

            List<Event> allEvents = List.of(
                new AccountCreatedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID, LocalDateTime.now(), 0),
                new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(100), LocalDateTime.now(), 1),
                new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(50), LocalDateTime.now(), 2)
            );
            given(eventStore.load(TEST_AGGREGATE_ID)).willReturn(allEvents);

            given(snapshotStrategy.shouldCreateSnapshot(any(Integer.class))).willReturn(false);
            BankAccount loadedAccount = commandService.deposit(new BankAccountDepositCommand(TEST_ACCOUNT_NUMBER, BigDecimal.valueOf(30)));

            verify(snapshotStore, times(1)).findLatest(TEST_AGGREGATE_ID, BankAccountSnapshot.class);
            verify(eventStore, times(1)).load(TEST_AGGREGATE_ID);
            verify(eventStore, never()).load(eq(TEST_AGGREGATE_ID), anyInt());

            assertThat(loadedAccount.getAggregateId()).isEqualTo(TEST_AGGREGATE_ID);
            assertThat(loadedAccount.getBalance()).isEqualTo(BigDecimal.valueOf(180));
            assertThat(loadedAccount.getCurrentVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("Load account without snapshot and no events throws exception")
        void loadAccount_without_snapshot_and_no_events_throws_exception() {
            given(snapshotStore.findLatest(TEST_AGGREGATE_ID, BankAccountSnapshot.class)).willReturn(Optional.empty());
            given(eventStore.load(TEST_AGGREGATE_ID)).willReturn(Collections.emptyList());

            assertThatThrownBy(() -> commandService.deposit(new BankAccountDepositCommand(TEST_ACCOUNT_NUMBER, BigDecimal.valueOf(30))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not found or no events for aggregateId");
        }
    }

    @Test
    @DisplayName("Create account fail - duplicate account number")
    void create_account_fail_duplicate() {
        given(validation.exists(TEST_ACCOUNT_NUMBER)).willReturn(true);
        BankAccountCreatedCommand command = new BankAccountCreatedCommand(TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID);

        assertThatThrownBy(() -> commandService.createAccount(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account number already exists");
    }

    @Test
    @DisplayName("Withdraw success - no snapshot")
    void withdraw_success_no_snapshot() {
        given(eventStore.load(TEST_AGGREGATE_ID)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, TEST_ACCOUNT_NUMBER, TEST_ACCOUNT_HOLDER, TEST_USER_ID, LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), TEST_AGGREGATE_ID, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));
        given(snapshotStrategy.shouldCreateSnapshot(eq(2))).willReturn(false);

        BankAccount account = commandService.withdraw(new BankAccountWithdrawCommand(TEST_ACCOUNT_NUMBER, BigDecimal.valueOf(50)));

        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(account.getCurrentVersion()).isEqualTo(2);
        verify(eventStore).save(eq(TEST_AGGREGATE_ID), any(MoneyWithdrawnEvent.class));
        verify(snapshotStore, never()).save(any(BankAccountSnapshot.class));
    }

    @Test
    @DisplayName("Transfer success")
    void transfer_success() {
        String fromAccountNumber = "1234567890";
        String toAccountNumber = "0987654321";
        UUID fromAggregateId = UUID.randomUUID();
        UUID toAggregateId = UUID.randomUUID();

        given(queryService.getAggregateIdByAccountNumber(fromAccountNumber)).willReturn(fromAggregateId);
        given(queryService.getAggregateIdByAccountNumber(toAccountNumber)).willReturn(toAggregateId);
        given(eventStore.load(fromAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), fromAggregateId, fromAccountNumber, "John Doe", UUID.randomUUID(), LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), fromAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));
        given(eventStore.load(toAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), toAggregateId, toAccountNumber, "John Doe", UUID.randomUUID(), LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), toAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));

        BankAccount fromAccount = commandService.transfer(new BankAccountTransferCommand(fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30)));

        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.valueOf(70));
        verify(eventStore).save(eq(fromAggregateId), any(MoneyTransferEvent.class));
        verify(eventStore).save(eq(toAggregateId), any(MoneyTransferEvent.class));
    }

    @Test
    @DisplayName("Transfer fail - insufficient funds")
    void transfer_fail_insufficient_funds() {
        String fromAccountNumber = "1234567890";
        String toAccountNumber = "0987654321";
        UUID fromAggregateId = UUID.randomUUID();
        UUID toAggregateId = UUID.randomUUID();

        given(queryService.getAggregateIdByAccountNumber(fromAccountNumber)).willReturn(fromAggregateId);
        given(queryService.getAggregateIdByAccountNumber(toAccountNumber)).willReturn(toAggregateId);
        given(eventStore.load(fromAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), fromAggregateId, fromAccountNumber, "John Doe", UUID.randomUUID(), LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), fromAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));
        given(eventStore.load(toAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), toAggregateId, toAccountNumber, "John Doe", UUID.randomUUID(), LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), toAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));

        assertThatThrownBy(() -> commandService.transfer(new BankAccountTransferCommand(fromAccountNumber, toAccountNumber, BigDecimal.valueOf(150))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}