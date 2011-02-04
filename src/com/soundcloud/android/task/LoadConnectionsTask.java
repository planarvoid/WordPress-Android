package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.objects.Connection;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LoadConnectionsTask extends JsonLoadTask<Connection> {
    public LoadConnectionsTask(CloudAPI api) {
        super(api);
    }

    protected List<Connection> doInBackground(String... path) {
        return list("/me/connections.json", Connection.class);
    }
}
