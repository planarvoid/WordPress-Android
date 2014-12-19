package com.soundcloud.android.events;

public class DeviceMetricsEvent extends TrackingEvent {

    private final long databaseSizeInBytes;

    public DeviceMetricsEvent(long databaseSizeInBytes) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        this.databaseSizeInBytes = databaseSizeInBytes;
    }

    public long getDatabaseSizeInBytes() {
        return databaseSizeInBytes;
    }
}
