package cqrs.bankaccount.model.event;

import cqrs.common.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class DepositEvent extends Event {
    private final BigDecimal amount;

    public DepositEvent(BigDecimal amount, int version) {
        super(UUID.randomUUID(), LocalDateTime.now(), version);
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((amount == null) ? 0 : amount.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DepositEvent other = (DepositEvent) obj;
        if (amount == null) {
            return other.amount == null;
        } else return amount.equals(other.amount);
    }


}
