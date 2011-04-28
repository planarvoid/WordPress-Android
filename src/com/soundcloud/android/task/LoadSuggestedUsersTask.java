package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Endpoints;

import java.util.List;

public class LoadSuggestedUsersTask extends LoadJsonTask<User> {
    public LoadSuggestedUsersTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<User> doInBackground(String... path) {
        return list(Endpoints.SUGGESTED_USERS, User.class);
    }
}
