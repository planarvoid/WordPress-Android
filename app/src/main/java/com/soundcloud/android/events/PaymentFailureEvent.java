package com.soundcloud.android.events;

public class PaymentFailureEvent extends TrackingEvent {

    private static final String KEY_REASON = "reason";

    public static PaymentFailureEvent create(String reason) {
        return new PaymentFailureEvent(reason);
    }

    private PaymentFailureEvent(String reason) {
        super(TrackingEvent.KIND_DEFAULT, System.currentTimeMillis());
        put(KEY_REASON, reason);
    }

    public String getReason() {
        return get(KEY_REASON);
    }

    @Override
    public String toString() {
        return "Payment failure: " + getReason();
    }

}
