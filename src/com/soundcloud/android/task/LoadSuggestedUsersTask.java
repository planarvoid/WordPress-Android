package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadSuggestedUsersTask extends LoadJsonTask<Connection> {
    public LoadSuggestedUsersTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<Connection> doInBackground(Request... path) {
        return list(Request.to(Endpoints.SUGGESTED_USERS), Connection.class);
    }
}
