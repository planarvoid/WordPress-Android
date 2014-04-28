package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Subscriber;

import android.net.Uri;

import javax.inject.Inject;

public class UserStorage extends ScheduledOperations implements Storage<User> {
    private UserDAO userDAO;

    @Deprecated // use @Inject instead
    public UserStorage() {
        this(new UserDAO(SoundCloudApplication.instance.getContentResolver()));
    }

    @Inject
    public UserStorage(UserDAO userDAO) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        this.userDAO = userDAO;
    }

    @Override
    public User store(User user) {
        userDAO.create(user.buildContentValues());
        return user;
    }

    @Override
    public Observable<User> storeAsync(final User user) {
        return schedule(Observable.create(new Observable.OnSubscribe<User>() {
            @Override
            public void call(Subscriber<? super User> observer) {
                observer.onNext(store(user));
                observer.onCompleted();
            }
        }));
    }

    public User createOrUpdate(User user) {
        userDAO.createOrUpdate(user.getId(), user.buildContentValues());
        return user;
    }

    public Observable<User> createOrUpdateAsync(final User user) {
        return schedule(Observable.create(new Observable.OnSubscribe<User>() {
            @Override
            public void call(Subscriber<? super User> observer) {
                observer.onNext(createOrUpdate(user));
                observer.onCompleted();
            }
        }));
    }

    public User getUser(long id) {
        return userDAO.queryById(id);
    }

    public User getUserByUri(Uri uri) {
        return userDAO.queryByUri(uri);
    }

}
