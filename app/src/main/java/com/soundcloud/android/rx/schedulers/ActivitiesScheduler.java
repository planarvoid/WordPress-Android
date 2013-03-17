package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.rx.ScObservables;
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


public class ActivitiesScheduler extends ReactiveScheduler<Activities> {

    private final ActivitiesStorage mStorage;
    private final LocalCollectionDAO mLocalCollectionsDao; //TODO: replace with storage facade

    public ActivitiesScheduler(Context context) {
        super(context);
        ContentResolver resolver = context.getContentResolver();
        mStorage = new ActivitiesStorage(resolver);
        mLocalCollectionsDao = new LocalCollectionDAO(resolver);
    }

    public Observable<Activities> loadActivitiesSince(final Uri contentUri, final long timestamp) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Activities>() {
            @Override
            protected void run(DetachableObserver<Activities> observer) {
                log("Loading activities since " + timestamp);
                // TODO: remove possibility of NULL, throw and propagate exception instead
                Activities activities = mStorage.getSince(contentUri, timestamp);
                if (activities == null) {
                    activities = new Activities();
                }
                for (Activity a : activities) {
                    a.resolve(mContext);
                }

                log("Found activities: " + activities.size());

                observer.onNext(activities);
                observer.onCompleted();
            }
        }));
    }

    public Observable<Activities> loadActivitiesBefore(final Uri contentUri, final long timestamp, final int limit) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Activities>() {
            @Override
            protected void run(DetachableObserver<Activities> observer) {
                // TODO: remove possibility of NULL, throw and propagate exception instead
                Activities activities = mStorage.getBefore(
                        contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(limit)).build(),
                        timestamp);

                observer.onNext(activities);
                observer.onCompleted();
            }
        }));
    }

    public Observable<Observable<Activities>> syncIfNecessary(final Uri contentUri) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Observable<Activities>>() {
            @Override
            public void run(DetachableObserver<Observable<Activities>> observer) {
                LocalCollection mLocalCollection = mLocalCollectionsDao.fromContentUri(contentUri, true);
                boolean syncRequired;
                if (mLocalCollection == null) {
                    log("Skipping sync: local collection information missing");
                    syncRequired = false;
                } else {
                    syncRequired = mLocalCollection.shouldAutoRefresh();
                }
                log("Sync required: " + syncRequired);

                if (true) {
                    observer.onNext(syncActivities(contentUri));
                } else {
                    observer.onNext(ScObservables.EMPTY);
                }

                observer.onCompleted();
            }
        }));
    }

    public Observable<Activities> syncActivities(final Uri contentUri) {
        return Observable.create(new Func1<Observer<Activities>, Subscription>() {
            @Override
            public Subscription call(final Observer<Activities> observer) {
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

            private void handleSyncResult(int resultCode, Bundle resultData, Observer<Activities> observer) {
                switch (resultCode) {
                    case ApiSyncService.STATUS_SYNC_FINISHED: {
                        final boolean dataChanged = resultData != null && resultData.getBoolean(contentUri.toString());
                        log("Sync successful; data changed: " + dataChanged);

                        Activities activities = loadActivitiesSince(contentUri, 0).last();
                        observer.onNext(activities);
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

//    public ScObservables.ConditionalObservable<Activities> pagingRequest(final Uri contentUri, final long since, final int limit) {
//        ScObservables.ConditionalObservable<Activities> wrapper = new ScObservables.ConditionalObservable<Activities>();
//        wrapper.observable = Observable.create(newBackgroundJob(new ObservedRunnable<Activities>() {
//            @Override
//            protected void run(DetachableObserver<Activities> observer) {
//                log("Running sync...");
//                Activities newActivities;
//                newActivities = getOlderActivities(contentUri, limit, since);
//                if (newActivities.size() < limit) {
//
//                    ApiSyncer.Result result = null;
//                    try {
//                        result = new ApiSyncer(mContext).syncContent(contentUri, ApiSyncService.ACTION_APPEND);
//                    } catch (CloudAPI.InvalidTokenException e) {
//                        // TODO, move this once we centralize our error handling
//                        // InvalidTokenException should expose the response code so we don't have to hardcode it here
//                        //returnData.responseCode = HttpStatus.SC_UNAUTHORIZED;
//                        //returnData.success = false;
//                        observer.onError(e);
//                    } catch (IOException e) {
//                        //Log.w(SoundCloudApplication.TAG, e);
//                        //returnData.success = false;
//                        observer.onError(e);
//                    }
//
//                    if (result != null && result.success) {
//                        newActivities = getOlderActivities(contentUri, limit, since);
//                    }
//
//                    log("Done, new records: " + newActivities.size());
//
//                    observer.onNext(newActivities);
//                } else {
//                    log("No sync necessary; found/limit = " + newActivities.size() + "/" + limit);
//                }
//                observer.onCompleted();
//            }
//        }));
//
//        wrapper.condition = new Func0<Boolean>() {
//            @Override
//            public Boolean call() {
//                return true;
//            }
//        };
//
//        return wrapper;
//    }
//
//    private Activities getOlderActivities(final Uri contentUri, final int limit, final long since) {
//        return ActivitiesDAO.getBefore(
//                contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(limit)).build(),
//                mResolver,
//                since);
//    }

}
