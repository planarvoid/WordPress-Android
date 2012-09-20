package com.soundcloud.android.cache;

import android.database.Cursor;
import android.os.Parcelable;

import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;

public class TrackCache extends LruCache<Long, Track> {
    public TrackCache() {
        super(200);
    }

    public Track put(Track t) {
        return put(t, true);
    }

    public Track put(Track t, boolean overwrite) {
        if (t != null && (!containsKey(t.id) || overwrite)){
            return put(t.id,t);
        }
        return null;
    }

    /*
    copy local fields to this *updated* track so they aren't overwritten
     */
    public Track putWithLocalFields(Track t) {
        if (t == null) return null;
        if (containsKey(t.id)) t.setAppFields(get(t.id));
        return put(t);
    }

    public void addCommentToTrack(Comment comment) {
        final Track track = get(comment.track_id);
        if (track != null) {
            if (track.comments == null) track.comments = new ArrayList<Comment>();
            track.comments.add(comment);
        }
    }

    public Track fromCursor(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(DBHelper.Tracks._ID));
        Track track = get(id);
        if (track == null) {
            track = new Track(cursor);
            put(track);
        }
        return track;
    }

    public Track fromActivityCursor(Cursor c) {
        final long id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.TRACK_ID));
        Track track = get(id);
        if (track == null) {
            track = new Track(c);
            put(track);
        }
        return track;
    }
}
