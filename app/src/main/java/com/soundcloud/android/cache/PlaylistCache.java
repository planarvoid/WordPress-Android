package com.soundcloud.android.cache;

import com.soundcloud.android.model.Playlist;

public class PlaylistCache extends LruCache<Long, Playlist> {
    public PlaylistCache() {
        super(200);
    }

    public Playlist put(Playlist p) {
        return p != null ? put(p.id, p) : null;
    }
}
