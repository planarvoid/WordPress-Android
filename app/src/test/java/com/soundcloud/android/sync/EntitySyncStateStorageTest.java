package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

/***
 * Note : While this class is very generic, and can be used with any entity, it is currently only being
 * used by Playlist Syncing, through {@link com.soundcloud.android.sync.playlists.SinglePlaylistSyncer}
 */

public class EntitySyncStateStorageTest extends StorageIntegrationTest {

    private static final Urn TRACK_ENTITY = Urn.forTrack(1);
    private EntitySyncStateStorage storage;
    private TestDateProvider dateProvider;
    private SharedPreferences sharedPreferences = sharedPreferences();

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider();
        storage = new EntitySyncStateStorage(sharedPreferences, dateProvider);
    }

    @Test
    public void entityHasSyncedWithinReturnFalseWhenNeverSynced() throws Exception {
        assertThat(storage.hasSyncedWithin(TRACK_ENTITY, TimeUnit.HOURS.toMillis(1))).isFalse();
    }

    @Test
    public void entityHasSyncedWithinReturnFalseWhenSyncedBefore() throws Exception {
        storage.synced(TRACK_ENTITY);

        dateProvider.advanceBy(1, TimeUnit.DAYS);

        assertThat(storage.hasSyncedWithin(TRACK_ENTITY, TimeUnit.HOURS.toMillis(1))).isFalse();
    }

    @Test
    public void entityHasSyncedWithinReturnTrueWhenSyncedWithinInterval() throws Exception {
        storage.synced(TRACK_ENTITY);

        dateProvider.advanceBy(1, TimeUnit.SECONDS);

        assertThat(storage.hasSyncedWithin(TRACK_ENTITY, TimeUnit.HOURS.toMillis(1))).isTrue();
    }

    @Test
    public void entityHasSyncedBeforeShouldReturnFalseWhenNotSynced() {
        assertThat(storage.hasSyncedBefore(TRACK_ENTITY)).isFalse();
    }

    @Test
    public void entityJasSyncedBeforeShouldReturnTrueWhenSynced() {
        storage.synced(TRACK_ENTITY);
        assertThat(storage.hasSyncedBefore(TRACK_ENTITY)).isTrue();
    }
}
