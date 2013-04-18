package com.soundcloud.android.rx.syncing;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.ApiSyncService;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

/**
 * Use this class whenever you need to coordinate sync operations for some content URI. More specifically, you can
 * obtain observable instances which will ensure that the proper actions are performed based on whether a sync is
 * actually necessary or not, and that these actions are executed on an appropriate thread.
 */
public class SyncOperations<T> extends ScheduledOperations {

    private final Context mContext;
    private final Observable<T> mLoadFromLocalStorage;
    private final LocalCollectionDAO mLocalCollectionsDao; //TODO: replace with storage facade

    public SyncOperations(Context context, Observable<T> loadFromLocalStorage) {
        mContext = context.getApplicationContext();
        mLoadFromLocalStorage = loadFromLocalStorage;
        mLocalCollectionsDao = new LocalCollectionDAO(context.getContentResolver());
    }

    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri) {
        return syncIfNecessary(contentUri, syncNow(contentUri));
    }

    /**
     * <p>Returns an observable that upon subscription determines whether a sync is due, and either emits the given sync
     * action if that's the case or if not, emits an observable that loads the same content from local storage.</p>
     * <p>
     * The returned observable is guaranteed to execute on a background thread.
     * </p>
     *
     * @param contentUri the URI pointing to the content to either sync or load from local storage
     * @param syncAction the action to emit when a sync is necessary
     * @return the observable
     */
    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri, final Observable<T> syncAction) {
        return schedule(Observable.create(new Func1<Observer<Observable<T>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<T>> observer) {
                log("syncIfNecessary");
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
                    // emit the observable that performs the actual sync
                    observer.onNext(syncAction);
                } else {
                    // emit the observable which simply loads what we have from local storage
                    observer.onNext(mLoadFromLocalStorage);
                }

                observer.onCompleted();

                return Subscriptions.empty();
            }
        }));
    }

    /**
     * <p>Returns an observable which upon subscription will initiate a sync for the given content URI.</p>
     * <p>This method is safe to call from any thread</p>
     *
     * @param contentUri the content URI for which to initiate a sync
     * @return the observable
     */
    public Observable<T> syncNow(final Uri contentUri) {
        return schedule(Observable.create(new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(final Observer<T> observer) {
                log("Requesting sync...");

                final BooleanSubscription subscription = new BooleanSubscription();

                // make sure the result receiver is invoked on the main thread, that's because after a sync error
                // we call through to the observer directly rather than going through a scheduler
                final ResultReceiver receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
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
                log("handleSyncResult");
                switch (resultCode) {
                    case ApiSyncService.STATUS_SYNC_FINISHED: {
                        log("Sync successful!");
                        mLoadFromLocalStorage.subscribe(observer);
                        break;
                    }
                    case ApiSyncService.STATUS_SYNC_ERROR:
                        //TODO: Proper Syncer error handling
                        observer.onError(new Exception("Sync failed"));
                        break;
                }
            }
        }));
    }
}
