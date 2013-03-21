package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

public class PlayQueueManagerDAO {
    public static void clearState(ContentResolver resolver) {
        resolver.delete(Content.PLAY_QUEUE.uri, null, null);
    }

    public static int getPlayQueuePositionFromUri(ContentResolver resolver, Uri collectionUri, long itemId) {
        Cursor cursor = resolver.query(collectionUri,
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
}
