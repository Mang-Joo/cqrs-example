package cqrs.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SnapshotJpaRepository extends JpaRepository<SnapshotEntity, UUID> {
}
