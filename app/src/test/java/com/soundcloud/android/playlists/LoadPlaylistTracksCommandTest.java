package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
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


        final List<TrackItem> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).containsExactly(
                fromApiTrack(apiTrack1),
                fromApiTrack(apiTrack2),
                expectedHighTierMonetizableTrackFor(apiTrack3)
        );
    }

    @Test
    public void doesNotIncludeTracksFromOtherPlaylists() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrackOther = testFixtures().insertPlaylistTrack(testFixtures().insertPlaylist(), 0);

        final List<TrackItem> tracks = command.call(apiPlaylist.getUrn());

        assertThat(tracks).doesNotContain(fromApiTrack(apiTrackOther));
    }

    @Test
    public void doesNotIncludeTracksWithoutPolicies() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        propeller().delete(Tables.TrackPolicies.TABLE);

        final List<TrackItem> tracks = command.call(apiPlaylist.getUrn());

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

        final List<TrackItem> tracks = command.call(apiPlaylist.getUrn());

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

        List<TrackItem> result = command.call(offlinePlaylist.getUrn());

        assertThat(result.get(0).getOfflineState()).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void loadUnavailableOfflineStateForPlaylistTracksWhenPlaylistNotMarkedForOffline() {
        final ApiPlaylist normalPlaylist = insertPostedPlaylist();
        ApiTrack track = testFixtures().insertPlaylistTrack(normalPlaylist.getUrn(), 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), new Date().getTime());

        List<TrackItem> result = command.call(normalPlaylist.getUrn());
        assertThat(result.get(0).getOfflineState()).isEqualTo(OfflineState.UNAVAILABLE);
    }

    private TrackItem expectedHighTierMonetizableTrackFor(ApiTrack track) {
        return ModelFixtures.highTierMonetizableTrack(track);

    }

    private TrackItem fromApiTrack(ApiTrack apiTrack) {
        return ModelFixtures.trackItem(apiTrack);
    }

    private PropertySet properties(ApiTrack apiTrack) {
        return PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
                TrackProperty.TITLE.bind(apiTrack.getTitle()),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(apiTrack.getImageUrlTemplate()),
                TrackProperty.SNIPPET_DURATION.bind(apiTrack.getSnippetDuration()),
                TrackProperty.FULL_DURATION.bind(apiTrack.getFullDuration()),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                TrackProperty.IS_PRIVATE.bind(apiTrack.isPrivate()),
                TrackProperty.CREATOR_NAME.bind(apiTrack.getUserName()),
                TrackProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn()),
                TrackProperty.BLOCKED.bind(apiTrack.isBlocked()),
                TrackProperty.SNIPPED.bind(apiTrack.isSnipped()),
                TrackProperty.SUB_MID_TIER.bind(apiTrack.isSubMidTier().get()),
                TrackProperty.SUB_HIGH_TIER.bind(apiTrack.isSubHighTier().get()),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE));
    }


    private TrackItem fromApiTrack(ApiTrack apiTrack, OfflineState offlineState) {
        return ModelFixtures.trackItemWithOfflineState(apiTrack, offlineState);
    }

    private ApiPlaylist insertPostedPlaylist() {
        return testFixtures().insertPostedPlaylist(ADDED_AT);
    }

}
