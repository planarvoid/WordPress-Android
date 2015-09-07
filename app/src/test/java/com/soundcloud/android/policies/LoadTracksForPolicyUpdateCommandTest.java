package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadTracksForPolicyUpdateCommandTest extends StorageIntegrationTest {

    private LoadTracksForPolicyUpdateCommand command;

    @Before
    public void setUp() {
        command = new LoadTracksForPolicyUpdateCommand(propeller());
    }

    @Test
    public void loadsLikedTracks() {
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(track.getUrn());
    }

    @Test
    public void loadsTrackFromPlaylist() {
        testFixtures().insertPlaylistTrack(Urn.forPlaylist(123L), Urn.forTrack(124L), 0);

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(Urn.forTrack(124L));
    }

    @Test
    public void doesNotDuplicateTracks() {
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertPlaylistTrack(Urn.forPlaylist(123L), track.getUrn(), 0);

        List<Urn> result = command.call(null);

        assertThat(result).containsExactly(track.getUrn());
    }
}