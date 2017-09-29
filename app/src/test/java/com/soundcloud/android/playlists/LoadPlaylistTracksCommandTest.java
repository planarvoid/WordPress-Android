package com.soundcloud.android.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.Track;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LoadPlaylistTracksCommandTest extends StorageIntegrationTest {

    private LoadPlaylistTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistTracksCommand(propeller());
    }

    @Test
    public void returnsPlaylistTracks() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrackWithPolicyHighTierMonetizable(apiPlaylist, 2);
        final List<Track> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).containsExactly(
                Track.from(apiTrack1),
                Track.from(apiTrack2),
                expectedHighTierMonetizableTrackFor(apiTrack3)
        );
    }

    @Test
    public void doesNotIncludeTracksFromOtherPlaylists() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrackOther = testFixtures().insertPlaylistTrack(testFixtures().insertPlaylist(), 0);

        final List<Track> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).doesNotContain(Track.from(apiTrackOther));
    }

    @Test
    public void doesNotIncludeTracksWithoutPolicies() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        propeller().delete(Tables.TrackPolicies.TABLE);

        final List<Track> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).isEmpty();
    }

    private Track expectedHighTierMonetizableTrackFor(ApiTrack track) {
        return Track.from(track).toBuilder().subMidTier(false).subHighTier(true).build();

    }
}
