package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Endpoints;

import java.util.List;

public class LoadFriendsTask extends LoadJsonTask<User> {
    public LoadFriendsTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<User> doInBackground(String... path) {
        return list(Endpoints.MY_FRIENDS, User.class);
    }
}
