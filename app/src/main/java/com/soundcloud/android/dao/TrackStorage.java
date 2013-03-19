package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

public class TrackStorage {
    private TrackDAO mTrackDAO;
    private final ContentResolver mResolver;


    public TrackStorage(ContentResolver resolver) {
        mTrackDAO = new TrackDAO(resolver);
        mResolver = resolver;
    }


    public boolean markTrackAsPlayed(Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.id);
        return mResolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    public Uri create(Track track) {
        return Content.TRACKS.forId(mTrackDAO.create(track));
    }

    public long createOrUpdate(Track track) {
        return mTrackDAO.createOrUpdate(track);
    }

    public Track getTrack(long id) {
        return mTrackDAO.queryForId(id);
    }
}


