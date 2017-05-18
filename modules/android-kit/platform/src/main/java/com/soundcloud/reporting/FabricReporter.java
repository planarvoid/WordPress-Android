package com.soundcloud.reporting;


import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import io.fabric.sdk.android.Fabric;

public class FabricReporter implements ReportingBackend {

    @Override
    public void post(Metric metric) {
        if (Fabric.isInitialized()) {
            Answers.getInstance().logCustom(buildCustomEvent(metric));
        }
    }

    private static CustomEvent buildCustomEvent(Metric metric) {
        final CustomEvent event = new CustomEvent(metric.name());
        for (DataPoint<?> dataPoint : metric.dataPoints()) {
            if (dataPoint.value() instanceof Number) {
                event.putCustomAttribute(dataPoint.key(), (Number) dataPoint.value());
            } else {
                event.putCustomAttribute(dataPoint.key(), dataPoint.value().toString());
            }
        }
        return event;
    }
}
