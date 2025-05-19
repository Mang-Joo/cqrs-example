package cqrs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AggregateRoot {
    private final UUID aggregateId;
    private final List<Event> uncommittedEvents;
    private int version;
    private final EventApplier eventApplier;

    /**
     * 새로운 애그리거트를 생성합니다.
     */
    public AggregateRoot(EventApplier eventApplier) {
        this.aggregateId = UUID.randomUUID();
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = -1;
    }

    /**
     * 기존 이벤트 목록으로부터 애그리거트를 재구성합니다. (예: 스냅샷 없이 처음부터 로드)
     */
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

    /**
     * 스냅샷으로부터 애그리거트를 재구성합니다.
     */
    public AggregateRoot(UUID aggregateId, int snapshotVersion, EventApplier eventApplier) {
        this.aggregateId = aggregateId;
        this.uncommittedEvents = new ArrayList<>();
        this.eventApplier = eventApplier;
        this.version = snapshotVersion;
    }

    /**
     * 새로운 비즈니스 이벤트를 기록하고 적용합니다.
     * 이벤트의 버전은 현재 애그리거트 버전 + 1 이어야 합니다.
     */
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

    /**
     * 스냅샷 이후의 이벤트를 재적용(replay)합니다.
     * 이 메서드는 uncommittedEvents에 이벤트를 추가하지 않습니다.
     */
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
