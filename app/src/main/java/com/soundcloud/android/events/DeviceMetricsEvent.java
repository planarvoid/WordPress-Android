package com.soundcloud.android.events;

public final class DeviceMetricsEvent extends TrackingEvent {
    private static final int ONE_MB = 1024 * 1024;

    public static final String KEY_DATABASE = "database_size";

    private DeviceMetricsEvent(String kind) {
        super(kind, System.currentTimeMillis());
    }

    public static TrackingEvent forDatabaseSize(long databaseSizeInBytes) {
        return new DeviceMetricsEvent(KEY_DATABASE)
                .put(KEY_DATABASE, toDatabaseSizeBucket(databaseSizeInBytes));
    }

    private static String toDatabaseSizeBucket(long sizeInBytes) {
        if (sizeInBytes < ONE_MB) {
            return "<1mb";
        } else if (sizeInBytes <= 5 * ONE_MB) {
            return "1mb to 5mb";
        } else if (sizeInBytes <= 10 * ONE_MB) {
            return "5mb to 10mb";
        } else if (sizeInBytes <= 20 * ONE_MB) {
            return "10mb to 20mb";
        } else if (sizeInBytes <= 50 * ONE_MB) {
            return "20mb to 50mb";
        } else if (sizeInBytes <= 100 * ONE_MB) {
            return "50mb to 100mb";
        } else if (sizeInBytes <= 200 * ONE_MB) {
            return "100mb to 200mb";
        } else if (sizeInBytes <= 500 * ONE_MB) {
            return "200mb to 500mb";
        } else {
            return ">500mb";
        }
    }
}
