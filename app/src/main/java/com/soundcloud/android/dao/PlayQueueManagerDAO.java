package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.playback.PlayQueueManager;

import java.util.ArrayList;
import java.util.List;

public class PlayQueueManagerDAO {
    public static List<PlayQueueManager.PlayQueueItem> getItems(ContentResolver resolver, Uri uri) {
        ArrayList<PlayQueueManager.PlayQueueItem> newQueue = null;

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, null, null, null, null);
        } catch (IllegalArgumentException e) {
            // in case we load a deprecated URI, just don't load the playlist
            Log.e(PlayQueueManager.class.getSimpleName(), "Tried to load an invalid uri " + uri);
        }
        boolean isActivityCursor = Content.match(uri).isActivitiesItem();

        if (cursor != null) {
            newQueue = new ArrayList<PlayQueueManager.PlayQueueItem>();
            if (cursor.moveToFirst()){
                do {
                    // tracks only, no playlists allowed past here
                    if (cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView._TYPE)) == Playable.DB_TYPE_TRACK) {

                        final Track trackFromCursor = isActivityCursor ?
                                SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor, DBHelper.ActivityView.SOUND_ID) :
                                SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor);

                        newQueue.add(new PlayQueueManager.PlayQueueItem(trackFromCursor,false));
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return newQueue;
    }

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
