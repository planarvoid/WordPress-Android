package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

class CheckoutResult {

    public static final Func1<CheckoutResult, String> TOKEN = new Func1<CheckoutResult, String>() {
        @Override
        public String call(CheckoutResult result) {
            return result.token;
        }
    };

    public final String token;

    @JsonCreator
    public CheckoutResult(@JsonProperty("checkout_token") String token) {
        this.token = token;
    }

}
