package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class TrackingPlayQueueTest {

    @Test
    public void shouldBeParcelable() throws Exception {

        final TrackSourceInfo trackSourceInfo = TrackSourceInfo.fromExplore("version:1");
        final PlaySessionSource playSessionSource = new PlaySessionSource(Uri.parse("origin:page"), 123L);

        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L,2L,3L), 0, playSessionSource, trackSourceInfo);
        playQueue.setAppendState(TrackingPlayQueue.AppendState.IDLE);
        playQueue.setCurrentTrackToUserTriggered();
        String eventLoggerParams = playQueue.getCurrentEventLoggerParams();

        Parcel parcel = Parcel.obtain();
        playQueue.writeToParcel(parcel, 0);
        TrackingPlayQueue copy = TrackingPlayQueue.CREATOR.createFromParcel(parcel);

        expect(copy).toContainExactly(1L,2L,3L);
        expect(copy.getPosition()).toBe(0);
        expect(copy.getAppendState()).toEqual(TrackingPlayQueue.AppendState.IDLE);
        expect(copy.getPlaySessionSource()).toEqual(playSessionSource);
        expect(copy.getCurrentEventLoggerParams()).toEqual(eventLoggerParams);
        expect(copy.getOriginPage()).toEqual(Content.ME_LIKES.uri);
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToNext(false)).toBeFalse();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void isLastTrackForMultipleTracksReflectsPositionInQueue() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 0);

        expect(playQueue.isLastTrack()).toBeFalse();
        playQueue.moveToNext(false);
        expect(playQueue.isLastTrack()).toBeTrue();
    }

    @Test
    public void shouldSetCurrentTriggerToManual() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 0);
        playQueue.setCurrentTrackToUserTriggered();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToNextShouldResultInAutoTrigger() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto");
    }

    @Test
    public void moveToNextShouldResultInManualTrigger() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(true)).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToPrevious()).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void shouldReturnSetAsPartOfLoggerParams() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(1L, 2L), 1, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&set_id=54321&set_position=1");
    }

    @Test
    public void shouldReturnExploreVersionInEventLoggerParamsWhenCurrentTrackIsInitialTrack() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(123L, 456L), 0, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&source=explore&source_version=exp1");
    }

    @Test
    public void shouldReturnRecommenderVersionInEventLoggerParamsWhenCurrentTrackIsNotInitialTrack() {
        TrackingPlayQueue playQueue = new TrackingPlayQueue(Lists.newArrayList(123L, 456L), 1, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&source=recommender&source_version=rec1");
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        expect(TrackingPlayQueue.EMPTY.getCurrentEventLoggerParams()).toEqual("");

    }

    private void checkManualTrigger(TrackingPlayQueue playQueue) {
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=manual");
    }
}
