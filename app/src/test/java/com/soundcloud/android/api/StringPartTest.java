package com.soundcloud.android.api;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class StringPartTest {

    @Test
    public void shouldDefineEqualsAndHashCode() {
        EqualsVerifier.forClass(StringPart.class).verify();
    }
}
