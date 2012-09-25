package com.soundcloud.android.cache;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;

import android.database.Cursor;

public class UserCache extends LruCache<Long, User> {
    public UserCache() {
        super(200);
    }

    public User put(User u) {
        return u != null ? put(u.id, u) : null;
    }

    /*
    copy local fields to this *updated* track so they aren't overwritten
     */
    public User putWithLocalFields(User u) {
        if (u == null) return null;
        if (containsKey(u.id)) u.setAppFields(get(u.id));
        return put(u);
    }

}
