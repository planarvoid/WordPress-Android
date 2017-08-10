package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PaymentFailureEventTest {

    @Test
    public void eventHasReasonFromCreate() {
        String reason = "Billing unavailable";
        assertThat(PaymentFailureEvent.create(reason).reason()).isEqualTo(reason);
    }

}
