package com.soundcloud.android.payments;

import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.java.objects.MoreObjects;

final class UpdateCheckout {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String REASON_OK = "ok";

    public final String state;
    public final String reason;
    public final String payload;
    public final String signature;

    public static UpdateCheckout fromSuccess(Payload payload) {
        return new UpdateCheckout(STATUS_SUCCESS, REASON_OK, payload.data, payload.signature);
    }

    public static UpdateCheckout fromFailure(String reason) {
        return new UpdateCheckout(STATUS_FAILURE, reason, null, null);
    }

    private UpdateCheckout(String state, String reason, String payload, String signature) {
        this.state = state;
        this.reason = reason;
        this.payload = payload;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateCheckout that = (UpdateCheckout) o;
        return MoreObjects.equal(state, that.state)
                && MoreObjects.equal(reason, that.reason)
                && MoreObjects.equal(payload, that.payload)
                && MoreObjects.equal(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(state, reason, payload, signature);
    }

}
