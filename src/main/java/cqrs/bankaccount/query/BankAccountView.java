package cqrs.bankaccount.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_account_view")
public class BankAccountView {
    @Id
    private String accountNumber;

    @Column(nullable = false, unique = true)
    private UUID aggregateId;

    @Column(nullable = false)
    private String accountHolder;

    @Column(nullable = false)
    private BigDecimal balance;

    protected BankAccountView() {
    }

    public BankAccountView(String accountNumber, UUID aggregateId, String accountHolder, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.aggregateId = aggregateId;
        this.accountHolder = accountHolder;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}