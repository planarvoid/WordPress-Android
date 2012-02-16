package com.soundcloud.android.task.fetch;

import android.content.ContentResolver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;

public class FetchUserTask extends FetchModelTask<User> {
    public FetchUserTask(SoundCloudApplication app, long userId) {
        super(app, User.class, userId);
    }

    protected void updateLocally(ContentResolver resolver, User user) {
        user.last_updated = System.currentTimeMillis();
        SoundCloudDB.upsertUser(resolver, user);
        SoundCloudApplication.USER_CACHE.putWithLocalFields(user);
    }

    public interface FetchUserListener extends FetchModelListener<User> {}
}
