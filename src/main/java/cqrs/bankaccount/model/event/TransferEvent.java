package cqrs.bankaccount.model.event;

import cqrs.common.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransferEvent extends Event {
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;

    public TransferEvent(UUID fromAccountId, UUID toAccountId, BigDecimal amount, int version) {
        super(UUID.randomUUID(), LocalDateTime.now(), version);
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}