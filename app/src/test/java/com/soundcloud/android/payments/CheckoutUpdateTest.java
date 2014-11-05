package com.soundcloud.android.payments;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutUpdateTest {

    @Test
    public void satisfiesEqualsContract() {
        EqualsVerifier.forClass(CheckoutUpdate.class).verify();
    }

}