package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        expect(playlistStorage.isOfflinePlaylist(playlistUrn)).toBeTrue();
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() throws Exception {
        expect(playlistStorage.isOfflinePlaylist(Urn.forPlaylist(123L))).toBeFalse();
    }
}