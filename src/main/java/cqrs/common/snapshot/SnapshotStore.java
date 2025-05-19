package cqrs.common.snapshot;

import java.util.Optional;
import java.util.UUID;

public interface SnapshotStore<T extends IsSnapshotData> {

    Optional<T> findLatest(UUID aggregateId, Class<T> snapshotType);

    void save(T snapshot);
}