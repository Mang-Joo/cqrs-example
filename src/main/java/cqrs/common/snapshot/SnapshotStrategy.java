package cqrs.common.snapshot;


@FunctionalInterface
public interface SnapshotStrategy {

    boolean shouldCreateSnapshot(Integer currentVersion);
}