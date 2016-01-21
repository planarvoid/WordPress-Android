package com.soundcloud.android.events;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class OfflineContentChangedEventTest {

    @Test
    public void implementsEqualsAndHashCode() {
        EqualsVerifier.forClass(OfflineContentChangedEvent.class).verify();
    }

}
