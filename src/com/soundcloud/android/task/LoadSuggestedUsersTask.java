package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadSuggestedUsersTask extends LoadJsonTask<Void, Connection> {
    public LoadSuggestedUsersTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<Connection> doInBackground(Void... unused) {
        return list(Request.to(Endpoints.SUGGESTED_USERS), Connection.class);
    }
}
