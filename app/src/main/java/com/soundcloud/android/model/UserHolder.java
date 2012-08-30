package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.USER_CACHE;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class UserHolder extends CollectionHolder<User> {

    public UserHolder(List<User> collection) {
        this.collection = collection;
    }

    public static UserHolder fromCursor(Cursor itemsCursor) {
        List<User> items = new ArrayList<User>();
        if (itemsCursor != null && itemsCursor.moveToFirst()) {
            do {
                final User t = USER_CACHE.fromCursor(itemsCursor);
                items.add(t);
            } while (itemsCursor.moveToNext());
        }
        return new UserHolder(items);
    }

    public void resolve(Context context) {
        for (User u : collection) {
            u.resolve(context);
        }
    }
}
