package com.soundcloud.android.offline;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackPolicyStorage;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;

public class LoadTracksWithStalePoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithStalePoliciesCommand command;

    @Before
    public void setup() {
        command = new LoadTracksWithStalePoliciesCommand(propeller(),
                                                         new LikesStorage(propellerRxV2()),
                                                         new TrackPolicyStorage(propellerRxV2()),
                                                         new LoadOfflinePlaylistsCommand(propeller()),
                                                         new LoadPlaylistTrackUrnsCommand(propeller()));
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
        testFixtures().clearTrackPolicy(apiTrack);

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(apiTrack.getUrn());
    }

    @Test
    public void ignoresLikeWithUpToDatePolicy() {
        testFixtures().insertLikesMarkedForOfflineSync();
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().updatePolicyTimestamp(apiTrack, new Date());

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

    private ApiTrack insertTrackAndUpdatePolicies() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().updatePolicyTimestamp(apiTrack, new Date(200));
        return apiTrack;
    }

    private ApiTrack insertPlaylistTrackAndUpdatePolicies(ApiPlaylist playlist, int position) {
        final ApiTrack track0 = testFixtures().insertPlaylistTrack(playlist, position);
        testFixtures().updatePolicyTimestamp(track0, new Date(200));
        return track0;
    }

}
