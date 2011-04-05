package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.android.objects.Connection;

import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Connection> {
    public LoadConnectionsTask(AndroidCloudAPI api) {
        super(api);
    }

    protected List<Connection> doInBackground(String... path) {
        return list(CloudAPI.Enddpoints.CONNECTIONS, Connection.class);
    }
}
