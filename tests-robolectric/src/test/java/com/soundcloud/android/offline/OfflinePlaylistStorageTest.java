package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class OfflinePlaylistStorageTest extends StorageIntegrationTest {

    private OfflinePlaylistStorage playlistStorage;

    @Before
    public void setUp() {
        playlistStorage = new OfflinePlaylistStorage(testScheduler());
    }

    @Test
    public void storesPlaylistInOfflineContentTable() throws PropellerWriteException {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        playlistStorage.storeAsOfflinePlaylist(playlistUrn).subscribe();

        databaseAssertions().assertPlaylistMarkedForOfflineSync(playlistUrn);
    }

    @Test
    public void removesPlaylistFromOfflineContentTable() throws PropellerWriteException {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        playlistStorage.removeFromOfflinePlaylists(playlistUrn).subscribe();

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlistUrn);
    }

    @Test
    public void isOfflinePlaylistReturnsTrueForOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        playlistStorage.isOfflinePlaylist(playlistUrn).subscribe(testObserver);

        testObserver.assertReceivedOnNext(Arrays.asList(true));
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() throws Exception {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        playlistStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).subscribe(testObserver);

        testObserver.assertReceivedOnNext(Arrays.asList(false));
    }
}