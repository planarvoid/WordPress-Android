package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Connection;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LoadConnectionsTask extends JsonLoadTask<Connection> {
    public LoadConnectionsTask(SoundCloudApplication app) {
        super(app);
    }

    protected List<Connection> doInBackground(HttpUriRequest... httpUriRequests) {
        try {
            InputStream is = httpGet("/me/connections.json");
            return new ObjectMapper().readValue(is,new TypeReference<List<Connection>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
