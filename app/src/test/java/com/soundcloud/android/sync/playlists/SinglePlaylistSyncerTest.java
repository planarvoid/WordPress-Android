package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.events.PlaylistEntityChangedEvent.forUpdate;
import static com.soundcloud.android.playlists.Playlist.from;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.sync.EntitySyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class SinglePlaylistSyncerTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forPlaylist(123L);

    @Mock private LoadPlaylistTracksWithChangesCommand loadPlaylistTracks;
    @Mock private FetchPlaylistWithTracksCommand fetchPlaylistWithTracks;
    @Mock private StorePlaylistsCommand storePlaylistCommand;
    @Mock private RemovePlaylistCommand removePlaylist;
    @Mock private ReplacePlaylistTracksCommand replacePlaylistTracks;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private WriteResult writeResult;
    @Mock private ApiRequest apiRequest;
    @Mock private ApiClient apiClient;
    @Mock private EntitySyncStateStorage entitySyncStateStorage;
    @Mock private PlaylistStorage playlistStorage;
    private TestEventBus eventBus = new TestEventBus();

    private SinglePlaylistSyncer singlePlaylistSyncer;
    private ApiPlaylist updatedPlaylist;


    @Before
    public void setUp() throws Exception {
        updatedPlaylist = ModelFixtures.create(ApiPlaylist.class);
        singlePlaylistSyncer = new SinglePlaylistSyncer(fetchPlaylistWithTracks, removePlaylist, loadPlaylistTracks,
                                                        apiClient, storeTracksCommand,
                                                        storePlaylistCommand, replacePlaylistTracks, playlistStorage, eventBus,
                                                        entitySyncStateStorage);
    }

    @Test
    public void removesNotFoundPlaylist() throws Exception {
        doThrow(ApiRequestException.notFound(apiRequest, null)).when(fetchPlaylistWithTracks).call();

        singlePlaylistSyncer.call();

        verify(storePlaylistCommand, never()).call(any(Iterable.class));
        verify(removePlaylist).call(or(isNull(), any(Urn.class)));
    }

    @Test
    public void removesInaccessiblePlaylist() throws Exception {
        doThrow(ApiRequestException.notAllowed(apiRequest, null)).when(fetchPlaylistWithTracks).call();

        singlePlaylistSyncer.call();

        verify(storePlaylistCommand, never()).call(any(Iterable.class));
        verify(removePlaylist).call(or(isNull(), any(Urn.class)));
    }

    @Test
    public void syncsPlaylistThatDoesNotExistLocally() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);
        withoutLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn());

        singlePlaylistSyncer.call();

        verifyNoChangesPushed();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(apiTrack);
        verifyPlaylistTrackStored(apiTrack.getUrn());
    }

    @Test
    public void sendsPlaylistChangedEventAfterSyncingPlaylistThatDoesNotExistLocally() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);
        withoutLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn());

        singlePlaylistSyncer.call();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED)).isEqualTo(forUpdate(singleton(from(apiPlaylistWithTracks.getPlaylist()))));
    }

    @Test
    public void syncsPlaylistWithNoChanges() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);

        withoutLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn());
        withLocalTracks(cleanTrack(apiTrack.getUrn()));

        singlePlaylistSyncer.call();

        verifyNoChangesPushed();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(apiTrack);
        verifyPlaylistTrackStored(apiTrack.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalRemoval() throws Exception {
        syncPlaylistWithLocalRemovalCommon(Optional.absent());
    }

    @Test
    public void syncsEditedPlaylistWithLocalRemoval() throws Exception {
        syncPlaylistWithLocalRemovalCommon(Optional.of(modifiedPlaylistFromStorage()));
    }

    private void syncPlaylistWithLocalRemovalCommon(Optional<LocalPlaylistChange> playlistFromStorage) throws Exception {
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(localRemoval);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(removedTrack(localRemoval.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyNoTracksStored();
        verifyEmptyTrackListStored();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED)).isEqualTo(forUpdate(singleton(from(updatedPlaylist))));
    }

    @Test
    public void syncsPlaylistWithLocalAddition() throws Exception {
        syncsPlaylistWithLocalAdditionCommon(Optional.absent());
    }

    @Test
    public void syncsEditedPlaylistWithLocalAddition() throws Exception {
        syncsPlaylistWithLocalAdditionCommon(Optional.of(modifiedPlaylistFromStorage()));
    }

    private void syncsPlaylistWithLocalAdditionCommon(Optional<LocalPlaylistChange> playlistFromStorage) throws Exception {
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(ModelFixtures.apiPlaylistWithNoTracks());

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(addedTrack(localAddition.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage, localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyNoTracksStored();
        verifyPlaylistTrackStored(localAddition.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalAdditionAndRemoval() throws Exception {
        syncsPlaylistWithLocalAdditionAndRemovalCommon(Optional.absent());
    }

    @Test
    public void syncsEditedPlaylistWithLocalAdditionAndRemoval() throws Exception {
        syncsPlaylistWithLocalAdditionAndRemovalCommon(Optional.of(modifiedPlaylistFromStorage()));
    }

    @Test
    public void syncEditedPlaylistWithLocalRemovals() throws Exception {
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks();

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), Optional.absent());
        withLocalTracks(removedTrack(localRemoval.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), Optional.absent());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyTracksStored();
        verifyPlaylistTrackStored();
    }

    private void syncsPlaylistWithLocalAdditionAndRemovalCommon(Optional<LocalPlaylistChange> playlistFromStorage) throws Exception {
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange, localRemoval);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(cleanTrack(noChange.getUrn()), addedTrack(localAddition.getUrn()), removedTrack(localRemoval.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(),
                   playlistFromStorage,
                   noChange.getUrn(),
                   localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyTracksStored(noChange);
        verifyPlaylistTrackStored(noChange.getUrn(), localAddition.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalAndRemoteAdditionsAndRemovals() throws Exception {
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange,
                                                                                       localRemoval,
                                                                                       remoteAddition);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), Optional.absent());
        withLocalTracks(cleanTrack(noChange.getUrn()),
                        cleanTrack(remoteRemoval.getUrn()),
                        addedTrack(localAddition.getUrn()),
                        removedTrack(localRemoval.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(),
                   Optional.absent(),
                   noChange.getUrn(),
                   remoteAddition.getUrn(),
                   localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyTracksStored(noChange, remoteAddition);
        verifyPlaylistTrackStored(noChange.getUrn(), remoteAddition.getUrn(), localAddition.getUrn());
    }

    @Test
    public void syncsEditedPlaylistWithLocalAndRemoteAdditionsAndRemovals() throws Exception {
        LocalPlaylistChange playlistFromStorage = modifiedPlaylistFromStorage();
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange,
                                                                                       localRemoval,
                                                                                       remoteAddition);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), Optional.of(playlistFromStorage));
        withLocalTracks(cleanTrack(noChange.getUrn()),
                        cleanTrack(remoteRemoval.getUrn()),
                        addedTrack(localAddition.getUrn()),
                        removedTrack(localRemoval.getUrn()));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(),
                   Optional.of(playlistFromStorage),
                   noChange.getUrn(),
                   localAddition.getUrn(),
                   remoteAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyTracksStored(noChange, remoteAddition);
        verifyPlaylistTrackStored(noChange.getUrn(), localAddition.getUrn(), remoteAddition.getUrn());
    }

    private ApiPlaylistWithTracks setupApiPlaylistWithTracks(ApiTrack... apiTrack) throws Exception {
        return setupApiPlaylistWithTracks(ModelFixtures.apiPlaylistWithTracks(Arrays.asList(apiTrack)));
    }

    private ApiPlaylistWithTracks setupApiPlaylistWithTracks(ApiPlaylistWithTracks apiPlaylistWithTracks) throws Exception {
        when(fetchPlaylistWithTracks.call()).thenReturn(apiPlaylistWithTracks);
        return apiPlaylistWithTracks;
    }

    private void verifyPlaylistStored(PlaylistRecord playlistRecord) throws PropellerWriteException {
        verify(storePlaylistCommand).call(singleton(playlistRecord));
        verify(entitySyncStateStorage).synced(playlistRecord.getUrn());
    }

    private void verifyPlaylistTrackStored(Urn... urns) throws PropellerWriteException {
        verify(replacePlaylistTracks).call();
        assertThat(replacePlaylistTracks.getInput()).containsExactly(urns);
    }

    private void verifyTracksStored(ApiTrack... apiTrack) throws PropellerWriteException {
        verify(storeTracksCommand).call(Arrays.asList(apiTrack));
    }

    private void verifyNoChangesPushed() throws ApiRequestException, IOException, ApiMapperException {
        verify(apiClient, never()).fetchMappedResponse(any(ApiRequest.class), any(TypeToken.class));
        verify(apiClient, never()).fetchMappedResponse(any(ApiRequest.class), any(Class.class));
    }


    private void verifyEmptyTrackListStored() throws PropellerWriteException {
        verify(replacePlaylistTracks).call();
        assertThat(replacePlaylistTracks.getInput()).isEmpty();
    }

    private void verifyNoTracksStored() throws PropellerWriteException {
        verify(storeTracksCommand).call(Collections.emptyList());
    }

    private void withLocalPlaylist(Urn urn, Optional<LocalPlaylistChange> playlistFromStorage) {
        when(playlistStorage.loadPlaylistModifications(urn)).thenReturn(playlistFromStorage);
    }

    private void withoutLocalPlaylist(Urn urn) {
        when(playlistStorage.loadPlaylistModifications(urn)).thenReturn(Optional.absent());
    }

    private void withLocalTracks(PlaylistTrackChange... tracks) throws Exception {
        when(loadPlaylistTracks.call()).thenReturn(Arrays.asList(tracks));
    }

    private void withPushes(Urn playlistUrn, Optional<LocalPlaylistChange> playlistMetadata, Urn... trackList) throws Exception {
        final PlaylistApiUpdateObject expectedUpdateObject = PlaylistApiUpdateObject.create(playlistMetadata,
                                                                                            Arrays.asList(trackList));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("put", ApiEndpoints.PLAYLISTS_UPDATE.path(playlistUrn))
                                                                           .withContent(expectedUpdateObject)), any(Class.class)))
                .thenReturn(new ApiPlaylistWrapper(updatedPlaylist));
    }

    private PlaylistTrackChange cleanTrack(Urn urn) {
        return PlaylistTrackChange.createEmpty(urn);
    }

    private PlaylistTrackChange removedTrack(Urn urn) throws Exception {
        return PlaylistTrackChange.createRemoved(urn);
    }

    private PlaylistTrackChange addedTrack(Urn urn) throws Exception {
        return PlaylistTrackChange.createAdded(urn);
    }

    private LocalPlaylistChange modifiedPlaylistFromStorage() {
        return LocalPlaylistChange.create(Urn.NOT_SET, "Playlist title", true);
    }
}
