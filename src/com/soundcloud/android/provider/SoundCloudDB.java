package com.soundcloud.android.provider;

import com.soundcloud.android.model.Origin;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.SearchHistoryItem;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
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

    public static Uri insertUser(ContentResolver resolver, User user) {
        return resolver.insert(Content.USERS.uri, user.buildContentValues(getUserId(resolver) == user.id));
    }

    public static Uri upsertUser(ContentResolver resolver, User user) {
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


    public static Track getTrackById(ContentResolver resolver, long id) {
        return getTrackByUri(resolver, Content.TRACKS.forId(id));
    }

    /* package */ static Track getTrackByUri(ContentResolver resolver, Uri uri) {
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
        contentValues.put(DBHelper.TrackPlays.TRACK_ID, track.id);
        return resolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    public static Uri addSearch(ContentResolver resolver, int searchType, String query) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Searches.CREATED_AT, System.currentTimeMillis());
        cv.put(DBHelper.Searches.QUERY, query);
        cv.put(DBHelper.Searches.SEARCH_TYPE, searchType);
        return resolver.insert(Content.SEARCHES.uri, cv);
    }

    public static List<SearchHistoryItem> getSearches(ContentResolver resolver) {
        Cursor cursor = resolver.query(Content.SEARCHES.uri,
                null,
                null,
                null,
                DBHelper.Searches.CREATED_AT + " DESC");

        List<SearchHistoryItem> list = new ArrayList<SearchHistoryItem>();
        while (cursor != null && cursor.moveToNext()) {
            list.add(new SearchHistoryItem(cursor));
        }
        if (cursor != null) cursor.close();
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<Track> getTracks(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null) {
            List<Track> tracks = new ArrayList<Track>(cursor.getCount());
            while (cursor.moveToNext()) {
                tracks.add(new Track(cursor));
            }
            cursor.close();
            return tracks;
        } else {
            return Collections.EMPTY_LIST;
        }
    }


    public static User getUserById(ContentResolver resolver, long userId) {
        return getUserByUri(resolver, Content.USERS.forId(userId));
    }

    public static User getUserByUri(ContentResolver resolver, Uri uri) {
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
                                            List<Parcelable> items,
                                            Uri collectionUri,
                                            long ownerId) {
        if (collectionUri != null && ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        Set<User> usersToInsert = new HashSet<User>();
        Set<Track> tracksToInsert = new HashSet<Track>();
        ContentValues[] bulkValues = collectionUri == null ? null : new ContentValues[items.size()];

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

            if (bulkValues != null) {
                ContentValues itemCv = new ContentValues();
                itemCv.put(DBHelper.CollectionItems.USER_ID, ownerId);
                itemCv.put(DBHelper.CollectionItems.POSITION, i);
                itemCv.put(DBHelper.CollectionItems.ITEM_ID, id);
                bulkValues[i] = itemCv;
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
        Log.d(TAG, tracksInserted + " tracks bulk inserted");

        int usersInserted = resolver.bulkInsert(Content.USERS.uri, usersCv);
        Log.d(TAG, usersInserted + " users bulk inserted");

        if (bulkValues != null) {
            int itemsInserted = resolver.bulkInsert(collectionUri, bulkValues);
            Log.d(TAG, itemsInserted + " collection items bulk inserted");
        }
        return usersInserted + tracksInserted;
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
}
