package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.RecommendedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.TrackUrn;
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

    private PlaySessionSource playSessionSource;

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(context, playQueueOperations, eventBus, modelManager);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);

        Playlist playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        playSessionSource  = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playSessionSource.setExploreVersion("1.0");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullValueWhenSettingNewPlayqueue(){
        playQueueManager.setNewPlayQueue(null, playSessionSource);
    }

    @Test
    public void shouldSetNewPlayQueueAsCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.getCurrentPlayQueue()).toEqual(playQueue);
    }

    @Test
    public void getPlayQueuePositionReturnsCurrentPositionFromPlayQueue() {
        when(playQueue.getPosition()).thenReturn(5);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.getCurrentPosition()).toEqual(5);
    }

    @Test
    public void getCurrentTrackIdReturnsCurrentTrackIdFromPlayQueue() {
        when(playQueue.getCurrentTrackUrn()).thenReturn(Urn.forTrack(5L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.getCurrentTrackUrn()).toEqual(Urn.forTrack(5L));
    }

    @Test
    public void getCurrentPlayQueueCountReturnsSizeOfCurrentQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        expect(playQueueManager.getQueueSize()).toBe(3);
    }

    @Test
    public void isQueueEmptyReturnsTrueIfQueueSizeIsZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeTrue();
    }

    @Test
    public void isQueueEmptyReturnsFalseIfQueueSizeGreaterThanZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeFalse();
    }

    @Test
    public void getUrnAtPositionReturnsTrackUrnForPlayQueueItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        expect(playQueueManager.getUrnAtPosition(2)).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldSetNewPlayQueueCurrentTrackToManuallyTriggered() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(playQueue).setCurrentTrackToUserTriggered();
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenSettingNewPlayqueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectBroadcastNewPlayQueue();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnSetNewPlayQueue() {
        when(playQueue.getCurrentTrackUrn()).thenReturn(Urn.forTrack(3L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toNumber(1);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getCurrentTrackUrn())
                .toEqual(playQueue.getCurrentTrackUrn());
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue(){
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getCurrentTrackUrn()).thenReturn(Urn.forTrack(3L));

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(playQueueOperations).saveQueue(playQueue, playSessionSource, 0);
    }

    @Test
    public void shouldStoreTracksWhenSettingNewPlayQueue(){
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(playQueueOperations).saveQueue(playQueue,playSessionSource, 0L);
    }

    @Test
    public void shouldNotUpdateCurrentPositionIfPlayqueueIsNull() {
        playQueueManager.saveCurrentPosition(22L);
        verifyZeroInteractions(sharedPreferences);
    }

    @Test
    public void saveProgressSavesPlayQueueInfoUsingPlayQueueOperations() {
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playQueueManager.saveCurrentPosition(123L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).saveQueue(playQueue, playSessionSource, 0L);
        inOrder.verify(playQueueOperations).saveQueue(playQueue, playSessionSource, 123L);
    }

    @Test
    public void getPlayProgressInfoReturnsLastSavedProgressInfo() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), 0, playSessionSource), playSessionSource);
        playQueueManager.saveCurrentPosition(456L);
        expect(playQueueManager.getPlayProgressInfo()).toEqual(new PlaybackProgressInfo(123L, 456L));
    }

    @Test
    public void doesNotChangePlayQueueIfPositionSetToCurrent() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 1, playSessionSource), playSessionSource);
        playQueueManager.setPosition(1);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendTrackChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 1, playSessionSource), playSessionSource);

        playQueueManager.setPosition(1);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE, new Predicate<PlayQueueEvent>() {
            @Override
            public boolean apply(PlayQueueEvent event) {
                return event.getKind() == PlayQueueEvent.TRACK_CHANGE;
            }
        })).toBeEmpty();
    }

    @Test
    public void shouldPublishTrackChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        playQueueManager.setPosition(2);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.TRACK_CHANGE);
    }

    @Test
    public void shouldPublishTrackChangeEventOnPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasPreviousTrack()).thenReturn(true);

        playQueueManager.previousTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.TRACK_CHANGE);
    }

    @Test
    public void shouldMoveToPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasPreviousTrack()).thenReturn(true);

        playQueueManager.previousTrack();

        verify(playQueue).moveToPrevious();
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingPreviousOnFirstTrack() {
        when(playQueue.moveToPrevious()).thenReturn(false);

        playQueueManager.previousTrack();

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toBeEmpty();
    }

    @Test
    public void shouldPublishTrackChangeEventOnNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);
        playQueueManager.nextTrack();

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE, new Predicate<PlayQueueEvent>() {
            @Override
            public boolean apply(PlayQueueEvent input) {
                return input.getKind() == PlayQueueEvent.TRACK_CHANGE;
            }
        })).not.toBeEmpty();
    }

    @Test
    public void shouldMoveToNextTrackWithManualSetToTrue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);

        playQueueManager.nextTrack();

        verify(playQueue).moveToNext(true);
    }

    @Test
    public void nextTrackReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(false);
        expect(playQueueManager.nextTrack()).toBeFalse();
    }

    @Test
    public void nextTrackReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);
        expect(playQueueManager.nextTrack()).toBeTrue();
    }

    @Test
    public void getNextTrackUrnReturnsNextTrackUrn() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), 0, playSessionSource), playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void getNextTrackUrnReturnsNotSetTrackUrnIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Arrays.asList(1L, 2L), 1, playSessionSource), playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(TrackUrn.NOT_SET);
    }

    @Test
    public void insertsAudioAdAtPosition() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.getPosition()).thenReturn(1);

        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAd(audioAd);

        verify(playQueue).insertAudioAdAtPosition(audioAd, 2);
    }

    @Test
    public void broadcastsQueueChangeEventWhenAudioAdIsInserted() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.getPosition()).thenReturn(1);

        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        playQueueManager.insertAd(audioAd);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldMoveToNextTrackWithManualSetToFalse() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);

        playQueueManager.autoNextTrack();

        verify(playQueue).moveToNext(false);
    }

    @Test
    public void autoNextReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(false);
        expect(playQueueManager.autoNextTrack()).toBeFalse();
    }

    @Test
    public void autoNextReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);
        expect(playQueueManager.autoNextTrack()).toBeTrue();
    }

    @Test
    public void hasNextTrackReturnsHasNextTrackFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);
        expect(playQueueManager.hasNextTrack()).toBeTrue();
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingNextOnLastTrack() {
        when(playQueue.moveToNext(true)).thenReturn(false);

        playQueueManager.nextTrack();

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toBeEmpty();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnLoadPlayQueueIfNoPlayQueueStore() {
        playQueueManager.loadPlayQueue();

        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldHaveNoPlayProgressInfoWhenPlaybackOperationsHasReturnsNoObservable(){
        playQueueManager.loadPlayQueue();
        expect(playQueueManager.getPlayProgressInfo()).toBeNull();
    }

    @Test
    public void shouldSetPlayProgressInfoWhenReloadingPlayQueue(){
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueue();
        PlaybackProgressInfo resumeInfo = playQueueManager.getPlayProgressInfo();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldReloadPlayQueueFromLocalStorage(){
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueue();

        final PlayQueue currentPlayQueue = playQueueManager.getCurrentPlayQueue();
        expect(currentPlayQueue).toBe(playQueue);
    }

    @Test
    public void shouldReloadShouldBeTrueIfThePlayQueueIsEmpty(){
        expect(playQueueManager.shouldReloadQueue()).toBeTrue();
    }

    @Test
    public void shouldReloadShouldBeFalseWithNonEmptyQueue(){
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
    }

    @Test
    public void reloadedPlayQueueIsNotSavedWhenSet(){
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.loadPlayQueue();
        verify(playQueueOperations, never()).saveQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void shouldGetRelatedTracksObservableWhenFetchingRelatedTracks(){
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>empty());

        playQueueManager.fetchRelatedTracks(Urn.forTrack(123L));
        verify(playQueueOperations).getRelatedTracks(Urn.forTrack(123L));
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks(){
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(observable);

        playQueueManager.fetchRelatedTracks(Urn.forTrack(123L));
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks(){
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>never());
        playQueueManager.fetchRelatedTracks(Urn.forTrack(123L));
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.LOADING);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldPublishPlayQueueRelatedTracksEventOnRelatedLoadingStateChange() throws CreateModelException {
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(Observable.<RecommendedTracksCollection>never());
        playQueueManager.fetchRelatedTracks(Urn.forTrack(123L));

        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), 0, playSessionSource), playSessionSource);
        playQueueManager.onNext(new RecommendedTracksCollection(Lists.newArrayList(trackSummary), "123"));

        expect(playQueueManager.getPlayQueueView()).toContainExactly(123L, trackSummary.getId());

        ArgumentCaptor<Track> captor = ArgumentCaptor.forClass(Track.class);
        verify(modelManager).cache(captor.capture());
        expect(captor.getValue().getId()).toEqual(trackSummary.getId());
    }

    @Test
    public void shouldSetIdleStateOnQueueAndBroadcastWhenDoneSuccessfulRelatedLoad(){
        playQueueManager.onNext(new RecommendedTracksCollection(Collections.<TrackSummary>emptyList(), "123"));
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.IDLE);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad(){
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getFetchRecommendedState()).toEqual(PlayQueueManager.FetchRecommendedState.EMPTY);
        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled(){
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
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L), 0, playSessionSource), playSessionSource);
        expect(playQueueManager.getCurrentPlayQueue()).not.toEqual(PlayQueue.empty());
        playQueueManager.clearAll();
        expect(playQueueManager.getCurrentPlayQueue()).toEqual(PlayQueue.empty());
    }

    @Test
    public void clearAllClearsPlaylistId() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L), 0, playSessionSource), playSessionSource);
        expect(playQueueManager.getPlaylistId()).not.toEqual((long) Playlist.NOT_SET);
        playQueueManager.clearAll();
        expect(playQueueManager.getPlaylistId()).toEqual((long) Playlist.NOT_SET);
    }

    @Test
    public void shouldReturnWhetherPlaylistIdIsCurrentPlayQueue() {
        Playlist playlist = new Playlist(6L);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isCurrentPlaylist(6L)).toBeTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        Playlist playlist = new Playlist(6L);
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isPlaylist()).toBeTrue();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsCurrentTrack() {
        when(playQueue.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(123))).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        when(playQueue.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(456))).toBeFalse();
    }

    @Test
    public void shouldRetryWithSameObservable() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(TrackUrn.class))).thenReturn(observable);

        playQueueManager.fetchRelatedTracks(Urn.forTrack(123L));
        playQueueManager.retryRelatedTracksFetch();
        expect(observable.subscribers()).toNumber(2);
    }

    @Test
    public void shouldReturnTrueOnIsAudioAdForPosition() {
        when(playQueue.isAudioAd(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isAudioAdAtPosition(0)).toBeTrue();
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
}
