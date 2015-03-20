package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadTracksWithStalePoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithStalePoliciesCommand command;

    @Before
    public void setup() {
        command = new LoadTracksWithStalePoliciesCommand(propeller());
    }

    @Test
    public void loadsLikeWithStalePolicyWhenFeatureEnabled() throws Exception {
        ApiTrack apiTrack = insertTrackAndUpdatePolicies();

        Collection<Urn> trackLikes = command.with(true).call();

        expect(trackLikes).toContainExactly(apiTrack.getUrn());
    }

    @Test
    public void loadsLikeWithMissingPolicyWhenFeatureEnabled() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        propeller().delete(Table.TrackPolicies, filter().whereEq(TableColumns.TrackPolicies.TRACK_ID, apiTrack.getId()));

        Collection<Urn> trackLikes = command.with(true).call();

        expect(trackLikes).toContainExactly(apiTrack.getUrn());
    }

    @Test
    public void ignoresLikeWithUpToDatePolicy() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicyTimestamp(apiTrack, new Date());

        Collection<Urn> trackLikes = command.with(true).call();

        expect(trackLikes).toBeEmpty();
    }

    @Test
    public void ignoresLikeWithRemovedAt() throws Exception {
        testFixtures().insertLikedTrackPendingRemoval(new Date(100));

        Collection<Urn> trackLikes = command.with(true).call();

        expect(trackLikes).toBeEmpty();
    }

    @Test
    public void doesNotLoadOfflineLikesWhenFeatureDisabled() throws Exception {
        insertTrackAndUpdatePolicies();

        Collection<Urn> trackLikes = command.with(false).call();

        expect(trackLikes).toBeEmpty();
    }

    @Test
    public void doesNotLoadLikedPlaylistWhenNotMarkedAsAvailableOffline() throws Exception {
        testFixtures().insertLikedPlaylist(new Date(100));

        Collection<Urn> trackLikes = command.with(true).call();

        expect(trackLikes).toBeEmpty();
    }

    @Test
    public void loadOfflinePlaylistTracksWithStalePolicies() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track0 = insertPlaylistTrackAndUpdatePolicies(playlist, 0);
        final ApiTrack track1 = insertPlaylistTrackAndUpdatePolicies(playlist, 1);

        Collection<Urn> tracksToStore = command.with(false).call();

        expect(tracksToStore).toContainExactly(track0.getUrn(), track1.getUrn());
    }

    @Test
    public void loadOfflinePlaylistTracksAndLikedTracksWithStalePolicies() throws Exception {
        final ApiTrack like = insertTrackAndUpdatePolicies();
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track0 = insertPlaylistTrackAndUpdatePolicies(playlist, 0);
        final ApiTrack track1 = insertPlaylistTrackAndUpdatePolicies(playlist, 1);

        Collection<Urn> tracksToStore = command.with(true).call();

        expect(tracksToStore).toContainExactlyInAnyOrder(like.getUrn(), track0.getUrn(), track1.getUrn());
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
