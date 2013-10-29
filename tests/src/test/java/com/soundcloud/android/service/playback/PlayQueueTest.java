package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(1L)
                .exploreTag("explore")
                .originUrl("url/123")
                .recommenderVersion("version1")
                .build();
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L,2L,3L), 0, playSourceInfo, Content.ME_LIKES.uri);
        playQueue.setAppendState(PlayQueue.AppendState.IDLE);

        Parcel parcel = Parcel.obtain();
        playQueue.writeToParcel(parcel, 0);
        PlayQueue copy = PlayQueue.CREATOR.createFromParcel(parcel);

        expect(copy).toContainExactly(1L,2L,3L);
        expect(copy.getPosition()).toBe(0);
        expect(copy.getAppendState()).toEqual(PlayQueue.AppendState.IDLE);
        expect(copy.getPlaySourceInfo()).toEqual(playSourceInfo);
        expect(copy.getSourceUri()).toEqual(Content.ME_LIKES.uri);
    }

    @Test
    public void shouldBuildInititialPlayQueueFromASingleTrackId() {
        PlayQueue playQueue = new PlayQueue(1L);
        expect(playQueue).toContainExactly(1L);
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullySetPositionIfNewPositionIsWithinLowerBounds() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(0)).toBeTrue();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullySetPositionIfNewPositionIsWithinUpperBounds() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(2)).toBeTrue();
        expect(playQueue.getPosition()).toBe(2);
    }

    @Test
    public void shouldFailToSetPositionIfNewPositionIsNotWithinBounds() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.setPosition(3)).toBeFalse();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullyMoveToPreviousTrack() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToPrevious()).toBeTrue();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToPrevious()).toBeFalse();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext()).toBeTrue();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToNext()).toBeFalse();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void isLastTrackForSingleTrackReturnsTrue() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L), 0);

        expect(playQueue.isLastTrack()).toBeTrue();
    }

    @Test
    public void isLastTrackForMultipleTracksReflectsPositionInQueue() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);

        expect(playQueue.isLastTrack()).toBeFalse();
        playQueue.moveToNext();
        expect(playQueue.isLastTrack()).toBeTrue();
    }
}
