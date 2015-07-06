package com.soundcloud.android.events;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class CurrentDownloadEventTest {

    @Test
    public void implementsEqualsAndHashCode() {
        EqualsVerifier.forClass(CurrentDownloadEvent.class).verify();
    }

}