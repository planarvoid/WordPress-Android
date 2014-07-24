package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIRequestException;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.tracks.LegacyTrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundAssociationOperationsTest {

    private SoundAssociationOperations operations;

    private TestEventBus eventBus = new TestEventBus();

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
    @Mock
    private LegacyTrackOperations legacyTrackOperations;
    @Captor
    private ArgumentCaptor<PlayableChangedEvent> eventCaptor;

    @Before
    public void setUp() throws Exception {
        operations = new SoundAssociationOperations(eventBus, storage, httpClient, modelManager, legacyTrackOperations);
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
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation trackLike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.toggleLike(true, track).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(trackLike);
    }

    @Test
    public void likingATrackShouldPublishPlayableChangedEvent() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation trackLike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.toggleLike(true, track).subscribe(observer);

        PlayableChangedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void whenLikingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.toggleLike(true, new PublicApiTrack(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toBeEmpty();
    }

    @Test
    public void unlikingATrackShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(false, track).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(trackUnlike);
    }

    @Test
    public void unlikingATrackShouldPublishChangeEvent() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(false, track).subscribe(observer);

        PlayableChangedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void whenUnlikingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.toggleLike(false, new PublicApiTrack(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toBeEmpty();
    }

    @Test
    public void whenUnlikingATrackFailsBecauseItWasAlreadyRemovedFromServerItShouldBeRemovedFromStorageToo() {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation trackUnlike = new SoundAssociation(track);

        APIResponse response404 = mock(APIResponse.class);
        when(response404.getStatusCode()).thenReturn(404);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), response404)));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(false, track).subscribe(observer);

        verify(observer).onNext(trackUnlike);
    }

    @Test
    public void whenUnlikingATrackFailsBecauseThereIsNoNetworkTheErrorShouldBePropagated() {
        final PublicApiTrack track = new PublicApiTrack(1L);
        APIResponse emptyResponse = mock(APIResponse.class);
        when(emptyResponse.getStatusCode()).thenThrow(new NullPointerException());
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), emptyResponse)));

        operations.toggleLike(false, track).subscribe(observer);

        verify(observer).onError(any(Exception.class));
    }

    @Test
    public void likingAPlaylistShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        final SoundAssociation like = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/playlist_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(playlist)).thenReturn(Observable.just(like));

        operations.toggleLike(true, playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(like);
    }

    @Test
    public void unlikingAPlaylistShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        final SoundAssociation unlike = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeLikeAsync(playlist)).thenReturn(Observable.just(unlike));

        operations.toggleLike(false, playlist).subscribe(observer);

        verify(observer).onNext(unlike);
    }

    @Test
    public void toggleTrackLikeShouldReturnPropertySetWithUpdatedLikeStatus() {
        TrackUrn trackUrn = Urn.forTrack(1L);
        PublicApiTrack track = new PublicApiTrack(1L);
        track.user_like = true;
        track.likes_count = 12;
        SoundAssociation trackLike = new SoundAssociation(track);
        when(legacyTrackOperations.loadTrack(eq(1L), any(Scheduler.class))).thenReturn(Observable.just(track));
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        Observable<PropertySet> result = operations.toggleTrackLike(trackUrn, true);

        PropertySet changeSet = result.toBlocking().first();
        expect(changeSet.get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(changeSet.get(PlayableProperty.LIKES_COUNT)).toBe(12);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REPOSTING / UN-REPOSTING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void repostingATrackShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation repost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(repost));

        operations.toggleRepost(true, track).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(repost);
    }

    @Test
    public void repostingATrackShouldPublishChangeEvent() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation repost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(repost));

        operations.toggleRepost(true, track).subscribe(observer);

        PlayableChangedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void whenRepostingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.toggleRepost(true, new PublicApiTrack(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toBeEmpty();
    }

    @Test
    public void unrepostingATrackShouldSendDELETERequestToApiAndThenStoreTheAssociation() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.toggleRepost(false, track).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(unrepost);
    }

    @Test
    public void unrepostingATrackShouldPublishChangeEvent() throws Exception {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.toggleRepost(false, track).subscribe(observer);

        PlayableChangedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void whenUnrepostingATrackFailsItShouldNotPublishChangeEvent() throws Exception {
        when(httpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>error(new Exception()));

        operations.toggleRepost(false, new PublicApiTrack(1L)).subscribe(observer);

        verify(observer).onError(any(Exception.class));
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toBeEmpty();
    }

    @Test
    public void whenUnrepostingATrackFailsBecauseItWasAlreadyRemovedFromServerItShouldBeRemovedFromStorageToo() {
        final PublicApiTrack track = new PublicApiTrack(1L);
        final SoundAssociation unrepost = new SoundAssociation(track);

        APIResponse response404 = mock(APIResponse.class);
        when(response404.getStatusCode()).thenReturn(404);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/1"))))
                .thenReturn(Observable.<APIResponse>error(APIRequestException.badResponse(mock(APIRequest.class), response404)));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(unrepost));

        operations.toggleRepost(false, track).subscribe(observer);

        verify(observer).onNext(unrepost);
    }

    @Test
    public void repostingAPlaylistShouldSendPUTRequestToApiAndThenStoreTheAssociation() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        final SoundAssociation repost = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("PUT", "/e1/me/playlist_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(playlist)).thenReturn(Observable.just(repost));

        operations.toggleRepost(true, playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(repost);
    }

    @Test
    public void unrepostingAPlaylistShouldSendDELETERequestToApiAndThenRemoveTheAssociation() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        final SoundAssociation unrepost = new SoundAssociation(playlist);
        when(httpClient.fetchResponse(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_reposts/1"))))
                .thenReturn(Observable.just(response));
        when(storage.removeRepostAsync(playlist)).thenReturn(Observable.just(unrepost));

        operations.toggleRepost(false, playlist).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(observer).onNext(unrepost);
    }
}
