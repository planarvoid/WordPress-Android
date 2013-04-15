package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
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
public class SyncOperations<T> {

    private final Context mContext;
    private final LocalStorageStrategy<T> mStorageStrategy;
    private final LocalCollectionDAO mLocalCollectionsDao; //TODO: replace with storage facade

    private Scheduler mBackgroundScheduler = ScSchedulers.BACKGROUND_SCHEDULER;
    private Scheduler mUIScheduler = ScSchedulers.UI_SCHEDULER;

    public SyncOperations(Context context, LocalStorageStrategy<T> localStorageStrategy) {
        mContext = context.getApplicationContext();
        mStorageStrategy = localStorageStrategy;
        mLocalCollectionsDao = new LocalCollectionDAO(context.getContentResolver());
    }

    public SyncOperations(Context context, LocalStorageStrategy<T> localStorageStrategy, Scheduler backgroundScheduler) {
        this(context, localStorageStrategy);
        mBackgroundScheduler = backgroundScheduler;
    }

    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri) {
        return syncIfNecessary(contentUri, syncNow(contentUri));
    }

    /**
     * <p>Returns an observable that upon subscription determines whether a sync is due, and either emits the given sync
     * action if that's the case or if not, emits an observable that loads the same content from local storage using
     * the {@link LocalStorageStrategy} this class instance was configured with.</p>
     * <p>
     * The returned observable is guaranteed to execute on a background thread.
     * </p>
     * @param contentUri the URI pointing to the content to either sync or load from local storage
     * @param syncAction the action to emit when a sync is necessary
     * @return the observable
     */
    public Observable<Observable<T>> syncIfNecessary(final Uri contentUri, final Observable<T> syncAction) {
        return Observable.create(new Func1<Observer<Observable<T>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<T>> observer) {
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
                    observer.onNext(loadFromLocalStorage(contentUri));
                }

                observer.onCompleted();

                return Subscriptions.empty();
            }
        }).subscribeOn(mBackgroundScheduler);
    }

    /**
     * <p>Returns an observable which upon subscription will initiate a sync for the given content URI.</p>
     * <p>This method is safe to call from any thread</p>
     * @param contentUri the content URI for which to initiate a sync
     * @return the observable
     */
    public Observable<T> syncNow(final Uri contentUri) {
        return Observable.create(new Func1<Observer<T>, Subscription>() {
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
                        loadFromLocalStorage(contentUri).subscribe(observer);
                        break;
                    }
                    case ApiSyncService.STATUS_SYNC_ERROR:
                        //TODO: Proper Syncer error handling
                        observer.onError(new Exception("Sync failed"));
                        break;
                }
            }
        });
    }

    private Observable<T> loadFromLocalStorage(final Uri contentUri) {
        return mStorageStrategy.loadFromContentUri(contentUri)
                .subscribeOn(mBackgroundScheduler)
                .observeOn(mUIScheduler);
    }

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

    public static interface LocalStorageStrategy<T> {
        @Deprecated
        Observable<T> loadFromContentUri(final Uri contentUri);
    }
}
