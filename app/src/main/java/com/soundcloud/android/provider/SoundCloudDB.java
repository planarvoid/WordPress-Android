package com.soundcloud.android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.model.Origin;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            insertUser(resolver, track.user);
        }
        return uri;
    }

    public static @Nullable Uri insertUser(ContentResolver resolver, User user) {
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


    public static @Nullable Track getTrackById(ContentResolver resolver, long id) {
        return getTrackByUri(resolver, Content.TRACKS.forId(id));
    }

    /* package */ static @Nullable Track getTrackByUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            Track track = new Track(cursor);
            cursor.close();
            return track;
        }

        if (cursor != null) cursor.close();
        return null;
    }

    public static boolean markTrackAsPlayed(ContentResolver resolver, Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.id);
        return resolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    public static @Nullable User getUserById(ContentResolver resolver, long id) {
        if (id >= 0) {
            return getUserByUri(resolver, Content.USERS.forId(id));
        } else {
            return null;
        }
    }

    public static @Nullable User getUserByUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }


    public static int bulkInsertParcelables(ContentResolver resolver, List<Parcelable> items) {
        return bulkInsertParcelables(resolver, items, null, -1);
    }

    public static int bulkInsertParcelables(ContentResolver resolver,
                                            List<? extends Parcelable> items,
                                            Uri uri,
                                            long ownerId) {
        if (uri != null && ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        if (items == null) return 0;

        Set<User> usersToInsert = new HashSet<User>();
        Set<Track> tracksToInsert = new HashSet<Track>();
        ContentValues[] bulkValues = uri == null ? null : new ContentValues[items.size()];

        for (int i=0; i <items.size(); i++) {
            Parcelable p = items.get(i);
            long id = ((ScModel) p).id;
            if (p instanceof Origin) {
                Origin origin = (Origin) p;
                Track track = origin.getTrack();
                if (track != null) {
                    tracksToInsert.add(track);
                }
                User user = origin.getUser();
                if (user != null) {
                    usersToInsert.add(user);
                }
            }

            if (uri != null) {
                ContentValues cv = new ContentValues();
                switch (Content.match(uri)) {
                    case PLAYLIST:
                        cv.put(DBHelper.PlaylistItems.USER_ID, ownerId);
                        cv.put(DBHelper.PlaylistItems.POSITION, i);
                        cv.put(DBHelper.PlaylistItems.TRACK_ID, id);
                        break;

                    default:
                        cv.put(DBHelper.CollectionItems.USER_ID, ownerId);
                        cv.put(DBHelper.CollectionItems.POSITION, i);
                        cv.put(DBHelper.CollectionItems.ITEM_ID, id);
                        break;
                }
                bulkValues[i] = cv;
            }
        }

        ContentValues[] tracksCv = new ContentValues[tracksToInsert.size()];
        ContentValues[] usersCv = new ContentValues[usersToInsert.size()];
        Track[] _tracksToInsert = tracksToInsert.toArray(new Track[tracksToInsert.size()]);
        User[] _usersToInsert = usersToInsert.toArray(new User[usersToInsert.size()]);

        for (int i=0; i< _tracksToInsert.length; i++) {
            tracksCv[i] = _tracksToInsert[i].buildContentValues();
        }
        for (int i=0; i< _usersToInsert.length; i++) {
            usersCv[i] = _usersToInsert[i].buildContentValues();
        }

        int tracksInserted = resolver.bulkInsert(Content.TRACKS.uri, tracksCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, tracksInserted + " tracks bulk inserted");

        int usersInserted = resolver.bulkInsert(Content.USERS.uri, usersCv);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, usersInserted + " users bulk inserted");

        if (bulkValues != null) {
            int itemsInserted = resolver.bulkInsert(uri, bulkValues);
            if (Log.isLoggable(TAG, Log.DEBUG))Log.d(TAG, itemsInserted + " collection items bulk inserted");
        }
        return usersInserted + tracksInserted;
    }

    public static @Nullable Recording insertRecording(ContentResolver resolver, Recording r) {
        if (r.getRecipient() != null) {
            SoundCloudDB.upsertUser(resolver, r.getRecipient());
        }
        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        if (uri != null) {
            r.id = Long.parseLong(uri.getLastPathSegment());
            return r;
        } else {
            return null;
        }
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
        // TODO, removefileextension is probably not the best way to account for encoded / raw handling
        String str = file.getAbsolutePath();
        Cursor cursor = resolver.query(Content.RECORDINGS.uri,
                null,
                DBHelper.Recordings.AUDIO_PATH + "= ?",
                new String[]{str.contains(".") ? str.substring(0, str.lastIndexOf(".")) : str},
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

    public static @NotNull List<Long> idCursorToList(Cursor c) {
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
