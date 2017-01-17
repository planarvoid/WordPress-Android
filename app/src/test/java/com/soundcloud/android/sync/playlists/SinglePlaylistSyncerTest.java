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
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.collections.PropertySet;
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
import java.util.Date;

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
    @Mock private PlaylistStorage playlistStorage;
    private TestEventBus eventBus = new TestEventBus();

    private SinglePlaylistSyncer singlePlaylistSyncer;
    private ApiPlaylist updatedPlaylist;

    @Before
    public void setUp() throws Exception {
        updatedPlaylist = ModelFixtures.create(ApiPlaylist.class);
        singlePlaylistSyncer = new SinglePlaylistSyncer(fetchPlaylistWithTracks, removePlaylist, loadPlaylistTracks,
                                                        apiClient, storeTracksCommand,
                                                        storePlaylistCommand, replacePlaylistTracks, playlistStorage, eventBus);
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
        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), PropertySet.create());

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
        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), PropertySet.create());

        singlePlaylistSyncer.call();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED)).isEqualTo(forUpdate(singleton(from(apiPlaylistWithTracks.getPlaylist()))));
    }

    @Test
    public void syncsPlaylistWithNoChanges() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), PropertySet.create());
        withLocalTracks(cleanTrack(apiTrack));

        singlePlaylistSyncer.call();

        verifyNoChangesPushed();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(apiTrack);
        verifyPlaylistTrackStored(apiTrack.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalRemoval() throws Exception {
        syncPlaylistWithLocalRemovalCommon(PropertySet.create());
    }

    @Test
    public void syncsEditedPlaylistWithLocalRemoval() throws Exception {
        syncPlaylistWithLocalRemovalCommon(modifiedPlaylistFromStorage());
    }

    private void syncPlaylistWithLocalRemovalCommon(PropertySet playlistFromStorage) throws Exception {
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(localRemoval);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(removedTrack(localRemoval));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyNoTracksStored();
        verifyEmptyTrackListStored();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED)).isEqualTo(forUpdate(singleton(from(updatedPlaylist))));
    }

    @Test
    public void syncsPlaylistWithLocalAddition() throws Exception {
        syncsPlaylistWithLocalAdditionCommon(PropertySet.create());
    }

    @Test
    public void syncsEditedPlaylistWithLocalAddition() throws Exception {
        syncsPlaylistWithLocalAdditionCommon(modifiedPlaylistFromStorage());
    }

    private void syncsPlaylistWithLocalAdditionCommon(PropertySet playlistFromStorage) throws Exception {
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(ModelFixtures.apiPlaylistWithNoTracks());

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(addedTrack(localAddition));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage, localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyNoTracksStored();
        verifyPlaylistTrackStored(localAddition.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalAdditionAndRemoval() throws Exception {
        syncsPlaylistWithLocalAdditionAndRemovalCommon(PropertySet.create());
    }

    @Test
    public void syncsEditedPlaylistWithLocalAdditionAndRemoval() throws Exception {
        syncsPlaylistWithLocalAdditionAndRemovalCommon(modifiedPlaylistFromStorage());
    }

    @Test
    public void syncEditedPlaylistWithLocalRemovals() throws Exception {
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks();

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), PropertySet.create());
        withLocalTracks(removedTrack(localRemoval));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(), PropertySet.create());

        singlePlaylistSyncer.call();

        verifyPlaylistStored(updatedPlaylist);
        verifyTracksStored();
        verifyPlaylistTrackStored();
    }

    private void syncsPlaylistWithLocalAdditionAndRemovalCommon(PropertySet playlistFromStorage) throws Exception {
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange, localRemoval);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(cleanTrack(noChange), addedTrack(localAddition), removedTrack(localRemoval));
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
        PropertySet playlistFromStorage = PropertySet.create();
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange,
                                                                                       localRemoval,
                                                                                       remoteAddition);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(cleanTrack(noChange),
                        cleanTrack(remoteRemoval),
                        addedTrack(localAddition),
                        removedTrack(localRemoval));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(),
                   playlistFromStorage,
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
        PropertySet playlistFromStorage = modifiedPlaylistFromStorage();
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack remoteRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange,
                                                                                       localRemoval,
                                                                                       remoteAddition);

        withLocalPlaylist(apiPlaylistWithTracks.getPlaylist().getUrn(), playlistFromStorage);
        withLocalTracks(cleanTrack(noChange),
                        cleanTrack(remoteRemoval),
                        addedTrack(localAddition),
                        removedTrack(localRemoval));
        withPushes(apiPlaylistWithTracks.getPlaylist().getUrn(),
                   playlistFromStorage,
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
        verify(storeTracksCommand).call(Collections.<TrackRecord>emptyList());
    }

    private void withLocalPlaylist(Urn urn, PropertySet playlistFromStorage) {
        when(playlistStorage.loadPlaylistModifications(urn)).thenReturn(playlistFromStorage);
    }

    private void withLocalTracks(PropertySet... tracks) throws Exception {
        when(loadPlaylistTracks.call()).thenReturn(Arrays.asList(tracks));
    }

    private void withPushes(Urn playlistUrn, PropertySet playlistMetadata, Urn... trackList) throws Exception {
        final PlaylistApiUpdateObject expectedUpdateObject = PlaylistApiUpdateObject.create(playlistMetadata,
                                                                                            Arrays.asList(trackList));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("put", ApiEndpoints.PLAYLISTS_UPDATE.path(playlistUrn))
                                                                           .withContent(expectedUpdateObject)), any(Class.class)))
                .thenReturn(new ApiPlaylistWrapper(updatedPlaylist));
    }

    private PropertySet cleanTrack(ApiTrack apiTrack) {
        return PropertySet.from(
                PlaylistTrackProperty.TRACK_URN.bind(apiTrack.getUrn())
        );
    }

    private PropertySet removedTrack(ApiTrack apiTrack) throws Exception {
        return PropertySet.from(
                PlaylistTrackProperty.TRACK_URN.bind(apiTrack.getUrn()),
                PlaylistTrackProperty.REMOVED_AT.bind(new Date())
        );
    }

    private PropertySet addedTrack(ApiTrack apiTrack) throws Exception {
        return PropertySet.from(
                PlaylistTrackProperty.TRACK_URN.bind(apiTrack.getUrn()),
                PlaylistTrackProperty.ADDED_AT.bind(new Date())
        );
    }

    private PropertySet modifiedPlaylistFromStorage() {
        return PropertySet.from(PlaylistProperty.TITLE.bind("Playlist title"), PlaylistProperty.IS_PRIVATE.bind(true));
    }
}
