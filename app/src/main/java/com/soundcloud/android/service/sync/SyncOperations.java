package com.soundcloud.android.service.sync;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

public class SyncOperations extends ScheduledOperations {

    private final Context mContext;

    public SyncOperations(Context context) {
        mContext = context;
    }

    public Observable<Void> pushFollowings() {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(final Observer<Void> observer) {
                log("Pushing followings...");

                final BooleanSubscription subscription = new BooleanSubscription();

                // make sure the result receiver is invoked on the main thread, that's because after a sync error
                // we call through to the observer directly rather than going through a scheduler
                final ResultReceiver receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (!subscription.isUnsubscribed()) {
                            handleSyncResult(resultCode, observer);
                        } else {
                            log("Not delivering results, was unsubscribed");
                        }
                    }
                };

                Intent intent = new Intent(mContext, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiver)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(Content.ME_FOLLOWINGS.uri)
                        .setAction(ApiSyncService.ACTION_PUSH);
                mContext.startService(intent);
                return subscription;
            }

            private void handleSyncResult(int resultCode, Observer<Void> observer) {
                log("handleSyncResult");
                switch (resultCode) {
                    case ApiSyncService.STATUS_SYNC_FINISHED:
                        log("Sync successful!");
                        observer.onCompleted();
                        break;

                    case ApiSyncService.STATUS_SYNC_ERROR:
                        //TODO: Proper Syncer error handling
                        observer.onError(new Exception("Sync failed"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unexpected syncer result code: " + resultCode);
                }
            }
        }));
    }


}
