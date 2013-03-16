package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;

public class TrackDAO extends BaseDAO<Track> {

    protected TrackDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public static Uri insert(Track track, ContentResolver contentResolver) {
        track.insertDependencies(contentResolver);
        return contentResolver.insert(track.toUri(), track.buildContentValues());
    }


    public static Track fromUri(Uri uri, ContentResolver resolver, boolean createDummy) {
        long id = -1l;
        try { // check the cache first
            id = Long.parseLong(uri.getLastPathSegment());
            final Track t = SoundCloudApplication.MODEL_MANAGER.getCachedTrack(id);
            if (t != null) return t;

        } catch (NumberFormatException e) {
            Log.e(UserBrowser.class.getSimpleName(), "Unexpected Track uri: " + uri.toString());
        }

        Cursor cursor = resolver.query(uri, null, null, null, null);
        try {

            if (cursor != null && cursor.moveToFirst()) {
                return SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor);
            } else if (createDummy && id >= 0) {
                return SoundCloudApplication.MODEL_MANAGER.cache(new Track(id));
            } else {
                return null;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public Content getContent() {
        return Content.TRACKS;
    }
}
