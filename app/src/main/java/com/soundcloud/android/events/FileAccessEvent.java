package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class FileAccessEvent extends NewTrackingEvent implements MetricEvent {

    private static final String FILE_ACCESS = "FileAccess";
    private static final String FILE_EXISTS = "FileExists";
    private static final String CAN_WRITE = "CanWrite";
    private static final String CAN_READ = "CanRead";

    abstract boolean fileExists();

    abstract boolean canWrite();

    abstract boolean canRead();

    public static FileAccessEvent create(boolean fileExists, boolean canWrite, boolean canRead) {
        return new AutoValue_FileAccessEvent(defaultId(), defaultTimestamp(), Optional.absent(), fileExists, canWrite, canRead);
    }

    @Override
    public FileAccessEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_FileAccessEvent(id(), timestamp(), Optional.of(referringEvent), fileExists(), canWrite(), canRead());
    }

    @Override
    public Metric toMetric() {
        return Metric.create(FILE_ACCESS,
                             DataPoint.string(FILE_EXISTS, String.valueOf(fileExists())),
                             DataPoint.string(CAN_WRITE, String.valueOf(canWrite())),
                             DataPoint.string(CAN_READ, String.valueOf(canRead())));
    }

}
