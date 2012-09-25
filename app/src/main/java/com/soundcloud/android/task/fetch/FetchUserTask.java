package com.soundcloud.android.task.fetch;

import android.content.ContentResolver;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;

public class FetchUserTask extends FetchModelTask<User> {
    public FetchUserTask(AndroidCloudAPI app) {
        this(app, -1);
    }

    public FetchUserTask(AndroidCloudAPI app, long userId) {
        super(app, User.class, userId);
    }

    protected void updateLocally(ContentResolver resolver, User user) {
        user.last_updated = System.currentTimeMillis();
        SoundCloudApplication.MODEL_MANAGER.write(user);
    }

    public interface FetchUserListener extends FetchModelListener<User> {}
}
