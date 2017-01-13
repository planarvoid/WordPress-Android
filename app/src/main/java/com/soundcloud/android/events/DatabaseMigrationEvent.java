package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

import android.support.annotation.NonNull;

import java.util.Locale;

@AutoValue
public abstract class DatabaseMigrationEvent extends NewTrackingEvent implements MetricEvent {

    private static final String MIGRATION_STATUS = "MigrationStatus";

    enum Kind {
        SUCCESS("Success"),
        FAILURE("Failed");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    abstract Kind kind();

    abstract long duration();

    abstract Optional<String> versions();

    abstract Optional<String> failureReason();

    public static DatabaseMigrationEvent forSuccessfulMigration(long duration) {
        return builder(Kind.SUCCESS, duration).build();
    }

    public static DatabaseMigrationEvent forFailedMigration(int fromVersion, int toVersion,
                                                            long duration, String failureReason) {
        return builder(Kind.FAILURE, duration).versions(Optional.of(getVersionToVersion(fromVersion, toVersion))).failureReason(Optional.of(failureReason)).build();
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_DatabaseMigrationEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @Override
    public Metric toMetric() {
        return Metric.create("DBMigrationsReport", getDataPoints());
    }

    @NonNull
    private DataPoint[] getDataPoints() {
        if (kind() == Kind.FAILURE) {
            return new DataPoint[]{
                    DataPoint.string(MIGRATION_STATUS, kind().toString()),
                    DataPoint.string("FailReason", failureReason().get()),
                    DataPoint.string("FailVersions", versions().get()),
                    DataPoint.numeric("FailDuration", duration())
            };
        }
        return new DataPoint[]{
                DataPoint.string(MIGRATION_STATUS, kind().toString()),
                DataPoint.numeric("SuccessDuration", duration())
        };
    }

    private static String getVersionToVersion(int fromVersion, int toVersion) {
        return String.format(Locale.getDefault(), "%1$d to %2$d", fromVersion, toVersion);
    }


    private static DatabaseMigrationEvent.Builder builder(Kind kind, long duration) {
        return new AutoValue_DatabaseMigrationEvent.Builder().id(defaultId())
                                                             .timestamp(defaultTimestamp())
                                                             .referringEvent(Optional.absent())
                                                             .kind(kind)
                                                             .duration(duration)
                                                             .versions(Optional.absent())
                                                             .failureReason(Optional.absent());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder kind(Kind kind);

        abstract Builder duration(long duration);

        abstract Builder versions(Optional<String> versions);

        abstract Builder failureReason(Optional<String> failureReason);

        abstract DatabaseMigrationEvent build();
    }
}
