package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.net.Uri;

import javax.inject.Inject;

public class UserStorage extends ScheduledOperations implements Storage<User> {
    private UserDAO mUserDAO;

    @Deprecated // use @Inject instead
    public UserStorage() {
        this(new UserDAO(SoundCloudApplication.instance.getContentResolver()));
    }

    @Inject
    public UserStorage(UserDAO userDAO) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        mUserDAO = userDAO;
    }

    @Override
    public User store(User user) {
        mUserDAO.create(user.buildContentValues());
        return user;
    }

    @Override
    public Observable<User> storeAsync(final User user) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<User>() {
            @Override
            public Subscription onSubscribe(Observer<? super User> observer) {
                observer.onNext(store(user));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public User createOrUpdate(User user) {
        mUserDAO.createOrUpdate(user.getId(), user.buildContentValues());
        return user;
    }

    public Observable<User> createOrUpdateAsync(final User user) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<User>() {
            @Override
            public Subscription onSubscribe(Observer<? super User> observer) {
                observer.onNext(createOrUpdate(user));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public User getUser(long id) {
        return mUserDAO.queryById(id);
    }

    public User getUserByUri(Uri uri) {
        return mUserDAO.queryByUri(uri);
    }

}
