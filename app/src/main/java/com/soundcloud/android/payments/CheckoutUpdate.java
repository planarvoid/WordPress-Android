package com.soundcloud.android.payments;

import com.google.common.base.Objects;
import com.soundcloud.android.payments.googleplay.PlayBillingResult;

final class CheckoutUpdate {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String REASON_OK = "ok";

    public final String state;
    public final String reason;
    public final String payload;
    public final String signature;

    public static CheckoutUpdate fromSuccess(PlayBillingResult result) {
        return new CheckoutUpdate(STATUS_SUCCESS, REASON_OK, result.getData(), result.getSignature());
    }

    public static CheckoutUpdate fromFailure(String reason) {
        return new CheckoutUpdate(STATUS_FAILURE, reason, null, null);
    }

    private CheckoutUpdate(String state, String reason, String payload, String signature) {
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

        CheckoutUpdate that = (CheckoutUpdate) o;
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
