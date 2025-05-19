package cqrs.infrastructure.snapshot;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cqrs.common.snapshot.IsSnapshotData;
import cqrs.common.snapshot.SnapshotStore;
import lombok.RequiredArgsConstructor;

@Component
@Primary
@RequiredArgsConstructor
public class JpaSnapshotStore<T extends IsSnapshotData> implements SnapshotStore<T> {

    private static final Logger logger = LoggerFactory.getLogger(JpaSnapshotStore.class);
    private final SnapshotJpaRepository snapshotJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<T> findLatest(UUID aggregateId, Class<T> snapshotType) {
        return snapshotJpaRepository.findById(aggregateId)
                .flatMap(entity -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends T> actualSnapshotClass = (Class<? extends T>) Class.forName(entity.getSnapshotType());
                        if (!snapshotType.isAssignableFrom(actualSnapshotClass)) {
                            logger.error("Requested snapshot type {} is not assignable from stored type {}", snapshotType.getName(), actualSnapshotClass.getName());
                            return Optional.empty();
                        }
                        T snapshot = objectMapper.readValue(entity.getSnapshotData(), actualSnapshotClass);
                        return Optional.of(snapshot);
                    } catch (IOException | ClassNotFoundException e) {
                        logger.error("Error deserializing snapshot for aggregateId: {}", aggregateId, e);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public void save(T snapshot) {
        try {
            String snapshotData = objectMapper.writeValueAsString(snapshot);
            SnapshotEntity entity = new SnapshotEntity(
                    snapshot.getAggregateId(),
                    snapshot.getVersion(),
                    snapshot.getClass().getName(),
                    snapshotData
            );
            snapshotJpaRepository.save(entity);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing snapshot for aggregateId: {}", snapshot.getAggregateId(), e);
            throw new RuntimeException("Failed to serialize snapshot", e);
        }
    }
}
