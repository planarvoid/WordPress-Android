package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class PaymentFailureEventTest extends AndroidUnitTest {

    @Test
    public void eventHasReasonFromCreate() {
        String reason = "Billing unavailable";
        assertThat(PaymentFailureEvent.create(reason).reason()).isEqualTo(reason);
    }

}
