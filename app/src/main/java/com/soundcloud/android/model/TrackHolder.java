package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TRACK_CACHE;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class TrackHolder extends CollectionHolder<Track> {

    public TrackHolder(List<Track> collection) {
        this.collection = collection;
    }

    public static TrackHolder fromCursor(Cursor itemsCursor) {
        List<Track> items = new ArrayList<Track>();
        if (itemsCursor != null && itemsCursor.moveToFirst()) {
            do {
                final Track t = TRACK_CACHE.fromCursor(itemsCursor);
                items.add(t);
            } while (itemsCursor.moveToNext());
        }
        return new TrackHolder(items);
    }

    public void resolve(Context context) {
        for (Track t : collection){
            t.resolve(context);
        }
    }
}
