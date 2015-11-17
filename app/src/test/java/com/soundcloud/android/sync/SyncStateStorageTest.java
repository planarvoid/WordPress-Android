package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.fakes.RoboSharedPreferences;
import rx.observers.TestSubscriber;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class SyncStateStorageTest extends StorageIntegrationTest {

    private SyncStateStorage storage;
    private TestSubscriber<Boolean> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        RoboSharedPreferences preferences = new RoboSharedPreferences(new HashMap<String, Map<String, Object>>(), "TEST", Context.MODE_PRIVATE);
        storage = new SyncStateStorage(propellerRx(), preferences, new TestDateProvider());
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

        storage.hasSyncedBefore(SyncContent.MyLikes.content.uri).subscribe(subscriber);

        subscriber.assertValues(true);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoSuccessfulLikesSyncStored() {
        testFixtures().insertSyncAttempt(SyncContent.MyLikes, 123L);

        storage.hasSyncedBefore(SyncContent.MyLikes.content.uri).subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedCollectionsBeforeIsFalseWithNoPlaylistsSyncDataStored() {
        storage.hasSyncedBefore(SyncContent.MyLikes.content.uri).subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedBeforeShouldReturnFalseWhenNotSynced() {
        assertThat(storage.hasSyncedBefore("test")).isFalse();
    }

    @Test
    public void hasSyncedBeforeShouldReturnTrueWhenSynced() {
        storage.synced("test");
        assertThat(storage.hasSyncedBefore("test")).isTrue();
    }

}
