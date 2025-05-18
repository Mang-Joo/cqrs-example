package cqrs.common;

import java.time.LocalDateTime;
import java.util.UUID;

public interface Event {
    UUID eventId();
    UUID aggregateId();
    LocalDateTime timestamp();
    int version();
}