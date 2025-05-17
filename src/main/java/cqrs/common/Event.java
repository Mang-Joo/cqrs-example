package cqrs.common;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class Event {
    private final UUID eventId;
    private final LocalDateTime timestamp;
    private final int version;

    protected Event(UUID eventId, LocalDateTime timestamp, int version) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.version = version;
    }

}
