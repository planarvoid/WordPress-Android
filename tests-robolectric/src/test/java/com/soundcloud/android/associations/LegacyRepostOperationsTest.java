package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LegacyPlaylistOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.testsupport.fixtures.TestApiResponses;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class LegacyRepostOperationsTest {

    private LegacyRepostOperations operations;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private SoundAssociationStorage storage;
    @Mock private ApiScheduler apiScheduler;
    @Mock private ScModelManager modelManager;
    @Mock private Observer observer;
    @Mock private ApiResponse response;
    @Mock private TrackStorage trackStorage;
    @Mock private LegacyPlaylistOperations legacyPlaylistOperations;
    @Captor private ArgumentCaptor<EntityStateChangedEvent> eventCaptor;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final Urn playlistUrn = Urn.forPlaylist(124L);
    private PublicApiTrack track;
    private PublicApiPlaylist playlist;
    private SoundAssociation trackRepost;
    private SoundAssociation trackUnpost;

    @Before
    public void setUp() throws Exception {
        operations = new LegacyRepostOperations(eventBus, storage, apiScheduler, modelManager,
                trackStorage, legacyPlaylistOperations);
        track = new PublicApiTrack(123L);
        playlist = new PublicApiPlaylist(124L);
        setupTestAssociations();
    }

    private void setupTestAssociations() {
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
    public void repostingTrackAddsRepostAndSendsPUTRequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addRepostAsync(track);
        verify(apiScheduler).response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123")));
    }

    @Test
    public void repostingTrackPublishesPlayableChangedEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        EntityStateChangedEvent event = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getNextUrn()).toEqual(track.getUrn());
        expect(event.getNextChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
    }

    @Test
    public void repostingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.addRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackRepost));
        when(storage.removeRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, true).subscribe(observer);

        PropertySet changes = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(true);

        PropertySet reverted = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet();
        expect(reverted.get(PlayableProperty.IS_REPOSTED)).toBe(false);
    }

    @Test
    public void unpostingTrackRemovesRepostAndSendsDELETERequestToApi() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) track, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeRepostAsync(track);
        verify(apiScheduler).response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123")));
    }

    @Test
    public void unpostingTrackPublishesChangeEvent() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        EntityStateChangedEvent event = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getNextUrn()).toEqual(track.getUrn());
        expect(event.getNextChangeSet().contains(PlayableProperty.IS_REPOSTED)).toBeTrue();
    }

    @Test
    public void unpostingTrackPublishesRevertedPlayableChangeEventWhenApiRequestFails() {
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>error(new Exception()));
        when(storage.removeRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackUnpost));
        when(storage.addRepostAsync(any(Playable.class))).thenReturn(Observable.just(trackRepost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        PropertySet reverted = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet();
        expect(reverted.get(PlayableProperty.IS_REPOSTED)).toBe(false);

        PropertySet changes = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(true);
    }

    @Test
    public void repostingPlaylistAddsRepostAndSendsPUTRequestToApi() {
        final SoundAssociation repost = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));

        when(storage.addRepostAsync(playlist)).thenReturn(Observable.just(repost));

        operations.toggleRepost(playlistUrn, true).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).addRepostAsync(playlist);
        verify(apiScheduler).response(argThat(isPublicApiRequestTo("PUT", "/e1/me/playlist_reposts/124")));
    }

    @Test
    public void unpostingPlaylistRemovesRepostAndSendsDELETERequestToApi() {
        final SoundAssociation unpost = new SoundAssociation(playlist);
        when(legacyPlaylistOperations.loadPlaylist(playlistUrn)).thenReturn(Observable.just(playlist));
        when(storage.removeRepostAsync(playlist)).thenReturn(Observable.just(unpost));

        operations.toggleRepost(playlistUrn, false).subscribe(observer);

        verify(modelManager).cache((Playable) playlist, PublicApiResource.CacheUpdateMode.NONE);
        verify(storage).removeRepostAsync(playlist);
        verify(apiScheduler).response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/playlist_reposts/124")));
    }

    @Test
    public void doesNotRevertUnpostWhenApiRequestFailsBecauseSoundIsNotReposted() {
        final SoundAssociation revertUnpost = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(TestApiResponses.status(404).getFailure()));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.just(trackUnpost));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(revertUnpost));

        operations.toggleRepost(trackUrn, false).subscribe(observer);

        verify(storage).removeRepostAsync(track);
        expect(eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED)).toNumber(1);
        PropertySet changes = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet();
        expect(changes.get(PlayableProperty.IS_REPOSTED)).toBe(false);
    }

    @Test
    public void returnsPropertySetWithUpdatedRepostStatus() {
        track.user_repost = true;
        track.reposts_count = 12;
        SoundAssociation trackRepost = new SoundAssociation(track);
        when(trackStorage.getTrackAsync(eq(123L))).thenReturn(Observable.just(track));
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(response));
        when(storage.addRepostAsync(track)).thenReturn(Observable.just(trackRepost));
        when(storage.removeRepostAsync(track)).thenReturn(Observable.<SoundAssociation>never());

        Observable<PropertySet> result = operations.toggleRepost(trackUrn, true);

        PropertySet changeSet = result.toBlocking().first();
        expect(changeSet.get(PlayableProperty.IS_REPOSTED)).toBeTrue();
        expect(changeSet.get(PlayableProperty.REPOSTS_COUNT)).toBe(12);
    }

}
