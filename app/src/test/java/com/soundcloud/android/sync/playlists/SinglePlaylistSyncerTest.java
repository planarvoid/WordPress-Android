package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistTrackProperty;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class SinglePlaylistSyncerTest extends AndroidUnitTest {

    @Mock private LoadPlaylistTracksWithChangesCommand loadPlaylistTracks;
    @Mock private PushPlaylistAdditionsCommand pushPlaylistAdditions;
    @Mock private PushPlaylistRemovalsCommand pushPlaylistRemovals;
    @Mock private FetchPlaylistWithTracksCommand fetchPlaylistWithTracks;
    @Mock private StorePlaylistCommand storePlaylistCommand;
    @Mock private RemovePlaylistCommand removePlaylist;
    @Mock private ReplacePlaylistTracksCommand replacePlaylistTracks;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private WriteResult writeResult;
    @Mock private ApiRequest apiRequest;

    private SinglePlaylistSyncer singlePlaylistSyncer;

    @Before
    public void setUp() throws Exception {
        singlePlaylistSyncer = new SinglePlaylistSyncer(fetchPlaylistWithTracks, removePlaylist, loadPlaylistTracks, pushPlaylistAdditions,
                pushPlaylistRemovals, storeTracksCommand,
                storePlaylistCommand, replacePlaylistTracks);

        when(storePlaylistCommand.call()).thenReturn(writeResult);
    }

    @Test
    public void removesNotFoundPlaylist() throws Exception {
        doThrow(ApiRequestException.notFound(apiRequest, null)).when(fetchPlaylistWithTracks).call();

        singlePlaylistSyncer.call();

        verify(storePlaylistCommand, never()).call();
        verify(removePlaylist).call(any(Urn.class));
    }

    @Test
    public void removesInaccessiblePlaylist() throws Exception {
        doThrow(ApiRequestException.notAllowed(apiRequest, null)).when(fetchPlaylistWithTracks).call();

        singlePlaylistSyncer.call();

        verify(storePlaylistCommand, never()).call();
        verify(removePlaylist).call(any(Urn.class));
    }

    @Test
    public void syncsPlaylistThatDoesNotExistLocally() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);

        singlePlaylistSyncer.call();

        verifyNoRemoteRemovals();
        verifyNoRemoteAdditions();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(apiTrack);
        verifyPlaylistTrackStored(apiTrack.getUrn());
    }

    @Test
    public void syncsPlaylistWithNoChanges() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(apiTrack);
        withLocalTracks(cleanTrack(apiTrack));

        singlePlaylistSyncer.call();

        verifyNoRemoteRemovals();
        verifyNoRemoteAdditions();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(apiTrack);
        verifyPlaylistTrackStored(apiTrack.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalRemoval() throws Exception {
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(localRemoval);
        withLocalTracks(removedTrack(localRemoval));

        singlePlaylistSyncer.call();

        verifyRemoteRemoval(localRemoval.getUrn());
        verifyNoRemoteAdditions();
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyNoTracksStored();
        verifyEmptyTrackListStored();
    }

    @Test
    public void syncsPlaylistWithLocalAddition() throws Exception {
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(ModelFixtures.apiPlaylistWithNoTracks());
        withLocalTracks(addedTrack(localAddition));
        withSuccessfulAdditions(localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyNoRemoteRemovals();
        verifyRemoteAddition(localAddition.getUrn());
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyNoTracksStored();
        verifyPlaylistTrackStored(localAddition.getUrn());
    }

    @Test
    public void syncsPlaylistWithLocalAdditionAndRemoval() throws Exception {
        final ApiTrack noChange = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localAddition = ModelFixtures.create(ApiTrack.class);
        final ApiTrack localRemoval = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange, localRemoval);
        withLocalTracks(cleanTrack(noChange), addedTrack(localAddition), removedTrack(localRemoval));
        withSuccessfulAdditions(localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyRemoteRemoval(localRemoval.getUrn());
        verifyRemoteAddition(localAddition.getUrn());
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
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
        final ApiPlaylistWithTracks apiPlaylistWithTracks = setupApiPlaylistWithTracks(noChange, localRemoval, remoteAddition);
        withLocalTracks(cleanTrack(noChange), cleanTrack(remoteRemoval), addedTrack(localAddition), removedTrack(localRemoval));
        withSuccessfulAdditions(localAddition.getUrn());

        singlePlaylistSyncer.call();

        verifyRemoteRemoval(localRemoval.getUrn());
        verifyRemoteAddition(localAddition.getUrn());
        verifyPlaylistStored(apiPlaylistWithTracks.getPlaylist());
        verifyTracksStored(noChange, remoteAddition);
        verifyPlaylistTrackStored(noChange.getUrn(), remoteAddition.getUrn(), localAddition.getUrn());
    }

    private ApiPlaylistWithTracks setupApiPlaylistWithTracks(ApiTrack... apiTrack) throws Exception {
        return setupApiPlaylistWithTracks(ModelFixtures.apiPlaylistWithTracks(Arrays.asList(apiTrack)));
    }

    private ApiPlaylistWithTracks setupApiPlaylistWithTracks(ApiPlaylistWithTracks apiPlaylistWithTracks) throws Exception {
        when(fetchPlaylistWithTracks.call()).thenReturn(apiPlaylistWithTracks);
        return apiPlaylistWithTracks;
    }

    private void verifyPlaylistStored(ApiPlaylist apiPlaylist) throws PropellerWriteException {
        verify(storePlaylistCommand).call();
        assertThat(storePlaylistCommand.getInput()).isSameAs(apiPlaylist);
    }

    private void verifyPlaylistTrackStored(Urn... urns) throws PropellerWriteException {
        verify(replacePlaylistTracks).call();
        assertThat(replacePlaylistTracks.getInput()).containsExactly(urns);
    }

    private void verifyTracksStored(ApiTrack... apiTrack) throws PropellerWriteException {
        verify(storeTracksCommand).call(Arrays.asList(apiTrack));
    }

    private void verifyNoRemoteAdditions() {
        assertThat(pushPlaylistAdditions.getInput()).isEmpty();
    }

    private void verifyNoRemoteRemovals() {
        assertThat(pushPlaylistRemovals.getInput()).isEmpty();
    }

    private void verifyRemoteAddition(Urn... urns) {
        assertThat(pushPlaylistAdditions.getInput()).containsExactly(urns);
    }

    private void verifyRemoteRemoval(Urn... urns) {
        assertThat(pushPlaylistRemovals.getInput()).containsExactly(urns);
    }

    private void verifyEmptyTrackListStored() throws PropellerWriteException {
        verify(replacePlaylistTracks).call();
        assertThat(replacePlaylistTracks.getInput()).isEmpty();
    }

    private void verifyNoTracksStored() throws PropellerWriteException {
        verify(storeTracksCommand).call(Collections.<TrackRecord>emptyList());
    }

    private void withLocalTracks(PropertySet... tracks) throws Exception {
        when(loadPlaylistTracks.call()).thenReturn(Arrays.asList(tracks));
    }

    private void withSuccessfulAdditions(Urn... additions) throws Exception {
        when(pushPlaylistAdditions.call()).thenReturn(Arrays.asList(additions));
    }

    private PropertySet cleanTrack(ApiTrack apiTrack){
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
}