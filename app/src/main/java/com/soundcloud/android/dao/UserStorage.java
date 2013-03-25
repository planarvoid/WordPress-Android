package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.User;

public class UserStorage {
    private UserDAO mUserDAO;
    private final ContentResolver mResolver;

    public UserStorage(Context context) {
        mResolver = context.getContentResolver();
        mUserDAO = new UserDAO(mResolver);
    }

    public void createOrUpdate(User u) {
       mUserDAO.createOrUpdate(u.id, u.buildContentValues());
    }

    public User getUser(long id) {
        return mUserDAO.queryForId(id);
    }

    public User getUserByUri(Uri uri) {
        return mUserDAO.queryForUri(uri);
    }
}
