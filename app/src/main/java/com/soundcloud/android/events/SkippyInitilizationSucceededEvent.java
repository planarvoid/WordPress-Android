package com.soundcloud.android.events;

public class SkippyInitilizationSucceededEvent extends LegacyTrackingEvent {

    public static final String FAILURE_COUNT = "failure_count";
    public static final String SUCCESS_COUNT = "success_count";
    public static final String HAS_FAILED = "has_failed";

    public SkippyInitilizationSucceededEvent(int failureCount, int successCount) {
        super(KIND_DEFAULT);
        put(FAILURE_COUNT, String.valueOf(failureCount));
        put(SUCCESS_COUNT, String.valueOf(successCount));
        put(HAS_FAILED, failureCount > 0 ? "true" : "false"); // for easier binary filtering in localytics
    }
}
