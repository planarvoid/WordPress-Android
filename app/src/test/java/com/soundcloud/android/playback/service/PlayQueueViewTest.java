package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueViewTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L, 3L), 0);

        Parcel parcel = Parcel.obtain();
        playQueue.writeToParcel(parcel, 0);
        PlayQueueView copy = PlayQueueView.CREATOR.createFromParcel(parcel);

        expect(copy).toContainExactly(1L, 2L, 3L);
        expect(copy.getPosition()).toBe(0);
    }

    @Test
    public void shouldBuildInititialPlayQueueFromASingleTrackId() {
        PlayQueueView playQueue = new PlayQueueView(1L);
        expect(playQueue).toContainExactly(1L);
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldBuildInititialPlayQueueWithPositionOutOfLowerBounds() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L), -1);
        expect(playQueue).toContainExactly(1L, 2L);
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldBuildInitialPlayQueueWithPositionOutOfUpperBounds() throws Exception {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L), 2);
        expect(playQueue).toContainExactly(1L, 2L);
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullySetPositionIfNewPositionIsWithinLowerBounds() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(0)).toBeTrue();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullySetPositionIfNewPositionIsWithinUpperBounds() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(2)).toBeTrue();
        expect(playQueue.getPosition()).toBe(2);
    }

    @Test
    public void shouldFailToSetPositionIfNewPositionIsNotWithinBounds() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(3)).toBeFalse();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void isLastTrackForSingleTrackReturnsTrue() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L), 0);

        expect(playQueue.isLastTrack()).toBeTrue();
    }

    @Test
    public void isLastTrackForMultipleTracksReflectsPositionInQueue() {
        PlayQueueView playQueue = new PlayQueueView(Lists.newArrayList(1L, 2L), 0);

        expect(playQueue.isLastTrack()).toBeFalse();
        playQueue.setPosition(1);
        expect(playQueue.isLastTrack()).toBeTrue();
    }

}
