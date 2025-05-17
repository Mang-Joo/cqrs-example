package cqrs.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AggregateTest {

    @Test
    void test_next_version() {
        TestAggregate aggregate = new TestAggregate();
        aggregate.applyEvent(new TestEvent(aggregate.nextVersion()));
        aggregate.applyEvent(new TestEvent(aggregate.nextVersion()));
        aggregate.applyEvent(new TestEvent(aggregate.nextVersion()));

        assertThat(aggregate.getEvents()).hasSize(3);
        assertThat(aggregate.nextVersion()).isEqualTo(3);
    }

    @Test
    void apply_event_success() {
        TestAggregate aggregate = new TestAggregate();
        aggregate.applyEvent(new TestEvent(aggregate.nextVersion()));

        assertThat(aggregate.getEvents()).hasSize(1);
    }


    public class TestAggregate extends Aggregate {

        @Override
        protected void apply(Event event) {
            // do nothing
        }
    }

    public class TestEvent extends Event {

        protected TestEvent(int version) {
            super(UUID.randomUUID(), LocalDateTime.now(), version);
        }
    }
}
