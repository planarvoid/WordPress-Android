package com.soundcloud.android.task.fetch;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.User;

public class FetchUserTask extends FetchModelTask<User> {
    public FetchUserTask(AndroidCloudAPI app) {
        super(app);
    }
}
