package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.tobedevoured.modelcitizen.CreateModelException;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueManagerTest {
    private final String playQueueUri = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400&playSource-recommenderVersion=v1&playSource-exploreVersion=2&playSource-originUrl=1&playSource-initialTrackId=1";

    @Inject
    PlayQueueManager playQueueManager;
    @Mock
    private Context context;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private PlayQueueStorage playQueueStorage;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor sharedPreferencesEditor;

    @Before
    public void before() {

        ObjectGraph.create(new TestModule()).inject(this);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeCollectionAsync(any(Collection.class))).thenReturn(Observable.<Collection<PlayQueueItem>>empty());
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
    public void shouldSetNewPlayQueueCurrentTrackToManuallyTriggered() throws Exception {
        playQueueManager.setNewPlayQueue(playQueue);
        verify(playQueue).setCurrentTrackToUserTriggered();
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
        final Observable mock = Mockito.mock(Observable.class);
        when(playQueueStorage.storeCollectionAsync(playQueue.getItems())).thenReturn(mock);
        playQueueManager.setNewPlayQueue(playQueue);
        verify(mock).subscribe(any(Observer.class));
    }

    @Test
    public void shouldNotUpdateCurrentPositionIfPlayqueueIsNull() throws Exception {
        playQueueManager.saveCurrentPosition(22L);
        verifyZeroInteractions(sharedPreferences);
    }

    @Test
    public void shouldNotReloadPlayqueueFromStorageWhenLastUriDoesNotExist(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(null);
        expect(playQueueManager.loadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldNotReloadPlayQueueWithInvalidUri(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn("asdf321");
        expect(playQueueManager.loadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenLastUriDoesNotExist(){
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(null);
        expect(playQueueManager.loadPlayQueue()).toBeNull();
        expectBroadcastPlayqueueChanged();
    }

    @Test
    public void shouldReturnResumeInfoWhenReloadingPlayQueue(){
        String uriString = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400";
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(uriString);
        Observable<List<PlayQueueItem>> observable = Mockito.mock(Observable.class);
        when(observable.observeOn(AndroidSchedulers.mainThread())).thenReturn(observable);
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(observable);

        PlaybackProgressInfo resumeInfo = playQueueManager.loadPlayQueue();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldReloadPlayQueueFromLocalStorage(){
        String uriString = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400";
        Observable<List<PlayQueueItem>> observable = Mockito.mock(Observable.class);
        when(observable.observeOn(AndroidSchedulers.mainThread())).thenReturn(observable);
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(uriString);
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(observable);

        playQueueManager.loadPlayQueue();
        verify(observable).subscribe(any(Action1.class));
    }

    @Test
    public void shouldSetNewPlayQueueWhenReloadingPlayQueueReturns(){
        String uriString = "content://com.soundcloud.android.provider.ScContentProvider/me/playqueue?trackId=456&playlistPos=2&seekPos=400";
        when(sharedPreferences.getString(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY, null)).thenReturn(uriString);
        final PlayQueueItem playQueueItem = Mockito.mock(PlayQueueItem.class);
        final PlayQueueItem playQueueItem2 = Mockito.mock(PlayQueueItem.class);
        when(playQueueItem.getId()).thenReturn(1L);
        when(playQueueItem2.getId()).thenReturn(2L);

        List<PlayQueueItem> playQueueItems = Lists.newArrayList(playQueueItem, playQueueItem2);
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(Observable.<List<PlayQueueItem>>just(playQueueItems));
        playQueueManager.loadPlayQueue();
        expect(playQueueManager.getPlayQueueView()).toContainExactly(1L, 2L);
    }

    @Test
    public void shouldReloadShouldBeTrueIfThePlayQueueIsEmpty(){
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
    public void shouldGetRelatedTracksObservableWhenFetchingRelatedTracks(){
        final Observable mock = Mockito.mock(Observable.class);
        when(playbackOperations.getRelatedTracks(anyLong())).thenReturn(mock);

        playQueueManager.fetchRelatedTracks(123L);
        verify(playbackOperations).getRelatedTracks(123L);
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks(){
        final Observable<RelatedTracksCollection> mock = Mockito.mock(Observable.class);
        when(playbackOperations.getRelatedTracks(anyLong())).thenReturn(mock);

        playQueueManager.fetchRelatedTracks(123L);
        verify(mock).subscribe(playQueueManager);
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks(){
        when(playbackOperations.getRelatedTracks(anyLong())).thenReturn(Mockito.mock(Observable.class));
        playQueueManager.fetchRelatedTracks(123L);
        expect(playQueueManager.getPlayQueueView().getAppendState()).toEqual(PlaybackOperations.AppendState.LOADING);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
        playQueueManager.setNewPlayQueue(new PlayQueue(123L));
        playQueueManager.onNext(new RelatedTracksCollection(Lists.<TrackSummary>newArrayList(trackSummary), "123"));

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
        when(playbackOperations.getRelatedTracks(anyLong())).thenReturn(observable);
        playQueueManager.fetchRelatedTracks(123L);
        playQueueManager.retryRelatedTracksFetch();
        verify(observable, times(2)).subscribe(any(Observer.class));
    }

    @Test
    public void shouldClearSharedPreferences() throws Exception {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        PlayQueueManager.clearPlayQueueUri(sharedPreferences);
        verify(sharedPreferencesEditor).remove(PlayQueueManager.PLAYQUEUE_URI_PREF_KEY);
        verify(sharedPreferencesEditor).apply();

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
        PlayQueueStorage providePlayQueueStorage(){
            return playQueueStorage;
        }

        @Provides
        PlaybackOperations providePlaybackOperations(){
            return playbackOperations;
        }

        @Provides
        ScModelManager provideModelManager(){
            return modelManager;
        }

        @Provides
        SharedPreferences provideSharedPreferences(){
            return sharedPreferences;
        }
    }
}
