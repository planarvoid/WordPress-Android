package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class AddTrackToPlaylistCommandTest extends StorageIntegrationTest {

    private static final Date ADDED_AT = new Date();
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private AddTrackToPlaylistCommand command;

    @Mock private DateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        command = new AddTrackToPlaylistCommand(propeller(), dateProvider);
        when(dateProvider.getDate()).thenReturn(ADDED_AT);
    }

    @Test
    public void addsTrackToAPlaylistReturnsUpdatedTrackCount() {
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        final Integer updatedCount = command.call(new AddTrackToPlaylistCommand.AddTrackToPlaylistParams(apiPlaylist.getUrn(), TRACK_URN));

        expect(updatedCount).toEqual(apiPlaylist.getTrackCount() + 1);
    }

    @Test
    public void addsTrackToPlaylistWritesTrackToPlaylistTracksTable() {
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        command.call(new AddTrackToPlaylistCommand.AddTrackToPlaylistParams(apiPlaylist.getUrn(), TRACK_URN));

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), Arrays.asList(TRACK_URN));
    }
}