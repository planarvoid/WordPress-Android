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
        testFixtures().insertSuccessfulSync(SyncContent.MyLikes, 123L);

        storage.hasSyncedBefore(SyncContent.MyLikes).subscribe(subscriber);

        subscriber.assertValues(true);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSuccessfulSyncStored() {
        testFixtures().insertSyncAttempt(SyncContent.MyLikes, 123L);

        storage.hasSyncedBefore(SyncContent.MyLikes).subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSyncDataStored() {
        storage.hasSyncedBefore(SyncContent.MyLikes).subscribe(subscriber);

        subscriber.assertValues(false);
    }
}