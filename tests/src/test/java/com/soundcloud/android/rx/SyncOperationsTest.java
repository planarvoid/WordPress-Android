package com.soundcloud.android.rx;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.rx.schedulers.SyncOperations;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(DefaultTestRunner.class)
public class SyncOperationsTest {

    private SyncOperations<String> syncOps;

    @Before
    public void setup() {
        syncOps = new SyncOperations<String>(Robolectric.application, new SyncOperations.LocalStorageStrategy<String>() {
            @Override
            public Observable<String> loadFromContentUri(final Uri contentUri) {
                return Observable.create(new Func1<Observer<String>, Subscription>() {
                    @Override
                    public Subscription call(Observer<String> stringObserver) {
                        stringObserver.onNext("loaded via " + contentUri);
                        stringObserver.onCompleted();
                        return Subscriptions.empty();
                    }
                });
            }
        });
    }

    @Test
    public void syncNowShouldSendSyncIntent() {
        syncOps.syncNow(Content.ME_ACTIVITIES.uri).subscribe(Functions.identity());

        Intent syncIntent = Robolectric.getShadowApplication().getNextStartedService();
        expect(syncIntent).not.toBeNull();
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

        verify(observer).onNext(eq("loaded via " + Content.ME_ACTIVITIES.uri));
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
}
