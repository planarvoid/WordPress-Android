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

    public UserDAO(ContentResolver contentResolver) {
        super(contentResolver);
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
