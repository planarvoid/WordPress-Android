package com.soundcloud.android.associations;

import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class RepostStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Date CREATED_AT = new Date();

    private TestObserver<WriteResult> testObserver = new TestObserver<>();

    private RepostStorage repostStorage;

    @Mock private DateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        repostStorage = new RepostStorage(testScheduler(), dateProvider);
        when(dateProvider.getCurrentDate()).thenReturn(CREATED_AT);
    }

    @Test
    public void shouldInsertTrackRepost() throws Exception {
        repostStorage.addRepost(TRACK_URN).subscribe(testObserver);

        databaseAssertions().assertTrackRepostInserted(TRACK_URN, CREATED_AT);
    }

    @Test
    public void shouldInsertPlaylistRepost() throws Exception {
        repostStorage.addRepost(PLAYLIST_URN).subscribe(testObserver);

        databaseAssertions().assertPlaylistRepostInserted(PLAYLIST_URN, CREATED_AT);
    }

    @Test
    public void shouldRemoveTrackRepost() throws Exception {
        testFixtures().insertTrackRepost(TRACK_URN.getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost(TRACK_URN).subscribe(testObserver);

        databaseAssertions().assertTrackRepostNotExistent(TRACK_URN);
    }

    @Test
    public void shouldRemovePlaylistRepost() throws Exception {
        testFixtures().insertPlaylistRepost(PLAYLIST_URN.getNumericId(), CREATED_AT.getTime());

        repostStorage.removeRepost(PLAYLIST_URN).subscribe(testObserver);

        databaseAssertions().assertPlaylistRepostNotExistent(PLAYLIST_URN);
    }
}