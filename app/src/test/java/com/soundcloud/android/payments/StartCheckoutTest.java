package com.soundcloud.android.payments;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class StartCheckoutTest {

    @Test
    public void satisfiesEqualsContract() {
        EqualsVerifier.forClass(StartCheckout.class).verify();
    }

}