package com.soundcloud.android.events;

import com.soundcloud.android.Consts;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

import android.content.Context;

import java.io.File;

public class ExternalDirectoryReportEvent extends TrackingEvent implements MetricEvent {

    private static final String ExternalDirectoryStatus = "ExternalDirectoryStatus";
    private final File contextPath;

    private boolean areExternalDirectoriesTheSame;

    public ExternalDirectoryReportEvent(Context context) {
        super(ExternalDirectoryStatus, System.currentTimeMillis());

        contextPath = context.getExternalFilesDir(null);
        areExternalDirectoriesTheSame = Consts.FILES_PATH.equals(contextPath);
    }

    @Override
    public Metric toMetric() {
        return Metric.create(ExternalDirectoryStatus,
                             DataPoint.string("directoriesEqual", areExternalDirectoriesTheSame ? "yes" : "no"),
                             DataPoint.string("Constant path", String.valueOf(Consts.FILES_PATH)),
                             DataPoint.string("Context path", String.valueOf(contextPath)));
    }

}
