package com.soundcloud.android.sync;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Subscriber;

import android.net.Uri;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class LegacyResultReceiverAdapterTest {

    private LegacyResultReceiverAdapter adapter;

    @Mock
    private Subscriber<Boolean> subscriber;

    @Before
    public void setup() {
        adapter = new LegacyResultReceiverAdapter(subscriber, Uri.parse("content://abc"));
    }

    @Test
    public void resultReceiverAdapterShouldForwardChangeFlagForGivenResourceToSubscriberWhenSyncFinished() {
        final Bundle resultData = new Bundle();
        resultData.putBoolean("content://abc", true);
        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext(true);
        inOrder.verify(subscriber).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardChangeFlagForGivenResourceToSubscriberWhenAppendingSyncFinished() {
        final Bundle resultData = new Bundle();
        resultData.putBoolean("content://abc", true);
        adapter.onReceiveResult(ApiSyncService.STATUS_APPEND_FINISHED, resultData);
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext(true);
        inOrder.verify(subscriber).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardErrorToSubscriberWhenSyncFailed() {
        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onError(isA(SyncFailedException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardErrorToSubscriberWhenAppendingSyncFailed() {
        adapter.onReceiveResult(ApiSyncService.STATUS_APPEND_ERROR, new Bundle());
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onError(isA(SyncFailedException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnUnexpectedSyncStatus() {
        adapter.onReceiveResult(12345, new Bundle());
    }
}