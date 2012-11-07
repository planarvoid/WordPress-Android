package com.soundcloud.android.cache;

import android.database.Cursor;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.DBHelper;

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

    public Track fromActivityCursor(Cursor c) {
        final long id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.SOUND_ID));
        Track track = get(id);
        if (track == null) {
            track = SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(c);
            put(track);
        }
        return track;
    }
}
