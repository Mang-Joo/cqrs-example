package cqrs.infrastructure.eventstore;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cqrs.common.Event;
import cqrs.common.EventStore;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaEventStore implements EventStore {
    private final ObjectMapper objectMapper;
    private final EventJpaRepository eventJpaRepository;

    @Override
    public void save(UUID aggregateId, Event event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            EventEntity entity = new EventEntity(
                    aggregateId,
                    event.eventId(),
                    event.getClass().getName(),
                    eventData,
                    event.version(),
                    event.timestamp()
            );
            eventJpaRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalArgumentException("Event serialization failed", e);
        }
    }

    @Override
    public List<Event> load(UUID aggregateId) {
        List<EventEntity> entities = eventJpaRepository.findByAggregateIdOrderByEventVersionAsc(aggregateId);
        return entities
                .stream()
                .map(this::deserializeEvent)
                .sorted(Comparator.comparing(Event::version))
                .toList();
    }

    private Event deserializeEvent(EventEntity entity) {
        try {
            Class<?> clazz = Class.forName(entity.getEventType());
            return (Event) objectMapper.readValue(entity.getEventData(), clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Event deserialization failed", e);
        }
    }

    @Override
    public List<Event> load(UUID aggregateId, int afterVersion) {
        List<EventEntity> entities = eventJpaRepository
                .findByAggregateIdAndEventVersionGreaterThanOrderByEventVersion(aggregateId, afterVersion);

        return entities
                .stream()
                .map(this::deserializeEvent)
                .sorted(Comparator.comparing(Event::version))
                .toList();
    }
}