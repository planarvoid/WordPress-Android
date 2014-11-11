package com.soundcloud.android.payments;

import com.google.common.base.Objects;
import com.soundcloud.android.payments.googleplay.Payload;

final class UpdateCheckout {

    private static final String STATUS_SUCCESS = "successful";
    private static final String STATUS_FAILURE = "failed";
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
        return Objects.equal(state, that.state)
                && Objects.equal(reason, that.reason)
                && Objects.equal(payload, that.payload)
                && Objects.equal(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(state, reason, payload, signature);
    }

}
