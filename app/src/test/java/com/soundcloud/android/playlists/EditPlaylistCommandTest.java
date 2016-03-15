package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class EditPlaylistCommandTest extends StorageIntegrationTest {

    public static final String NEW_TITLE = "new title";

    @Mock CurrentDateProvider dateProvider;

    private EditPlaylistCommand command;

    @Before
    public void setUp() throws Exception {
        when(dateProvider.getCurrentTime()).thenReturn(123L);
        command = new EditPlaylistCommand(propeller(), dateProvider);
    }

    @Test
    public void doesNotModifyTracksIfNoPlaylistFound() {
        final Urn urn = Urn.forPlaylist(1);

        assertThat(command.call(getInput(urn, Arrays.asList(Urn.forTrack(1), Urn.forTrack(2))))).isEqualTo(0);

        databaseAssertions().assertPlaylistTracksNotStored(urn);

    }

    @Test
    public void reordersTracks() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 1);
        final List<Urn> newTrackList = Arrays.asList(apiTrack2.getUrn(), apiTrack1.getUrn());

        assertThat(command.call(getInput(apiPlaylist.getUrn(), newTrackList))).isEqualTo(2);

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), newTrackList);
    }

    @Test
    public void reordersTracksAndRemovesOne() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 2);
        final List<Urn> newTrackList = Arrays.asList(apiTrack3.getUrn(), apiTrack2.getUrn());

        assertThat(command.call(getInput(apiPlaylist.getUrn(), newTrackList))).isEqualTo(2);

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), newTrackList);
        databaseAssertions().assertPlaylistTrackForRemoval(apiPlaylist.getUrn(), apiTrack1.getUrn());
    }

    @Test
    public void updatesMetadata() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        assertThat(command.call(getInput(apiPlaylist.getUrn(), Collections.<Urn>emptyList()))).isEqualTo(0);

        apiPlaylist.setTitle(NEW_TITLE);
        apiPlaylist.setSharing(Sharing.PRIVATE);

        databaseAssertions().assertPlaylistInserted(apiPlaylist);
    }

    @Test
    public void updatesMetadataTracklistAndRemovals() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 2);
        final List<Urn> newTrackList = Arrays.asList(apiTrack3.getUrn(), apiTrack2.getUrn());

        assertThat(command.call(getInput(apiPlaylist.getUrn(), newTrackList))).isEqualTo(2);

        apiPlaylist.setTitle(NEW_TITLE);
        apiPlaylist.setSharing(Sharing.PRIVATE);

        databaseAssertions().assertPlaylistInserted(apiPlaylist);
        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), newTrackList);
        databaseAssertions().assertPlaylistTrackForRemoval(apiPlaylist.getUrn(), apiTrack1.getUrn());
    }

    @Test
    public void addedTrackStillMarkedAfterReordering() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist.getUrn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrackPendingAddition(apiPlaylist, 1, new Date());
        final List<Urn> newTrackList = Arrays.asList(apiTrack2.getUrn(), apiTrack1.getUrn());

        assertThat(command.call(getInput(apiPlaylist.getUrn(), newTrackList))).isEqualTo(2);

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), newTrackList);
        databaseAssertions().assertPlaylistTrackForAddition(apiPlaylist.getUrn(), apiTrack2.getUrn());
    }

    @NonNull
    private EditPlaylistCommandParams getInput(Urn urn, List<Urn> trackList) {
        return new EditPlaylistCommandParams(urn, NEW_TITLE, true, trackList);
    }
}
