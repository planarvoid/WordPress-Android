package com.soundcloud.android.events;

public class SkippyInitilizationFailedEvent extends LegacyTrackingEvent {

    public static final String THROWABLE = "throwable";
    public static final String MESSAGE = "message";
    public static final String FAILURE_COUNT = "failure_count";
    public static final String SUCCESS_COUNT = "success_count";
    public static final String HAS_SUCCEEDED = "has_succeeded";

    public SkippyInitilizationFailedEvent(Throwable throwable, String message, int failureCount, int successCount) {
        super(KIND_DEFAULT);
        put(THROWABLE, throwable.toString());
        put(MESSAGE, message);
        put(FAILURE_COUNT, String.valueOf(failureCount));
        put(SUCCESS_COUNT, String.valueOf(successCount));
        put(HAS_SUCCEEDED, successCount > 0 ? "true" : "false"); // for easier binary filtering in localytics
    }

}
