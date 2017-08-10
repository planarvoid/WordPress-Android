package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.Filter.filter;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;

public class LoadTracksWithStalePoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithStalePoliciesCommand command;

    @Before
    public void setup() {
        command = new LoadTracksWithStalePoliciesCommand(propeller());
    }

    @Test
    public void loadsLikeWithStalePolicyWhenFeatureEnabled() {
        testFixtures().insertLikesMarkedForOfflineSync();
        ApiTrack apiTrack = insertTrackAndUpdatePolicies();

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(apiTrack.getUrn());
    }

    @Test
    public void loadsLikeWithMissingPolicyWhenFeatureEnabled() {
        testFixtures().insertLikesMarkedForOfflineSync();
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        clearTrackPolicy(apiTrack);

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(apiTrack.getUrn());
    }

    @Test
    public void ignoresLikeWithUpToDatePolicy() {
        testFixtures().insertLikesMarkedForOfflineSync();
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicyTimestamp(apiTrack, new Date());

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void ignoresLikeWithRemovedAt() {
        testFixtures().insertLikesMarkedForOfflineSync();
        testFixtures().insertLikedTrackPendingRemoval(new Date(0), new Date(100));

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void ignoresOrphanLikes() {
        testFixtures().insertLikesMarkedForOfflineSync();
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        // insert a track like with the same ID as the playlist to test that we are joining on tracks only
        testFixtures().insertLike(apiPlaylist.getId(), Tables.Sounds.TYPE_TRACK, new Date(100));

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void doesNotLoadOfflineLikesWhenFeatureDisabled() {
        insertTrackAndUpdatePolicies();

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void doesNotLoadLikedPlaylistWhenNotMarkedAsAvailableOffline() {
        testFixtures().insertLikesMarkedForOfflineSync();
        testFixtures().insertLikedPlaylist(new Date(100));

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void loadOfflinePlaylistTracksWithStalePolicies() {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track0 = insertPlaylistTrackAndUpdatePolicies(playlist, 0);
        final ApiTrack track1 = insertPlaylistTrackAndUpdatePolicies(playlist, 1);

        Collection<Urn> tracksToStore = command.call(null);

        assertThat(tracksToStore).containsExactly(track0.getUrn(), track1.getUrn());
    }

    @Test
    public void loadOfflinePlaylistTracksAndLikedTracksWithStalePolicies() {
        testFixtures().insertLikesMarkedForOfflineSync();
        final ApiTrack like = insertTrackAndUpdatePolicies();
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track0 = insertPlaylistTrackAndUpdatePolicies(playlist, 0);
        final ApiTrack track1 = insertPlaylistTrackAndUpdatePolicies(playlist, 1);

        Collection<Urn> tracksToStore = command.call(null);

        assertThat(tracksToStore).contains(like.getUrn(), track0.getUrn(), track1.getUrn());
    }

    private void clearTrackPolicy(ApiTrack apiTrack) {
        propeller().delete(Tables.TrackPolicies.TABLE, filter()
                .whereEq(Tables.TrackPolicies.TRACK_ID, apiTrack.getId()));
    }

    private ApiTrack insertTrackAndUpdatePolicies() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicyTimestamp(apiTrack, new Date(200));
        return apiTrack;
    }

    private ApiTrack insertPlaylistTrackAndUpdatePolicies(ApiPlaylist playlist, int position) {
        final ApiTrack track0 = testFixtures().insertPlaylistTrack(playlist, position);
        updatePolicyTimestamp(track0, new Date(200));
        return track0;
    }

    private void updatePolicyTimestamp(ApiTrack track, Date date) {
        database().execSQL("UPDATE TrackPolicies SET last_updated = " + date.getTime() + " where track_id=" + track.getId());
    }

}
