package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;

public class LoadTracksForPolicyUpdateCommandTest extends StorageIntegrationTest {

    private LoadTracksForPolicyUpdateCommand command;
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() {
        command = new LoadTracksForPolicyUpdateCommand(propeller(), featureFlags);
    }

    @Test
    public void shouldLoadAllTracks() {
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(true);
        final Urn track1 = testFixtures().insertTrack().getUrn();
        final Urn track2 = testFixtures().insertTrack().getUrn();

        final List<Urn> result = command.call(null);

        assertThat(result).containsExactly(track1, track2);
    }

    @Test
    public void beforeLaunchingSubsItLoadsLikedTracks() {
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(false);
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(track.getUrn());
    }

    @Test
    public void beforeLaunchingSubsItLoadsTrackFromPlaylist() {
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(false);
        testFixtures().insertPlaylistTrack(Urn.forPlaylist(123L), Urn.forTrack(124L), 0);

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(Urn.forTrack(124L));
    }

    @Test
    public void beforeLaunchingSubsItDoesNotDuplicateTracks() {
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(false);
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPlaylistTrack(Urn.forPlaylist(123L), track.getUrn(), 0);

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(track.getUrn());
    }
}
