package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.objects.Connection;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;

public class LoadConnectionsTask extends LoadJsonTask<Connection> {
    public LoadConnectionsTask(CloudAPI api) {
        super(api);
    }

    protected List<Connection> doInBackground(String... path) {
        return list(Connection.REQUEST, Connection.class);
    }
}
