package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;

import java.util.List;

public class PlayQueueManagerDAO {
    private final ContentResolver mResolver;

    public PlayQueueManagerDAO(ContentResolver resolver) {
        mResolver = resolver;
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
        SoundCloudDB.insertCollection(mResolver, tracks, Content.PLAY_QUEUE.uri, userId);
    }
}
