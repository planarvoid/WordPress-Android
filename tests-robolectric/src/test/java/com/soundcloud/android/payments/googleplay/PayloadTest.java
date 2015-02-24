package com.soundcloud.android.payments.googleplay;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PayloadTest {

    @Test
    public void satisfiesEqualsContract() {
        EqualsVerifier.forClass(Payload.class).verify();
    }

}