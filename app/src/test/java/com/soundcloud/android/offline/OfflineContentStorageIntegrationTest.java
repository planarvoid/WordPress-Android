package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;

public class OfflineContentStorageIntegrationTest extends StorageIntegrationTest {

    private OfflineContentStorage contentStorage;

    @Before
    public void setUp() {
        contentStorage = new OfflineContentStorage(sharedPreferences(), Schedulers.trampoline());
    }

    @Test
    public void storesPlaylistInOfflineContentTable() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        contentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn)).test().assertComplete();

        contentStorage.isOfflinePlaylist(playlistUrn).test().assertValue(true).assertComplete();
    }

    @Test
    public void removePlaylistFromOffline() {
        final Urn playlistUrn = insertOfflinePlaylist();

        contentStorage.removePlaylistsFromOffline(singletonList(playlistUrn)).test().assertComplete();

        contentStorage.isOfflinePlaylist(playlistUrn).test().assertValue(false).assertComplete();
    }

    @Test
    public void isOfflinePlaylistReturnsTrueForOfflinePlaylist() {
        final Urn playlistUrn = insertOfflinePlaylist();

        final io.reactivex.observers.TestObserver<Boolean> testObserver = contentStorage.isOfflinePlaylist(playlistUrn).test().assertComplete();

        testObserver.assertValue(Boolean.TRUE);
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() {
        final io.reactivex.observers.TestObserver<Boolean> testObserver = contentStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).test().assertComplete();

        testObserver.assertValue(Boolean.FALSE);
    }

    @Test
    public void storeLikedTrackCollectionAsOffline() {
        contentStorage.addLikedTrackCollection().test().assertComplete();

        contentStorage.isOfflineLikesEnabled().test().assertValue(true).assertComplete();
    }

    @Test
    public void removeLikedTracksFromOffline() {
        makeLikesAvailableOffline();

        contentStorage.removeLikedTrackCollection().test().assertComplete();

        contentStorage.isOfflineLikesEnabled().test().assertValue(false).assertComplete();
    }

    @Test
    public void deleteLikedTrackCollectionFromTable() {
        contentStorage.removeLikedTrackCollection().test().assertComplete();

        contentStorage.isOfflineLikesEnabled().test().assertValue(false).assertComplete();
    }

    @Test
    public void setOfflinePlaylists() {
        makeLikesAvailableOffline();
        final Urn previousPlaylist = insertOfflinePlaylist();
        final Urn expectedPlaylist = Urn.forPlaylist(345678L);

        contentStorage.resetOfflinePlaylists(singletonList(expectedPlaylist)).test().assertComplete();

        contentStorage.isOfflinePlaylist(previousPlaylist).test().assertValue(false).assertComplete();
        contentStorage.isOfflinePlaylist(expectedPlaylist).test().assertValue(true).assertComplete();
        contentStorage.isOfflineLikesEnabled().test().assertValue(true).assertComplete();
    }

    private Urn insertOfflinePlaylist() {
        final Urn playlist = testFixtures().insertPlaylist().getUrn();
        contentStorage.storeAsOfflinePlaylists(singletonList(playlist)).test().assertComplete();
        return playlist;
    }

    private void makeLikesAvailableOffline() {
        contentStorage.addLikedTrackCollection().test().assertComplete();
    }
}
