package com.soundcloud.android.tasks;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.LegacyUserStorage;

public class FetchUserTask extends FetchModelTask<PublicApiUser> {
    public FetchUserTask(PublicCloudAPI app) {
        super(app);
    }

    @Override
    protected void persist(PublicApiUser user) {
        new LegacyUserStorage().createOrUpdate(user);
    }
}
