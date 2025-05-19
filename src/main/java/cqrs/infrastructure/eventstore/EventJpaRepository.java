package cqrs.infrastructure.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findByAggregateIdOrderByEventVersionAsc(UUID aggregateId);

    List<EventEntity> findByAggregateIdAndEventVersionGreaterThanOrderByEventVersion(UUID aggregateId, int eventVersionIsGreaterThan);
}