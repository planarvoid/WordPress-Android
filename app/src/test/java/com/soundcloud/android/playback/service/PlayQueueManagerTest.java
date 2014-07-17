package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueManagerTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";

    private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private Context context;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock
    private PlayQueueOperations playQueueOperations;

    private PublicApiPlaylist playlist;
    private PlaySessionSource playSessionSource;

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(context, playQueueOperations, eventBus, modelManager);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);
        when(playQueue.copy()).thenReturn(playQueue);

        playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playSessionSource.setExploreVersion("1.0");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullValueWhenSettingNewPlayqueue() {
        playQueueManager.setNewPlayQueue(null, playSessionSource);
    }

    @Test
    public void shouldSetNewPlayQueueAsCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void getCurrentPositionReturnsCurrentPosition() {
        int newPosition = 5;
        when(playQueue.size()).thenReturn(6);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.setPosition(newPosition);

        expect(playQueueManager.getCurrentPosition()).toEqual(newPosition);
    }

    @Test
    public void getCurrentTrackUrnReturnsCurrentTrackUrnFromPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.getUrn(5)).thenReturn(Urn.forTrack(5L));

        expect(playQueueManager.getCurrentTrackUrn()).toEqual(Urn.forTrack(5L));
    }

    @Test
    public void getCurrentPlayQueueCountReturnsSizeOfCurrentQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getQueueSize()).toBe(3);
    }

    @Test
    public void isQueueEmptyReturnsTrueIfQueueSizeIsZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeTrue();
    }

    @Test
    public void isQueueEmptyReturnsFalseIfQueueSizeGreaterThanZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeFalse();
    }

    @Test
    public void getUrnAtPositionReturnsTrackUrnForPlayQueueItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getUrnAtPosition(2)).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldSetNewPlayQueueCurrentTrackToManuallyTriggered() {
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldReturnEmptyEventLoggerParamsWhenQueueIsEmpty() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playSessionSource);
        expect(playQueueManager.getCurrentTrackSourceInfo()).toBeNull();
    }

    @Test
    public void shouldReturnSetAsPartOfLoggerParams() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getPlaylistId()).toEqual(playlist.getId());
        expect(trackSourceInfo.getPlaylistPosition()).toEqual(1);
    }

    @Test
    public void shouldReturnExploreVersionAsPartOfLoggerParams() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getSource()).toEqual("explore");
        expect(trackSourceInfo.getSourceVersion()).toEqual("1.0");
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenSettingNewPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectBroadcastNewPlayQueue();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnSetNewPlayQueue() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(3L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toNumber(1);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldPublishTrackChangedEventOnSetNewPlayQueue(){
        final TrackUrn trackUrn = Urn.forTrack(3L);
        when(playQueue.getUrn(0)).thenReturn(trackUrn);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE_TRACK)).toNumber(1);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(trackUrn);
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue() {
        int currentPosition = 5;
        TrackUrn currentUrn = Urn.forTrack(3L);
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getUrn(currentPosition)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, currentPosition, playSessionSource);
        verify(playQueueOperations).saveQueue(playQueue, currentPosition, currentUrn, playSessionSource, 0);
    }

    @Test
    public void shouldStoreTracksWhenSettingNewPlayQueue() {
        TrackUrn currentUrn = Urn.forTrack(3L);
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getUrn(0)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(playQueueOperations).saveQueue(playQueue, 0, currentUrn, playSessionSource, 0L);
    }

    @Test
    public void shouldNotUpdateCurrentPositionIfPlayqueueIsNull() {
        playQueueManager.saveCurrentProgress(22L);
        verifyZeroInteractions(sharedPreferences);
    }

    @Test
    public void saveProgressSavesPlayQueueInfoUsingPlayQueueOperations() {
        TrackUrn currentUrn = Urn.forTrack(3L);
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getUrn(0)).thenReturn(currentUrn);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playQueueManager.saveCurrentProgress(123L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).saveQueue(playQueue, 0, currentUrn, playSessionSource, 0L);
        inOrder.verify(playQueueOperations).saveQueue(playQueue, 0, currentUrn, playSessionSource, 123L);
    }

    @Test
    public void saveProgressDoesNotPassAudioAdsInPlayQueue() throws CreateModelException {
        playQueueManagerWithOneTrackAndAd();

        playQueueManager.saveCurrentProgress(12L);

        ArgumentCaptor<PlayQueue> captor = ArgumentCaptor.forClass(PlayQueue.class);
        verify(playQueueOperations, times(2)).saveQueue(captor.capture(), anyInt(), any(TrackUrn.class), any(PlaySessionSource.class), anyLong());
        expect(captor.getValue().size()).toBe(1);
    }

    @Test
    public void saveProgressUpdatesSavePositionIfAdIsRemovedFromQueue() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L, 3L), playSessionSource), 1, playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        playQueueManager.setPosition(3);
        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).saveQueue(any(PlayQueue.class), eq(1), any(TrackUrn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).saveQueue(any(PlayQueue.class), eq(2), any(TrackUrn.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void getPlayProgressInfoReturnsLastSavedProgressInfo() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), playSessionSource), playSessionSource);
        playQueueManager.saveCurrentProgress(456L);
        expect(playQueueManager.getPlayProgressInfo()).toEqual(new PlaybackProgressInfo(123L, 456L));
    }

    @Test
    public void doesNotChangePlayQueueIfPositionSetToCurrent() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), playSessionSource);
        playQueueManager.setPosition(1);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendTrackChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), 1, playSessionSource);

        final CurrentPlayQueueTrackEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK);
        playQueueManager.setPosition(1);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK)).toBe(lastEvent);
    }

    @Test
    public void shouldPublishTrackChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource), playSessionSource);

        playQueueManager.setPosition(2);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldPublishTrackChangeEventOnPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.getUrn(4)).thenReturn(Urn.forTrack(3L));

        playQueueManager.moveToPreviousTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldMoveToPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentPosition()).toBe(4);
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingPreviousOnFirstTrack() {
        when(playQueue.hasPreviousTrack(0)).thenReturn(false);

        playQueueManager.moveToPreviousTrack();

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toBeEmpty();
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L), playSessionSource), playSessionSource);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentPosition()).toBe(0);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.isEmpty()).thenReturn(false);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldPublishTrackChangeEventOnNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 0, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getUrn(1)).thenReturn(Urn.forTrack(3L));

        playQueueManager.nextTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldToNextTrackSetUserTriggeredFlagToTrue() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.nextTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldAutoNextTrackSetUserTriggerTrackFlagToFalse() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.autoNextTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeFalse();
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), playSessionSource);

        expect(playQueueManager.nextTrack()).toBeTrue();
        expect(playQueueManager.getCurrentPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), 1, playSessionSource);

        expect(playQueueManager.nextTrack()).toBeFalse();
        expect(playQueueManager.getCurrentPosition()).toBe(1);
    }

    @Test
    public void shouldReturnPlayQueueViewWithAppendState() {
        PlayQueue playQueue = PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), playSessionSource);
        playQueueManager.setNewPlayQueue(playQueue, 2, playSessionSource);

        final PlayQueueView playQueueView = playQueueManager.getViewWithAppendState(PlayQueueManager.FetchRecommendedState.LOADING);
        expect(playQueueView).toContainExactly(1L, 2L, 3L);
        expect(playQueueView.getPosition()).toBe(2);
        expect(playQueueView.getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.LOADING);
    }

    @Test
    public void nextTrackReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        expect(playQueueManager.nextTrack()).toBeFalse();
    }

    @Test
    public void nextTrackReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.nextTrack()).toBeTrue();
    }

    @Test
    public void getNextTrackUrnReturnsNextTrackUrn() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void getNextTrackUrnReturnsNotSetTrackUrnIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), 1, playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(TrackUrn.NOT_SET);
    }

    @Test
    public void insertsAudioAdAtPosition() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, 1, playSessionSource);

        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        verify(playQueue).insertAudioAd(audioAd, 2);
    }

    @Test
    public void publishesQueueChangeEventWhenAudioAdIsInserted() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, 1, playSessionSource);

        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void clearAdRemovedTheAd() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L, 3L), playSessionSource), playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        playQueueManager.clearAudioAd();

        expect(playQueueManager.getQueueSize()).toEqual(3);
        expect(playQueueManager.getUrnAtPosition(1)).toEqual(TrackUrn.forTrack(2L));
    }

    @Test
    public void clearAdDoesNotRemoveAdFromCurrentPosition() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L, 3L), playSessionSource), playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        playQueueManager.setPosition(1);
        playQueueManager.clearAudioAd();

        expect(playQueueManager.getQueueSize()).toEqual(4);
    }

    @Test
    public void clearAdDoesNothingWhenThereIsNoAdInQueue() throws CreateModelException {
        playQueueManagerWithOneTrackAndAd();

        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), playSessionSource);
        playQueueManager.clearAudioAd();

        expect(playQueueManager.getQueueSize()).toBe(2);
    }

    @Test
    public void publishesQueueChangeEventWhenAdIsCleared() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        playQueueManager.clearAudioAd();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void doesNotPublishQueueChangeEventWhenAdIsNotCleared() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), playSessionSource), playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);

        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);

        playQueueManager.setPosition(1);
        playQueueManager.clearAudioAd();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).toBe(lastEvent);
    }

    @Test
    public void autoNextReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        expect(playQueueManager.autoNextTrack()).toBeFalse();
    }

    @Test
    public void autoNextReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.autoNextTrack()).toBeTrue();
    }

    @Test
    public void hasNextTrackReturnsHasNextTrackFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.hasNextTrack()).toBeTrue();
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingNextOnLastTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L), playSessionSource), playSessionSource);
        final CurrentPlayQueueTrackEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK);
        playQueueManager.nextTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK)).toBe(lastEvent);
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnLoadPlayQueueIfNoPlayQueueStore() {
        playQueueManager.loadPlayQueue();

        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldHaveNoPlayProgressInfoWhenPlaybackOperationsHasReturnsNoObservable() {
        playQueueManager.loadPlayQueue();
        expect(playQueueManager.getPlayProgressInfo()).toBeNull();
    }

    @Test
    public void shouldSetPlayProgressInfoWhenReloadingPlayQueue() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueue();
        PlaybackProgressInfo resumeInfo = playQueueManager.getPlayProgressInfo();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldReloadPlayQueueFromLocalStorage() {
        PlayQueue playQueue = PlayQueue.fromIdList(Arrays.asList(1L, 2L, 3L), playSessionSource);

        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueue();

        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void shouldReloadShouldBeTrueIfThePlayQueueIsEmpty() {
        expect(playQueueManager.shouldReloadQueue()).toBeTrue();
    }

    @Test
    public void shouldReloadShouldBeFalseWithNonEmptyQueue() {
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
    }

    @Test
    public void reloadedPlayQueueIsNotSavedWhenSet() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.loadPlayQueue();
        verify(playQueueOperations, never()).saveQueue(any(PlayQueue.class), anyInt(), any(TrackUrn.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void shouldGetRelatedTracksObservableWhenFetchingRelatedTracks() {
        TrackUrn currentUrn = Urn.forTrack(123L);
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>empty());
        when(playQueue.getUrn(0)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        verify(playQueueOperations).getRelatedTracks(currentUrn);
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(observable);

        playQueueManager.fetchTracksRelatedToCurrentTrack();
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks() {
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>never());
        playQueueManager.fetchTracksRelatedToCurrentTrack();
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.LOADING);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldNotPublishPlayQueueRelatedTracksEventOnEmptyRelatedLoad() throws CreateModelException {
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>empty());
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        eventBus.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldPublishPlayQueueUpdateEventOnRelatedTracksReturned() throws Exception {
        final ApiTrack apiTrack = TestHelper.getModelFactory().createModel(ApiTrack.class);
        final TrackUrn trackUrn = Urn.forTrack(123L);
        when(playQueueOperations.getRelatedTracks(trackUrn)).thenReturn(Observable.just(new RecommendedTracksCollection(Lists.newArrayList(apiTrack), "123")));
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), playSessionSource), playSessionSource);
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final ApiTrack apiTrack = TestHelper.getModelFactory().createModel(ApiTrack.class);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), playSessionSource), playSessionSource);
        playQueueManager.onNext(new RecommendedTracksCollection(Lists.newArrayList(apiTrack), "123"));

        expect(playQueueManager.getPlayQueueView()).toContainExactly(123L, apiTrack.getId());

        ArgumentCaptor<PublicApiTrack> captor = ArgumentCaptor.forClass(PublicApiTrack.class);
        verify(modelManager).cache(captor.capture());
        expect(captor.getValue().getId()).toEqual(apiTrack.getId());
    }

    @Test
    public void shouldSetIdleStateOnQueueAndBroadcastWhenDoneSuccessfulRelatedLoad() {
        playQueueManager.onNext(new RecommendedTracksCollection(Collections.<ApiTrack>emptyList(), "123"));
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.IDLE);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad() {
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.EMPTY);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled() {
        playQueueManager.onError(new Throwable());
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.ERROR);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void clearAllShouldClearPreferences() {
        playQueueManager.clearAll();
        verify(playQueueOperations).clear();
    }

    @Test
    public void clearAllShouldClearStorage() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.clearAll();
        verify(playQueueOperations).clear();
    }

    @Test
    public void clearAllShouldSetPlayQueueToEmpty() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L), playSessionSource), playSessionSource);
        expect(playQueueManager.isQueueEmpty()).toBeFalse();
        playQueueManager.clearAll();
        expect(playQueueManager.isQueueEmpty()).toBeTrue();
    }

    @Test
    public void clearAllClearsPlaylistId() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L), playSessionSource), playSessionSource);
        expect(playQueueManager.getPlaylistId()).not.toEqual((long) PublicApiPlaylist.NOT_SET);
        playQueueManager.clearAll();
        expect(playQueueManager.getPlaylistId()).toEqual((long) PublicApiPlaylist.NOT_SET);
    }

    @Test
    public void shouldReturnWhetherPlaylistIdIsCurrentPlayQueue() {
        PublicApiPlaylist playlist = new PublicApiPlaylist(6L);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isCurrentPlaylist(6L)).toBeTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        PublicApiPlaylist playlist = new PublicApiPlaylist(6L);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isPlaylist()).toBeTrue();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsCurrentTrack() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(123))).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(456))).toBeFalse();
    }

    @Test
    public void shouldRetryWithSameObservable() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(observable);

        playQueueManager.fetchTracksRelatedToCurrentTrack();
        playQueueManager.retryRelatedTracksFetch();
        expect(observable.subscribers()).toNumber(2);
    }

    @Test
    public void shouldReturnTrueOnIsAudioAdForPosition() {
        when(playQueue.isAudioAd(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isAudioAdAtPosition(0)).toBeTrue();
    }

    private void playQueueManagerWithOneTrackAndAd() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L), playSessionSource), playSessionSource);
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAudioAd(audioAd);
    }

    private void expectBroadcastNewPlayQueue() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
    }

    private void expectBroadcastPlayQueueUpdate() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context, atLeastOnce()).sendBroadcast(captor.capture());
        expect(Iterables.find(captor.getAllValues(), new Predicate<Intent>() {
            @Override
            public boolean apply(@Nullable Intent input) {
                return input.getAction().equals(PlayQueueManager.RELATED_LOAD_STATE_CHANGED_ACTION);
            }
        })).not.toBeNull();
    }

    private void expectPlayQueueContentToBeEqual(PlayQueueManager playQueueManager, PlayQueue playQueue) {
        expect(playQueueManager.getQueueSize()).toBe(playQueue.size());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            expect(playQueueManager.getUrnAtPosition(i)).toEqual(playQueue.getUrn(i));
        }
    }
}
