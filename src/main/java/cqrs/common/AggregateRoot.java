package cqrs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AggregateRoot {
    private final UUID aggregateId;
    private final List<Event> uncommittedEvents;
    private int version;
    private final EventApplier eventApplier;

    public AggregateRoot(EventApplier eventApplier) {
        this.aggregateId = UUID.randomUUID();
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = -1;
    }

    public AggregateRoot(UUID aggregateId, List<Event> historicalEvents, EventApplier eventApplier) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        this.aggregateId = aggregateId;
        this.eventApplier = eventApplier;
        this.uncommittedEvents = new ArrayList<>();
        this.version = -1;
        for (Event event : historicalEvents) {
            this.eventApplier.apply(event);
            this.version = event.version();
        }
    }

    public AggregateRoot(UUID aggregateId, int snapshotVersion, EventApplier eventApplier) {
        this.aggregateId = aggregateId;
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = snapshotVersion;
    }

    public void recordAndApplyEvent(Event event) {
        if (event.version() != this.version + 1) {
            throw new IllegalStateException(
                    String.format("Event version mismatch. Expected: %d, Actual: %d for event: %s, aggregateId: %s",
                            this.version + 1, event.version(), event.getClass().getSimpleName(), this.aggregateId)
            );
        }
        uncommittedEvents.add(event);
        eventApplier.apply(event);
        this.version = event.version();
    }

    public void replayEvent(Event event) {
        if (event.version() <= this.version) {
             throw new IllegalStateException(
                    String.format("Replay event version mismatch. Current version: %d, Event version: %d for event: %s, aggregateId: %s",
                            this.version, event.version(), event.getClass().getSimpleName(), this.aggregateId)
            );
        }
        eventApplier.apply(event);
        this.version = event.version();
    }

    public List<Event> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public int getCurrentVersion() {
        return version;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public interface EventApplier {
        void apply(Event event);
    }
}
