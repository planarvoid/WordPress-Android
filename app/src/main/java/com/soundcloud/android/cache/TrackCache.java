package com.soundcloud.android.cache;

import com.soundcloud.android.model.Track;

public class TrackCache extends LruCache<Long, Track> {
    public TrackCache() {
        super(200);
    }

    public Track put(Track t) {
        return t != null ? put(t.id, t) : null;
    }
}
