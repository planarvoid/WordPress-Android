package com.soundcloud.reporting;


import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.soundcloud.java.collections.Lists;
import io.fabric.sdk.android.Fabric;

import java.util.Iterator;
import java.util.List;

public class FabricReporter implements ReportingBackend {
    private final List<CustomEvent> eventQueue = Lists.newArrayList();

    @Override
    public void post(Metric metric) {
        synchronized (eventQueue) {
            eventQueue.add(buildCustomEvent(metric));
            if (Fabric.isInitialized()) {
                Iterator<CustomEvent> iterator = eventQueue.iterator();
                while (iterator.hasNext()) {
                    Answers.getInstance().logCustom(iterator.next());
                    iterator.remove();
                }
            }
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
