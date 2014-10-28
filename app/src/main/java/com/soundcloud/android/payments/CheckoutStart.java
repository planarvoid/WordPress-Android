package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

class CheckoutStart {

    public static final Func1<CheckoutStart, String> TOKEN = new Func1<CheckoutStart, String>() {
        @Override
        public String call(CheckoutStart result) {
            return result.token;
        }
    };

    public final String token;

    @JsonCreator
    public CheckoutStart(@JsonProperty("checkout_token") String token) {
        this.token = token;
    }

}
