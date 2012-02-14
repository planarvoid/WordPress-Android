package com.soundcloud.android.task.fetch;

import android.content.ContentResolver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;

public class FetchTrackTask extends FetchModelTask<Track> {
    public FetchTrackTask(SoundCloudApplication app, long trackId) {
        super(app, Track.class, trackId);
    }

    protected void updateLocally(ContentResolver resolver, Track track) {
        track.last_updated = System.currentTimeMillis();
        track.full_track_info_loaded = true;
        SoundCloudApplication.TRACK_CACHE.putWithLocalFields(track);
        SoundCloudDB.upsertTrack(resolver, track);
    }

    public interface FetchTrackListener extends FetchModelListener<Track> {}
}
