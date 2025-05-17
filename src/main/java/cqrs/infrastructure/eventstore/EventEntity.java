package cqrs.infrastructure.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "event_store")
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(nullable = false)
    private int eventVersion;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected EventEntity() {
    }

    public EventEntity(
            UUID aggregateId,
            UUID eventId,
            String eventType,
            String eventData,
            int eventVersion,
            LocalDateTime createdAt
    ) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventVersion = eventVersion;
        this.createdAt = createdAt;
    }
}