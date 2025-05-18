package cqrs.bankaccount.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import cqrs.common.Event;

public record MoneyWithdrawnEvent(
    UUID eventId,
    UUID aggregateId,
    BigDecimal amount,
    LocalDateTime timestamp,
    int version
) implements Event {
}