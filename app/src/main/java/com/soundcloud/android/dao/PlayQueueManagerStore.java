package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.List;

public class PlayQueueManagerStore {
    private final ContentResolver mResolver;
    private final TrackDAO mTrackDAO;

    public PlayQueueManagerStore(Context context) {
        mResolver = context.getContentResolver();
        mTrackDAO = new TrackDAO(mResolver);
    }

    public void clearState() {
        mResolver.delete(Content.PLAY_QUEUE.uri, null, null);
    }

    public int getPlayQueuePositionFromUri(Uri collectionUri, long itemId) {
        Cursor cursor = mResolver.query(collectionUri,
                new String[]{ DBHelper.CollectionItems.POSITION },
                DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[] {String.valueOf(itemId)},
                null);

        int position = -1;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            position = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        return position;
    }

    public void insertQueue(List<Track> tracks, long userId) {
        mTrackDAO.createCollection(tracks);
        ContentValues[] contentValues = new ContentValues[tracks.size()];
        for (int i=0; i<tracks.size(); i++) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.PlayQueue.POSITION, i);
            cv.put(DBHelper.PlayQueue.TRACK_ID, tracks.get(i).getId());
            cv.put(DBHelper.CollectionItems.USER_ID, userId);

            contentValues[i] = cv;
        }
        mResolver.bulkInsert(Content.PLAY_QUEUE.uri, contentValues);
    }
}
