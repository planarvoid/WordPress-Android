package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;

import java.util.Locale;

public class DatabaseMigrationEvent extends LegacyTrackingEvent implements MetricEvent {

    private static final String MIGRATION_STATUS = "MigrationStatus";

    private static final String KIND_SUCCESS = "Success";
    private static final String KIND_FAILURE = "Failed";

    private static final String KEY_DURATION = "Duration";
    private static final String KEY_VERSIONS = "Versions";
    private static final String KEY_REASON = "Reason";

    public static DatabaseMigrationEvent forSuccessfulMigration(long duration) {
        return new DatabaseMigrationEvent(KIND_SUCCESS, duration);
    }

    public static DatabaseMigrationEvent forFailedMigration(int fromVersion, int toVersion,
                                                            long duration, String failureReason) {
        return new DatabaseMigrationEvent(KIND_FAILURE, fromVersion, toVersion, duration, failureReason);
    }

    private DatabaseMigrationEvent(@NotNull String kind, long duration) {
        super(kind);
        put(KEY_DURATION, String.valueOf(duration));
    }

    private DatabaseMigrationEvent(@NotNull String kind, int fromVersion, int toVersion,
                                   long duration, String failureReason) {
        this(kind, duration);
        put(KEY_VERSIONS, getVersionToVersion(fromVersion, toVersion));
        put(KEY_REASON, failureReason);
    }

    @Override
    public Metric toMetric() {
        return Metric.create("DBMigrationsReport", getDataPoints());
    }

    @NonNull
    private DataPoint[] getDataPoints() {
        if (kind.equals(KIND_FAILURE)) {
            return new DataPoint[]{
                    DataPoint.string(MIGRATION_STATUS, kind),
                    DataPoint.string("FailReason", getReason()),
                    DataPoint.string("FailVersions", getVersionToVersion()),
                    DataPoint.numeric("FailDuration", getDuration())
            };
        }
        return new DataPoint[]{
                DataPoint.string(MIGRATION_STATUS, kind),
                DataPoint.numeric("SuccessDuration", getDuration())
        };
    }

    private String getReason() {
        return get(KEY_REASON);
    }

    private String getVersionToVersion() {
        return get(KEY_VERSIONS);
    }

    private long getDuration() {
        return Long.valueOf(get(KEY_DURATION));
    }

    private String getVersionToVersion(int fromVersion, int toVersion) {
        return String.format(Locale.getDefault(), "%1$d to %2$d", fromVersion, toVersion);
    }
}
