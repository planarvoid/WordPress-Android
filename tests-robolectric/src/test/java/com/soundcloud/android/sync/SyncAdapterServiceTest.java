package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.sync.likes.MyLikesStateProvider;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Ignore;
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
    @Ignore
    public void shouldFlagSoftErrorWhenIOError() throws Exception {
        addPendingHttpResponse(500, "errors");

        SyncResult result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null).result;
        expect(result.hasHardError()).toBeFalse();
        expect(result.hasSoftError()).toBeTrue();
    }

    @Test
    @Ignore
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addCannedActivities(
                "e1_activities.json",
                "empty_collection.json"
        );
        expect(doPerformSyncWithValidToken(DefaultTestRunner.application, true, null).notifications).toBeEmpty();
    }


    @Test
    public void performSyncShouldReturnFalseIfNoSyncStarted() throws Exception {
        TestHelper.connectedViaWifi(false);
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, true)
                .apply();
        SyncOutcome result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }

    @Test
    public void shouldNotPerformSyncWithNullToken(){
        SyncResult syncResult = new SyncResult();
        final SoundCloudApplication application = Mockito.mock(SoundCloudApplication.class);
        final MyLikesStateProvider myLikesStateProvider = Mockito.mock(MyLikesStateProvider.class);
        final SyncServiceResultReceiver.Factory resultReceiverFactory = Mockito.mock(SyncServiceResultReceiver.Factory.class);
        final PlaylistStorage playlistStorage = Mockito.mock(PlaylistStorage.class);
        final SyncConfig syncConfig = Mockito.mock(SyncConfig.class);
        expect(SyncAdapterService.performSync(application, null, syncResult, null, null, resultReceiverFactory, myLikesStateProvider, playlistStorage, syncConfig)).toBeFalse();
        expect(syncResult.stats.numAuthExceptions).toBeGreaterThan(0L);
    }
}
