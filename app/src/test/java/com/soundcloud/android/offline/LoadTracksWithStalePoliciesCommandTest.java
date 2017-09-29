package com.soundcloud.android.offline;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackPolicyStorage;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class LoadTracksWithStalePoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithStalePoliciesCommand command;
    private final OfflineContentStorage offlineContentStorage = new OfflineContentStorage(sharedPreferences(), Schedulers.trampoline());

    @Before
    public void setup() {
        command = new LoadTracksWithStalePoliciesCommand(
                new LikesStorage(propellerRxV2()),
                                                         new TrackPolicyStorage(propellerRxV2()),
                                                         new LoadPlaylistTrackUrnsCommand(propeller()),
                                                         offlineContentStorage);
    }

    @Test
    public void loadsLikeWithStalePolicyWhenFeatureEnabled() {
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();

        ApiTrack apiTrack = insertTrackAndUpdatePolicies();

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(apiTrack.getUrn());
    }

    @Test
    public void loadsLikeWithMissingPolicyWhenFeatureEnabled() {
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().clearTrackPolicy(apiTrack);

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).containsExactly(apiTrack.getUrn());
    }

    @Test
    public void ignoresLikeWithUpToDatePolicy() {
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().updatePolicyTimestamp(apiTrack, new Date());

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void ignoresLikeWithRemovedAt() {
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
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
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
        testFixtures().insertLikedPlaylist(new Date(100));

        Collection<Urn> trackLikes = command.call(null);

        assertThat(trackLikes).isEmpty();
    }

    @Test
    public void loadOfflinePlaylistTracksWithStalePolicies() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        offlineContentStorage.storeAsOfflinePlaylists(Collections.singletonList(playlist.getUrn())).test().assertComplete();
        final ApiTrack track0 = insertPlaylistTrackAndUpdatePolicies(playlist, 0);
        final ApiTrack track1 = insertPlaylistTrackAndUpdatePolicies(playlist, 1);

        Collection<Urn> tracksToStore = command.call(null);

        assertThat(tracksToStore).containsExactly(track0.getUrn(), track1.getUrn());
    }

    @Test
    public void loadOfflinePlaylistTracksAndLikedTracksWithStalePolicies() {
        offlineContentStorage.addLikedTrackCollection().test().assertComplete();
        final ApiTrack like = insertTrackAndUpdatePolicies();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        offlineContentStorage.storeAsOfflinePlaylists(Collections.singletonList(playlist.getUrn())).test().assertComplete();
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
