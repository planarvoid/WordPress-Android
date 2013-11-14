package com.soundcloud.android.playlists;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.api.AsyncApiTask;
import com.soundcloud.api.Request;

import java.io.IOException;

public class NewPlaylistTask extends AsyncApiTask<Request, Void, Playlist> {
    public NewPlaylistTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected Playlist doInBackground(Request... params) {
        Request request = params[0];

        try {
            return mApi.create(request);
        } catch (IOException e) {
            warn("IO error", e);
            return null;
        }
    }
}