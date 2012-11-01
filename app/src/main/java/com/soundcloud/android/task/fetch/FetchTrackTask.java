package com.soundcloud.android.task.fetch;

import android.content.ContentResolver;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Model;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;

public class FetchTrackTask extends FetchModelTask<Track> {

    public FetchTrackTask(AndroidCloudAPI app) {
        this(app, 0);
    }

    public FetchTrackTask(AndroidCloudAPI app, long trackId) {
        super(app, Track.class, trackId);
    }

    public interface FetchTrackListener extends FetchModelListener<Track> {}
}
