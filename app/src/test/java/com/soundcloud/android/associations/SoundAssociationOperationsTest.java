package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.SoundAssociationStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundAssociationOperationsTest {

    private SoundAssociationOperations operations;

    @Mock
    private EventBus eventBus;
    @Mock
    private SoundAssociationStorage storage;
    @Mock
    private RxHttpClient httpClient;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private Observer observer;
    @Mock
    private APIResponse response;
    @Captor
    private ArgumentCaptor<PlayableChangedEvent> eventCaptor;

    @Before
    public void setUp() throws Exception {
        operations = new SoundAssociationOperations(eventBus, storage, httpClient, modelManager);
    }

    @Test
    public void shouldObtainIdsOfLikedTracksFromLocalStorage() {
        final ArrayList<Long> idList = Lists.newArrayList(1L, 2L, 3L);
        when(storage.getTrackLikesAsIdsAsync()).thenReturn(rx.Observable.<List<Long>>from(idList));
        operations.getLikedTracksIds().subscribe(observer);
        verify(observer).onNext(idList);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Test
    public void likingATrackShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation trackLike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.like(track).subscribe(observer);

        verify(modelManager).cache((Playable) track, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(trackLike);
    }

    @Test
    public void likingATrackShouldPublishPlayableChangedEvent() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation trackLike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.like(track).subscribe(observer);

        verify(eventBus).publish(refEq(EventQueue.PLAYABLE_CHANGED), eventCaptor.capture());
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void whenLikingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.like(new Track(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        verify(eventBus, never()).publish(refEq(EventQueue.PLAYABLE_CHANGED), any(PlayableChangedEvent.class));
    }

    @Test
    public void unlikingATrackShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.unlike(track).subscribe(observer);

        verify(modelManager).cache((Playable) track, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(trackUnlike);
    }

    @Test
    public void unlikingATrackShouldPublishChangeEvent() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.unlike(track).subscribe(observer);

        verify(eventBus).publish(refEq(EventQueue.PLAYABLE_CHANGED), eventCaptor.capture());
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void whenUnlikingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.unlike(new Track(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        verify(eventBus, never()).publish(refEq(EventQueue.PLAYABLE_CHANGED), any(PlayableChangedEvent.class));
    }

    @Test
    public void whenUnlikingATrackFailsBecauseItWasAlreadyRemovedFromServerItShouldBeRemovedFromStorageToo() {
        final Track track = new Track(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);

        APIResponse response404 = mock(APIResponse.class);
        when(response404.getStatusCode()).thenReturn(404);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), response404)));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.unlike(track).subscribe(observer);

        verify(observer).onNext(trackUnlike);
    }

    @Test
    public void whenUnlikingATrackFailsBecauseThereIsNoNetworkTheErrorShouldBePropagated() {
        final Track track = new Track(1L);
        APIResponse emptyResponse = mock(APIResponse.class);
        when(emptyResponse.getStatusCode()).thenThrow(new NullPointerException());
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), emptyResponse)));

        operations.unlike(track).subscribe(observer);

        verify(observer).onError(any(Exception.class));
    }

    @Test
    public void likingAPlaylistShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final Playlist playlist = new Playlist(1L);
        final SoundAssociation like = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/playlist_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(playlist)).thenReturn(Observable.just(like));

        operations.like(playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(like);
    }

    @Test
    public void unlikingAPlaylistShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final Playlist playlist = new Playlist(1L);
        final SoundAssociation unlike = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(playlist)).thenReturn(Observable.just(unlike));

        operations.unlike(playlist).subscribe(observer);

        verify(observer).onNext(unlike);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REPOSTING / UN-REPOSTING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void repostingATrackShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation repost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(repost));

        operations.repost(track).subscribe(observer);

        verify(modelManager).cache((Playable) track, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(repost);
    }

    @Test
    public void repostingATrackShouldPublishChangeEvent() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation repost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(repost));

        operations.repost(track).subscribe(observer);

        verify(eventBus).publish(refEq(EventQueue.PLAYABLE_CHANGED), eventCaptor.capture());
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void whenRepostingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.repost(new Track(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        verify(eventBus, never()).publish(refEq(EventQueue.PLAYABLE_CHANGED), any(PlayableChangedEvent.class));
    }

    @Test
    public void unrepostingATrackShouldSendDELETERequestToApiAndThenStoreTheAssociation() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.unrepost(track).subscribe(observer);

        verify(modelManager).cache((Playable) track, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(unrepost);
    }

    @Test
    public void unrepostingATrackShouldPublishChangeEvent() throws Exception {
        final Track track = new Track(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.unrepost(track).subscribe(observer);

        verify(eventBus).publish(refEq(EventQueue.PLAYABLE_CHANGED), eventCaptor.capture());
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(eventCaptor.getValue().getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void whenUnrepostingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.unrepost(new Track(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        verify(eventBus, never()).publish(refEq(EventQueue.PLAYABLE_CHANGED), any(PlayableChangedEvent.class));
    }

    @Test
    public void whenUnrepostingATrackFailsBecauseItWasAlreadyRemovedFromServerItShouldBeRemovedFromStorageToo() {
        final Track track = new Track(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);

        APIResponse response404 = mock(APIResponse.class);
        when(response404.getStatusCode()).thenReturn(404);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), response404)));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.unrepost(track).subscribe(observer);

        verify(observer).onNext(unrepost);
    }

    @Test
    public void repostingAPlaylistShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final Playlist playlist = new Playlist(1L);
        final SoundAssociation repost = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/playlist_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(playlist)).thenReturn(Observable.just(repost));

        operations.repost(playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(repost);
    }

    @Test
    public void unrepostingAPlaylistShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final Playlist playlist = new Playlist(1L);
        final SoundAssociation unrepost = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(playlist)).thenReturn(Observable.just(unrepost));

        operations.unrepost(playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(unrepost);
    }
}
