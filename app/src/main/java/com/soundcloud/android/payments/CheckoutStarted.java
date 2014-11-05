package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

class CheckoutStarted {

    public static final Func1<CheckoutStarted, String> TOKEN = new Func1<CheckoutStarted, String>() {
        @Override
        public String call(CheckoutStarted result) {
            return result.token;
        }
    };

    public final String token;

    @JsonCreator
    public CheckoutStarted(@JsonProperty("checkout_token") String token) {
        this.token = token;
    }

}
