package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PaymentFailureEventTest {

    @Test
    public void eventHasReasonFromCreate() {
        String reason = "Billing unavailable";
        assertThat(PaymentFailureEvent.create(reason).getReason()).isEqualTo(reason);
    }

}