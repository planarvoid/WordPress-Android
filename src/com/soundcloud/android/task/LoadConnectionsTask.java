package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Connection> {
    public static final Request REQUEST = Request.to(Endpoints.MY_CONNECTIONS);

    public LoadConnectionsTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected List<Connection> doInBackground(Request... path) {
        return list(REQUEST, Connection.class);
    }
}
