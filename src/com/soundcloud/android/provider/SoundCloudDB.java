package com.soundcloud.android.provider;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.DBHelper.Users;

import android.content.ContentResolver;
import android.database.Cursor;

import java.util.*;

public class SoundCloudDB {
    private static final String TAG = "SoundCloudDB";

    public static Uri insertTrack(ContentResolver resolver, Track track, long currentUserId) {
        Uri uri = resolver.insert(Content.TRACKS.uri, track.buildContentValues());
        if (uri != null && track.user != null) {
            insertUser(resolver, track.user, currentUserId);
        }
        return uri;
    }

    public static Uri upsertTrack(ContentResolver resolver, Track track, long currentUserId) {
        if (!track.isSaved()) {
            return insertTrack(resolver, track, currentUserId);
        } else {
            // XXX make more efficient
            Cursor cursor = resolver.query(track.toUri(), null, null, null, null);
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    return updateTrack(resolver, track, currentUserId);
                } else {
                    return insertTrack(resolver, track, currentUserId);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    private static Uri updateTrack(ContentResolver resolver, Track track, long currentUserId) {
        Uri uri = track.toUri();
        resolver.update(uri, track.buildContentValues(), null, null);
        if (track.user != null) {
            insertUser(resolver, track.user, currentUserId);
        }
        return uri;
    }

    public static Uri insertUser(ContentResolver resolver, User user, long currentUserId) {
        return resolver.insert(Content.USERS.uri, user.buildContentValues(currentUserId == user.id));
    }

    public static Uri upsertUser(ContentResolver resolver, User user, long currentUserId) {
        if (!user.isSaved()) {
            return insertUser(resolver, user, currentUserId);
        } else {
            // XXX make more efficient
            Cursor cursor = resolver.query(user.toUri(), null, null, null, null);
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    return updateUser(resolver, user, currentUserId);
                } else {
                    return insertUser(resolver, user, currentUserId);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    private static Uri updateUser(ContentResolver resolver, User user, long currentUserId) {
        Uri uri = user.toUri();
        resolver.update(uri, user.buildContentValues(currentUserId == user.id), null, null);
        return uri;
    }


    // ---Make sure the database is up to date with this track info---

    public static Track getTrackById(ContentResolver resolver, long id) {
        return getTrackByUri(resolver, Content.TRACKS.forId(id));
    }

    public static Track getTrackByUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            Track track = new Track(cursor);
            cursor.close();

            User user = getUserById(resolver, track.user_id);
            if (user != null) {
                track.user = user;
                track.user_id = user.id;
            }
            return track;
        }

        if (cursor != null) cursor.close();
        return null;
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
        Cursor cursor = resolver.query(Content.USERS.forId(userId), null, null, null, null);
        User user = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        return user;
    }

    /** usage: {@link com.soundcloud.android.adapter.MyTracksAdapter#loadRecordings(Cursor cursor)} */
    public static String getUsernameById(ContentResolver resolver, long userId) {
        Cursor cursor = resolver.query(Content.USER.uri, new String[]{Users.USERNAME}, Users._ID + "= ?", new String[]{Long.toString(userId)}, null);
        String username = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            username = cursor.getString(0);
        }
        if (cursor != null) cursor.close();
        return username;
    }

    /** usage: {@link com.soundcloud.android.service.playback.CloudPlaybackService#onFavoriteStatusSet(long, boolean)} */
    public static void setTrackIsFavorite(ContentResolver resolver, long trackId, boolean isFavorite, long currentUserId) {
        Track t = getTrackById(resolver, trackId);
        if (t != null) {
            t.user_favorite = isFavorite;
            updateTrack(resolver, t, currentUserId);
        }
    }

    public static int bulkInsertParcelables(ContentResolver resolver, List<Parcelable> items) {
        return bulkInsertParcelables(resolver, items, null, -1, -1);
    }

    public static int bulkInsertParcelables(ContentResolver resolver,
                                            List<Parcelable> items,
                                            Uri collectionUri,
                                            long owner,
                                            int startIndex) {
        int i = 0;
        Set<User> usersToInsert = new HashSet<User>();
        Set<Track> tracksToInsert = new HashSet<Track>();
        ContentValues[] bulkValues = null;
        if (collectionUri != null) {
            bulkValues = new ContentValues[items.size()];
        }

        // XXX lookup should be in model (Origin)
        for (Parcelable p : items) {
            long id = ((ScModel) p).id;
            if (p instanceof User) {
                usersToInsert.add((User) p);
            } else if (p instanceof Track) {
                usersToInsert.add(((Track) p).user);
                tracksToInsert.add((Track) p);
            } else if (p instanceof Friend) {
                id = ((Friend) p).user.id;
                usersToInsert.add(((Friend) p).user);
            }

            if (bulkValues != null) {
                ContentValues itemCv = new ContentValues();
                itemCv.put(DBHelper.CollectionItems.USER_ID, owner);
                itemCv.put(DBHelper.CollectionItems.POSITION, startIndex + i);
                itemCv.put(DBHelper.CollectionItems.ITEM_ID, id);
                bulkValues[i] = itemCv;
                i++;
            }
        }

        ContentValues[] tracksCv = new ContentValues[tracksToInsert.size()];
        i = 0;
        for (Track t : tracksToInsert) {
            tracksCv[i] = t.buildContentValues();
            i++;
        }
        ContentValues[] usersCv = new ContentValues[usersToInsert.size()];
        i = 0;
        for (User u : usersToInsert) {
            usersCv[i] = u.buildContentValues();
            i++;
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
}
