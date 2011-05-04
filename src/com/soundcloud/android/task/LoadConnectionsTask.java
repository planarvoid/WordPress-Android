package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Connection> {
    public LoadConnectionsTask(AndroidCloudAPI api) {
        super(api);
    }

    protected List<Connection> doInBackground(Request... path) {
        return list(Request.to(Endpoints.MY_CONNECTIONS), Connection.class);
    }
}
