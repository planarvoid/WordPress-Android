package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.sync.CollectionSyncRequestTest.NON_INTERACTIVE;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.mockito.Mockito;

import android.content.SyncResult;
import android.preference.PreferenceManager;

/**
 * General sync adapter tests.
 */
public class SyncAdapterServiceTest extends SyncAdapterServiceTestBase {

    @Test
    public void shouldNotSyncWhenTokenIsInvalidAndFlagError() throws Exception {
        // will throw if actually syncing
        SyncResult result = doPerformSync(DefaultTestRunner.application, false, null, mock(Token.class)).result;
        expect(result.hasError()).toBeTrue();
        expect(result.hasHardError()).toBeTrue();
        expect(result.hasSoftError()).toBeFalse();
    }

    @Test
    public void shouldFlagSoftErrorWhenIOError() throws Exception {
        addCannedActivities("empty_collection.json");
        addPendingHttpResponse(500, "errors");

        SyncResult result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null).result;
        expect(result.hasHardError()).toBeFalse();
        expect(result.hasSoftError()).toBeTrue();
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addCannedActivities(
                "empty_collection.json",
                "e1_activities.json"
        );
        expect(doPerformSyncWithValidToken(DefaultTestRunner.application, true, null).notifications).toBeEmpty();
    }

    @Test
    public void shouldSyncLocalCollections() throws Exception {
        SyncContent.MySounds.setEnabled(Robolectric.application, true);
        TestHelper.addCannedResponse(getClass(), "/e1/me/sounds/mini?limit=200&representation=mini&linked_partitioning=1" + NON_INTERACTIVE, "me_sounds_mini.json");

        // dashboard
        addCannedActivities(
                "empty_collection.json",
                "empty_collection.json");

        doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        final SyncStateManager syncStateManager = new SyncStateManager();
        LocalCollection lc = syncStateManager.fromContent(Content.ME_SOUNDS);
        expect(lc.extra).toBeNull();
        expect(lc.size).toEqual(50);
        expect(lc.last_sync_success).toBeGreaterThan(0L);

        // reset sync time & rerun sync
        addCannedActivities(
                "empty_collection.json",
                "empty_collection.json");


        syncStateManager.updateLastSyncSuccessTime(Content.ME_SOUNDS, 0);

        doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        lc = syncStateManager.fromContent(Content.ME_SOUNDS);
        expect(lc.extra).toEqual(String.valueOf(1)); // incremented sync miss for backoff
        expect(lc.size).toEqual(50);
        expect(lc.last_sync_success).toBeGreaterThan(0L);
    }

    @Test
    public void performSyncShouldReturnFalseIfNoSyncStarted() throws Exception {
        TestHelper.connectedViaWifi(false);
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, true)
                .commit();
        SyncOutcome result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }

    @Test public void shouldNotPerformSyncWithNullToken(){
        SyncResult syncResult = new SyncResult();
        expect(SyncAdapterService.performSync(Mockito.mock(SoundCloudApplication.class), null, syncResult, null, null)).toBeFalse();
        expect(syncResult.stats.numAuthExceptions).toBeGreaterThan(0L);
    }
}