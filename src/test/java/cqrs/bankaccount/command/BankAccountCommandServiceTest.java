package cqrs.bankaccount.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.BankAccountValidation;
import cqrs.bankaccount.model.event.CreateAccountEvent;
import cqrs.bankaccount.model.event.DepositEvent;
import cqrs.bankaccount.model.event.TransferEvent;
import cqrs.bankaccount.model.event.WithdrawEvent;
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
        BankAccountCreateCommand command = new BankAccountCreateCommand("1234567890", "John Doe");

        // When
        BankAccount account = commandService.createAccount(command);

        // Then
        assertThat(account.getAccountNumber()).isEqualTo("1234567890");
        assertThat(account.getAccountHolder()).isEqualTo("John Doe");
    }

    @Test
    void create_account_fail_duplicate() {
        // Given
        given(validation.exists("1234567890")).willReturn(true);
        BankAccountCreateCommand command = new BankAccountCreateCommand("1234567890", "John Doe");

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
        given(eventStore.load(aggregateId)).willReturn(List.of(new CreateAccountEvent(accountNumber, "John Doe", 0)));

        // When
        BankAccount account = commandService.deposit(accountNumber, BigDecimal.valueOf(100));

        // Then
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100));
        verify(eventStore).save(eq(aggregateId), any(DepositEvent.class));
    }

    @Test
    void withdraw_success() {
        // Given
        String accountNumber = "1234567890";
        UUID aggregateId = UUID.randomUUID();
        given(queryService.getAggregateIdByAccountNumber(accountNumber)).willReturn(aggregateId);
        given(eventStore.load(aggregateId)).willReturn(List.of(new CreateAccountEvent(accountNumber, "John Doe", 0), new DepositEvent(BigDecimal.valueOf(100), 1)));

        // When
        BankAccount account = commandService.withdraw(accountNumber, BigDecimal.valueOf(50));

        // Then
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(50));
        verify(eventStore).save(eq(aggregateId), any(WithdrawEvent.class));
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
        given(eventStore.load(fromAggregateId)).willReturn(List.of(new CreateAccountEvent(fromAccountNumber, "John Doe", 0), new DepositEvent(BigDecimal.valueOf(100), 1)));
        given(eventStore.load(toAggregateId)).willReturn(List.of(new CreateAccountEvent(toAccountNumber, "John Doe", 0)));

        // When
        BankAccount fromAccount = commandService.transfer(fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30));

        // Then
        assertThat(fromAccount.getBalance()).isEqualTo(BigDecimal.valueOf(70));
        verify(eventStore).save(eq(fromAggregateId), any(TransferEvent.class));
        verify(eventStore).save(eq(toAggregateId), any(TransferEvent.class));
    }
}