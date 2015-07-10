package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import java.util.Collections;

public class OfflineContentStorageTest extends StorageIntegrationTest {

    private OfflineContentStorage contentStorage;

    @Before
    public void setUp() {
        contentStorage = new OfflineContentStorage(propellerRx());
    }

    @Test
    public void storesPlaylistInOfflineContentTable() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        contentStorage.storeAsOfflinePlaylist(playlistUrn).subscribe();

        databaseAssertions().assertPlaylistMarkedForOfflineSync(playlistUrn);
    }

    @Test
    public void removesPlaylistFromOfflineContentTable() {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.removeFromOfflinePlaylists(playlistUrn).subscribe();

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlistUrn);
    }

    @Test
    public void isOfflinePlaylistReturnsTrueForOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.isOfflinePlaylist(playlistUrn).subscribe(testObserver);

        testObserver.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).subscribe(testObserver);

        testObserver.assertReceivedOnNext(Collections.singletonList(false));
    }

    @Test
    public void storeOfflineLikesEnabledWritesToOfflineContentTable() {
        contentStorage.storeOfflineLikesEnabled().subscribe();

        databaseAssertions().assertOfflineLikesEnabled();
    }

    @Test
    public void storeOfflineLikesDisabledWritesToOfflineContentTable() {
        contentStorage.storeOfflineLikesDisabled().subscribe();

        databaseAssertions().assertOfflineLikesDisabled();
    }

    @Test
    public void isOfflineLikesEnabledReturnsStoredValue() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.storeOfflineLikesEnabled().subscribe();
        contentStorage.isOfflineLikesEnabled().subscribe(testObserver);

        testObserver.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void isOfflineLikesEnabledReturnsFalseWhenNothingStoredInDB() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.isOfflineLikesEnabled().subscribe(testObserver);

        testObserver.assertReceivedOnNext(Collections.singletonList(false));
    }

}