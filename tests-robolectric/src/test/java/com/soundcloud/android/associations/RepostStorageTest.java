package com.soundcloud.android.associations;

import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class RepostStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Date CREATED_AT = new Date();

    private RepostStorage repostStorage;

    @Mock private DateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        repostStorage = new RepostStorage(propeller(), dateProvider);
        when(dateProvider.getDate()).thenReturn(CREATED_AT);
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