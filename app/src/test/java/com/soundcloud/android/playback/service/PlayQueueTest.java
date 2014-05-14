package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";
    private static final PlayQueueItem PLAY_QUEUE_ITEM_1 = new PlayQueueItem(1L, "source1", "version1");
    private static final PlayQueueItem PLAY_QUEUE_ITEM_2 = new PlayQueueItem(2L, "source2", "version2");

    private PlaySessionSource playSessionSource;
    private Playlist playlist;

    @Before
    public void setUp() throws Exception {
        playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        playSessionSource  = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist);
        playSessionSource.setExploreVersion("1.0");
    }

    @Test
    public void shouldCreatePlayQueueWithItems() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2), 0);
        expect(playQueue.getItems()).toContainExactly(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2);
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2, playSessionSource);

        playQueue.addTrack(123L, "source3", "version3");

        expect(playQueue.getItems().size()).toEqual(4);
        expect(playQueue.setPosition(3)).toBeTrue();
        expect(playQueue.getCurrentTrackId()).toEqual(123L);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo(playSessionSource);
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

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo(playSessionSource);
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
    public void shouldReturnHasPreviousIfNotInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);

        expect(playQueue.hasPreviousTrack()).toBeTrue();
    }

    @Test
    public void shouldReturnNoPreviousIfInFirstPosition() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);

        expect(playQueue.hasPreviousTrack()).toBeFalse();
    }

    @Test
    public void hasNextTrackIsTrueIfNotAtEnd() {
        expect(createPlayQueue(Lists.newArrayList(1L, 2L), 0).hasNextTrack()).toBeTrue();
    }

    @Test
    public void hasNextTrackIsFalseIfAtEnd() {
        expect(createPlayQueue(Lists.newArrayList(1L, 2L), 1).hasNextTrack()).toBeFalse();
    }

    @Test
    public void shouldReturnSetAsPartOfLoggerParams() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo(playSessionSource);
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo.getPlaylistId()).toEqual(playlist.getId());
        expect(trackSourceInfo.getPlaylistPosition()).toEqual(1);
    }

    @Test
    public void shouldReturnExploreVersionAsPartOfLoggerParams() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo(playSessionSource);
        expect(trackSourceInfo.getIsUserTriggered()).toEqual(false);
        expect(trackSourceInfo.getSource()).toEqual("explore");
        expect(trackSourceInfo.getSourceVersion()).toEqual("1.0");
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        expect(PlayQueue.empty().getCurrentTrackSourceInfo(playSessionSource)).toBeNull();

    }

    private void checkManualTrigger(PlayQueue playQueue) {
        expect(playQueue.getCurrentTrackSourceInfo(playSessionSource).getIsUserTriggered()).toEqual(true);
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

    @Test
    public void returnsNotSetIDWithEmptyQueue() throws Exception {
        expect(PlayQueue.empty().getCurrentTrackId()).toEqual(Long.valueOf(Playable.NOT_SET));

    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition, PlaySessionSource source) {
        return PlayQueue.fromIdList(idList, startPosition, source);
    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition) {
        return createPlayQueue(idList, startPosition, PlaySessionSource.EMPTY);
    }

}
