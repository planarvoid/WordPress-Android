package com.soundcloud.android.cache;

import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;

import java.util.ArrayList;

public class TrackCache extends LruCache<Long, Track> {
    public TrackCache() {
        super(32);
    }

    public Track put(Track t) {
        return t != null ? put(t.id, t) : null;
    }

    public void addCommentToTrack(Comment comment) {
        final Track track = get(comment.track_id);
        if (track != null) {
            if (track.comments == null) track.comments = new ArrayList<Comment>();
            track.comments.add(comment);
        }
    }
}
