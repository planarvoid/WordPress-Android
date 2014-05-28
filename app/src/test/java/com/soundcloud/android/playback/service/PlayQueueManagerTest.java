package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.tobedevoured.modelcitizen.CreateModelException;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
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

import javax.inject.Inject;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueManagerTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";

    @Inject
    PlayQueueManager playQueueManager;
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
    @Mock
    private EventBus eventBus;

    private EventMonitor eventMonitor;

    private PlaySessionSource playSessionSource;

    @Before
    public void before() throws CreateModelException {

        ObjectGraph.create(new TestModule()).inject(this);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);

        Playlist playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        playSessionSource  = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist);
        playSessionSource.setExploreVersion("1.0");

        eventMonitor = EventMonitor.on(eventBus);
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
        when(playQueue.getCurrentTrackId()).thenReturn(5L);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.getCurrentTrackId()).toEqual(5L);
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
    public void getIdAtPositionReturnsIdFromPlayQueueItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        expect(playQueueManager.getIdAtPosition(2)).toBe(3L);
    }

    @Test
    public void shouldSetNewPlayQueueCurrentTrackToManuallyTriggered() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(playQueue).setCurrentTrackToUserTriggered();
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenSettingNewPlayqueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectBroadcastPlayqueueChanged();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnSetNewPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        PlayQueueEvent playQueueEvent = eventMonitor.verifyEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue(){
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getCurrentTrackId()).thenReturn(3L);

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
        Mockito.reset(eventBus);

        playQueueManager.setPosition(1);

        eventMonitor.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldPublishTrackChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(1L, 2L, 3L), 0, playSessionSource), playSessionSource);

        playQueueManager.setPosition(2);

        PlayQueueEvent playQueueEvent = eventMonitor.verifyLastEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.TRACK_CHANGE);
    }

    @Test
    public void shouldPublishTrackChangeEventOnPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasPreviousTrack()).thenReturn(true);

        playQueueManager.previousTrack();

        PlayQueueEvent playQueueEvent = eventMonitor.verifyLastEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.TRACK_CHANGE);
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

        eventMonitor.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldPublishTrackChangeEventOnNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack()).thenReturn(true);
        playQueueManager.nextTrack();

        PlayQueueEvent playQueueEvent = eventMonitor.verifyLastEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.TRACK_CHANGE);
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

        eventMonitor.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldNotReloadPlayqueueFromStorageWhenPlaybackOperationsHasReturnsNoObservable(){
        expect(playQueueManager.loadPlayQueue()).toBeNull();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnLoadPlayQueueIfNoPlayQueueStore() {
        playQueueManager.loadPlayQueue();

        PlayQueueEvent playQueueEvent = eventMonitor.verifyEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldReturnResumeInfoWhenReloadingPlayQueue(){
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        PlaybackProgressInfo resumeInfo = playQueueManager.loadPlayQueue();
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
        when(playQueueOperations.getRelatedTracks(anyLong())).thenReturn(Observable.<RelatedTracksCollection>empty());

        playQueueManager.fetchRelatedTracks(123L);
        verify(playQueueOperations).getRelatedTracks(123L);
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks(){
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(anyLong())).thenReturn(observable);

        playQueueManager.fetchRelatedTracks(123L);
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks(){
        when(playQueueOperations.getRelatedTracks(anyLong())).thenReturn(Observable.<RelatedTracksCollection>never());
        playQueueManager.fetchRelatedTracks(123L);
        expect(playQueueManager.getPlayQueueView().getAppendState()).toEqual(PlaybackOperations.AppendState.LOADING);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldPublishPlayQueueRelatedTracksEventOnRelatedLoadingStateChange() throws CreateModelException {
        when(playQueueOperations.getRelatedTracks(anyLong())).thenReturn(Observable.<RelatedTracksCollection>never());
        playQueueManager.fetchRelatedTracks(123L);

        PlayQueueEvent playQueueEvent = eventMonitor.verifyEventOn(EventQueue.PLAY_QUEUE);
        expect(playQueueEvent.getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
        playQueueManager.setNewPlayQueue(PlayQueue.fromIdList(Lists.newArrayList(123L), 0, playSessionSource), playSessionSource);
        playQueueManager.onNext(new RelatedTracksCollection(Lists.newArrayList(trackSummary), "123"));

        expect(playQueueManager.getPlayQueueView()).toContainExactly(123L, trackSummary.getId());

        ArgumentCaptor<Track> captor = ArgumentCaptor.forClass(Track.class);
        verify(modelManager).cache(captor.capture());
        expect(captor.getValue().getId()).toEqual(trackSummary.getId());
    }

    @Test
    public void shouldSetIdleStateOnQueueAndBroadcastWhenDoneSuccessfulRelatedLoad(){
        playQueueManager.onNext(new RelatedTracksCollection(Collections.<TrackSummary>emptyList(), "123"));
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getAppendState()).toEqual(PlaybackOperations.AppendState.IDLE);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad(){
        playQueueManager.onCompleted();
        expect(playQueueManager.getPlayQueueView().getAppendState()).toEqual(PlaybackOperations.AppendState.EMPTY);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled(){
        playQueueManager.onError(new Throwable());
        expect(playQueueManager.getPlayQueueView().getAppendState()).toEqual(PlaybackOperations.AppendState.ERROR);
        expectBroadcastRelatedLoadChanges();
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
        playSessionSource.setPlaylist(playlist);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isCurrentPlaylist(6L)).toBeTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        Playlist playlist = new Playlist(6L);
        playSessionSource.setPlaylist(playlist);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isPlaylist()).toBeTrue();
    }

    @Test
    public void shouldRetryWithSameObservable() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(anyLong())).thenReturn(observable);

        playQueueManager.fetchRelatedTracks(123L);
        playQueueManager.retryRelatedTracksFetch();
        expect(observable.subscribers()).toNumber(2);
    }

    private void expectBroadcastPlayqueueChanged() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
    }

    private void expectBroadcastRelatedLoadChanges() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(PlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED);
    }

    @Module(library = true, injects = PlayQueueManagerTest.class)
    class TestModule {
        @Provides
        Context provideContext(){
            return context;
        }

        @Provides
        PlayQueue provideTrackingPlayQueue(){
            return playQueue;
        }

        @Provides
        ScModelManager provideModelManager(){
            return modelManager;
        }

        @Provides
        SharedPreferences provideSharedPreferences(){
            return sharedPreferences;
        }

        @Provides
        PlayQueueOperations providePlayQueueOperations(){
            return playQueueOperations;
        }

        @Provides
        EventBus provideEventBus() {
            return eventBus;
        }

    }
}
