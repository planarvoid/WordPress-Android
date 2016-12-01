package com.soundcloud.android.events;

import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

public class FileAccessEvent extends LegacyTrackingEvent implements MetricEvent {

    private static final String FILE_ACCESS = "FileAccess";
    private static final String FILE_EXISTS = "FileExists";
    private static final String CAN_WRITE = "CanWrite";
    private static final String CAN_READ = "CanRead";

    private final DataPoint<String> fileExists;
    private final DataPoint<String> canWrite;
    private final DataPoint<String> canRead;

    public FileAccessEvent(boolean fileExists, boolean canWrite, boolean canRead) {
        super(FILE_ACCESS);
        this.fileExists = DataPoint.string(FILE_EXISTS, String.valueOf(fileExists));
        this.canWrite = DataPoint.string(CAN_WRITE, String.valueOf(canWrite));
        this.canRead = DataPoint.string(CAN_READ, String.valueOf(canRead));
    }

    @Override
    public Metric toMetric() {
        return Metric.create(FILE_ACCESS, fileExists, canWrite, canRead);
    }

}
