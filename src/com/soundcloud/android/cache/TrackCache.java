package com.soundcloud.android.cache;

import android.database.Cursor;
import android.os.Parcelable;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Origin;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;

public class TrackCache extends LruCache<Long, Track> {
    public TrackCache() {
        super(200);
    }

    public Track put(Track t) {
        return t != null ? put(t.id, t) : null;
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

    public Parcelable fromListItem(Parcelable listItem) {
        if (listItem instanceof TracklistItem){
            final TracklistItem t = (TracklistItem)listItem;
            Track track = get(((TracklistItem) listItem).id);
            if (track == null) {
                track = new Track(t);
                put(track);
            } else {
                track.updateFromTracklistItem((TracklistItem) listItem);
            }
            return track;
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
