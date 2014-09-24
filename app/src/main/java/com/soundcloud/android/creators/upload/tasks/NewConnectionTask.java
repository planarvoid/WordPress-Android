package com.soundcloud.android.creators.upload.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.soundcloud.android.api.legacy.AsyncApiTask;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.Connection;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.net.Uri;

import java.io.IOException;

public class NewConnectionTask extends AsyncApiTask<Connection.Service, Void, Uri> {
    public static final String URL_SCHEME = "new-connection://";

    public NewConnectionTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Connection.Service... params) {
        Connection.Service svc = params[0];

        try {
            HttpResponse response = api.post(Request.to(MY_CONNECTIONS).with(
                    "service", svc.name,
                    "format", "json",
                    "redirect_uri", URL_SCHEME + svc));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                JsonNode node = api.getMapper().readTree(response.getEntity().getContent());
                return Uri.parse(node.get("authorize_url").asText());
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
