package com.soundcloud.android.task.fetch;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Track;

public class FetchTrackTask extends FetchModelTask<Track> {
    public FetchTrackTask(AndroidCloudAPI api) {
        super(api);
    }

    public FetchTrackTask(AndroidCloudAPI api, long id) {
        super(api, id);
    }
}
