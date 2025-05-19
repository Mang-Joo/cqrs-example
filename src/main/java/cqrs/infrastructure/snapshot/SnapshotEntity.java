package cqrs.infrastructure.snapshot;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "snapshot")
public class SnapshotEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String snapshotType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshotData;


    public SnapshotEntity(
            UUID aggregateId,
            int version,
            String snapshotType,
            String snapshotData
    ) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.snapshotType = snapshotType;
        this.snapshotData = snapshotData;
    }
}
