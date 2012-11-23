package com.soundcloud.android.provider;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public static Uri insertTrack(ContentResolver resolver, Track track) {
        Uri uri = resolver.insert(Content.TRACKS.uri, track.buildContentValues());
        if (uri != null && track.user != null) {
            insertUser(resolver, track.user);
        }
        return uri;
    }

    public static Uri upsertTrack(ContentResolver resolver, Track track) {
        if (!track.isSaved()) {
            return insertTrack(resolver, track);
        } else {
            // XXX make more efficient
            Cursor cursor = resolver.query(track.toUri(), null, null, null, null);
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    return updateTrack(resolver, track);
                } else {
                    return insertTrack(resolver, track);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    private static Uri updateTrack(ContentResolver resolver, Track track) {
        Uri uri = track.toUri();
        resolver.update(uri, track.buildContentValues(), null, null);
        if (track.user != null) {
            upsertUser(resolver, track.user);
        }
        return uri;
    }

    public static @Nullable
    Uri insertUser(ContentResolver resolver, User user) {
        return resolver.insert(Content.USERS.uri, user.buildContentValues(getUserId(resolver) == user.id));
    }

    public static @Nullable Uri upsertUser(ContentResolver resolver, User user) {
        if (!user.isSaved()) {
            return insertUser(resolver, user);
        } else {
            // XXX make more efficient
            Cursor cursor = resolver.query(user.toUri(), null, null, null, null);
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    return updateUser(resolver, user);
                } else {
                    return insertUser(resolver, user);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    private static Uri updateUser(ContentResolver resolver, User user) {
        Uri uri = user.toUri();
        resolver.update(uri, user.buildContentValues(getUserId(resolver) == user.id), null, null);
        return uri;
    }

    public static int bulkInsertModels(ContentResolver resolver, List<? extends ScResource> items) {
            return bulkInsertModels(resolver, items, null, -1);
        }
    public static int bulkInsertModels(ContentResolver resolver,
                                       List<? extends ScResource> items,
                                       @Nullable Uri uri,
                                       long ownerId) {
        if (uri != null && ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        if (items == null) return 0;

        Set<User> usersToInsert = new HashSet<User>();
        Set<Sound> soundsToInsert = new HashSet<Sound>();

        ContentValues[] bulkValues = uri == null ? null : new ContentValues[items.size()];

        int index = 0;
        for (int i=0; i <items.size(); i++) {

            ScResource p = items.get(i);
            if (p != null) {

                Sound sound = p.getSound();
                if (sound != null) {
                    soundsToInsert.add(sound);
                }

                User user = p.getUser();
                if (user != null) {
                    usersToInsert.add(user);
                }

                long id = p.id;
                if (uri != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, ownerId);
                    switch (Content.match(uri)) {
                        case PLAY_QUEUE:
                            cv.put(DBHelper.PlayQueue.POSITION, i);
                            cv.put(DBHelper.PlayQueue.TRACK_ID, id);
                            break;

                        // this will not be necessary once we use e1 likes endpoint
                        case ME_LIKES:
                        case USER_LIKES:
                            //cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, Track.DB_TYPE_TRACK);
                            // fallthrough
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
        Sound[] _soundssToInsert = soundsToInsert.toArray(new Sound[soundsToInsert.size()]);
        User[] _usersToInsert = usersToInsert.toArray(new User[usersToInsert.size()]);

        for (int i=0; i< _soundssToInsert.length; i++) {
            soundsCv[i] = _soundssToInsert[i].buildContentValues();
        }
        for (int i=0; i< _usersToInsert.length; i++) {
            usersCv[i] = _usersToInsert[i].buildContentValues();
        }

        int tracksInserted = resolver.bulkInsert(Content.SOUNDS.uri, soundsCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, tracksInserted + " tracks bulk inserted");

        int usersInserted = resolver.bulkInsert(Content.USERS.uri, usersCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, usersInserted + " users bulk inserted");

        if (bulkValues != null) {
            int itemsInserted = resolver.bulkInsert(uri, bulkValues);
            if (Log.isLoggable(TAG, Log.DEBUG))Log.d(TAG, itemsInserted + " collection items bulk inserted");
        }
        return usersInserted + tracksInserted;
    }

    public static @Nullable
    Recording upsertRecording(ContentResolver resolver, Recording r, @Nullable ContentValues values) {
        if (r.getRecipient() != null) {
            upsertUser(resolver, r.getRecipient());
        }
        final ContentValues contentValues = values == null ? r.buildContentValues() : values;
        if (!r.isSaved() || resolver.update(r.toUri(), contentValues, null, null) == 0) {
            Uri uri = resolver.insert(Content.RECORDINGS.uri, contentValues);
            if (uri != null) {
                r.id = Long.parseLong(uri.getLastPathSegment());
            }
        }
        return r;

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

    private static long getUserId(ContentResolver resolver) {
        Cursor c = resolver.query(Content.ME_USERID.uri, null, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            } else {
                return -1;
            }
        } finally {
            if (c != null) c.close();
        }
    }

    public static @NotNull
    List<Long> idCursorToList(Cursor c) {
        List<Long> ids = new ArrayList<Long>();
        if (c != null && c.moveToFirst()) {
            do {
                ids.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return ids;
    }

    public static Uri addPagingParams(Uri uri, int offset, int limit) {
        if (uri == null) return null;

        Uri.Builder b = uri.buildUpon();
        if (offset > 0) {
            b.appendQueryParameter("offset", String.valueOf(offset));
        }
        b.appendQueryParameter("limit", String.valueOf(limit));
        return b.build();

    }
}
