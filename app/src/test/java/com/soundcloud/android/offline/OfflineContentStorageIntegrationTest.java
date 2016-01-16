package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.List;

public class OfflineContentStorageIntegrationTest extends StorageIntegrationTest {

    private OfflineContentStorage contentStorage;

    @Before
    public void setUp() {
        contentStorage = new OfflineContentStorage(propellerRx(), null);
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

        testObserver.assertReceivedOnNext(singletonList(true));
    }

    @Test
    public void isOfflinePlaylistReturnsFalseForNonOfflinePlaylist() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.isOfflinePlaylist(Urn.forPlaylist(123L)).subscribe(testObserver);

        testObserver.assertReceivedOnNext(singletonList(false));
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

        testObserver.assertReceivedOnNext(singletonList(true));
    }

    @Test
    public void isOfflineLikesEnabledReturnsFalseWhenNothingStoredInDB() {
        final TestObserver<Boolean> testObserver = new TestObserver<>();

        contentStorage.isOfflineLikesEnabled().subscribe(testObserver);

        testObserver.assertReceivedOnNext(singletonList(false));
    }

    @Test
    public void resetOfflinePlaylistsWithEmptyContentEmitsEmptyList() {
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

        contentStorage.setOfflinePlaylists(Collections.<Urn>emptyList()).subscribe(subscriber);

        subscriber.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void resetOfflinePlaylistsEmitsAddedPlaylists() {
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
        final Urn expectedUrn = Urn.forPlaylist(123L);

        contentStorage.setOfflinePlaylists(singletonList(expectedUrn)).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(PropertySet.from(
                        PlaylistProperty.URN.bind(expectedUrn),
                        OfflineProperty.OFFLINE_STATE.bind(OfflineState.REQUESTED)
                ))
        );
        databaseAssertions().assertPlaylistMarkedForOfflineSync(expectedUrn);
    }

    @Test
    public void resetOfflinePlaylistsEmitsDeletedPlaylists() {
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
        final Urn playlistToDelete = testFixtures().insertPlaylistMarkedForOfflineSync().getUrn();

        contentStorage.setOfflinePlaylists(Collections.<Urn>emptyList()).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(PropertySet.from(
                        PlaylistProperty.URN.bind(playlistToDelete),
                        OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE)
                ))
        );
        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlistToDelete);
    }
}
