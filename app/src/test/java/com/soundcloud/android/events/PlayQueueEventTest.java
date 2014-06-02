package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueEventTest {

    @Test
    public void shouldCreateEventForNewQueue() {
        PlayQueueEvent event = PlayQueueEvent.fromNewQueue(Urn.forTrack(123));
        expect(event.getKind()).toEqual(0);
        expect(event.getCurrentTrackUrn()).toEqual(Urn.forTrack(123));
    }

    @Test
    public void shouldCreateEventForTrackChange() {
        PlayQueueEvent event = PlayQueueEvent.fromTrackChange(Urn.forTrack(123));
        expect(event.getKind()).toEqual(1);
        expect(event.getCurrentTrackUrn()).toEqual(Urn.forTrack(123));
    }

    @Test
    public void shouldCreateEventForQueueUpdate() {
        PlayQueueEvent event = PlayQueueEvent.fromQueueUpdate(Urn.forTrack(123));
        expect(event.getKind()).toEqual(2);
        expect(event.getCurrentTrackUrn()).toEqual(Urn.forTrack(123));
    }

}