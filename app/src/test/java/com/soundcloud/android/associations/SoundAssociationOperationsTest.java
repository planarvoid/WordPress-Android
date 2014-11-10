package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LegacyPlaylistOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundAssociationOperationsTest {

    private SoundAssociationOperations operations;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private SoundAssociationStorage storage;
    @Mock private ApiScheduler apiScheduler;
    @Mock private ScModelManager modelManager;
    @Mock private Observer observer;
    @Mock private ApiResponse response;
    @Mock private TrackStorage trackStorage;
    @Mock private LegacyPlaylistOperations legacyPlaylistOperations;
    @Captor private ArgumentCaptor<PlayableUpdatedEvent> eventCaptor;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final Urn playlistUrn = Urn.forPlaylist(124L);
    private PublicApiTrack track;
    private PublicApiPlaylist playlist;
    private SoundAssociation trackLike;
    private SoundAssociation trackUnlike;
    private SoundAssociation trackRepost;
    private SoundAssociation trackUnpost;

    @Before
    public void setUp() throws Exception {
        operations = new SoundAssociationOperations(eventBus, storage, apiScheduler, modelManager,
                trackStorage, legacyPlaylistOperations);
        track = new PublicApiTrack(123L);
        playlist = new PublicApiPlaylist(124L);
        setupTestAssociations();
    }

    private void setupTestAssociations() {
        final PublicApiTrack liked = new PublicApiTrack(123L);
        liked.likes_count = 1;
        liked.user_like = true;
        trackLike = new SoundAssociation(liked);

        final PublicApiTrack unliked = new PublicApiTrack(123L);
        unliked.likes_count = 0;
        unliked.user_like = false;
        trackUnlike = new SoundAssociation(unliked);

        final PublicApiTrack reposted = new PublicApiTrack(123L);
        reposted.reposts_count = 1;
        reposted.user_repost = true;
        trackRepost = new SoundAssociation(reposted);

        final PublicApiTrack unposted = new PublicApiTrack(123L);
        unposted.reposts_count = 0;
        unposted.user_repost = false;
        trackUnpost = new SoundAssociation(unposted);
    }

    @Test
    public void shouldObtainIdsOfLikedTracksFromLocalStorage() {
        final List<Urn> idsListFromStorage = TestHelper.createTracksUrn(1L, 2L, 3L);
        when(storage.getLikesTrackUrnsAsync()).thenReturn(Observable.just(idsListFromStorage));

        operations.getLikedTracks().subscribe(observer);

        verify(observer).onNext(createTracksUrn(1L, 2L, 3L));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void likingTrackAddsLikeAndSendsPUTRequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/123"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.toggleLike(trackUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addLikeAsync(track);
    }

    @Test
    public void likingTrackPublishesPlayableChangedEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));

        operations.toggleLike(trackUrn, true).subscribe(observer);

        PlayableUpdatedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void likingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.addLikeAsync(any(Playable.class))).thenReturn(Observable.just(trackLike));
        when(storage.removeLikeAsync(any(Playable.class))).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(trackUrn, true).subscribe(observer);

        PropertySet changes = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_LIKED)).toBe(true);
        expect(changes.get(PlayableProperty.LIKES_COUNT)).toBe(1);

        PropertySet reverted = eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(reverted.get(PlayableProperty.IS_LIKED)).toBe(false);
        expect(reverted.get(PlayableProperty.LIKES_COUNT)).toBe(0);
    }

    @Test
    public void unlikingTrackRemovesLikeAndSendsDELETERequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(trackUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeLikeAsync(track);
        verify(apiScheduler).response(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/123")));
    }

    @Test
    public void unlikingTrackPublishesChangeEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(trackUrn, false).subscribe(observer);

        PlayableUpdatedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void unlikingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.addLikeAsync(any(Playable.class))).thenReturn(Observable.just(trackLike));
        when(storage.removeLikeAsync(any(Playable.class))).thenReturn(Observable.just(trackUnlike));

        operations.toggleLike(trackUrn, false).subscribe(observer);

        PropertySet reverted = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(reverted.get(PlayableProperty.IS_LIKED)).toBe(false);
        expect(reverted.get(PlayableProperty.LIKES_COUNT)).toBe(0);

        PropertySet changes = eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_LIKED)).toBe(true);
        expect(changes.get(PlayableProperty.LIKES_COUNT)).toBe(1);
    }

    @Test
    public void likingPlaylistAddsLikeAndSendsPUTRequestToApi() {
        final SoundAssociation like = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));

        when(storage.addLikeAsync(playlist)).thenReturn(Observable.just(like));

        operations.toggleLike(playlistUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addLikeAsync(playlist);
        verify(apiScheduler).response(argThat(isApiRequestTo("PUT", "/e1/me/playlist_likes/124")));
    }

    @Test
    public void unlikingPlaylistRemovesLikeAndSendsDELETERequestToApi() {
        final SoundAssociation unlike = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));
        when(storage.removeLikeAsync(playlist)).thenReturn(Observable.just(unlike));

        operations.toggleLike(playlistUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeLikeAsync(playlist);
        verify(apiScheduler).response(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_likes/124")));
    }

    @Test
    public void doesNotRevertUnlikeWhenApiRequestFailsBecauseSoundIsNotLiked() {
        final SoundAssociation revertUnlike = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isApiRequestTo("DELETE", "/e1/me/track_likes/123"))))
                .thenReturn(Observable.<ApiResponse>error(ApiRequestException.notFound(mock(ApiRequest.class))));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.just(trackUnlike));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(revertUnlike));

        operations.toggleLike(trackUrn, false).subscribe(observer);

        verify(storage).removeLikeAsync(track);
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toNumber(1);
        PropertySet changes = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_LIKED)).toBe(false);
    }

    @Test
    public void returnsPropertySetWithUpdatedLikeStatus() {
        track.user_like = true;
        track.likes_count = 12;
        SoundAssociation trackLike = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isApiRequestTo("PUT", "/e1/me/track_likes/123"))))
                .thenReturn(Observable.just(response));
        when(storage.addLikeAsync(track)).thenReturn(Observable.just(trackLike));
        when(storage.removeLikeAsync(track)).thenReturn(Observable.<SoundAssociation>never());

        Observable<PropertySet> result = operations.toggleLike(trackUrn, true);

        PropertySet changeSet = result.toBlocking().first();
        expect(changeSet.get(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(changeSet.get(PlayableProperty.LIKES_COUNT)).toBe(12);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REPOSTING / UN-REPOSTING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void repostingTrackAddsRepostAndSendsPUTRequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addRepostAsync(track);
        verify(apiScheduler).response(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/123")));
    }

    @Test
    public void repostingTrackPublishesPlayableChangedEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        PlayableUpdatedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void repostingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.addRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackRepost));
        when(storage.removeRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        PropertySet changes = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(true);
        expect(changes.get(PlayableProperty.REPOSTS_COUNT)).toBe(1);

        PropertySet reverted = eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(reverted.get(PlayableProperty.IS_REPOSTED)).toBe(false);
        expect(reverted.get(PlayableProperty.REPOSTS_COUNT)).toBe(0);
    }

    @Test
    public void unpostingTrackRemovesRepostAndSendsDELETERequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeRepostAsync(track);
        verify(apiScheduler).response(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/123")));
    }

    @Test
    public void unpostingTrackPublishesChangeEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        PlayableUpdatedEvent event = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED);
        expect(event.getUrn()).toEqual(track.getUrn());
        expect(event.getChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(event.getChangeSet().contains(PlayableProperty.REPOSTS_COUNT)).toBeTrue();
    }

    @Test
    public void unpostingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.removeRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackUnpost));
        when(storage.addRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        PropertySet reverted = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(reverted.get(PlayableProperty.IS_REPOSTED)).toBe(false);
        expect(reverted.get(PlayableProperty.REPOSTS_COUNT)).toBe(0);

        PropertySet changes = eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(true);
        expect(changes.get(PlayableProperty.REPOSTS_COUNT)).toBe(1);
    }

    @Test
    public void repostingPlaylistAddsRepostAndSendsPUTRequestToApi() {
        final SoundAssociation repost = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));

        when(storage.addRepostAsync(playlist)).thenReturn(Observable.just(repost));

        operations.toggleRepost(playlistUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addRepostAsync(playlist);
        verify(apiScheduler).response(argThat(isApiRequestTo("PUT", "/e1/me/playlist_reposts/124")));
    }

    @Test
    public void unpostingPlaylistRemovesRepostAndSendsDELETERequestToApi() {
        final SoundAssociation unpost = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));
        when(storage.removeRepostAsync(playlist)).thenReturn(Observable.just(unpost));

        operations.toggleRepost(playlistUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeRepostAsync(playlist);
        verify(apiScheduler).response(argThat(isApiRequestTo("DELETE", "/e1/me/playlist_reposts/124")));
    }

    @Test
    public void doesNotRevertUnpostWhenApiRequestFailsBecauseSoundIsNotReposted() {
        final SoundAssociation revertUnpost = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(ApiRequestException.notFound(mock(ApiRequest.class))));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(revertUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        verify(storage).removeRepostAsync(track);
        expect(eventBus.eventsOn(EventQueue.PLAYABLE_CHANGED)).toNumber(1);
        PropertySet changes = eventBus.firstEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(false);
    }

    @Test
    public void returnsPropertySetWithUpdatedRepostStatus() {
        track.user_repost = true;
        track.reposts_count = 12;
        SoundAssociation trackRepost = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.<SoundAssociation>never());

        Observable<PropertySet> result = operations.toggleRepost(trackUrn, true);

        PropertySet changeSet = result.toBlocking().first();
        expect(changeSet.get(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(changeSet.get(PlayableProperty.REPOSTS_COUNT)).toBe(12);
    }

}
