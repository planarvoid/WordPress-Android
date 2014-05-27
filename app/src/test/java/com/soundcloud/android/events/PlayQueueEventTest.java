package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueEventTest {

    @Test
    public void shouldCreateEventForNewQueue() {
        PlayQueueEvent event = PlayQueueEvent.fromNewQueue();
        expect(event.getKind()).toEqual(0);
    }

    @Test
    public void shouldCreateEventForTrackChange() {
        PlayQueueEvent event = PlayQueueEvent.fromTrackChange();
        expect(event.getKind()).toEqual(1);
    }

    @Test
    public void shouldCreateEventForQueueUpdate() {
        PlayQueueEvent event = PlayQueueEvent.fromQueueUpdate();
        expect(event.getKind()).toEqual(2);
    }

}