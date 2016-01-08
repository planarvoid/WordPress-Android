package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;

public class OfflineContentStorageIntegrationTest extends StorageIntegrationTest {

    private OfflineContentStorage contentStorage;
    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand;

    @Before
    public void setUp() {
        contentStorage = new OfflineContentStorage(propellerRx(), null, isOfflineLikedTracksEnabledCommand);
    }

    @Test
    public void storesPlaylistInOfflineContentTable() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        contentStorage.storeAsOfflinePlaylist(playlistUrn).subscribe();

        databaseAssertions().assertIsOfflinePlaylist(playlistUrn);
    }

    @Test
    public void removePlaylistFromOffline() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.removePlaylistFromOffline(playlistUrn).subscribe();

        databaseAssertions().assertIsNotOfflinePlaylist(playlistUrn);
    }

    @Test
    public void isOfflinePlaylistReturnsTrueForOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.isOfflinePlaylist(playlistUrn).subscribe(testObserver);

        testObserver.assertReceivedOnNext(singletonList(true));
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).subscribe(testObserver);

        testObserver.assertReceivedOnNext(singletonList(false));
    }

    @Test
    public void storeLikedTrackCollectionAsOffline() {
        contentStorage.storeLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsOffline();
    }

    @Test
    public void removeLikedTracksFromOffline() {
        testFixtures().insertLikesMarkedForOfflineSync();

        contentStorage.deleteLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsNotOffline();
    }

    @Test
    public void deleteLikedTrackCollectionFromTable() {
        contentStorage.deleteLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsNotOffline();
    }

    @Test
    public void addOfflinePlaylists() {
        final Urn expectedPlaylist = Urn.forPlaylist(345L);
        contentStorage.addOfflinePlaylists(singletonList(expectedPlaylist)).subscribe();

        databaseAssertions().assertIsOfflinePlaylist(expectedPlaylist);
    }
}
