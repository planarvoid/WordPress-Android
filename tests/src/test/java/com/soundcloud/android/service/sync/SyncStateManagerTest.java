package com.soundcloud.android.service.sync;


import android.net.Uri;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class SyncStateManagerTest {

    SyncStateManager syncStateManager;

    @Before public void before() {
        syncStateManager = new SyncStateManager(Robolectric.application.getContentResolver());
    }

    @Test
    public void shouldGetLastSyncAttempt() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = syncStateManager.insertLocalCollection(uri, 1, 100, 1, 0, null);
        expect(c).not.toBeNull();
        expect(syncStateManager.getLastSyncAttempt(uri)).toEqual(100L);
        expect(syncStateManager.getLastSyncAttempt(Uri.parse("notfound"))).toEqual(-1L);
    }

    @Test
    public void shouldGetLastSyncSuccess() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = syncStateManager.insertLocalCollection(uri, 1, 1, 100, 0, null);
        expect(c).not.toBeNull();
        expect(syncStateManager.getLastSyncSuccess(uri)).toEqual(100L);
        expect(syncStateManager.getLastSyncSuccess(Uri.parse("notfound"))).toEqual(-1L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        insertLocalCollection(uri);
        syncStateManager.updateLastSyncSuccessTime(uri, 200);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(200L);
    }

    @Test
    public void shouldForceToStale() throws Exception {
        final Uri uri = Uri.parse("foo");
        insertLocalCollection(uri);
        syncStateManager.updateLastSyncSuccessTime(uri, 200);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(200L);

        syncStateManager.forceToStale(uri);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(0L);
    }

    @Test
    public void shouldChangeAutoRefresh() throws Exception {
        Uri uri = Content.ME_LIKES.uri;
        LocalCollection lc = syncStateManager.insertLocalCollection(uri, 0, 1, 0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeTrue();
        syncStateManager.updateSyncState(lc.id, LocalCollection.SyncState.SYNCING);
        expect(syncStateManager.fromContent(uri).shouldAutoRefresh()).toBeFalse();
    }

    private LocalCollection insertLocalCollection(Uri contentUri) {
        return syncStateManager.insertLocalCollection(contentUri, 0, -1, -1, -1, null);
    }

}
