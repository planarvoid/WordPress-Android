package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.net.Uri;

public class UserStorage extends ScheduledOperations implements Storage<User> {
    private UserDAO mUserDAO;

    public UserStorage() {
        ContentResolver resolver = SoundCloudApplication.instance.getContentResolver();
        mUserDAO = new UserDAO(resolver);
    }

    @Override
    public Observable<User> create(final User user) {
        return schedule(Observable.create(new Func1<Observer<User>, Subscription>() {
            @Override
            public Subscription call(Observer<User> observer) {
                mUserDAO.create(user.buildContentValues());
                observer.onNext(user);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public void createOrUpdate(User u) {
        mUserDAO.createOrUpdate(u.id, u.buildContentValues());
    }

    public User getUser(long id) {
        return mUserDAO.queryById(id);
    }

    public User getUserByUri(Uri uri) {
        return mUserDAO.queryByUri(uri);
    }

}
