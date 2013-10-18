package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueManagerTest {
    String playQueueUri = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400&playSource-recommenderVersion=v1&playSource-exploreTag=2&playSource-originUrl=1&playSource-initialTrackId=1";

    PlayQueueManager playQueueManager;

    @Mock
    PlayQueue playQueue = Mockito.mock(PlayQueue.class);
    @Mock
    Context context;
    @Mock
    PlayQueueStorage playQueueStorage;
    @Mock
    ExploreTracksOperations exploreTracksOperations;
    @Mock
    PlaySourceInfo trackingInfo;
    @Mock
    ScModelManager modelManager;
    @Mock
    SharedPreferences sharedPreferences;
    @Mock
    SharedPreferences.Editor sharedPreferencesEditor;

    @Before
    public void before() {
        playQueueManager = new PlayQueueManager(context, playQueueStorage, exploreTracksOperations, sharedPreferences, modelManager);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeAsync(any(PlayQueue.class))).thenReturn(Observable.just(PlayQueue.EMPTY));
        when(playQueue.isEmpty()).thenReturn(true);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullValueWhenSettingNewPlayqueue(){
        playQueueManager.setNewPlayQueue(null);
    }

    @Test
    public void shouldSetNewPlayQueueAsCurrentPlayQueue() throws Exception {

        playQueueManager.setNewPlayQueue(playQueue);
        expect(playQueueManager.getCurrentPlayQueue()).toEqual(playQueue);
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenSettingNewPlayqueue() throws Exception {
        playQueueManager.setNewPlayQueue(playQueue);
        expectBroadcastPlayqueueChanged();
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue(){
        final String playQueueState = "play-queue-state";

        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getCurrentTrackId()).thenReturn(3L);
        when(playQueue.getPlayQueueState(0, 3L)).thenReturn(Uri.parse(playQueueState));

        playQueueManager.setNewPlayQueue(playQueue);
        verify(sharedPreferencesEditor).putString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, playQueueState);
    }

    @Test
    public void shouldStoreTracksWhenSettingNewPlayQueue(){
        Observable<PlayQueue> observable = Mockito.mock(Observable.class);
        when(playQueueStorage.storeAsync(playQueue)).thenReturn(observable);
        playQueueManager.setNewPlayQueue(playQueue);
        verify(observable).subscribe(DefaultObserver.NOOP_OBSERVER);

    }

    @Test
    public void shouldNotUpdateCurrentPositionIfPlayqueueIsNull() throws Exception {
        playQueueManager.saveCurrentPosition(22L);
        verifyZeroInteractions(sharedPreferences);
    }

    @Test
    public void shouldNotReloadPlayqueueFromStorageWhenLastUriDoesNotExist(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(null);
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldNotReloadPlayQueueWithInvalidUri(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn("asdf321");
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenLastUriDoesNotExist(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(null);
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        expectBroadcastPlayqueueChanged();
    }

    @Test
    public void shouldReturnResumeInfoWhenReloadingPlayQueue(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(playQueueUri);
        when(playQueueStorage.getTrackIds()).thenReturn(Mockito.mock(Observable.class));

        PlayQueueManager.ResumeInfo resumeInfo = playQueueManager.reloadPlayQueue();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldLoadTrackIdsWhenReloadingPlayQueue(){
        Observable<List<Long>> observable = Mockito.mock(Observable.class);
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(playQueueUri);
        when(playQueueStorage.getTrackIds()).thenReturn(observable);

        playQueueManager.reloadPlayQueue();
        verify(observable).subscribe(any(Action1.class));
    }

    @Test
    public void shouldAddIdsWhenReloadingPlayQueueReturns(){

        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(playQueueUri);
        when(playQueueStorage.getTrackIds()).thenReturn(Observable.<List<Long>>just(Lists.newArrayList(1L, 2L, 3L)));
        playQueueManager.reloadPlayQueue();
        expect(playQueueManager.getCurrentPlayQueue().getCurrentTrackIds()).toContainExactly(1L,2L,3L);
    }

    @Test
    public void shouldReloadShouldBeTrue(){
        expect(playQueueManager.shouldReloadQueue()).toBeTrue();
    }

    @Test
    public void shouldReloadShouldBeFalseWithNonEmptyQueue(){
        final String playQueueState = "play-queue-state";

        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.getCurrentTrackId()).thenReturn(3L);
        when(playQueue.getPlayQueueState(0, 3L)).thenReturn(Uri.parse(playQueueState));

        playQueueManager.setNewPlayQueue(playQueue);
        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
    }

    @Test
    public void shouldReloadShouldBeFalseIfAlreadyReloading(){
        String uriString = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400";
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(uriString);
        final Observable mock = Mockito.mock(Observable.class);
        when(playQueueStorage.getTrackIds()).thenReturn(mock);
        when(mock.subscribe(any(Action1.class))).thenReturn(Subscriptions.empty());
        playQueueManager.reloadPlayQueue();

        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
    }

    @Test
    public void shouldGetRelatedTracksObservableWhenFetchingRelatedTracks(){
        final Observable mock = Mockito.mock(Observable.class);
        when(exploreTracksOperations.getRelatedTracks(anyLong())).thenReturn(mock);

        playQueueManager.fetchRelatedTracks(123L);
        verify(exploreTracksOperations).getRelatedTracks(123L);
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks(){
        final Observable<RelatedTracksCollection> mock = Mockito.mock(Observable.class);
        when(exploreTracksOperations.getRelatedTracks(anyLong())).thenReturn(mock);

        playQueueManager.fetchRelatedTracks(123L);
        verify(mock).subscribe(playQueueManager);
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks(){
        when(exploreTracksOperations.getRelatedTracks(anyLong())).thenReturn(Mockito.mock(Observable.class));
        playQueueManager.fetchRelatedTracks(123L);
        expect(playQueueManager.getCurrentPlayQueue().getAppendState()).toEqual(PlayQueue.AppendState.LOADING);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
        playQueueManager.setNewPlayQueue(new PlayQueue(123L));
        playQueueManager.onNext(new RelatedTracksCollection(Lists.<TrackSummary>newArrayList(trackSummary), "123"));

        expect(playQueueManager.getCurrentPlayQueue().getCurrentTrackIds()).toContainExactly(123L, trackSummary.getId());

        ArgumentCaptor<Track> captor = ArgumentCaptor.forClass(Track.class);
        verify(modelManager).cache(captor.capture());
        expect(captor.getValue().getId()).toEqual(trackSummary.getId());
    }

    @Test
    public void shouldSetIdleStateOnQueueAndBroadcastWhenDoneSuccessfulRelatedLoad(){
        playQueueManager.onNext(new RelatedTracksCollection(Collections.<TrackSummary>emptyList(), "123"));
        playQueueManager.onCompleted();
        expect(playQueueManager.getCurrentPlayQueue().getAppendState()).toEqual(PlayQueue.AppendState.IDLE);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad(){
        playQueueManager.onCompleted();
        expect(playQueueManager.getCurrentPlayQueue().getAppendState()).toEqual(PlayQueue.AppendState.EMPTY);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled(){
        playQueueManager.onError(new Throwable());
        expect(playQueueManager.getCurrentPlayQueue().getAppendState()).toEqual(PlayQueue.AppendState.ERROR);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void clearAllShouldClearPreferences() throws Exception {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.clearAll();
        verify(sharedPreferencesEditor).remove(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void clearAllShouldClearStorage() throws Exception {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.clearAll();
        verify(playQueueStorage).clearState();
    }

    @Test
    public void clearAllShouldSetPlayQueueToEmpty() throws Exception {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(new PlayQueue(1L));
        expect(playQueueManager.getCurrentPlayQueue()).not.toBe(PlayQueue.EMPTY);
        playQueueManager.clearAll();
        expect(playQueueManager.getCurrentPlayQueue()).toBe(PlayQueue.EMPTY);

    }

    @Test
    public void shouldRetryWithSameObservable() throws Exception {
        final Observable observable = Mockito.mock(Observable.class);
        when(exploreTracksOperations.getRelatedTracks(anyLong())).thenReturn(observable);
        playQueueManager.fetchRelatedTracks(123L);
        playQueueManager.retryRelatedTracksFetch();
        verify(observable, times(2)).subscribe(any(Observer.class));
    }

    private void expectBroadcastPlayqueueChanged() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
    }

    private void expectBroadcastRelatedLoadChanges() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED);
    }
}
