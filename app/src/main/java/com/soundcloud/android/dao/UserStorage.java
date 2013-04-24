package com.soundcloud.android.dao;

import com.soundcloud.android.model.User;

import android.content.Context;
import android.net.Uri;

public class UserStorage implements Storage<User> {
    private UserDAO mUserDAO;

    public UserStorage(Context context) {
        mUserDAO = new UserDAO(context.getContentResolver());
    }

    @Override
    public void create(User resource) {
        mUserDAO.create(resource.buildContentValues());
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
