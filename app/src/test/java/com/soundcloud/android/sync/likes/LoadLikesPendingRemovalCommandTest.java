package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLikesPendingRemovalCommandTest extends StorageIntegrationTest {

    private LoadLikesPendingRemovalCommand command;

    @Before
    public void setup() {
        command = new LoadLikesPendingRemovalCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesPendingRemoval() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrackPendingRemoval(new Date(100));
        testFixtures().insertLikedTrack(new Date(200)); // must not be returned

        List<LikeRecord> toBeRemoved = command.call(TYPE_TRACK);

        assertThat(toBeRemoved).containsExactly(expectedLikeFor(track.getUrn(), new Date(0)));
    }

    @Test
    public void shouldLoadPlaylistLikesPendingRemoval() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylistPendingRemoval(new Date(100));
        testFixtures().insertLikedPlaylist(new Date(200)); // must not be returned

        List<LikeRecord> toBeRemoved = command.call(TYPE_PLAYLIST);

        assertThat(toBeRemoved).containsExactly(expectedLikeFor(playlist.getUrn(), new Date(0)));
    }

    private LikeRecord expectedLikeFor(Urn urn, Date removedAt) {
        return DatabaseLikeRecord.create(urn, removedAt);
    }
}
