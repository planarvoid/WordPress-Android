package com.soundcloud.android.associations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class RepostStorageTest extends StorageIntegrationTest {

    private static final Date CREATED_AT = new Date();

    private RepostStorage repostStorage;
    private ApiPlaylist playlist;
    private ApiTrack track;

    @Before
    public void setUp() throws Exception {
        playlist = testFixtures().insertPlaylist();
        track = testFixtures().insertTrack();

        repostStorage = new RepostStorage(propeller(), new TestDateProvider(CREATED_AT));
    }

    @Test
    public void shouldInsertTrackRepost() {
        repostStorage.addRepost().call(track.getUrn());

        databaseAssertions().assertTrackRepostInserted(track.getUrn(), CREATED_AT);
    }

    @Test
    public void shouldInsertPlaylistRepost() {
        repostStorage.addRepost().call(playlist.getUrn());

        databaseAssertions().assertPlaylistRepostInserted(playlist.getUrn(), CREATED_AT);
    }

    @Test
    public void shouldRemoveTrackRepost() {
        testFixtures().insertTrackRepost(track.getUrn().getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost().call(track.getUrn());

        databaseAssertions().assertTrackRepostNotExistent(track.getUrn());
    }

    @Test
    public void shouldReturnUpdatedRepostCountForRepost() {
        testFixtures().insertPlaylistRepost(playlist.getId(), CREATED_AT.getTime());

        final int updatedRepostCount = repostStorage.addRepost().call(playlist.getUrn());

        assertThat(updatedRepostCount).isEqualTo(playlist.getRepostsCount() + 1);
        databaseAssertions().assertRepostCount(playlist.getUrn(), playlist.getRepostsCount() + 1);
    }

    @Test
    public void shouldReturnUpdatedRepostCountForUnpost() {
        testFixtures().insertPlaylistRepost(playlist.getId(), CREATED_AT.getTime());

        final int updatedRepostCount = repostStorage.removeRepost().call(playlist.getUrn());

        assertThat(updatedRepostCount).isEqualTo(playlist.getRepostsCount() - 1);
        databaseAssertions().assertRepostCount(playlist.getUrn(), playlist.getRepostsCount() - 1);
    }

    @Test
    public void shouldRemovePlaylistRepost() {
        testFixtures().insertPlaylistRepost(playlist.getUrn().getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost().call(playlist.getUrn());

        databaseAssertions().assertPlaylistRepostNotExistent(playlist.getUrn());
    }
}
