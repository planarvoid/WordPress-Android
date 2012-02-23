package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Void, Connection> {
    public static final Request REQUEST = Request.to(Endpoints.MY_CONNECTIONS);

    public LoadConnectionsTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<Connection> doInBackground(Void... params) {
        return list(REQUEST, Connection.class);
    }
}
