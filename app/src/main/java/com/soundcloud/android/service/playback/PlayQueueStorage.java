package com.soundcloud.android.service.playback;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.TrackDAO;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

public class PlayQueueStorage {
    private final ContentResolver mResolver;
    private final TrackDAO mTrackDAO;

    public PlayQueueStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mTrackDAO = new TrackDAO(mResolver);
    }

    public void clearState() {
        mResolver.delete(Content.PLAY_QUEUE.uri, null, null);
    }

    public int getPlayQueuePositionFromUri(Uri collectionUri, long itemId) {
        Cursor cursor = mResolver.query(collectionUri,
                new String[]{DBHelper.SoundAssociationView._ID},
                DBHelper.SoundAssociationView._TYPE + " = ?",
                new String[]{String.valueOf(Playable.DB_TYPE_TRACK)},
                null);

        int position = -1;
        if (cursor != null) {
            while (cursor.moveToNext() && position == -1) {
                if (cursor.getLong(0) == itemId) position = cursor.getPosition();
            }
            cursor.close();
        }
        return position;
    }

    public void insertQueue(List<Long> playQueueItems, long userId) {
        ContentValues[] contentValues = new ContentValues[playQueueItems.size()];
        for (int i = 0; i < playQueueItems.size(); i++) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.PlayQueue.POSITION, i);
            cv.put(DBHelper.PlayQueue.TRACK_ID, playQueueItems.get(i));
            cv.put(DBHelper.CollectionItems.USER_ID, userId);
            contentValues[i] = cv;
        }
        mResolver.bulkInsert(Content.PLAY_QUEUE.uri, contentValues);
    }
}
