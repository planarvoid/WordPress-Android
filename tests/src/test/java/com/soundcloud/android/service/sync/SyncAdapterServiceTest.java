package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.sync.CollectionSyncRequestTest.NON_INTERACTIVE;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.Consts;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import android.accounts.Account;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * General sync adapter tests.
 */
public class SyncAdapterServiceTest extends SyncAdapterServiceTestBase {

    @Test
    public void shouldNotSyncWhenTokenIsInvalidAndFlagError() throws Exception {
        // will throw if actually syncing
        SyncResult result = doPerformSync(new TestApplication(new Token(null, null, null)), false, null).result;
        expect(result.hasError()).toBeTrue();
        expect(result.hasHardError()).toBeTrue();
        expect(result.hasSoftError()).toBeFalse();
    }

    @Test
    public void shouldFlagSoftErrorWhenIOError() throws Exception {
        addCannedActivities("empty_events.json");
        addPendingHttpResponse(500, "errors");
        addPendingHttpResponse(500, "errors");

        SyncResult result = doPerformSync(DefaultTestRunner.application, false, null).result;
        expect(result.hasHardError()).toBeFalse();
        expect(result.hasSoftError()).toBeTrue();
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "e1_activities.json"
        );
        expect(doPerformSync(DefaultTestRunner.application, true, null).notifications).toBeEmpty();
    }

    @Test
    public void shouldSyncLocalCollections() throws Exception {
        SyncContent.MySounds.setEnabled(Robolectric.application, true);

        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1" + NON_INTERACTIVE, 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3" + NON_INTERACTIVE, "tracks.json");

        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "empty_events.json");

        doPerformSync(DefaultTestRunner.application, false, null);

        LocalCollection lc = LocalCollection.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).not.toBeNull();
        expect(lc.extra).toEqual("0");
        expect(lc.size).toEqual(3);
        expect(lc.last_sync_success).not.toEqual(0L);

        // reset sync time & rerun sync
        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "empty_events.json");

        lc.updateLastSyncSuccessTime(0, DefaultTestRunner.application.getContentResolver());

        doPerformSync(DefaultTestRunner.application, false, null);

        lc = LocalCollection.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).not.toBeNull();
        expect(lc.extra).toEqual("1");    // 1 miss
        expect(lc.size).toEqual(3);
        expect(lc.last_sync_success).not.toEqual(0L);
    }

    @Test
    public void performSyncShouldReturnFalseIfNoSyncStarted() throws Exception {
        TestHelper.connectedViaWifi(false);
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, true)
                .commit();

        boolean hasSynced = SyncAdapterService.performSync(DefaultTestRunner.application,
                new Account("foo", "bar"),
                new Bundle(),
                new SyncResult(),
                null);

        expect(hasSynced).toBeFalse();
    }
}