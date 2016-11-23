package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class SyncStateStorageTest extends StorageIntegrationTest {

    private SyncStateStorage storage;
    private TestDateProvider dateProvider;
    private SharedPreferences sharedPreferences = sharedPreferences();

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider();
        storage = new SyncStateStorage(sharedPreferences, dateProvider);
    }

    @Test
    public void hasSyncedWithinReturnFalseWhenNeverSynced() throws Exception {
        assertThat(storage.hasSyncedWithin("MY_ENTITY", TimeUnit.HOURS.toMillis(1))).isFalse();
    }

    @Test
    public void hasSyncedWithinReturnFalseWhenSyncedBefore() throws Exception {
        storage.synced("MY_ENTITY");

        dateProvider.advanceBy(1, TimeUnit.DAYS);

        assertThat(storage.hasSyncedWithin("MY_ENTITY", TimeUnit.HOURS.toMillis(1))).isFalse();
    }

    @Test
    public void hasSyncedWithinReturnTrueWhenSyncedWithinInterval() throws Exception {
        storage.synced("MY_ENTITY");

        dateProvider.advanceBy(1, TimeUnit.SECONDS);

        assertThat(storage.hasSyncedWithin("MY_ENTITY", TimeUnit.HOURS.toMillis(1))).isTrue();
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

    @Test
    public void getSyncMissesReturnsZeroIfNeverSet() {
        assertThat(storage.getSyncMisses(Syncable.CHARTS)).isEqualTo(0);
    }

    @Test
    public void getSyncMissesReturnsSetValue() {
        sharedPreferences.edit().putInt(Syncable.CHARTS.name() + "_misses", 5).apply();

        assertThat(storage.getSyncMisses(Syncable.CHARTS)).isEqualTo(5);
    }

    @Test
    public void resetSyncMissesStoresValue() {
        storage.resetSyncMisses(Syncable.CHARTS);

        assertThat(sharedPreferences.getInt(Syncable.CHARTS.name() + "_misses", 4)).isEqualTo(0);
    }

    @Test
    public void incrementSyncMissesStoresValue() {
        storage.incrementSyncMisses(Syncable.CHARTS);

        assertThat(sharedPreferences.getInt(Syncable.CHARTS.name() + "_misses", 0)).isEqualTo(1);
    }
}
