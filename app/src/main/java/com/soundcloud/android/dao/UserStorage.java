package com.soundcloud.android.dao;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.util.EnumSet;

public class UserStorage extends ScheduledOperations implements Storage<User> {
    private UserDAO mUserDAO;
    private final ContentResolver mResolver;

    public UserStorage(Context context) {
        mResolver = context.getContentResolver();
        mUserDAO = new UserDAO(mResolver);
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

    public void clearLoggedInUser() {
        for (Content c : EnumSet.of(
                Content.ME_SOUNDS,
                Content.ME_LIKES,
                Content.ME_FOLLOWINGS,
                Content.ME_FOLLOWERS)) {
            mResolver.delete(Content.COLLECTIONS.uri,
                    DBHelper.Collections.URI + " = ?", new String[]{c.uri.toString()});
        }
    }

}
