package com.soundcloud.android.events;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class TrackingEventTest {

    @Test
    public void shouldImplementEqualsAndHashCode() {
        EqualsVerifier.forClass(TrackingEvent.class).usingGetClass().allFieldsShouldBeUsed().verify();
    }
}