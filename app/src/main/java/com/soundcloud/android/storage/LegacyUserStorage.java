package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

@Deprecated
public class LegacyUserStorage {
    private UserDAO userDAO;
    private final Scheduler scheduler;

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
        this.userDAO = userDAO;
        this.scheduler = scheduler;
    }

    public PublicApiUser createOrUpdate(PublicApiUser user) {
        userDAO.createOrUpdate(user.getId(), user.buildContentValues());
        return user;
    }

    public Observable<PublicApiUser> getUserAsync(final long id) {
        return Observable.create(new Observable.OnSubscribe<PublicApiUser>() {
            @Override
            public void call(Subscriber<? super PublicApiUser> subscriber) {
                subscriber.onNext(getUser(id));
                subscriber.onCompleted();
            }
        }).subscribeOn(scheduler);
    }

    public PublicApiUser getUser(long id) {
        return userDAO.queryById(id);
    }
}
