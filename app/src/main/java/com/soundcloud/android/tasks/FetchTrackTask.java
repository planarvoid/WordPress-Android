package com.soundcloud.android.tasks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.model.Track;

public class FetchTrackTask extends FetchModelTask<Track> {
    public FetchTrackTask(PublicCloudAPI api) {
        super(api);
    }

    public FetchTrackTask(PublicCloudAPI api, long id) {
        super(api, id);
    }

    @Override
    protected void persist(Track track) {
        new TrackStorage().createOrUpdate(track);
    }
}
