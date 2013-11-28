package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToNext(false)).toBeFalse();
        expect(playQueue.getPosition()).toBe(1);
    }

    @Test
    public void shouldSetCurrentTriggerToManual() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        playQueue.setCurrentTrackToUserTriggered();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToNextShouldResultInAutoTrigger() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto");
    }

    @Test
    public void moveToNextShouldResultInManualTrigger() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(true)).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToPrevious()).toBeTrue();
        checkManualTrigger(playQueue);
    }

    @Test
    public void shouldReturnSetAsPartOfLoggerParams() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&set_id=54321&set_position=1");
    }

    @Test
    public void shouldReturnExploreVersionInEventLoggerParamsWhenCurrentTrackIsInitialTrack() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(123L, 456L), 0);
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=auto&source=explore&source_version=exp1");
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        expect(PlayQueue.empty().getCurrentEventLoggerParams()).toEqual("");

    }

    private void checkManualTrigger(PlayQueue playQueue) {
        expect(playQueue.getCurrentEventLoggerParams()).toEqual("trigger=manual");
    }

    @Test
    public void shouldSuccessfullyMoveToPreviousTrack() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToPrevious()).toBeTrue();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToPrevious()).toBeFalse();
        expect(playQueue.getPosition()).toBe(0);
    }

    @Test
    public void shouldReturnPlayQueueViewWithAppendState() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2);
        final PlayQueueView playQueueView = playQueue.getViewWithAppendState(PlaybackOperations.AppendState.LOADING);
        expect(playQueueView).toContainExactly(1L, 2L, 3L);
        expect(playQueueView.getPosition()).toBe(2);
        expect(playQueueView.getAppendState()).toEqual(PlaybackOperations.AppendState.LOADING);
    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition) {
        return new PlayQueue(idList, startPosition, PlaySessionSource.EMPTY);
    }

}
