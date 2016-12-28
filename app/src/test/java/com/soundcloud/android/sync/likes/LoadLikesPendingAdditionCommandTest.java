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

public class LoadLikesPendingAdditionCommandTest extends StorageIntegrationTest {

    private LoadLikesPendingAdditionCommand command;

    @Before
    public void setup() {
        command = new LoadLikesPendingAdditionCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesPendingAddition() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrackPendingAddition(new Date(100));
        testFixtures().insertLikedTrack(new Date(200)); // must not be returned

        List<LikeRecord> toBeAdded = command.call(TYPE_TRACK);

        assertThat(toBeAdded).containsExactly(expectedLikeFor(track.getUrn(), new Date(100)));
    }

    @Test
    public void shouldLoadPlaylistLikesPendingAddition() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylistPendingAddition(new Date(100));
        testFixtures().insertLikedPlaylist(new Date(200)); // must not be returned

        List<LikeRecord> toBeAdded = command.call(TYPE_PLAYLIST);

        assertThat(toBeAdded).containsExactly(expectedLikeFor(playlist.getUrn(), new Date(100)));
    }

    private LikeRecord expectedLikeFor(Urn urn, Date createdAt) {
        return DatabaseLikeRecord.create(urn, createdAt);
    }
}
