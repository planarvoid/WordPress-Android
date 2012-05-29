package com.soundcloud.android.cache;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.android.provider.DBHelper;

import android.database.Cursor;
import android.os.Parcelable;

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

    public Parcelable fromListItem(Parcelable listItem) {
        if (listItem instanceof UserlistItem){
            final UserlistItem u = (UserlistItem)listItem;
            User user = get(((UserlistItem) listItem).id);
            if (user == null) {
                user = new User(u);
                put(user);
            } else {
                user.updateFrom((ScModel) listItem);
            }
            return user;
        } else {
            throw new IllegalArgumentException("Illegal param, tracklistitem required");
        }
    }

    public Parcelable fromCursor(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(DBHelper.Users._ID));
        User user = get(id);
        if (user == null) {
            user = new User(cursor);
            put(user);
        }
        return user;
    }

    public User fromTrackView(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackView.USER_ID));
        User user = get(id);
        if (user == null) {
            user = User.fromTrackView(cursor);
            put(user);
        }
        return user;
    }

    public User assertCached(User user) {
        // todo, update stale cache objects...
        User stored = get(user.id);
        if (stored == null) {
            put(user);
            return user;
        } else {
            stored.updateFrom(user);
            return stored;
        }
    }
}
