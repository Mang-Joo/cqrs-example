package cqrs.bankaccount.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import cqrs.common.Event;

public record MoneyTransferEvent(
    UUID eventId,
    UUID aggregateId,
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount,
    LocalDateTime timestamp,
    int version
) implements Event {
}
