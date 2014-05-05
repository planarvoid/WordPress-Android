package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.shadows.ShadowContentResolver.Status;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.accounts.Account;
import android.content.Intent;
import android.content.SyncResult;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class SyncInitiatorTest {

    private SyncInitiator initiator;
    private Subscriber<Boolean> syncSubscriber = new TestSubscriber<Boolean>();

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
    public void shouldCreateIntentForSyncingTheSoundStream() {
        initiator.syncSoundStream().subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getAction()).toBeNull();
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingOlderSoundStreamItems() {
        initiator.backfillSoundStream().subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getAction()).toEqual(ApiSyncService.ACTION_APPEND);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingLocalPlaylists() throws Exception {
        initiator.syncLocalPlaylists();

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_PLAYLISTS.uri);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeNull();
    }

    @Test
    public void shouldCreateIntentForSyncingSinglePlaylist() throws Exception {
        initiator.syncPlaylist(Urn.forPlaylist(1L)).subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.PLAYLISTS.forQuery(String.valueOf(1L)));
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingSingleTrack() throws Exception {
        initiator.syncTrack(Urn.forTrack(1L)).subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.TRACKS.forQuery(String.valueOf(1L)));
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }
}
