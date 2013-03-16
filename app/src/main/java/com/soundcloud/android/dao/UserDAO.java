package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.EnumSet;

public class UserDAO extends BaseDAO<User> {

    protected UserDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public static User fromUri(Uri uri, ContentResolver resolver, boolean createDummy) {
        long id = -1l;
        try {
            //check the cache first
            id = Long.parseLong(uri.getLastPathSegment());
            final User u = SoundCloudApplication.MODEL_MANAGER.getCachedUser(id);
            if (u != null) return u;

        } catch (NumberFormatException e) {
            Log.e(UserBrowser.class.getSimpleName(), "Unexpected User uri: " + uri.toString());
        }

        Cursor cursor = resolver.query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return SoundCloudApplication.MODEL_MANAGER.getUserFromCursor(cursor);
            } else if (createDummy && id >= 0) {
                return new User(id);
            } else {
                return null;
            }

        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // TODO doesn't belong here
    public static void clearLoggedInUserFromStorage(ContentResolver resolver) {
        for (Content c : EnumSet.of(
                Content.ME_SOUNDS,
                Content.ME_LIKES,
                Content.ME_FOLLOWINGS,
                Content.ME_FOLLOWERS)) {
            resolver.delete(Content.COLLECTIONS.uri,
                    DBHelper.Collections.URI + " = ?", new String[]{ c.uri.toString() });
        }
    }

    @Override
    public Content getContent() {
        return Content.USERS;
    }
}
