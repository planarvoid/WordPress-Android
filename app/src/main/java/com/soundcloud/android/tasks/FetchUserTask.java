package com.soundcloud.android.tasks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.model.User;

public class FetchUserTask extends FetchModelTask<User> {
    public FetchUserTask(PublicCloudAPI app) {
        super(app);
    }

    @Override
    protected void persist(User user) {
        new UserStorage().createOrUpdate(user);
    }
}
