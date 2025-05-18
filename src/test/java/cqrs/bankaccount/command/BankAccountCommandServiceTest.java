package cqrs.bankaccount.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.model.event.AccountCreatedEvent;
import cqrs.bankaccount.model.event.MoneyDepositedEvent;
import cqrs.bankaccount.model.event.MoneyTransferEvent;
import cqrs.bankaccount.model.event.MoneyWithdrawnEvent;
import cqrs.bankaccount.query.BankAccountQueryService;
import cqrs.common.EventStore;

class BankAccountCommandServiceTest {

    private EventStore eventStore = mock(EventStore.class);
    private BankAccountValidation validation = mock(BankAccountValidation.class);
    private BankAccountQueryService queryService = mock(BankAccountQueryService.class);
    private BankAccountCommandService commandService;

    @BeforeEach
    void setup() {
        commandService = new BankAccountCommandService(eventStore, validation, queryService);
    }

    @Test
    void create_account_success() {
        // Given
        given(validation.exists("1234567890")).willReturn(false);
        BankAccountCreatedCommand command = new BankAccountCreatedCommand("1234567890", "John Doe");

        // When
        BankAccount account = commandService.createAccount(command);

        // Then
        assertThat(account.getAccountNumber()).isEqualTo("1234567890");
        assertThat(account.getAccountHolder()).isEqualTo("John Doe");
        verify(eventStore, times(1)).save(any(UUID.class), any(AccountCreatedEvent.class));
    }

    @Test
    void create_account_fail_duplicate() {
        // Given
        given(validation.exists("1234567890")).willReturn(true);
        BankAccountCreatedCommand command = new BankAccountCreatedCommand("1234567890", "John Doe");

        // When & Then
        assertThatThrownBy(() -> commandService.createAccount(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account number already exists");
    }

    @Test
    void deposit_success() {
        // Given
        String accountNumber = "1234567890";
        UUID aggregateId = UUID.randomUUID();
        given(queryService.getAggregateIdByAccountNumber(accountNumber)).willReturn(aggregateId);
        given(eventStore.load(aggregateId)).willReturn(List.of(new AccountCreatedEvent(UUID.randomUUID(), aggregateId, accountNumber, "John Doe", LocalDateTime.now(), 0)));

        // When
        BankAccount account = commandService.deposit(new BankAccountDepositCommand(accountNumber, BigDecimal.valueOf(100)));

        // Then
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100));
        verify(eventStore).save(eq(aggregateId), any(MoneyDepositedEvent.class));
    }

    @Test
    void withdraw_success() {
        // Given
        String accountNumber = "1234567890";
        UUID aggregateId = UUID.randomUUID();
        given(queryService.getAggregateIdByAccountNumber(accountNumber)).willReturn(aggregateId);
        given(eventStore.load(aggregateId)).willReturn(List.of(new AccountCreatedEvent(UUID.randomUUID(), aggregateId, accountNumber, "John Doe", LocalDateTime.now(), 0), new MoneyDepositedEvent(UUID.randomUUID(), aggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)));

        // When
        BankAccount account = commandService.withdraw(new BankAccountWithdrawCommand(accountNumber, BigDecimal.valueOf(50)));

        // Then
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(50));
        verify(eventStore).save(eq(aggregateId), any(MoneyWithdrawnEvent.class));
    }

    @Test
    void transfer_success() {
        // Given
        String fromAccountNumber = "1234567890";
        String toAccountNumber = "0987654321";
        UUID fromAggregateId = UUID.randomUUID();
        UUID toAggregateId = UUID.randomUUID();

        given(queryService.getAggregateIdByAccountNumber(fromAccountNumber)).willReturn(fromAggregateId);
        given(queryService.getAggregateIdByAccountNumber(toAccountNumber)).willReturn(toAggregateId);
        given(eventStore.load(fromAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), fromAggregateId, fromAccountNumber, "John Doe", LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), fromAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));
        given(eventStore.load(toAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), toAggregateId, toAccountNumber, "John Doe", LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), toAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));

        // When
        BankAccount fromAccount = commandService.transfer(new BankAccountTransferCommand(fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30)));

        // Then
        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.valueOf(70));
        verify(eventStore).save(eq(fromAggregateId), any(MoneyTransferEvent.class));
        verify(eventStore).save(eq(toAggregateId), any(MoneyTransferEvent.class));
    }

    @Test
    void transfer_fail_insufficient_funds() {
        // Given
        String fromAccountNumber = "1234567890";
        String toAccountNumber = "0987654321";
        UUID fromAggregateId = UUID.randomUUID();
        UUID toAggregateId = UUID.randomUUID();

        given(queryService.getAggregateIdByAccountNumber(fromAccountNumber)).willReturn(fromAggregateId);
        given(queryService.getAggregateIdByAccountNumber(toAccountNumber)).willReturn(toAggregateId);
        given(eventStore.load(fromAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), fromAggregateId, fromAccountNumber, "John Doe", LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), fromAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));
        given(eventStore.load(toAggregateId)).willReturn(List.of(
            new AccountCreatedEvent(UUID.randomUUID(), toAggregateId, toAccountNumber, "John Doe", LocalDateTime.now(), 0),
            new MoneyDepositedEvent(UUID.randomUUID(), toAggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1)
        ));

        // When & Then
        assertThatThrownBy(() -> commandService.transfer(new BankAccountTransferCommand(fromAccountNumber, toAccountNumber, BigDecimal.valueOf(150))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}