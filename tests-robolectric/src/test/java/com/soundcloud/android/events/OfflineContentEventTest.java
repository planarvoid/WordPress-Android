package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentEventTest {

    @Test
    public void implementsEqualsAndHashcode() {
        EqualsVerifier.forClass(OfflineContentEvent.class).verify();
    }

    @Test
    public void createsEventForIdle() {
        OfflineContentEvent event = OfflineContentEvent.idle();
        expect(event.getKind()).toEqual(OfflineContentEvent.IDLE);
    }

    @Test
    public void createsEventForStartSync() {
        OfflineContentEvent event = OfflineContentEvent.start();
        expect(event.getKind()).toEqual(OfflineContentEvent.START);
    }

    @Test
    public void createsEventForStopSync() {
        OfflineContentEvent event = OfflineContentEvent.stop();
        expect(event.getKind()).toEqual(OfflineContentEvent.STOP);
    }

}