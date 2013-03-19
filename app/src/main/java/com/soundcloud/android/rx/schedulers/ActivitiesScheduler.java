package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.rx.observers.DetachableObserver;
import rx.Observable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;


public class ActivitiesScheduler extends SyncingScheduler<Activities> {

    private final ActivitiesStorage mStorage;

    public ActivitiesScheduler(Context context) {
        super(context);
        ContentResolver resolver = context.getContentResolver();
        mStorage = new ActivitiesStorage(resolver);
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

    @Override
    public Observable<Activities> loadFromLocalStorage(Uri contentUri) {
        return loadActivitiesSince(contentUri, 0);
    }

    @Override
    public Observable<Activities> loadFromLocalStorage(long id) {
        throw new UnsupportedOperationException("Activities must still be loaded using content URIs");
    }

    @Override
    protected Activities emptyResult() {
        return Activities.EMPTY;
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
