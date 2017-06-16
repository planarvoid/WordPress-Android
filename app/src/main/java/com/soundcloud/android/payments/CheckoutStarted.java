package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class CheckoutStarted {

    public final String token;

    @JsonCreator
    CheckoutStarted(@JsonProperty("checkout_token") String token) {
        this.token = token;
    }

}
