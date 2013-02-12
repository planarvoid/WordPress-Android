package com.soundcloud.android.task.create;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class NewPlaylistTask extends AsyncApiTask<Request, Void, Playlist> {
    public NewPlaylistTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Playlist doInBackground(Request... params) {
        Request request = params[0];

        try {
            HttpResponse response = mApi.post(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                return SoundCloudApplication.MODEL_MANAGER.getModelFromStream(response.getEntity().getContent());
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