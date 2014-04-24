package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.sync.SyncInitiator.ResultReceiverAdapter;
import static com.xtremelabs.robolectric.shadows.ShadowContentResolver.Status;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Subscriber;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class SyncInitiatorTest {

    private SyncInitiator initiator;

    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ResultReceiver resultReceiver;
    @Mock
    private Subscriber subscriber;

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
    public void shouldCreateIntentForSyncingOlderSoundStreamItems() {
        initiator.backfillSoundStream(resultReceiver);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getAction()).toEqual(ApiSyncService.ACTION_APPEND);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBe(resultReceiver);
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
        initiator.syncPlaylist(Urn.forPlaylist(1L), resultReceiver);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.PLAYLISTS.forQuery(String.valueOf(1L)));
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBe(resultReceiver);
    }

    @Test
    public void resultReceiverAdapterShouldForwardResultToSubscriberWhenSyncFinished() {
        ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber, "result");
        receiverAdapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext("result");
        inOrder.verify(subscriber).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardResultToSubscriberWhenAppendingSyncFinished() {
        ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber, "result");
        receiverAdapter.onReceiveResult(ApiSyncService.STATUS_APPEND_FINISHED, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext("result");
        inOrder.verify(subscriber).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardErrorToSubscriberWhenSyncFailed() {
        ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber, "result");
        receiverAdapter.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onError(isA(SyncInitiator.SyncFailedException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardErrorToSubscriberWhenAppendingSyncFailed() {
        ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber, "result");
        receiverAdapter.onReceiveResult(ApiSyncService.STATUS_APPEND_ERROR, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onError(isA(SyncInitiator.SyncFailedException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldCreateIntentForSyncingSingleTrack() throws Exception {
        initiator.syncTrack(Urn.forTrack(1L), resultReceiver);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.TRACKS.forQuery(String.valueOf(1L)));
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBe(resultReceiver);
    }
}
