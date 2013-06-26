package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.ScActions;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observer;
import rx.concurrency.Schedulers;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class SyncOperationsTest {

    private SyncOperations syncOps;

    @Before
    public void setup() {
        syncOps = new SyncOperations(Robolectric.application).subscribeOn(Schedulers.immediate());
    }

    @Test
    public void shouldSendPushFollowingsIntent() {
        syncOps.pushFollowings().subscribe(ScActions.NO_OP);

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent).not.toBeNull();
        expect(syncIntent.getAction()).toBe(ApiSyncService.ACTION_PUSH);
        expect(syncIntent.getData()).toEqual(Content.ME_FOLLOWINGS.uri);
    }



    @Test
    public void pushFollowingsResultReceiverShouldInvokeObserverOnSuccess() {
        Observer<Void> observer = mock(Observer.class);

        syncOps.pushFollowings().subscribe(observer);

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent.hasExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeTrue();

        ResultReceiver resultReceiver = syncIntent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, new Bundle());

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void syncNowResultReceiverShouldInvokeObserverOnError() {
        Observer<Void> observer = mock(Observer.class);

        syncOps.pushFollowings().subscribe(observer);

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent.hasExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeTrue();

        ResultReceiver resultReceiver = syncIntent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_ERROR, new Bundle());

        // TODO: expect a proper syncer exception type here
        verify(observer).onError(any(Exception.class));
        verifyNoMoreInteractions(observer);
    }

}
