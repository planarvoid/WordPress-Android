package com.soundcloud.android.task;

import android.net.Uri;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.objects.Connection;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

public class NewConnectionTask extends AsyncApiTask<Connection.Service, Void, Uri> {

    public static final String URL_SCHEME = "new-connection://";

    public NewConnectionTask(CloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Connection.Service... params) {
        Connection.Service svc = params[0];

        try {
            HttpResponse response = api().postContent(CONNECTIONS, params(
                    "service", svc.name,
                    "format",  "json",
                    "redirect_uri", URL_SCHEME+svc));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                JsonNode node = api().getMapper().readTree(response.getEntity().getContent());
                return Uri.parse(node.get("authorize_url").getTextValue());
            } else {
                warn("error creating connection", response);
                return null;
            }
        } catch (IOException e) {
            warn("IO error", e);
            return null;
        }
    }
}
