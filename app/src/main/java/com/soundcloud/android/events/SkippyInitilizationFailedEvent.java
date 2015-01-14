package com.soundcloud.android.events;

public class SkippyInitilizationFailedEvent extends TrackingEvent {

    public static final String THROWABLE = "throwable";
    public static final String MESSAGE = "message";
    public static final String FAILURE_COUNT = "failure_count";

    public SkippyInitilizationFailedEvent(Throwable throwable, String message, int failureCount) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put(THROWABLE, throwable.toString());
        put(MESSAGE, message);
        put(FAILURE_COUNT, String.valueOf(failureCount));
    }

}
