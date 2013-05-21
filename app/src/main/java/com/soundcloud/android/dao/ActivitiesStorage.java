package com.soundcloud.android.dao;

import android.content.Context;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.net.Uri;

public class ActivitiesStorage extends ScheduledOperations {
    private SyncStateManager mSyncStateManager;
    private ActivityDAO mActivitiesDAO;
    private final ContentResolver mResolver;

    public ActivitiesStorage() {
        this(SoundCloudApplication.instance);
    }

    public ActivitiesStorage(Context context) {
        mResolver = context.getContentResolver();
        mSyncStateManager = new SyncStateManager(context);
        mActivitiesDAO = new ActivityDAO(mResolver);
    }

    public Observable<Activities> getCollectionSince(final Uri contentUri, final long since, final int limit)  {
        return schedule(Observable.create(new Func1<Observer<Activities>, Subscription>() {
            @Override
            public Subscription call(Observer<Activities> observer) {
                log("get activities " + contentUri + ", since=" + since);

                Activities activities = new Activities();
                LocalCollection lc = mSyncStateManager.fromContent(contentUri);
                activities.future_href = lc.extra;

                BaseDAO.QueryBuilder query = mActivitiesDAO.buildQuery(contentUri);
                if (since > 0) {
                    query.where(DBHelper.ActivityView.CREATED_AT + "> ?", String.valueOf(since));
                }
                if (limit > 0) {
                    query.limit(limit);
                }

                activities.collection = query.queryAll();
                observer.onNext(activities);
                observer.onCompleted();

                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Activities> getCollectionSince(final Uri contentUri, final long since)  {
        return getCollectionSince(contentUri, since, 0);
    }

    public Observable<Activity> getLatestActivities(final Uri contentUri, final int limit)  {
        return getCollectionSince(contentUri, 0, limit).mapMany(new Func1<Activities, Observable<Activity>>() {
            @Override
            public Observable<Activity> call(final Activities activities) {
                return Observable.from(activities.collection);
            }
        });
    }

    public Observable<Activity> getOldestActivity(final Content content) {
        return schedule(Observable.create(new Func1<Observer<Activity>, Subscription>() {
            @Override
            public Subscription call(Observer<Activity> activityObserver) {
                Activity activity = mActivitiesDAO.buildQuery(content.uri)
                        .where(DBHelper.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                        .order(DBHelper.ActivityView.CREATED_AT + " ASC")
                        .first();
                if (activity != null) {
                    activityObserver.onNext(activity);
                }
                activityObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Activity> getLatestActivity(final Content content) {
        return schedule(Observable.create(new Func1<Observer<Activity>, Subscription>() {
            @Override
            public Subscription call(Observer<Activity> activityObserver) {
                Activity activity = mActivitiesDAO.buildQuery(content.uri)
                        .where(DBHelper.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                        .order(DBHelper.ActivityView.CREATED_AT + " DESC")
                        .first();
                if (activity != null) {
                    activityObserver.onNext(activity);
                }
                activityObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Activities> getCollectionBefore(final Uri contentUri, final long before)  {
        return schedule(Observable.create(new Func1<Observer<Activities>, Subscription>() {
            @Override
            public Subscription call(Observer<Activities> observer) {
                log("get activities " + contentUri + ", before=" + before);

                BaseDAO.QueryBuilder query = mActivitiesDAO.buildQuery(contentUri);
                if (before > 0) {
                    query.where(DBHelper.ActivityView.CREATED_AT + "< ?", String.valueOf(before));
                }

                Activities activities = new Activities();
                activities.collection = query.queryAll();
                observer.onNext(activities);
                observer.onCompleted();

                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Activity> getActivitiesBefore(final Uri contentUri, final long since)  {
        return getCollectionBefore(contentUri, since).mapMany(new Func1<Activities, Observable<Activity>>() {
            @Override
            public Observable<Activity> call(final Activities activities) {
                return Observable.from(activities.collection);
            }
        });
    }

    public int getCountSince(long since, Content content) {
        String selection = DBHelper.ActivityView.CONTENT_ID + " = ? AND " + DBHelper.ActivityView.CREATED_AT + "> ?";
        return mActivitiesDAO.count(selection, String.valueOf(content.id), String.valueOf(since));
    }

    public int clear(@Nullable Content content) {
        Content contentToDelete = Content.ME_ALL_ACTIVITIES;
        if (content != null) {
            contentToDelete = content;
        }
        if (!Activity.class.isAssignableFrom(contentToDelete.modelType)) {
            throw new IllegalArgumentException("specified content is not an activity");
        }
        // make sure to delete corresponding collection
        if (contentToDelete == Content.ME_ALL_ACTIVITIES) {

            for (Content c : Content.ACTIVITIES) {
                mSyncStateManager.delete(c);
            }
        } else {
            mSyncStateManager.delete(contentToDelete);
        }
        return mResolver.delete(contentToDelete.uri, null, null);
    }

    public int insert(Content content, Activities activities) {
        return mActivitiesDAO.insert(content, activities);
    }

}
