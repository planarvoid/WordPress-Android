package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SyncContentTest {
    ContentResolver resolver;
    SyncStateManager syncStateManager;

    private static final int NON_ACTIVITY_ACTIVE_SYNC_CONTENT = SyncContent.NON_ACTIVITIES.size() - 1; /* follower disabled */

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
    }

    @Test
    public void shouldSyncIncoming() throws Exception {
        expect(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).toBeTrue();
    }

    @Test
    public void shouldSyncIncomingMaxMisses() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySoundStream.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                -1l, // last sync
                LocalCollection.SyncState.IDLE,
                2, // size
                String.valueOf(SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS.length) // extra
        );
        new LocalCollectionDAO(resolver).create(c);
        expect(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).toBeTrue();
    }

    @Test
    public void shouldNotSyncIncoming() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySoundStream.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                LocalCollection.SyncState.IDLE,
                2, // size
                "some-extra" // extra
        );
        new LocalCollectionDAO(resolver).create(c);
        expect(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).toBeFalse();
    }

    @Test
    public void shouldNotSyncIncoming1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySoundStream.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME + 5000, // last sync
                LocalCollection.SyncState.IDLE,
                2, // size
                "1" // extra
        );
        new LocalCollectionDAO(resolver).create(c);
        expect(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).toBeFalse();
    }

    @Test
    public void shouldSyncIncoming1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySoundStream.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
        );
        new LocalCollectionDAO(resolver).create(c);

        expect(syncStateManager.isContentDueForSync(SyncContent.MySoundStream)).toBeTrue();
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        expect(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).toBeTrue();
    }

    @Test
    public void shouldNotSyncActivities() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                LocalCollection.SyncState.IDLE,
                2, // size
                "some-extra" // extra
        );
        new LocalCollectionDAO(resolver).create(c);
        expect(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).toBeFalse();
    }

    @Test
    public void shouldNotSyncActivities1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME + 5000, // last sync
                LocalCollection.SyncState.IDLE,
                2, // size
                "1" // extra
        );
        new LocalCollectionDAO(resolver).create(c);
        expect(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).toBeFalse();
    }

    @Test
    public void shouldSyncActivities1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MyActivities.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.ACTIVITY_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
        );
        new LocalCollectionDAO(resolver).create(c);

        expect(syncStateManager.isContentDueForSync(SyncContent.MyActivities)).toBeTrue();
    }

    @Test
    public void shouldSyncAll() throws Exception {
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }

    @Test
    public void shouldSyncAllExceptMySounds() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "some-extra" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT - 1);
    }

    @Test
    public void shouldSyncAllExceptMySounds1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME + 5000, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT -1);
    }

    @Test
    public void shouldSyncAllMySounds1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }

    @Test
    public void shouldSyncAllIncludingMySoundsMaxMisses() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                1, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                String.valueOf(SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS.length) // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
        expect(urisToSync).toContain(SyncContent.MySounds.content.uri);
    }

    @Test
    public void shouldNotSyncAfterMiss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri,// uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(),false);
        SyncContent.updateCollections(syncStateManager, syncResult);

        urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT - 1);
    }

    @Test
    public void shouldNotSyncAfterNonMiss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri,// uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "" // extra
        );

        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(),true);
        SyncContent.updateCollections(syncStateManager, syncResult);

        urisToSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, false);
        expect(urisToSync.size()).toEqual(NON_ACTIVITY_ACTIVE_SYNC_CONTENT);
    }
}
