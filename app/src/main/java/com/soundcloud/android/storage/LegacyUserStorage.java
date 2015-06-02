package com.soundcloud.android.storage;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.net.Uri;

import javax.inject.Inject;

@Deprecated
public class LegacyUserStorage extends ScheduledOperations implements Storage<PublicApiUser> {
    private UserDAO userDAO;

    @Deprecated // use @Inject instead
    public LegacyUserStorage() {
        this(new UserDAO(SoundCloudApplication.instance.getContentResolver()));
    }

    @Inject
    public LegacyUserStorage(UserDAO userDAO) {
        this(userDAO, ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @VisibleForTesting
    LegacyUserStorage(UserDAO userDAO, Scheduler scheduler) {
        super(scheduler);
        this.userDAO = userDAO;
    }

    @Override
    public PublicApiUser store(PublicApiUser user) {
        userDAO.create(user.buildContentValues());
        return user;
    }

    public Observable<PublicApiUser> storeAsync(final PublicApiUser user) {
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiUser>() {
            @Override
            public void call(Subscriber<? super PublicApiUser> observer) {
                observer.onNext(store(user));
                observer.onCompleted();
            }
        }));
    }

    public PublicApiUser createOrUpdate(PublicApiUser user) {
        userDAO.createOrUpdate(user.getId(), user.buildContentValues());
        return user;
    }

    public Observable<PublicApiUser> getUserAsync(final long id) {
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiUser>() {
            @Override
            public void call(Subscriber<? super PublicApiUser> subscriber) {
                subscriber.onNext(getUser(id));
                subscriber.onCompleted();
            }
        }));
    }

    public PublicApiUser getUser(long id) {
        return userDAO.queryById(id);
    }

    public PublicApiUser getUserByUri(Uri uri) {
        return userDAO.queryByUri(uri);
    }

}
