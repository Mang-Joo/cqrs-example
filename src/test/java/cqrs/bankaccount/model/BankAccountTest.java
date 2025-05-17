package cqrs.bankaccount.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import cqrs.bankaccount.model.event.CreateAccountEvent;
import cqrs.bankaccount.model.event.DepositEvent;
import cqrs.bankaccount.model.event.TransferEvent;
import cqrs.bankaccount.model.event.WithdrawEvent;

class BankAccountTest {

    @Test
    void create_account_success() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));

        assertThat(bankAccount.getAccountNumber()).isEqualTo("1234567890");
        assertThat(bankAccount.getAccountHolder()).isEqualTo("John Doe");
        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(bankAccount.getEvents()).hasSize(1);
    }

    @Test
    void deposit_success() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void deposit_fail_less_than_10() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        BigDecimal depositAmount = BigDecimal.valueOf(9);
        DepositEvent depositEvent = new DepositEvent(depositAmount, bankAccount.nextVersion());

        assertThatThrownBy(() -> bankAccount.deposit(depositEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 10");
    }

    @Test
    void event_load_success() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(50), bankAccount.nextVersion()));

        BankAccount loadedBankAccount = new BankAccount(bankAccount.getAggregateId(), bankAccount.getEvents());
        assertThat(loadedBankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(loadedBankAccount.getEvents()).hasSize(3);
    }

    @Test
    void withdraw_success() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));
        bankAccount.withdraw(new WithdrawEvent(BigDecimal.valueOf(50), bankAccount.nextVersion()));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void withdraw_fail_insufficient_balance() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));
        WithdrawEvent withdrawEvent = new WithdrawEvent(BigDecimal.valueOf(150), bankAccount.nextVersion());

        assertThatThrownBy(() -> bankAccount.withdraw(withdrawEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");
    }

    @Test
    void withdraw_fail_less_than_0() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));
        WithdrawEvent withdrawEvent = new WithdrawEvent(BigDecimal.valueOf(0), bankAccount.nextVersion());

        assertThatThrownBy(() -> bankAccount.withdraw(withdrawEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");
    }

    @Test
    void transfer_success_sender() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));
        UUID toAccountId = UUID.randomUUID();

        bankAccount.withdrawForTransfer(new TransferEvent(bankAccount.getAggregateId(), toAccountId, BigDecimal.valueOf(50), bankAccount.nextVersion()));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void transfer_success_receiver() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.depositForTransfer(new TransferEvent(UUID.randomUUID(), bankAccount.getAggregateId(), BigDecimal.valueOf(50), bankAccount.nextVersion()));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void transfer_fail_insufficient_balance_sender() {
        BankAccount bankAccount = BankAccount.create(new CreateAccountEvent("1234567890", "John Doe", 0));
        bankAccount.deposit(new DepositEvent(BigDecimal.valueOf(100), bankAccount.nextVersion()));

        TransferEvent transferEvent = new TransferEvent(bankAccount.getAggregateId(), UUID.randomUUID(), BigDecimal.valueOf(150), bankAccount.nextVersion());

        assertThatThrownBy(() -> bankAccount.withdrawForTransfer(transferEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");
    }

}
