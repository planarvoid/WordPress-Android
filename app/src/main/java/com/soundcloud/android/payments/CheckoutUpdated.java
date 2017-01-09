package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

class CheckoutUpdated {

    public final String state;
    public final String reason;
    public final String token;

    public static final Func1<CheckoutUpdated, PurchaseStatus> TO_STATUS = update -> {
        switch (update.state) {
            case "pending":
                return PurchaseStatus.PENDING;
            case "successful":
                return PurchaseStatus.SUCCESS;
            case "failed":
                return PurchaseStatus.VERIFY_FAIL;
            default:
                return PurchaseStatus.NONE;
        }
    };

    @JsonCreator
    public CheckoutUpdated(@JsonProperty("state") String state,
                           @JsonProperty("reason") String reason,
                           @JsonProperty("checkout_token") String token) {
        this.state = state;
        this.reason = reason;
        this.token = token;
    }

}
