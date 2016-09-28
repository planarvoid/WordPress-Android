package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TrackPlayQueueUIItemTest {

    @Test
    public void shouldBeDifferentInstance() {
        TrackPlayQueueUIItem track1 = PlayQueueFixtures.getPlayQueueItem(123);
        TrackPlayQueueUIItem track2 = PlayQueueFixtures.getPlayQueueItem(124);

        assertThat(track1).isNotEqualTo(track2);
    }

    @Test
    public void shouldBeSameInstance() {
        TrackPlayQueueUIItem track1 = PlayQueueFixtures.getPlayQueueItem(123);

        assertThat(track1).isEqualTo(track1);
    }
}