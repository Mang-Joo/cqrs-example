package cqrs.common.snapshot;

import java.util.UUID;

public interface IsSnapshotData {
    UUID getAggregateId();
    int getVersion();
}
