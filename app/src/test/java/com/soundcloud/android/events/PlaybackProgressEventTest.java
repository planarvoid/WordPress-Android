package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class PlaybackProgressEventTest {

    @Test
    public void handleZeroedDurationsGracefullyOnGettingProgressProportion() {
        PlaybackProgressEvent event = new PlaybackProgressEvent(0L, 0L);
        expect(event.getProgressProportion()).toEqual(0.0f);
    }

    @Test
    public void calculateProgressProportion() {
        PlaybackProgressEvent event = new PlaybackProgressEvent(31L, 100L);
        expect(event.getProgressProportion()).toEqual(0.31f);
    }
}