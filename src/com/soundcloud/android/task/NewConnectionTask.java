package com.soundcloud.android.task;

import android.net.Uri;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

public class NewConnectionTask extends AsyncApiTask<Connection.Service, Void, Uri> {
    public static final String URL_SCHEME = "new-connection://";

    public NewConnectionTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Connection.Service... params) {
        Connection.Service svc = params[0];

        try {
            HttpResponse response = mApi.post(Request.to(MY_CONNECTIONS).with(
                    "service", svc.name,
                    "format", "json",
                    "redirect_uri", URL_SCHEME + svc));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                JsonNode node = mApi.getMapper().readTree(response.getEntity().getContent());
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
