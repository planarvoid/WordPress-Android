package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TRACK_CACHE;
import static com.soundcloud.android.SoundCloudApplication.USER_CACHE;

import com.soundcloud.android.SoundCloudApplication;

import android.database.Cursor;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class UserHolder extends CollectionHolder<User> {

    public UserHolder(List collection) {
        this.collection = collection;
    }

    public static UserHolder fromCursor(Cursor itemsCursor) {
        List<Parcelable> items = new ArrayList<Parcelable>();
        if (itemsCursor != null && itemsCursor.moveToFirst()) {
            do {
                final Parcelable t = USER_CACHE.fromCursor(itemsCursor);
                items.add(t);
            } while (itemsCursor.moveToNext());
        }
        return new UserHolder(items);
    }

    public void resolve(SoundCloudApplication application) {
        for (User u : collection) {
            u.resolve(application);
        }
    }
}
