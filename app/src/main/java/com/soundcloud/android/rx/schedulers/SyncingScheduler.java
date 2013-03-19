package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.rx.ScObservables;
import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.service.sync.ApiSyncService;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

public abstract class SyncingScheduler<T> extends ReactiveScheduler<T> {

    private final LocalCollectionDAO mLocalCollectionsDao; //TODO: replace with storage facade

    public SyncingScheduler(Context context) {
        super(context);
        ContentResolver resolver = context.getContentResolver();
        mLocalCollectionsDao = new LocalCollectionDAO(resolver);
    }

    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri) {
        return syncIfNecessary(contentUri, syncNow(contentUri));
    }

    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri, final Observable<T> syncAction) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Observable<T>>() {
            @Override
            public void run(DetachableObserver<Observable<T>> observer) {
                LocalCollection mLocalCollection = mLocalCollectionsDao.fromContentUri(contentUri, true);
                boolean syncRequired;
                if (mLocalCollection == null) {
                    log("Skipping sync: local collection information missing");
                    syncRequired = false;
                } else {
                    syncRequired = mLocalCollection.shouldAutoRefresh();
                }
                log("Sync required: " + syncRequired);

                if (syncRequired) {
                    observer.onNext(syncAction);
                } else {
                    observer.onNext(ScObservables.EMPTY);
                }

                observer.onCompleted();
            }
        }));
    }

    public Observable<T> syncNow(final Uri contentUri) {
        return Observable.create(new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(final Observer<T> observer) {
                log("Requesting sync...");

                final BooleanSubscription subscription = new BooleanSubscription();

                final ResultReceiver receiver = new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (!subscription.isUnsubscribed()) {
                            handleSyncResult(resultCode, resultData, observer);
                        } else {
                            log("Not delivering results, was unsubscribed");
                        }
                    }
                };

                Intent intent = new Intent(mContext, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiver)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(contentUri);
                mContext.startService(intent);

                return subscription;
            }

            private void handleSyncResult(int resultCode, Bundle resultData, Observer<T> observer) {
                switch (resultCode) {
                    case ApiSyncService.STATUS_SYNC_FINISHED: {
                        final boolean dataChanged = resultData != null && resultData.getBoolean(contentUri.toString());
                        log("Sync successful; data changed: " + dataChanged);

                        if (dataChanged) {
                            T result = loadFromLocalStorage(contentUri).last();
                            observer.onNext(result);
                        } else {
                            observer.onNext(emptyResult());
                        }
                        observer.onCompleted();

                        break;
                    }
                    case ApiSyncService.STATUS_SYNC_ERROR:
                        //TODO: Proper Syncer error handling
                        observer.onError(new Exception("Sync failed"));
                        break;
                }
                observer.onCompleted();
            }
        });
    }

    @Deprecated
    public abstract Observable<T> loadFromLocalStorage(final Uri contentUri);

    protected abstract T emptyResult();
}
