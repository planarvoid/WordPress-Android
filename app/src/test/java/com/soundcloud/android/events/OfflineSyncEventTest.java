package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class OfflineSyncEventTest {

    @Test
    public void createsEventForIdle() {
        OfflineSyncEvent event = OfflineSyncEvent.idle();
        expect(event.getKind()).toEqual(OfflineSyncEvent.IDLE);
    }

    @Test
    public void createsEventForStartSync() {
        OfflineSyncEvent event = OfflineSyncEvent.start();
        expect(event.getKind()).toEqual(OfflineSyncEvent.START);
    }

    @Test
    public void createsEventForStopSync() {
        OfflineSyncEvent event = OfflineSyncEvent.stop();
        expect(event.getKind()).toEqual(OfflineSyncEvent.STOP);
    }

}