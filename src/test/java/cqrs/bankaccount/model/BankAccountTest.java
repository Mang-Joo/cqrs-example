package cqrs.bankaccount.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BankAccountTest {

    @Test
    void create_account_success() {
        // When
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");

        // Then
        assertThat(bankAccount.getAccountNumber()).isEqualTo("1234567890");
        assertThat(bankAccount.getAccountHolder()).isEqualTo("John Doe");
        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(bankAccount.getUncommittedEvents()).hasSize(1);
    }

    @Test
    void create_account_fail_invalid_accountNumber() {
        // When & Then
        assertThatThrownBy(() -> new BankAccount("", "John Doe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_account_fail_invalid_accountHolder() {
        // When & Then
        assertThatThrownBy(() -> new BankAccount("1234567890", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deposit_success() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void deposit_fail_less_than_10() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        BigDecimal depositAmount = BigDecimal.valueOf(9);

        assertThatThrownBy(() -> bankAccount.deposit(depositAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 10");
    }

    @Test
    void event_load_success() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));
        bankAccount.deposit(BigDecimal.valueOf(50));

        BankAccount loadedBankAccount = new BankAccount(bankAccount.getAggregateId(), bankAccount.getUncommittedEvents());
        assertThat(loadedBankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(loadedBankAccount.getUncommittedEvents()).hasSize(3);
    }

    @Test
    void withdraw_success() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));
        bankAccount.withdraw(BigDecimal.valueOf(50));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void withdraw_fail_insufficient_balance() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> bankAccount.withdraw(BigDecimal.valueOf(150)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");
    }

    @Test
    void withdraw_fail_less_than_0() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> bankAccount.withdraw(BigDecimal.valueOf(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    @Test
    void transfer_success_sender() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));

        bankAccount.transferTo("0987654321", BigDecimal.valueOf(50));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void transfer_success_receiver() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.transferFrom("0987654321", BigDecimal.valueOf(50));

        assertThat(bankAccount.getBalance()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void transfer_fail_insufficient_balance_sender() {
        BankAccount bankAccount = new BankAccount("1234567890", "John Doe");
        bankAccount.deposit(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> bankAccount.transferTo("0987654321", BigDecimal.valueOf(150)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");
    }

}
