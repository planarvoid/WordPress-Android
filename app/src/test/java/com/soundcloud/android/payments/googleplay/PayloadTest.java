package com.soundcloud.android.payments.googleplay;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class PayloadTest {

    @Test
    public void satisfiesEqualsContract() {
        EqualsVerifier.forClass(Payload.class).verify();
    }

}