package com.soundcloud.android.payments;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class UpdateCheckoutTest {

    @Test
    public void satisfiesEqualsContract() {
        EqualsVerifier.forClass(UpdateCheckout.class).verify();
    }

}