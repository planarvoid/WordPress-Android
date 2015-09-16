package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;

public class AddTrackToPlaylistCommandTest extends StorageIntegrationTest {

    private static final Date ADDED_AT = new Date();
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private AddTrackToPlaylistCommand command;

    @Mock private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        command = new AddTrackToPlaylistCommand(propeller(), dateProvider);
        when(dateProvider.getDate()).thenReturn(ADDED_AT);
    }

    @Test
    public void addsTrackToAPlaylistReturnsUpdatedTrackCount() {
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        final Integer updatedCount = command.call(new AddTrackToPlaylistCommand.AddTrackToPlaylistParams(apiPlaylist.getUrn(), TRACK_URN));

        assertThat(updatedCount).isEqualTo(apiPlaylist.getTrackCount() + 1);
    }

    @Test
    public void addsTrackToPlaylistWritesTrackToPlaylistTracksTable() {
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        command.call(new AddTrackToPlaylistCommand.AddTrackToPlaylistParams(apiPlaylist.getUrn(), TRACK_URN));

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), Collections.singletonList(TRACK_URN));
    }
}