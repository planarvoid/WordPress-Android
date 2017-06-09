package com.soundcloud.android.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.SingleEmitter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;

public class ResultReceiverAdapterTest extends AndroidUnitTest {

    private ResultReceiverAdapter adapter;

    @Mock private SingleEmitter<SyncJobResult> subscriber;

    @Before
    public void setUp() throws Exception {
        adapter = new ResultReceiverAdapter(subscriber, Looper.getMainLooper());
        reset(subscriber);
    }

    @Test
    public void shouldForwardSyncResultSuccessToSubscriberWhenSyncFinished() {
        when(subscriber.isDisposed()).thenReturn(false);
        final Bundle resultData = new Bundle();
        SyncJobResult syncJobResult = SyncJobResult.success("action", true);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, syncJobResult);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        verify(subscriber).onSuccess(syncJobResult);
    }

    @Test
    public void shouldForwardSyncResultFailureToSubscriberWhenSyncFinished() {
        when(subscriber.isDisposed()).thenReturn(false);
        final Bundle resultData = new Bundle();
        IOException exception = new IOException();
        SyncJobResult resultEvent = SyncJobResult.failure("action", exception);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        verify(subscriber).onError(exception);
    }

    @Test
    public void shouldDropResultIfSubscriberHasUnsubscribed() {
        when(subscriber.isDisposed()).thenReturn(true);
        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, new Bundle());

        verify(subscriber, never()).onSuccess(any(SyncJobResult.class));
        verify(subscriber, never()).onError(any(Throwable.class));
    }
}
