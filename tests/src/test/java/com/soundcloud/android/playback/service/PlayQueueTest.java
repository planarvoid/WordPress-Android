package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToNext(false)).toBeFalse();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldSetCurrentTriggerToManual() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        playQueue.setCurrentTrackToUserTriggered();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToNextShouldResultInAutoTrigger() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto");
    }

    @Test
    public void moveToNextShouldResultInManualTrigger() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(true)).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToPrevious()).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void shouldReturnSetAsPartOfLoggerParams() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(1L, 2L), 1, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&set_id=54321&set_position=1");
    }

    @Test
    public void shouldReturnExploreVersionInEventLoggerParamsWhenCurrentTrackIsInitialTrack() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(123L, 456L), 0, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&source=explore&source_version=exp1");
    }

    @Test
    public void shouldReturnRecommenderVersionInEventLoggerParamsWhenCurrentTrackIsNotInitialTrack() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(123L, 456L), 1, PlaySessionSource.EMPTY);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&source=recommender&source_version=rec1");
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        expect(PlayQueue.EMPTY.getCurrentEventLoggerParams()).toEqual("");

    }

    private void checkManualTrigger(PlayQueue playQueue) {
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=manual");
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

}
