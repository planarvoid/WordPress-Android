package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.shadows.ShadowContentResolver.Status;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class SyncInitiatorTest {

    private SyncInitiator initiator;

    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ResultReceiver resultReceiver;

    @Before
    public void setup() {
        initiator = new SyncInitiator(Robolectric.application, accountOperations);
    }

    @Test
    public void shouldCreateSyncIntentForPushingFollowingsForValidAccount() throws Exception {
        Account account = new Account("soundcloud", "account");
        when(accountOperations.getSoundCloudAccount()).thenReturn(account);
        expect(initiator.pushFollowingsToApi()).toBeTrue();

        Status syncStatus = ShadowContentResolver.getStatus(account, ScContentProvider.AUTHORITY);
        expect(syncStatus.syncRequests).toBe(1);
        expect(syncStatus.syncExtras.getBoolean(SyncAdapterService.EXTRA_SYNC_PUSH)).toBeTrue();
        expect(syncStatus.syncExtras.getString(SyncAdapterService.EXTRA_SYNC_PUSH_URI)).toEqual(Content.ME_FOLLOWINGS.uri.toString());
    }

    @Test
    public void shouldReturnFalseWhenPushingFollowingsWithInvalidAccount() throws Exception {
        expect(initiator.pushFollowingsToApi()).toBeFalse();
    }

    @Test
    public void shouldCreateIntentForSyncingLocalPlaylists() throws Exception {
        initiator.syncLocalPlaylists(resultReceiver);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_PLAYLISTS.uri);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBe(resultReceiver);
    }

    @Test
    public void shouldCreateIntentForSyncingSinglePlaylist() throws Exception {
        final Uri playlistUri = Content.PLAYLISTS.forQuery(String.valueOf(1L));
        initiator.syncContentUri(playlistUri, resultReceiver);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(playlistUri);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBe(resultReceiver);
    }


}
