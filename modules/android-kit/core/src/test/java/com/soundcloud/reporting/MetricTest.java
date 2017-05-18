package com.soundcloud.reporting;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class MetricTest {

    @Test
    public void shouldDefineEqualsAndHashCode() {
        EqualsVerifier.forClass(Metric.class).verify();
    }

}
