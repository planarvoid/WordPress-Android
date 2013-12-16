package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";
    private static final PlayQueueItem PLAY_QUEUE_ITEM_1 = new PlayQueueItem(1L, "source1", "version1");
    private static final PlayQueueItem PLAY_QUEUE_ITEM_2 = new PlayQueueItem(2L, "source2", "version2");
    private static final PlaySessionSource PLAY_SESSION_SOURCE = new PlaySessionSource(ORIGIN_PAGE, 54321L, "1.0");

    @Test
    public void shouldCreatePlayQueueWithItemsAndSource() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2), 0, PLAY_SESSION_SOURCE);

        expect(playQueue.getItems()).toContainExactly(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2);
        expect(playQueue.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(playQueue.getPlaylistId()).toEqual(54321L);
    }

    @Test
    public void shouldReturnTrackSourceInfoFromPlaylist() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2), 0, PLAY_SESSION_SOURCE);

        expect(playQueue.getCurrentTrackId()).toEqual(1L);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo.getOriginScreen()).toEqual(ORIGIN_PAGE);
        expect(trackSourceInfo.getSource()).toEqual("source1");
        expect(trackSourceInfo.getSourceVersion()).toEqual("version1");
        expect(trackSourceInfo.getPlaylistId()).toEqual(54321L);
        expect(trackSourceInfo.getPlaylistPosition()).toEqual(0);

        expect(playQueue.setPosition(1)).toBeTrue();
        expect(playQueue.getCurrentTrackId()).toEqual(2L);

        final TrackSourceInfo trackSourceInfo2 = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo2.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo2.getOriginScreen()).toEqual(ORIGIN_PAGE);
        expect(trackSourceInfo2.getSource()).toEqual("source2");
        expect(trackSourceInfo2.getSourceVersion()).toEqual("version2");
        expect(trackSourceInfo2.getPlaylistId()).toEqual(54321L);
        expect(trackSourceInfo2.getPlaylistPosition()).toEqual(1);
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2, PLAY_SESSION_SOURCE);

        playQueue.addTrack(123L, "source3", "version3");

        expect(playQueue.getItems().size()).toEqual(4);
        expect(playQueue.setPosition(3)).toBeTrue();
        expect(playQueue.getCurrentTrackId()).toEqual(123L);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getSource()).toEqual("source3");
        expect(trackSourceInfo.getSourceVersion()).toEqual("version3");
    }

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

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
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
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1, new PlaySessionSource(ScTextUtils.EMPTY_STRING, 54321));

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo.getPlaylistId()).toEqual(54321L);
        expect(trackSourceInfo.getPlaylistPosition()).toEqual(1);
    }

    @Test
    public void shouldReturnExploreVersionAsPartOfLoggerParams() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1, new PlaySessionSource(ScTextUtils.EMPTY_STRING, "exploreVersion1"));

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo.getSource()).toEqual("explore");
        expect(trackSourceInfo.getSourceVersion()).toEqual("exploreVersion1");
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        expect(PlayQueue.empty().getCurrentTrackSourceInfo()).toBeNull();

    }

    private void checkManualTrigger(PlayQueue playQueue) {
        expect(playQueue.getCurrentTrackSourceInfo().getIsUserTriggered()).toEqual(true);
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

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition, PlaySessionSource source) {
        return PlayQueue.fromIdList(idList, startPosition, source);
    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition) {
        return createPlayQueue(idList, startPosition, PlaySessionSource.EMPTY);
    }

}
