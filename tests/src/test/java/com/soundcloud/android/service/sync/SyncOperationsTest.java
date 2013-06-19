package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(DefaultTestRunner.class)
public class SyncOperationsTest {

    private SyncOperations<String> syncOps;
    private Observable<String> localStorageOp;

    @Before
    public void setup() {
        localStorageOp = Observable.create(new Func1<Observer<String>, Subscription>() {
            @Override
            public Subscription call(Observer<String> stringObserver) {
                stringObserver.onNext("string data");
                stringObserver.onCompleted();
                return Subscriptions.empty();
            }
        });

        syncOps = new SyncOperations<String>(Robolectric.application, localStorageOp).subscribeOn(Schedulers.immediate());
    }

    @Test
    public void syncNowShouldSendSyncIntent() {
        syncOps.syncNow(Content.ME_ACTIVITIES.uri).subscribe(Functions.identity());

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent).not.toBeNull();
        expect(syncIntent.getAction()).toBeNull();
        expect(syncIntent.getData()).toEqual(Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void syncNowShouldSendSyncIntentForSpecificAction() {
        syncOps.syncNow(Content.ME_ACTIVITIES.uri, ApiSyncService.ACTION_APPEND).subscribe(Functions.identity());

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent).not.toBeNull();
        expect(syncIntent.getAction()).toEqual(ApiSyncService.ACTION_APPEND);
        expect(syncIntent.getData()).toEqual(Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void syncNowResultReceiverShouldInvokeObserverOnSuccess() {
        Observer<String> observer = mock(Observer.class);

        syncOps.syncNow(Content.ME_ACTIVITIES.uri).subscribe(observer);

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent.hasExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeTrue();

        ResultReceiver resultReceiver = syncIntent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, new Bundle());

        verify(observer).onNext(eq("string data"));
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void syncNowResultReceiverShouldInvokeObserverOnError() {
        Observer<String> observer = mock(Observer.class);

        syncOps.syncNow(Content.ME_ACTIVITIES.uri).subscribe(observer);

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent.hasExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeTrue();

        ResultReceiver resultReceiver = syncIntent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_ERROR, new Bundle());

        // TODO: expect a proper syncer exception type here
        verify(observer).onError(any(Exception.class));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void syncIfNecessaryShouldEmitSyncOperationIfSyncIsNecessary() {
        LocalCollection syncState = new LocalCollection(Content.ME_ACTIVITIES.uri);
        TestHelper.insert(syncState); // this will insert with default values

        Observable<String> syncOp = syncOps.syncNow(Content.ME_ACTIVITIES.uri);
        Observable<String> observable = syncOps.syncIfNecessary(Content.ME_ACTIVITIES.uri, syncOp).toBlockingObservable().last();
        expect(observable).toBe(syncOp);
    }

    @Test
    public void syncIfNecessaryShouldEmitLoadStorageOperationIfNoSyncNecessary() {
        LocalCollection syncState = new LocalCollection(Content.ME_ACTIVITIES.uri);
        syncState.last_sync_success = System.currentTimeMillis();
        TestHelper.insert(syncState);

        Observable<String> syncOp = syncOps.syncNow(Content.ME_ACTIVITIES.uri);
        Observable<String> observable = syncOps.syncIfNecessary(Content.ME_ACTIVITIES.uri, syncOp).toBlockingObservable().last();

        // we can't actually test the emitted observable for identity, but if it calls the observer with the result
        // from local storage, we know it was the local storage observable
        expect(observable.toBlockingObservable().last()).toEqual("string data");
    }
}
