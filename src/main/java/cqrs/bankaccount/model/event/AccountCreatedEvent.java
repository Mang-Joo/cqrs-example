package cqrs.bankaccount.model.event;

import java.time.LocalDateTime;
import java.util.UUID;

import cqrs.common.Event;

public record AccountCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    String accountNumber,
    String accountHolder,
    LocalDateTime timestamp,
    int version
) implements Event {
}