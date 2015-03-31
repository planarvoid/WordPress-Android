package com.soundcloud.android.sync;

import static org.mockito.Mockito.inOrder;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;

import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class ResultReceiverAdapterTest {

    private ResultReceiverAdapter adapter;

    @Mock private Observer<SyncResult> observer;

    @Before
    public void setUp() throws Exception {
        adapter = new ResultReceiverAdapter(observer, Looper.getMainLooper());
    }

    @Test
    public void resultReceiverAdapterShouldForwardSyncResultSuccessToSubscriberWhenSyncFinished() {
        final Bundle resultData = new Bundle();
        SyncResult resultEvent = SyncResult.success("action", true);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(resultEvent);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resultReceiverAdapterShouldForwardSyncResultFailureToSubscriberWhenSyncFinished() {
        final Bundle resultData = new Bundle();
        IOException exception = new IOException();
        SyncResult resultEvent = SyncResult.failure("action", exception);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(exception);
        inOrder.verifyNoMoreInteractions();
    }
}