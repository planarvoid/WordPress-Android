package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";
    private static final PlayQueueItem PLAY_QUEUE_ITEM_1 = PlayQueueItem.fromTrack(1L, "source1", "version1");
    private static final PlayQueueItem PLAY_QUEUE_ITEM_2 = PlayQueueItem.fromTrack(2L, "source2", "version2");

    private PlaySessionSource playSessionSource;
    private PublicApiPlaylist playlist;

    @Before
    public void setUp() throws Exception {
        playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        playSessionSource  = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playSessionSource.setExploreVersion("1.0");
    }

    @Test
    public void shouldCreatePlayQueueWithItems() {
        PlayQueue playQueue = new PlayQueue(Lists.newArrayList(PLAY_QUEUE_ITEM_1, PLAY_QUEUE_ITEM_2), 0);
        expect(playQueue.getUrnAtPosition(0)).toEqual(PLAY_QUEUE_ITEM_1.getTrackUrn());
        expect(playQueue.getUrnAtPosition(1)).toEqual(PLAY_QUEUE_ITEM_2.getTrackUrn());
    }

    @Test
    public void shouldAddTrackToPlayQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2, playSessionSource);

        playQueue.addTrack(123L, "source3", "version3");

        expect(playQueue.size()).toEqual(4);
        expect(playQueue.setPosition(3)).toBeTrue();
        expect(playQueue.getCurrentTrackId()).toEqual(123L);

        final TrackSourceInfo trackSourceInfo = playQueue.getCurrentTrackSourceInfo(playSessionSource);
        expect(trackSourceInfo.getSource()).toEqual("source3");
        expect(trackSourceInfo.getSourceVersion()).toEqual("version3");
    }

    @Test
    public void insertsAudioAdAtPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource);

        final AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueue.insertAudioAdAtPosition(audioAd, 1);

        expect(playQueue.getUrnAtPosition(1)).toEqual(audioAd.getApiTrack().getUrn());
        expect(playQueue.size()).toBe(4);
    }

    @Test
    public void shouldReportCorrectSize() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2, playSessionSource);
        expect(playQueue.size()).toEqual(3);
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToNext(false)).toBeTrue();
        expect(playQueue.getCurrentPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 1);
        expect(playQueue.moveToNext(false)).toBeFalse();
        expect(playQueue.getCurrentPosition()).toBe(1);
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
        expect(playQueue.getCurrentPosition()).toBe(0);
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L), 0);
        expect(playQueue.moveToPrevious()).toBeFalse();
        expect(playQueue.getCurrentPosition()).toBe(0);
    }

    @Test
    public void shouldReturnPlayQueueViewWithAppendState() {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 2);
        final PlayQueueView playQueueView = playQueue.getViewWithAppendState(PlayQueueManager.FetchRecommendedState.LOADING);
        expect(playQueueView).toContainExactly(1L, 2L, 3L);
        expect(playQueueView.getPosition()).toBe(2);
        expect(playQueueView.getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.LOADING);
    }

    @Test
    public void getCurrentTrackUrnReturnsUrnForCurrentPlayQueueItem() throws Exception {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        playQueue.moveToNext(false);
        expect(playQueue.getCurrentTrackUrn()).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void getCurrentTrackUrnReturnsNotSetUrnWithEmptyQueue() throws Exception {
        expect(PlayQueue.empty().getCurrentTrackUrn()).toEqual(Urn.forTrack(Consts.NOT_SET));
    }

    @Test
    public void shouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        final AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        final PlayQueue playQueue = new PlayQueue(Arrays.asList(PlayQueueItem.fromAudioAd(audioAd)), 0);

        expect(playQueue.isAudioAd(0)).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfCurrentItemIsNotAudioAd() {
        final PlayQueue playQueue = new PlayQueue(Arrays.asList(PlayQueueItem.fromTrack(2L, "", "")), 0);

        expect(playQueue.isAudioAd(0)).toBeFalse();
    }

    @Test
    public void isAudioAdReturnsFalseAtInvalidPopsition() {
        final PlayQueue playQueue = new PlayQueue(Arrays.asList(PlayQueueItem.fromTrack(2L, "", "")), 0);

        expect(playQueue.isAudioAd(1)).toBeFalse();
        expect(playQueue.isAudioAd(-1)).toBeFalse();
    }

    @Test
    public void getUrnAtPositionReturnsNotSetForEmptyQueue() throws Exception {
        expect(PlayQueue.empty().getUrnAtPosition(0)).toBe(TrackUrn.NOT_SET);
    }

    @Test
    public void getUrnAtInvalidPositionReturnsNotSet() throws Exception {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.getUrnAtPosition(-1)).toBe(TrackUrn.NOT_SET);
        expect(playQueue.getUrnAtPosition(3)).toBe(TrackUrn.NOT_SET);
    }

    @Test
    public void getUrnAtPositionReturnsUrnAtPosition() throws Exception {
        PlayQueue playQueue = createPlayQueue(Lists.newArrayList(1L, 2L, 3L), 0);
        expect(playQueue.getUrnAtPosition(2)).toEqual(Urn.forTrack(3));
    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition, PlaySessionSource source) {
        return PlayQueue.fromIdList(idList, startPosition, source);
    }

    private PlayQueue createPlayQueue(List<Long> idList, int startPosition) {
        return createPlayQueue(idList, startPosition, PlaySessionSource.EMPTY);
    }

}
