package com.soundcloud.android.sync;

import static com.soundcloud.propeller.query.Query.from;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.fakes.RoboSharedPreferences;
import rx.Scheduler;
import rx.schedulers.TestScheduler;

import android.content.Context;
import android.net.Uri;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncStateManagerTest extends StorageIntegrationTest {
    private static final Uri CONTENT_URI = SyncContent.MySoundStream.content.uri;
    private TestDateProvider dateProvider = new TestDateProvider(123L);
    private SyncStateManager syncStateManager;
    private Scheduler scheduler = new TestScheduler();

    private static final int NON_ACTIVITY_ACTIVE_SYNC_CONTENT = SyncContent.NON_ACTIVITIES.size(); /* follower disabled */

    @Before
    public void before() {
        final Map<String, Map<String, Object>> prefsMap = new HashMap<>();
        final RoboSharedPreferences sharedPrefs = new RoboSharedPreferences(prefsMap, "sync_state", Context.MODE_PRIVATE);
        final SyncStateStorage syncStateStorage = new SyncStateStorage(propeller(), sharedPrefs, new CurrentDateProvider());
        syncStateManager = new SyncStateManager(syncStateStorage, dateProvider, scheduler);
    }

    @Test
    public void shouldSyncIncoming() throws Exception {
        assertThat(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).isTrue();
    }

    @Test
    public void shouldSyncIncomingMaxMisses() throws Exception {
        testFixtures().insertSyncState(
                CONTENT_URI, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                -1l, // last sync
                String.valueOf(SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS.length) // extra
        );
        assertThat(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).isTrue();
    }

    @Test
    public void shouldNotSyncIncoming() throws Exception {
        testFixtures().insertSyncState(
                CONTENT_URI, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                "" // extra
        );
        
        assertThat(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).isFalse();
    }

    @Test
    public void shouldNotSyncIncoming1Miss() throws Exception {
        testFixtures().insertSyncState(
                CONTENT_URI, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME + 5000, // last sync
                "1" // extra
        );

        assertThat(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).isFalse();
    }

    @Test
    public void shouldSyncIncoming1Miss() throws Exception {
        testFixtures().insertSyncState(
                CONTENT_URI, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME, // last sync
                "1" // extra
        );

        assertThat(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).isTrue();
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        assertThat(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).isTrue();
    }

    @Test
    public void shouldNotSyncActivities() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                "" // extra
        );
        
        assertThat(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).isFalse();
    }

    @Test
    public void shouldNotSyncActivities1Miss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME + 5000, // last sync
                "1" // extra
        );

        assertThat(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).isFalse();
    }

    @Test
    public void shouldSyncActivities1Miss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME, // last sync
                "1" // extra
        );
        

        assertThat(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).isTrue();
    }

    @Test
    public void shouldSyncAll() throws Exception {
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }

    @Test
    public void shouldSyncAllExceptMySounds() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                "" // extra
        );
        
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT - 1);
    }

    @Test
    public void shouldSyncAllExceptMySounds1Miss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME + 5000, // last sync
                "1" // extra
        );
        
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT - 1);
    }

    @Test
    public void shouldSyncAllMySounds1Miss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME, // last sync
                "1" // extra
        );
        
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }

    @Test
    public void shouldSyncAllIncludingMySoundsMaxMisses() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                1, // last sync
                String.valueOf(SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS.length) // extra
        );

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
        assertThat(urisToSync).contains(SyncContent.MySounds.content.uri);
    }

    @Test
    public void shouldNotSyncAfterMiss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri,// uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                "" // extra
        );

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(), false);
        SyncContent.updateCollections(syncStateManager, syncResult);

        urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT - 1);
    }

    @Test
    public void shouldNotSyncAfterNonMiss() throws Exception {
        testFixtures().insertSyncState(
                SyncContent.MySounds.content.uri,// uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                "" // extra
        );

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(), true);
        SyncContent.updateCollections(syncStateManager, syncResult);

        urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        assertThat(urisToSync.size()).isEqualTo(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }

    @Test
    public void shouldUpdateLastSyncAttemptForUri() {
        testFixtures().insertSyncState(Content.ME_SOUND_STREAM.uri, 0L, 0L, "0");
        syncStateManager.updateLastSyncAttempt(Content.ME_SOUND_STREAM.uri);

        final Long lastSyncAttempt = propeller().query(from(Table.Collections)
                        .select(TableColumns.Collections.LAST_SYNC_ATTEMPT)
                        .whereEq(TableColumns.Collections.URI, Content.ME_SOUND_STREAM.uri)
        ).firstOrDefault(Long.class, -1L);
        assertThat(lastSyncAttempt).isEqualTo(dateProvider.getCurrentTime());
    }
}
