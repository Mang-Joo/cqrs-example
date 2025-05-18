package cqrs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AggregateRoot {
    private final UUID aggregateId;
    private final List<Event> uncommittedEvents;
    private final int version;
    private final EventApplier eventApplier;

    public AggregateRoot(EventApplier eventApplier) {
        this.aggregateId = UUID.randomUUID();
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = -1;
    }

    public AggregateRoot(UUID aggregateId, List<Event> events, EventApplier eventApplier) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        this.aggregateId = aggregateId;
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = events.size();
        events.forEach(this::applyEvent);
    }

    public void applyEvent(Event event) {
        uncommittedEvents.add(event);
        eventApplier.apply(event);
    }

    public List<Event> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public int nextVersion() {
        return version + 1;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public interface EventApplier {
        void apply(Event event);
    }
}
