package cqrs.bankaccount.model.event;

import cqrs.common.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


public class WithdrawEvent extends Event {

    private final BigDecimal amount;

    public WithdrawEvent(BigDecimal amount, int version) {
        super(UUID.randomUUID(), LocalDateTime.now(), version);
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
