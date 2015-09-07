package com.soundcloud.android.sync;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

public class SyncStateStorageTest extends StorageIntegrationTest {

    private SyncStateStorage storage;

    private TestSubscriber<Boolean> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        storage = new SyncStateStorage(propellerRx());
    }

    @Test
    public void hasSyncedBeforeIsTrueWithSuccessfulSyncDataStored() {
        testFixtures().insertSuccessfulSync(SyncContent.MySounds, 123L);

        storage.hasSyncedMyPostsBefore().subscribe(subscriber);

        subscriber.assertValues(true);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSuccessfulSyncStored() {
        testFixtures().insertSyncAttempt(SyncContent.MySounds, 123L);

        storage.hasSyncedMyPostsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSyncDataStored() {
        storage.hasSyncedMyPostsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsTrueWithSuccessfulSyncDataStored() {
        testFixtures().insertSuccessfulSync(SyncContent.MyLikes, 123L);
        testFixtures().insertSuccessfulSync(SyncContent.MyPlaylists, 123L);

        storage.hasSyncedCollectionsBefore().subscribe(subscriber);

        subscriber.assertValues(true);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoSuccessfulLikesSyncStored() {
        testFixtures().insertSyncAttempt(SyncContent.MyLikes, 123L);
        testFixtures().insertSuccessfulSync(SyncContent.MyPlaylists, 123L);

        storage.hasSyncedCollectionsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoLikesSyncStored() {
        testFixtures().insertSuccessfulSync(SyncContent.MyPlaylists, 123L);

        storage.hasSyncedCollectionsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoSuccessfulPlaylistsSyncDataStored() {
        testFixtures().insertSuccessfulSync(SyncContent.MyLikes, 123L);
        testFixtures().insertSyncAttempt(SyncContent.MyPlaylists, 123L);

        storage.hasSyncedCollectionsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoPlaylistsSyncDataStored() {
        testFixtures().insertSuccessfulSync(SyncContent.MyLikes, 123L);

        storage.hasSyncedCollectionsBefore().subscribe(subscriber);

        subscriber.assertValues(false);
    }
}
