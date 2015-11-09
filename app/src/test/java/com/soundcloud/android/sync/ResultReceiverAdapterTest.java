package com.soundcloud.android.sync;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;

public class ResultReceiverAdapterTest extends AndroidUnitTest {

    private ResultReceiverAdapter adapter;

    private TestSubscriber<SyncResult> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        adapter = new ResultReceiverAdapter(subscriber, Looper.getMainLooper());
    }

    @Test
    public void shouldForwardSyncResultSuccessToSubscriberWhenSyncFinished() {
        final Bundle resultData = new Bundle();
        SyncResult syncResult = SyncResult.success("action", true);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, syncResult);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        subscriber.assertValue(syncResult);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldForwardSyncResultFailureToSubscriberWhenSyncFinished() {
        final Bundle resultData = new Bundle();
        IOException exception = new IOException();
        SyncResult resultEvent = SyncResult.failure("action", exception);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, resultEvent);

        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        subscriber.assertError(exception);
    }

    @Test
    public void shouldDropResultIfSubscriberHasUnsubscribed() {
        subscriber.unsubscribe();
        adapter.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, new Bundle());

        subscriber.assertNoValues();
    }
}
