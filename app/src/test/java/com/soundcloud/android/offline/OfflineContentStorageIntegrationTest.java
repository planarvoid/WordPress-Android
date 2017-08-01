package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OfflineContentStorageIntegrationTest extends StorageIntegrationTest {

    private OfflineContentStorage contentStorage;
    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand;

    @Before
    public void setUp() {
        contentStorage = new OfflineContentStorage(propellerRxV2(), null, isOfflineLikedTracksEnabledCommand);
    }

    @Test
    public void storesPlaylistInOfflineContentTable() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        contentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn)).subscribe();

        databaseAssertions().assertIsOfflinePlaylist(playlistUrn);
    }

    @Test
    public void removePlaylistFromOffline() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.removePlaylistsFromOffline(singletonList(playlistUrn)).subscribe();

        databaseAssertions().assertIsNotOfflinePlaylist(playlistUrn);
    }

    @Test
    public void isOfflinePlaylistReturnsTrueForOfflinePlaylist() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        final io.reactivex.observers.TestObserver<Boolean> testObserver = contentStorage.isOfflinePlaylist(playlistUrn).test();

        testObserver.assertValue(Boolean.TRUE);
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() {
        final io.reactivex.observers.TestObserver<Boolean> testObserver = contentStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).test();

        testObserver.assertValue(Boolean.FALSE);
    }

    @Test
    public void storeLikedTrackCollectionAsOffline() {
        contentStorage.addLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsOffline();
    }

    @Test
    public void removeLikedTracksFromOffline() {
        testFixtures().insertLikesMarkedForOfflineSync();

        contentStorage.removeLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsNotOffline();
    }

    @Test
    public void deleteLikedTrackCollectionFromTable() {
        contentStorage.removeLikedTrackCollection().subscribe();

        databaseAssertions().assertLikedTracksIsNotOffline();
    }

    @Test
    public void setOfflinePlaylists() {
        testFixtures().insertLikesMarkedForOfflineSync();
        final Urn previousPlaylist = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();
        final Urn expectedPlaylist = Urn.forPlaylist(345678L);

        contentStorage.resetOfflinePlaylists(singletonList(expectedPlaylist)).subscribe();

        databaseAssertions().assertIsNotOfflinePlaylist(previousPlaylist);
        databaseAssertions().assertIsOfflinePlaylist(expectedPlaylist);
        databaseAssertions().assertLikedTracksIsOffline();
    }
}
