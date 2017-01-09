package com.soundcloud.android.events;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TrackingEventTest {

    @Test
    public void shouldImplementEqualsAndHashCode() {
        EqualsVerifier.forClass(LegacyTrackingEvent.class).usingGetClass().verify();
    }
}
