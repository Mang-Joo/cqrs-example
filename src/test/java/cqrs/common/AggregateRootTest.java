package cqrs.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AggregateRootTest {

    @Test
    void create_new_aggregate_root() {
        // Given
        TestEventApplier eventApplier = new TestEventApplier();

        // When
        AggregateRoot aggregateRoot = new AggregateRoot(eventApplier);

        // Then
        assertThat(aggregateRoot.getAggregateId()).isNotNull();
        assertThat(aggregateRoot.getUncommittedEvents()).isEmpty();
        assertThat(aggregateRoot.getCurrentVersion()).isEqualTo(-1);
    }

    @Test
    void create_aggregate_root_from_history() {
        // Given
        UUID aggregateId = UUID.randomUUID();
        TestEventApplier eventApplier = new TestEventApplier();
        TestEvent event1 = new TestEvent("이벤트1", aggregateId, 0);
        TestEvent event2 = new TestEvent("이벤트2", aggregateId, 1);
        List<Event> events = List.of(event1, event2);

        // When
        AggregateRoot aggregateRoot = new AggregateRoot(aggregateId, events, eventApplier);

        // Then
        assertThat(aggregateRoot.getAggregateId()).isEqualTo(aggregateId);
        assertThat(aggregateRoot.getUncommittedEvents()).isEmpty();
        assertThat(aggregateRoot.getCurrentVersion()).isEqualTo(events.size() - 1);
        assertThat(eventApplier.getAppliedEvents()).containsExactly(event1, event2);
    }

    @Test
    void throw_exception_when_id_is_null() {
        // Given
        TestEventApplier eventApplier = new TestEventApplier();
        List<Event> events = new ArrayList<>();

        // When & Then
        assertThatThrownBy(() -> new AggregateRoot(null, events, eventApplier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aggregate ID is required");
    }

    @Test
    void apply_event_adds_to_uncommitted_events_and_applies() {
        // Given
        TestEventApplier eventApplier = new TestEventApplier();
        AggregateRoot aggregateRoot = new AggregateRoot(eventApplier);
        TestEvent event = new TestEvent("테스트 이벤트", aggregateRoot.getAggregateId(), aggregateRoot.getCurrentVersion() + 1);

        // When
        aggregateRoot.recordAndApplyEvent(event);

        // Then
        assertThat(aggregateRoot.getUncommittedEvents()).containsExactly(event);
        assertThat(eventApplier.getAppliedEvents()).containsExactly(event);
    }

    @Test
    void clear_uncommitted_events() {
        // Given
        TestEventApplier eventApplier = new TestEventApplier();
        AggregateRoot aggregateRoot = new AggregateRoot(eventApplier);
        TestEvent event = new TestEvent("테스트 이벤트", aggregateRoot.getAggregateId(), aggregateRoot.getCurrentVersion() + 1);
        aggregateRoot.recordAndApplyEvent(event);

        // When
        aggregateRoot.clearUncommittedEvents();

        // Then
        assertThat(aggregateRoot.getUncommittedEvents()).isEmpty();
        assertThat(eventApplier.getAppliedEvents()).containsExactly(event);
    }

    @Test
    void uncommitted_events_returns_immutable_copy() {
        // Given
        TestEventApplier eventApplier = new TestEventApplier();
        AggregateRoot aggregateRoot = new AggregateRoot(eventApplier);
        TestEvent event = new TestEvent("테스트 이벤트", aggregateRoot.getAggregateId(), aggregateRoot.getCurrentVersion() + 1);
        aggregateRoot.recordAndApplyEvent(event);

        // When
        List<Event> uncommittedEvents = aggregateRoot.getUncommittedEvents();

        // Then
        assertThatThrownBy(() -> uncommittedEvents.add(new TestEvent("새 이벤트", UUID.randomUUID(), 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static class TestEvent implements Event {
        private final UUID eventId;
        private final String name;
        private final UUID aggregateId;
        private final LocalDateTime timestamp;
        private final int version;

        public TestEvent(String name, UUID aggregateId, int version) {
            this.eventId = UUID.randomUUID();
            this.name = name;
            this.aggregateId = aggregateId;
            this.timestamp = LocalDateTime.now();
            this.version = version;
        }

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public UUID aggregateId() {
            return aggregateId;
        }

        @Override
        public LocalDateTime timestamp() {
            return timestamp;
        }

        @Override
        public int version() {
            return version;
        }

        @Override
        public String toString() {
            return "TestEvent{" +
                    "name='" + name + '\'' +
                    ", eventId=" + eventId +
                    ", aggregateId=" + aggregateId +
                    ", timestamp=" + timestamp +
                    ", version=" + version +
                    '}';
        }
    }

    private static class TestEventApplier implements AggregateRoot.EventApplier {
        private final List<Event> appliedEvents = new ArrayList<>();

        @Override
        public void apply(Event event) {
            appliedEvents.add(event);
        }

        public List<Event> getAppliedEvents() {
            return appliedEvents;
        }
    }
}