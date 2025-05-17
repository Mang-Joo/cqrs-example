package cqrs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Aggregate {
    private final UUID aggregateId;
    private final List<Event> events;

    protected Aggregate() {
        this.aggregateId = UUID.randomUUID();
        this.events = new ArrayList<>();
    }

    protected Aggregate(UUID aggregateId, List<Event> events) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        this.aggregateId = aggregateId;
        this.events = new ArrayList<>();
        events.forEach(this::applyEvent);
    }

    protected void applyEvent(Event event) {
        events.add(event);
        apply(event);
    }

    protected abstract void apply(Event event);

    public List<Event> getEvents() {
        return List.copyOf(events);
    }

    public int nextVersion() {
        return events.size();
    }

    public UUID getAggregateId() {
        return aggregateId;
    }
}
