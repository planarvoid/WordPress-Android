package com.soundcloud.android.dao;

import android.content.ContentResolver;
import com.soundcloud.android.model.User;

public class UserStorage {

    private UserDAO mUserDAO;
    private final ContentResolver mResolver;


    public UserStorage(ContentResolver resolver) {
        mResolver = resolver;
        mUserDAO = new UserDAO(resolver);
    }

    public void createOrUpdate(User u) {
       mUserDAO.createOrUpdate(u.id, u.buildContentValues());
    }

    public User getUser(long id) {
        return mUserDAO.queryForId(id);
    }
}
