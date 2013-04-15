package com.soundcloud.android.rx.syncing;

import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;
import android.net.Uri;

import java.util.Collections;
import java.util.List;


public class LoadActivitiesStrategy implements SyncOperations.LocalStorageStrategy<Activity> {

    private final ActivitiesStorage mStorage;

    public LoadActivitiesStrategy(Context context) {
        mStorage = new ActivitiesStorage(context);
    }

    public Observable<Activity> loadActivitiesSince(final Uri contentUri, final long timestamp) {
        return Observable.create(new Func1<Observer<Activity>, Subscription>() {
            @Override
            public Subscription call(Observer<Activity> observer) {
                log("Loading activities since " + timestamp);
                List<Activity> result;
                // TODO: remove possibility of NULL, throw and propagate exception instead
                Activities activities = mStorage.getSince(contentUri, timestamp);
                if (activities == null) {
                    result = Collections.emptyList();
                } else {
                    result = activities.collection;
                }

                log("Found activities: " + result.size());

                for (Activity activity : result) {
                    observer.onNext(activity);
                }

                observer.onCompleted();

                return Subscriptions.empty();
            }
        });
    }

    public Observable<Activities> loadActivitiesBefore(final Uri contentUri, final long timestamp, final int limit) {
        return Observable.create(new Func1<Observer<Activities>, Subscription>() {
            @Override
            public Subscription call(Observer<Activities> observer) {
                // TODO: remove possibility of NULL, throw and propagate exception instead
                Activities activities = mStorage.getBefore(
                        contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(limit)).build(),
                        timestamp);

                observer.onNext(activities);
                observer.onCompleted();

                return Subscriptions.empty();
            }
        });
    }

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

    @Override
    public Observable<Activity> loadFromContentUri(Uri contentUri) {
        return loadActivitiesSince(contentUri, 0);
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
