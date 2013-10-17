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
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
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

import java.io.IOException;
import java.util.ArrayList;
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
        verify(sharedPreferencesEditor).putString(PlayQueueManager.SC_PLAYQUEUE_URI, playQueueState);
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
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(null);
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldNotReloadPlayQueueWithInvalidUri(){
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn("asdf321");
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        verifyZeroInteractions(playQueueStorage);
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenLastUriDoesNotExist(){
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(null);
        expect(playQueueManager.reloadPlayQueue()).toBeNull();
        expectBroadcastPlayqueueChanged();
    }

    @Test
    public void shouldReturnResumeInfoWhenReloadingPlayQueue(){
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(playQueueUri);
        when(playQueueStorage.getTrackIds()).thenReturn(Mockito.mock(Observable.class));

        PlayQueueManager.ResumeInfo resumeInfo = playQueueManager.reloadPlayQueue();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldLoadTrackIdsWhenReloadingPlayQueue(){
        Observable<List<Long>> observable = Mockito.mock(Observable.class);
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(playQueueUri);
        when(playQueueStorage.getTrackIds()).thenReturn(observable);

        playQueueManager.reloadPlayQueue();
        verify(observable).subscribe(any(Action1.class));
    }

    @Test
    public void shouldAddIdsWhenReloadingPlayQueueReturns(){

        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(playQueueUri);
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
        when(sharedPreferences.getString(PlayQueueManager.SC_PLAYQUEUE_URI, null)).thenReturn(uriString);
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
        expect(playQueueManager.getCurrentPlayQueue().getCurrentAppendState()).toEqual(PlayQueue.AppendState.LOADING);
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
        expect(playQueueManager.getCurrentPlayQueue().getCurrentAppendState()).toEqual(PlayQueue.AppendState.IDLE);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad(){
        playQueueManager.onCompleted();
        expect(playQueueManager.getCurrentPlayQueue().getCurrentAppendState()).toEqual(PlayQueue.AppendState.EMPTY);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled(){
        playQueueManager.onError(new Throwable());
        expect(playQueueManager.getCurrentPlayQueue().getCurrentAppendState()).toEqual(PlayQueue.AppendState.ERROR);
        expectBroadcastRelatedLoadChanges();
    }

    @Test
    public void clearAllShouldClearPreferences() throws Exception {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.clearAll();
        verify(sharedPreferencesEditor).remove(PlayQueueManager.SC_PLAYQUEUE_URI);
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


//
//    @Test
//    public void shouldHandleEmptyPlaylistWithAddItemsFromUri() throws Exception {
//        pm.loadUri(Content.TRACKS.uri, 0, trackingInfo);
//        expect(pm.length()).toEqual(0);
//        expect(pm.isEmpty()).toBeTrue();
//        expect(pm.next()).toBeFalse();
//        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)).toBeNull();
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(trackingInfo);
//    }
//
//    @Test
//    public void shouldAddItemsFromUri() throws Exception {
//        List<Track> tracks = createTracks(3, true, 0);
//        pm.loadUri(Content.TRACKS.uri, 0, null, 0, trackingInfo);
//
//        expect(pm.getUri()).not.toBeNull();
//        expect(pm.getUri()).toEqual(Content.TRACKS.uri);
//
//        expect(pm.length()).toEqual(tracks.size());
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(trackingInfo);
//
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #0");
//
//        expect(pm.next()).toBeTrue();
//        track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #1");
//        expect(pm.getPrev().toBlockingObservable().lastOrDefault(null).title).toEqual("track #0");
//        expect(pm.getNext().toBlockingObservable().lastOrDefault(null).title).toEqual("track #2");
//
//        expect(pm.next()).toBeTrue();
//        track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #2");
//        expect(pm.getPrev().toBlockingObservable().lastOrDefault(null).title).toEqual("track #1");
//        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)).toBeNull();
//
//        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)).toBeNull();
//        expect(pm.next()).toBeFalse();
//    }
//
//    @Test
//    public void shouldAddItemsFromUriWithPosition() throws Exception {
//        List<Track> tracks = createTracks(3, true, 0);
//        pm.loadUri(Content.TRACKS.uri, 1, trackingInfo);
//
//        expect(pm.length()).toEqual(tracks.size());
//        expect(pm.isEmpty()).toBeFalse();
//
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #1");
//        expect(pm.getPrev()).not.toBeNull();
//        expect(pm.getPrev().toBlockingObservable().lastOrDefault(null).title).toEqual("track #0");
//
//        expect(pm.next()).toBeTrue();
//        track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #2");
//        expect(pm.getPrev()).not.toBeNull();
//        expect(pm.getPrev().toBlockingObservable().lastOrDefault(null).title).toEqual("track #1");
//        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)).toBeNull();
//    }
//
//    @Test
//    public void shouldAddItemsFromUriWithInvalidPosition() throws Exception {
//        List<Track> tracks = createTracks(3, true, 0);
//        pm.loadUri(Content.TRACKS.uri, tracks.size() + 100, trackingInfo); // out of range
//
//        expect(pm.length()).toEqual(tracks.size());
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #0");
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=origin-url&exploreTag=explore-tag&trigger=auto&source=recommender&source_version=version_1");
//    }
//
//    @Test
//    public void shouldAddItemsFromUriWithIncorrectPositionDown() throws Exception {
//        List<Track> tracks = createTracks(10, true, 0);
//        pm.loadUri(Content.TRACKS.uri, 7, new long[]{5L}, 0, trackingInfo);
//
//        expect(pm.length()).toEqual(tracks.size());
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #5");
//    }
//
//    @Test
//    public void shouldAddItemsFromUriWithIncorrectPositionUp() throws Exception {
//        List<Track> tracks = createTracks(10, true, 0);
//        pm.loadUri(Content.TRACKS.uri, 5, new long[]{7L}, 0, trackingInfo);
//
//        expect(pm.length()).toEqual(tracks.size());
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #7");
//    }
//
//    @Test
//    public void shouldAddItemsFromUriWithNegativePosition() throws Exception {
//        List<Track> tracks = createTracks(3, true, 0);
//        pm.loadUri(Content.TRACKS.uri, -10, null, trackingInfo); // out of range
//
//        expect(pm.length()).toEqual(tracks.size());
//        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
//        expect(track).not.toBeNull();
//        expect(track.title).toEqual("track #0");
//    }
//
////    @Test
////    public void shouldReturnPlayQueueState() throws Exception {
////        pm.setFromTrackIds(createTracks(3, true, 0), 1, new PlaySourceInfo.Builder(2L).build());
////        final PlayQueueState state = pm.getState();
////        expect(state.getCurrentTrackIds()).toContainExactly(0L, 1L, 2L);
////        expect(state.getPlayPosition()).toBe(1);
////        expect(state.getCurrentAppendState()).toBe(PlayQueueState.AppendState.IDLE);
////    }
////
////    @Test
////    public void shouldSetPlaySourceTriggerBasedOnInitalId() throws Exception {
////        pm.setFromTrackIds(createTracks(3, true, 0), 0, new PlaySourceInfo.Builder(2L).build());
////        expect(pm.getPlayQueueItem(0).getTrackSourceInfo().getTrigger()).toEqual("auto");
////        expect(pm.getPlayQueueItem(1).getTrackSourceInfo().getTrigger()).toEqual("auto");
////        expect(pm.getPlayQueueItem(2).getTrackSourceInfo().getTrigger()).toEqual("manual");
////    }
////
////    @Test
////    public void shouldSetPlaySourceRecommenderVersion() throws Exception {
////        pm.setFromTrackIds(createTracks(3, true, 0), 0, new PlaySourceInfo.Builder(2L).recommenderVersion("version1").build());
////        expect(pm.getPlayQueueItem(0).getTrackSourceInfo().getRecommenderVersion()).toEqual("version1");
////        expect(pm.getPlayQueueItem(1).getTrackSourceInfo().getRecommenderVersion()).toEqual("version1");
////        expect(pm.getPlayQueueItem(2).getTrackSourceInfo().getRecommenderVersion()).toBeNull();
////    }
////
////    @Test
////    public void shouldSupportSetPlaylistWithTrackObjects() throws Exception {
////        pm.setFromTrackIds(createTracks(3, true, 0), 2, new PlaySourceInfo.Builder(2L).build());
////        expect(pm.length()).toEqual(3);
////
////        Track track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
////        expect(track).not.toBeNull();
////        expect(track.title).toEqual("track #2");
////
////        expect(pm.prev()).toBeTrue();
////        track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
////        expect(track).not.toBeNull();
////        expect(track.title).toEqual("track #1");
////        expect(pm.getPrev().toBlockingObservable().lastOrDefault(null).title).toEqual("track #0");
////        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)..title).toEqual("track #2");
////
////        expect(pm.prev()).toBeTrue();
////        track = pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null);
////        expect(track).not.toBeNull();
////        expect(track.title).toEqual("track #0");
////        expect(pm.getNext().toBlockingObservable().lastOrDefault(null)..title).toEqual("track #1");
////        expect(pm.getPrev()).toBeNull();
////
////        expect(pm.getPrev()).toBeNull();
////        expect(pm.prev()).toBeFalse();
////    }
////
////    @Test
////    public void shouldSetEventLoggerParamsWhenSettingPlaylist() throws Exception {
////        pm.setFromTrackIds(createTracks(3, true, 0), 2, new PlaySourceInfo.Builder(2L).exploreTag("exploreTag").originUrl("originUrl").build());
////        expect(pm.length()).toEqual(3);
////        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=manual");
////        expect(pm.prev()).toBeTrue();
////        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=auto");
////        expect(pm.prev()).toBeTrue();
////        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=auto");
////    }
////
////    @Test
////    public void shouldClearPlaylist() throws Exception {
////        pm.setFromTrackIds(createTracks(10, true, 0), 0, trackingInfo);
////        pm.clear();
////        expect(pm.isEmpty()).toBeTrue();
////        expect(pm.length()).toEqual(0);
////        expect(pm.getCurrentPlaySourceInfo()).toEqual(PlaySourceInfo.EMPTY);
////        expect(pm.getCurrentEventLoggerParams()).toEqual(ScTextUtils.EMPTY_STRING);
////    }
////
////    @Test
////    public void shouldSaveCurrentTracksToDB() throws Exception {
////        expect(Content.PLAY_QUEUE).toBeEmpty();
////        expect(Content.PLAY_QUEUE.uri).toBeEmpty();
////        pm.setFromTrackIds(createTracks(10, true, 0), 0, trackingInfo);
////        expect(Content.PLAY_QUEUE.uri).toHaveCount(10);
////    }
//
//    @Test
//    public void shouldLoadLikesAsPlaylist() throws Exception {
//        insertLikes();
//
//        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(56142962l).build();
//        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962), playSourceInfo);
//
//        expect(pm.length()).toEqual(2);
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56142962l);
//        expect(pm.next()).toBeTrue();
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56143158l);
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(playSourceInfo);
//    }
//
//    @Test
//    public void shouldSetTrackingInfoWhenLoadingPlaylistFromLikes() throws Exception {
//        insertLikes();
//
//        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(56142962l).exploreTag("exploreTag").originUrl("originUrl").build();
//        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962), playSourceInfo);
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=manual");
//        expect(pm.next()).toBeTrue();
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=auto");
//    }
//
//    @Test
//    public void shouldSaveAndRestoreLikesAsPlaylist() throws Exception {
//        insertLikes();
//        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(56143158L).exploreTag("exploreTag").originUrl("originUrl").build();
//        pm.loadUri(Content.ME_LIKES.uri, 0, new Track(56143158L), playSourceInfo);
//        expect(pm.length()).toEqual(2);
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56143158L);
//        expect(pm.getPosition()).toEqual(1);
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=manual");
//
//        pm.saveQueue(1000l);
//        pm.clear();
//
//        expect(pm.reloadQueue()).toEqual(1000l);
//        expect(pm.getCurrentTrackId()).toEqual(56143158L);
//        expect(pm.getPosition()).toEqual(1);
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=originUrl&exploreTag=exploreTag&trigger=manual");
//    }
//
//    @Test
//    public void shouldSaveAndRestoreLikesAsPlaylistTwice() throws Exception {
//        insertLikes();
//        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962l), trackingInfo);
//        expect(pm.length()).toEqual(2);
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56142962l);
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(trackingInfo);
//        pm.saveQueue(1000l);
//        pm.clear();
//
//        expect(pm.reloadQueue()).toEqual(1000l);
//        expect(pm.getCurrentTrackId()).toEqual(56142962l);
//        expect(pm.getPosition()).toEqual(0);
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(trackingInfo);
//
//        // test overwrite
//        expect(pm.next()).toBeTrue();
//        expect(pm.getCurrentTrackId()).toEqual(56143158l);
//        pm.saveQueue(2000l);
//        expect(pm.reloadQueue()).toEqual(2000l);
//        expect(pm.getCurrentTrackId()).toEqual(56143158l);
//        expect(pm.getPosition()).toEqual(1);
//        expect(pm.getCurrentPlaySourceInfo()).toEqual(trackingInfo);
//    }
//
//    @Test
//    public void shouldSaveAndRestoreLikesAsPlaylistWithMovedTrack() throws Exception {
//        insertLikes();
//        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962l), trackingInfo);
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56142962l);
//        expect(pm.next()).toBeTrue();
//
//        pm.saveQueue(1000l);
//
//        expect(pm.reloadQueue()).toEqual(1000l);
//        expect(pm.getCurrentTrackId()).toEqual(56143158l);
//        expect(pm.getPosition()).toEqual(1);
//    }
//
//    @Test
//    public void shouldSavePlaylistStateInUri() throws Exception {
//        insertLikes();
//        final Track track = new Track(56142962l);
//        pm.loadUri(Content.ME_LIKES.uri, 1, track, trackingInfo);
//        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(56142962l);
//        expect(pm.next()).toBeTrue();
//        expect(pm.getPlayQueueState(123L, 56143158L)).toEqual(
//          Content.ME_LIKES.uri + "?trackId=56143158&playlistPos=1&seekPos=123&playSource-recommenderVersion=version_1&playSource-exploreTag=explore-tag&playSource-originUrl=origin-url&playSource-initialTrackId=123"
//        );
//    }
//
////    @Test
////    public void shouldSavePlaylistStateInUriWithSetPlaylist() throws Exception {
////        pm.setFromTrackIds(createTracks(10, true, 0), 5, trackingInfo);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(5L);
////        expect(pm.getPlayQueueState(123L, 5L)).toEqual(
////                Content.PLAY_QUEUE.uri + "?trackId=5&playlistPos=5&seekPos=123&playSource-recommenderVersion=version_1&playSource-exploreTag=explore-tag&playSource-originUrl=origin-url&playSource-initialTrackId=123"
////
////        );
////    }
////
////    @Test
////    public void shouldSkipUnstreamableTrackNext() throws Exception {
////        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
////        playables.addAll(createTracks(1, true, 0));
////        playables.addAll(createTracks(1, false, 1));
////
////        pm.setFromTrackIds(playables, 0, trackingInfo);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(0L);
////        expect(pm.next()).toEqual(false);
////
////        playables.addAll(createTracks(1, true, 2));
////        pm.setFromTrackIds(playables, 0, trackingInfo);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(0L);
////        expect(pm.next()).toEqual(true);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(2L);
////    }
////
////    @Test
////    public void shouldSkipUnstreamableTrackPrev() throws Exception {
////        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
////        playables.addAll(createTracks(1, false, 0));
////        playables.addAll(createTracks(1, true, 1));
////
////        pm.setFromTrackIds(playables, 1, trackingInfo);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(1L);
////        expect(pm.prev()).toEqual(false);
////
////        playables.addAll(0, createTracks(1, true, 2));
////        pm.setFromTrackIds(playables, 2, trackingInfo);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(1L);
////        expect(pm.prev()).toEqual(true);
////        expect(pm.getCurrentTrack().toBlockingObservable().lastOrDefault(null).getId()).toEqual(2L);
////    }
////
////    @Test
////    public void shouldRespondToUriChanges() throws Exception {
////        Playlist p = TestHelper.readResource("/com/soundcloud/android/service/sync/playlist.json");
////        TestHelper.insertWithDependencies(p);
////
////        Uri playlistUri = p.toUri();
////        expect(playlistUri).toEqual(Content.PLAYLIST.forQuery(String.valueOf(2524386)));
////
////        pm.loadUri(playlistUri, 5, new Track(7L), trackingInfo);
////        pm.saveQueue(1000l);
////
////        expect(pm.reloadQueue()).toEqual(1000l);
////        expect(pm.getUri().getPath()).toEqual(playlistUri.getPath());
////
////        final Uri newUri = Content.PLAYLIST.forQuery("321");
////        PlayQueueManager.onPlaylistUriChanged(pm, DefaultTestRunner.application, playlistUri, newUri);
////        expect(pm.getUri().getPath()).toEqual(newUri.getPath());
////    }
////
////    @Test
////    public void shouldClearPlaylistState() throws Exception {
////        pm.setFromTrackIds(createTracks(10, true, 0), 5, trackingInfo);
////        pm.saveQueue(1235);
////
////        pm.clearAllLocalState();
////        expect(pm.reloadQueue()).toEqual(-1L);
////
////        PlayQueueManager pm2 = new PlayQueueManager(Robolectric.application, USER_ID, exploreTracksOperations, trackStorage);
////        expect(pm2.reloadQueue()).toEqual(-1L);
////        expect(pm2.getPosition()).toEqual(0);
////        expect(pm2.length()).toEqual(0);
////    }
////
////    @Test
////    public void shouldSetSingleTrack() throws Exception {
////        List<Track> tracks = createTracks(1, true, 0);
////        pm.loadTrack(tracks.get(0), true, trackingInfo);
////        expect(pm.length()).toEqual(1);
////        expect(pm.getCurrentTrack()).toBe(tracks.get(0));
////        expect(pm.getCurrentEventLoggerParams()).toEqual("context=origin-url&exploreTag=explore-tag&trigger=manual");
////    }
//
//    @Test
//    public void shouldAddSeedTrackAndSetLoadingStateWhileLoadingExploreTracks() throws Exception {
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<RelatedTracksCollection>never());
//
//        expect(pm.length()).toBe(0);
//        pm.loadTrack(track, false, trackingInfo);
//        pm.fetchRelatedTracks(track);
//        expect(pm.length()).toBe(1);
//        expect(pm.getState().isLoading()).toBeTrue();
//    }
//
//    @Test
//    public void shouldSetFailureStateAfterFailedRelatedLoad() throws Exception {
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<RelatedTracksCollection>error(new Exception()));
//
//        pm.fetchRelatedTracks(track);
//        final PlayQueueState state = pm.getState();
//        expect(state.isLoading()).toBeFalse();
//        expect(state.lastLoadFailed()).toBeTrue();
//    }
//
//    @Test
//    public void shouldAddTrackSetIdleStateAndBroadcastChangeAfterSuccessfulRelatedLoad() throws Exception {
//        Context context = Mockito.mock(Context.class);
//        setupSuccesfulRelatedLoad(context);
//
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        expect(pm.length()).toBe(0);
//        pm.loadTrack(track, false, trackingInfo);
//        pm.fetchRelatedTracks(track);
//        expect(pm.length()).toBe(2);
//
//        final PlayQueueState state = pm.getState();
//        expect(state.isLoading()).toBeFalse();
//        expect(state.lastLoadFailed()).toBeFalse();
//
//        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
//        verify(context, times(3)).sendBroadcast(argumentCaptor.capture());
//        final List<Intent> allValues = argumentCaptor.getAllValues();
//        expect(allValues.size()).toEqual(3);
//        assertThat(allValues.get(0).getAction(), is(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED));
//        assertThat(allValues.get(1).getAction(), is(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
//        assertThat(allValues.get(2).getAction(), is(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
//    }
//
//    @Test
//    public void shouldSetEventLoggerParamsOnRelatedTracks() throws Exception {
//        Context context = Mockito.mock(Context.class);
//        setupSuccesfulRelatedLoad(context);
//
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        pm.loadTrack(track, false, trackingInfo);
//        pm.fetchRelatedTracks(track);
//        expect(pm.next()).toBeTrue();
//        expect(pm.getCurrentEventLoggerParams()).toEqual("context=origin-url&exploreTag=explore-tag&trigger=auto&source=recommender&source_version=recommenderVersion2");
//    }
//
//    private void setupSuccesfulRelatedLoad(Context context) throws CreateModelException {
//        pm = new PlayQueue(context, exploreTracksOperations);
//
//        TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
//        RelatedTracksCollection relatedTracks = new RelatedTracksCollection(Lists.newArrayList(trackSummary), "recommenderVersion2");
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<RelatedTracksCollection>just(relatedTracks));
//    }
//
//    @Test
//    public void shouldRetryRelatedLoadWithSameObservable() throws Exception {
//        Context context = Mockito.mock(Context.class);
//        pm = new PlayQueue(context, exploreTracksOperations);
//
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        final Observable<RelatedTracksCollection> observable = Mockito.mock(Observable.class);
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);
//
//        pm.fetchRelatedTracks(track);
//        pm.retryRelatedTracksFetch();
//        verify(observable, times(2)).subscribe(any(Observer.class));
//    }
//
//    @Test
//    public void shouldUnsubscribeAndNotBeLoadingAfterCallingSetTrack() throws Exception {
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        final Observable<RelatedTracksCollection> observable = Mockito.mock(Observable.class);
//        final Subscription subscription = Mockito.mock(Subscription.class);
//
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);
//        when(observable.subscribe(any(Observer.class))).thenReturn(subscription);
//
//        pm.fetchRelatedTracks(track);
//        pm.loadTrack(track, false, trackingInfo);
//
//        expect(pm.getState().isLoading()).toBeFalse();
//        verify(subscription).unsubscribe();
//    }
//
//    @Test
//    public void shouldUnsubscribeAndNotBeLoadingAfterCallingloadUri() throws Exception {
//        Track track = TestHelper.getModelFactory().createModel(Track.class);
//        final Observable<RelatedTracksCollection> observable = Mockito.mock(Observable.class);
//        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);
//
//        final Subscription subscription = Mockito.mock(Subscription.class);
//        when(observable.subscribe(any(Observer.class))).thenReturn(subscription);
//
//        pm.fetchRelatedTracks(track);
//        pm.loadUri(Content.TRACKS.uri, 0, null, trackingInfo);
//
//        expect(pm.getState().isLoading()).toBeFalse();
//        verify(subscription).unsubscribe();
//    }

    private void insertLikes() throws IOException {
        List<Playable> likes = TestHelper.readResourceList("/com/soundcloud/android/service/sync/e1_likes.json");
        expect(TestHelper.bulkInsert(Content.ME_LIKES.uri, likes)).toEqual(3);
    }

    // TODO : replace with model factory
    private List<Track> createTracks(int n, boolean streamable, int startPos) {
        List<Track> list = new ArrayList<Track>();

        User user = new User();
        user.setId(0L);

        for (int i=0; i<n; i++) {
            Track t = new Track();
            t.setId((startPos +i));
            t.title = "track #"+(startPos+i);
            t.user = user;
            t.stream_url = streamable ? "http://www.soundcloud.com/sometrackurl" : null;
            TestHelper.insertWithDependencies(t);
            list.add(t);
        }
        return list;
    }
}
