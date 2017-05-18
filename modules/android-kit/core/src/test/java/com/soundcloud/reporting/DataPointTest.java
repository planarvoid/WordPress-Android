package com.soundcloud.reporting;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class DataPointTest {

    @Test
    public void shouldDefineEqualsAndHashCode() {
        EqualsVerifier.forClass(DataPoint.class).verify();
    }
}
