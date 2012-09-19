package com.soundcloud.android.task.fetch;

import android.content.ContentResolver;
import android.util.Log;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;

public class FetchTrackTask extends FetchModelTask<Track> {

    public FetchTrackTask(AndroidCloudAPI app) {
        this(app, 0);
    }

    public FetchTrackTask(AndroidCloudAPI app, long trackId) {
        super(app, Track.class, trackId);
    }

    protected void updateLocally(ContentResolver resolver, Track track) {
        track.commitLocally(resolver,SoundCloudApplication.TRACK_CACHE);
    }

    public interface FetchTrackListener extends FetchModelListener<Track> {}
}
