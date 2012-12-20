package com.soundcloud.android.cache;

import com.soundcloud.android.model.User;

public class UserCache extends LruCache<Long, User> {
    public UserCache() {
        super(200);
    }

    public User put(User u) {
        return u != null ? put(u.id, u) : null;
    }
}
