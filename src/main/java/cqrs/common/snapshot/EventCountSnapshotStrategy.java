package cqrs.common.snapshot;

import org.springframework.stereotype.Component;

@Component
public class EventCountSnapshotStrategy implements SnapshotStrategy {

    private final int eventInterval;

    public EventCountSnapshotStrategy() {
        this(3);
    }

    public EventCountSnapshotStrategy(int eventInterval) {
        if (eventInterval <= 0) {
            throw new IllegalArgumentException("Event interval must be positive.");
        }
        this.eventInterval = eventInterval;
    }

    @Override
    public boolean shouldCreateSnapshot(Integer currentVersion) {
        if (currentVersion == null) {
            return false;
        }
        if (currentVersion < 0) return false;

        return (currentVersion + 1) % this.eventInterval == 0;
    }
}