package com.soundcloud.android.dao;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;

public class UserDAO extends BaseDAO<User> {

    public UserDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.USERS;
    }
}
