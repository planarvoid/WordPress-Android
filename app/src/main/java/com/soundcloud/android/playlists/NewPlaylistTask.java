package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.AsyncApiTask;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.Request;

import java.io.IOException;

public class NewPlaylistTask extends AsyncApiTask<Request, Void, PublicApiPlaylist> {
    public NewPlaylistTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected PublicApiPlaylist doInBackground(Request... params) {
        Request request = params[0];

        try {
            return api.create(request);
        } catch (IOException e) {
            warn("IO error", e);
            return null;
        }
    }
}