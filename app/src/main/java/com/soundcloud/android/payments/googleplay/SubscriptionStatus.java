package com.soundcloud.android.payments.googleplay;

public final class SubscriptionStatus {

    private final String token;
    private final Payload payload;

    public static SubscriptionStatus subscribed(String token, Payload payload) {
        return new SubscriptionStatus(token, payload);
    }

    public static SubscriptionStatus notSubscribed() {
        return new SubscriptionStatus(null, null);
    }

    private SubscriptionStatus(String token, Payload payload) {
        this.token = token;
        this.payload = payload;
    }

    public boolean isSubscribed() {
        return payload != null;
    }

    public String getToken() {
        return token;
    }

    public Payload getPayload() {
        return payload;
    }

}
