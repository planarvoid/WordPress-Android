package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueEventTest {

    @Test
    public void shouldCreateEventForPlayQueueChange() {
        PlayQueueEvent event = PlayQueueEvent.fromQueueChange();
        expect(event.getKind()).toEqual(0);
    }

    @Test
    public void shouldCreateEventForTrackChange() {
        PlayQueueEvent event = PlayQueueEvent.fromTrackChange();
        expect(event.getKind()).toEqual(1);
    }

}