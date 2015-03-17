package com.soundcloud.android.analytics.eventlogger;

import dagger.Lazy;

import javax.inject.Inject;

public class EventLoggerDataBuilderFactory {

    private final Lazy<EventLoggerJsonDataBuilder> eventLoggerJsonDataBuilder;
    private final Lazy<EventLoggerUrlDataBuilder> eventLoggerUrlDataBuilder;

    @Inject
    EventLoggerDataBuilderFactory(Lazy<EventLoggerJsonDataBuilder> eventLoggerJsonDataBuilder, Lazy<EventLoggerUrlDataBuilder> eventLoggerUrlDataBuilder) {
        this.eventLoggerJsonDataBuilder = eventLoggerJsonDataBuilder;
        this.eventLoggerUrlDataBuilder = eventLoggerUrlDataBuilder;
    }

    EventLoggerDataBuilder create(String backend) {
        if (EventLoggerAnalyticsProvider.BATCH_BACKEND_NAME.equals(backend)) {
            return eventLoggerJsonDataBuilder.get();
        } else {
            return eventLoggerUrlDataBuilder.get();
        }
    }

}
