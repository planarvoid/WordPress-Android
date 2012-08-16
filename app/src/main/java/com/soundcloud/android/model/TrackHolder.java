package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TRACK_CACHE;

import com.soundcloud.android.SoundCloudApplication;

import android.database.Cursor;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TrackHolder extends CollectionHolder<Track> {

    public TrackHolder(List collection) {
        this.collection = collection;
    }

    public static TrackHolder fromCursor(Cursor itemsCursor) {
        List<Parcelable> items = new ArrayList<Parcelable>();
        if (itemsCursor != null && itemsCursor.moveToFirst()) {
            do {
                final Parcelable t = TRACK_CACHE.fromCursor(itemsCursor);
                items.add(t);
            } while (itemsCursor.moveToNext());
        }
        return new TrackHolder(items);
    }

    public void resolve(SoundCloudApplication application) {
        for (Track t : collection){
            t.resolve(application);
        }
    }
}
