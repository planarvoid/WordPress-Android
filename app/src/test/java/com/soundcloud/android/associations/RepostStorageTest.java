package com.soundcloud.android.associations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class RepostStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Date CREATED_AT = new Date();

    private RepostStorage repostStorage;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider(CREATED_AT);
        repostStorage = new RepostStorage(propeller(), dateProvider);
    }

    @Test
    public void shouldInsertTrackRepost() throws Exception {
        repostStorage.addRepost().call(TRACK_URN);

        databaseAssertions().assertTrackRepostInserted(TRACK_URN, CREATED_AT);
    }

    @Test
    public void shouldInsertPlaylistRepost() throws Exception {
        repostStorage.addRepost().call(PLAYLIST_URN);

        databaseAssertions().assertPlaylistRepostInserted(PLAYLIST_URN, CREATED_AT);
    }

    @Test
    public void shouldRemoveTrackRepost() throws Exception {
        testFixtures().insertTrackRepost(TRACK_URN.getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost().call(TRACK_URN);

        databaseAssertions().assertTrackRepostNotExistent(TRACK_URN);
    }

    @Test
    public void shouldRemovePlaylistRepost() throws Exception {
        testFixtures().insertPlaylistRepost(PLAYLIST_URN.getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost().call(PLAYLIST_URN);

        databaseAssertions().assertPlaylistRepostNotExistent(PLAYLIST_URN);
    }
}