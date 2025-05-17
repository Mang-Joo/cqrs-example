package cqrs.common;

import java.util.List;
import java.util.UUID;

public interface EventStore {
    void save(UUID aggregateId, Event event);

    List<Event> load(UUID aggregateId);
}
