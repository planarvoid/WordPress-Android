package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CheckoutStartedTest {

    @Test
    public void mapsCheckoutResultToTokenString() {
        CheckoutStarted result = new CheckoutStarted("token_123");
        assertThat(CheckoutStarted.TOKEN.call(result)).isEqualTo("token_123");
    }

}