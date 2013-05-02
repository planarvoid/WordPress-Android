package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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

    private static final int ACTIVE_SYNC_ENDPOINTS = SyncContent.values().length - 1; /* follower disabled */

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        syncStateManager = new SyncStateManager();

        SyncContent.setAllSyncEnabledPrefs(Robolectric.application,true);
    }

    @Test
    public void shouldSyncAll() throws Exception {
        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS);
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

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS - 1);
    }

    @Test
    public void shouldSyncAllExceptMySounds1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME + 5000, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS -1);
    }

    @Test
    public void shouldSyncAllMySounds1Miss() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                "1" // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS );
    }

    @Test
    public void shouldSyncAllExceptMySoundsMaxMisses() throws Exception {
        LocalCollection c = new LocalCollection(
                SyncContent.MySounds.content.uri, // uri
                -1l, // last sync attempt, ignored in the sync adapter
                1, // last sync
                LocalCollection.SyncState.PENDING,
                2, // size
                String.valueOf(SyncConfig.TRACK_BACKOFF_MULTIPLIERS.length) // extra
                );
        new LocalCollectionDAO(resolver).create(c);

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS -1);
        expect(urisToSync).not.toContain(SyncContent.MySounds.content.uri);
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

        List<Uri> urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(),false);
        SyncContent.updateCollections(Robolectric.application, syncResult);

        urisToSync = syncStateManager.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS-1);
    }
}
