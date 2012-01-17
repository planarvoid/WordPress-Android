package com.soundcloud.android.cache;

import android.database.Cursor;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;

public class TrackCache extends LruCache<Long, Track> implements IResourceCache{
    public TrackCache() {
        super(200);
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

    @Override
    public Parcelable fromListItem(Parcelable listItem) {
        if (listItem instanceof TracklistItem){
            final TracklistItem t = (TracklistItem)listItem;
            final Track track = get(((TracklistItem)listItem).id);
            return track == null ? put(new Track(t)) : track.updateFromTracklistItem(t);
        } else {
            throw new IllegalArgumentException("Illegal param, tracklistitem required");
        }

    }

    public Parcelable fromCursor(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(DBHelper.Tracks._ID));
        Track track = get(id);
        if (track == null) {
            track = new Track(cursor);
            put(track);
        }
        return track;
    }
}
