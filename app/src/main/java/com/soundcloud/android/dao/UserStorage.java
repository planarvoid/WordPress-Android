package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.EnumSet;

public class UserStorage implements Storage<User> {
    private UserDAO mUserDAO;
    private final ContentResolver mResolver;

    public UserStorage(Context context) {
        mResolver = context.getContentResolver();
        mUserDAO = new UserDAO(mResolver);
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
