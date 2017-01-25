package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.Track;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadPlaylistTracksCommandTest extends StorageIntegrationTest {

    private static final Date ADDED_AT = new Date();
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

    @Test
    public void returnsPlaylistTracksWithOfflineInfo() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertCompletedTrackDownload(apiTrack1.getUrn(), 0, 100L);
        testFixtures().insertTrackPendingDownload(apiTrack2.getUrn(), 200L);
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack3.getUrn(), 300L);

        final List<Track> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).contains(
                fromApiTrack(apiTrack1, OfflineState.DOWNLOADED),
                fromApiTrack(apiTrack2, OfflineState.REQUESTED),
                fromApiTrack(apiTrack3, OfflineState.NOT_OFFLINE)
        );
    }


    @Test
    public void loadUnavailableOfflineStateForPlaylistTracksWhenPlaylistMarkedForOffline() {
        final ApiPlaylist offlinePlaylist = insertPostedPlaylist();
        testFixtures().insertPlaylistMarkedForOfflineSync(offlinePlaylist);
        ApiTrack track = testFixtures().insertPlaylistTrack(offlinePlaylist.getUrn(), 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), new Date().getTime());

        List<Track> result = command.call(offlinePlaylist.getUrn());

        assertThat(result.get(0).offlineState()).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void loadUnavailableOfflineStateForPlaylistTracksWhenPlaylistNotMarkedForOffline() {
        final ApiPlaylist normalPlaylist = insertPostedPlaylist();
        ApiTrack track = testFixtures().insertPlaylistTrack(normalPlaylist.getUrn(), 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), new Date().getTime());

        List<Track> result = command.call(normalPlaylist.getUrn());
        assertThat(result.get(0).offlineState()).isEqualTo(OfflineState.UNAVAILABLE);
    }

    private Track expectedHighTierMonetizableTrackFor(ApiTrack track) {
        return Track.builder(Track.from(track)).subMidTier(false).subHighTier(true).build();

    }

    private Track fromApiTrack(ApiTrack apiTrack, OfflineState offlineState) {
        return Track.builder(Track.from(apiTrack)).offlineState(offlineState).build();
    }

    private ApiPlaylist insertPostedPlaylist() {
        return testFixtures().insertPostedPlaylist(ADDED_AT);
    }

}
