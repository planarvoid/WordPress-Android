package com.soundcloud.android.provider;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";
    public static final int RESOLVER_BATCH_SIZE = 100;

    public static int bulkInsertModels(ContentResolver resolver, List<? extends ScResource> models) {
        return bulkInsertModels(resolver, models, null, -1);
    }

    public static int bulkInsertModels(ContentResolver resolver,
                                       List<? extends ScResource> models,
                                       @Nullable Uri uri,
                                       long ownerId) {
        if (uri != null && ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        if (models == null) return 0;

        Set<User> usersToInsert = new HashSet<User>();
        Set<Playable> soundsToInsert = new HashSet<Playable>();

        ContentValues[] bulkValues = uri == null ? null : new ContentValues[models.size()];

        int index = 0;
        for (int i=0; i <models.size(); i++) {

            ScResource r = models.get(i);
            if (r != null) {
                Playable playable = r.getPlayable();
                if (playable != null) {
                    soundsToInsert.add(playable);
                }

                User user = r.getUser();
                if (user != null) {
                    usersToInsert.add(user);
                }

                long id = r.id;
                if (uri != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, ownerId);
                    switch (Content.match(uri)) {
                        case PLAY_QUEUE:
                            cv.put(DBHelper.PlayQueue.POSITION, i);
                            cv.put(DBHelper.PlayQueue.TRACK_ID, id);
                            break;

                        default:
                            cv.put(DBHelper.CollectionItems.POSITION, i);
                            cv.put(DBHelper.CollectionItems.ITEM_ID, id);
                            break;

                    }
                    bulkValues[index] = cv;
                    index++;
                }
            }
        }

        ContentValues[] soundsCv = new ContentValues[soundsToInsert.size()];
        ContentValues[] usersCv = new ContentValues[usersToInsert.size()];
        Playable[] _soundssToInsert = soundsToInsert.toArray(new Playable[soundsToInsert.size()]);
        User[] _usersToInsert = usersToInsert.toArray(new User[usersToInsert.size()]);

        for (int i=0; i< _soundssToInsert.length; i++) {
            soundsCv[i] = _soundssToInsert[i].buildContentValues();
        }
        for (int i=0; i< _usersToInsert.length; i++) {
            usersCv[i] = _usersToInsert[i].buildContentValues();
        }

        int tracksInserted = resolver.bulkInsert(Content.SOUNDS.uri, soundsCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, tracksInserted + " sounds bulk inserted");

        int usersInserted = resolver.bulkInsert(Content.USERS.uri, usersCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, usersInserted + " users bulk inserted");

        if (bulkValues != null) {
            int itemsInserted = resolver.bulkInsert(uri, bulkValues);
            if (Log.isLoggable(TAG, Log.DEBUG))Log.d(TAG, itemsInserted + " collection items bulk inserted");
        }
        return usersInserted + tracksInserted;
    }

    public static @Nullable Recording getRecordingByUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        Recording recording = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();
        return recording;
    }

    public static @Nullable Recording getRecordingByPath(ContentResolver resolver, File file) {
        Cursor cursor = resolver.query(Content.RECORDINGS.uri,
                null,
                DBHelper.Recordings.AUDIO_PATH + " LIKE ?",
                new String[]{ IOUtils.removeExtension(file).getAbsolutePath() + "%" },
                null);

        Recording recording = null;
        if (cursor != null && cursor.moveToFirst()) {
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();

        return recording;
    }

    public static @NotNull List<Long> idCursorToList(Cursor c) {
        if (c == null) return Collections.emptyList();
        List<Long> ids = new ArrayList<Long>(c.getCount());
        while (c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        c.close();
        return ids;
    }

    public static Uri.Builder addPagingParams(Uri uri, int offset, int limit) {
        if (uri == null) return null;

        Uri.Builder b = uri.buildUpon();
        if (offset > 0) {
            b.appendQueryParameter("offset", String.valueOf(offset));
        }
        b.appendQueryParameter("limit", String.valueOf(limit));
        return b;

    }

    /**
     * @return a list of all ids for which objects are store in the database
     */
    public static List<Long> getStoredIdsBatched(ContentResolver resolver, List<Long> ids, Content content) {
        int i = 0;
        List<Long> storedIds = new ArrayList<Long>();
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, ids.size()));
            storedIds.addAll(idCursorToList(
                    resolver.query(content.uri, new String[]{BaseColumns._ID},
                            DBHelper.getWhereInClause(BaseColumns._ID, batch) + " AND " + DBHelper.ResourceTable.LAST_UPDATED + " > 0"
                            , ScModelManager.longListToStringArr(batch), null)
            ));
            i += RESOLVER_BATCH_SIZE;
        }
        return storedIds;
    }

    public static List<Long> getStoredIds(ContentResolver resolver, Uri uri, int offset, int limit) {
        return idCursorToList(resolver.query(SoundCloudDB.addPagingParams(uri, offset, limit)
                .appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build(),
                null, null, null, null));
    }
}
