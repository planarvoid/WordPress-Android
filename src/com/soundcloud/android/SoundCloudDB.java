package com.soundcloud.android;

import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.Track.Tracks;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.objects.User.Users;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class SoundCloudDB {

        /**
         *
         */
        private SoundCloudDB () {
        }

        public enum WriteState {
            none, insert_only, update_only, all
        }

        private HashMap<Uri, String[]> dbColumns = new HashMap<Uri, String[]>();

        private static final SoundCloudDB  instance = new SoundCloudDB ();

        public static SoundCloudDB  getInstance() {
            return instance;
        }

        // ---Make sure the database is up to date with this track info---
        public void resolveTrack(ContentResolver contentResolver, Track track, WriteState writeState, Long currentUserId) {

            Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.ID + "='" + track.id + "'", null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    // add local urls and update database
                    cursor.moveToFirst();

                    track.user_played = cursor.getInt(cursor.getColumnIndex("user_played")) == 1;

                    if (writeState == WriteState.update_only || writeState == WriteState.all)
                        updateTrack(contentResolver,track);

                } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                    insertTrack(contentResolver, track);
                }
                cursor.close();

                // write with insert only because a track will never come in with
                resolveUser(contentResolver, track.user, WriteState.insert_only, currentUserId);
            }
        }

        // ---Make sure the database is up to date with this track info---
        public Track resolveTrackById(ContentResolver contentResolver, long TrackId,
                long currentUserId) {

            Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.ID + "='" + TrackId + "'", null, null);
            if (cursor.getCount() != 0) {

                Track track = new Track(cursor);
                cursor.close();
                cursor.close();

                User user = resolveUserById(contentResolver, track.user_id);
                if (user != null){
                    track.user = user;
                    track.user_id = user.id;
                }


                return track;
            }

            cursor.close();
            return null;

        }


        // adds a new note with a given title and text
        public void insertTrack(ContentResolver contentResolver, Track track) {
            ContentValues contentValues = buildTrackArgs(contentResolver,track);
            contentResolver.insert(Tracks.CONTENT_URI, contentValues);
        }

        // checks to see if a note with a given title is in our database
        public boolean isTrackInDb(ContentResolver contentResolver, long id) {
            boolean ret = false;
            Cursor cursor = contentResolver.query(Tracks.CONTENT_URI, null, Tracks.ID + "='" + id + "'", null, null);
            if (null != cursor && cursor.moveToNext()) {
                ret = true;
            }
            cursor.close();
            return ret;
        }

        // adds a new note with a given title and text
        public void updateTrack(ContentResolver contentResolver, Track track) {
            ContentValues contentValues = buildTrackArgs(contentResolver,track);
            contentResolver.update(Tracks.CONTENT_URI, contentValues, Tracks.ID + "='" + track.id + "'", null);
        }

        public int trimTracks(ContentResolver contentResolver, long[] currentPlaylist) {
            String[] whereArgs = new String[2];
            whereArgs[0] = whereArgs[1] = Boolean.toString(false);
            return contentResolver.delete(Tracks.CONTENT_URI,
                    "(user_favorite = 0 AND user_played = 0) AND id NOT IN ("
                            + joinArray(currentPlaylist, ",") + ")", null);
        }

        // ---Make sure the database is up to date with this track info---
        public void resolveUser(ContentResolver contentResolver,User user, WriteState writeState,
                Long currentUserId) {

            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + user.id + "'", null, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    user.update(cursor); // update the parcelable with values from the db

                    if (writeState == WriteState.update_only || writeState == WriteState.all)
                        updateUser(contentResolver, user, currentUserId.compareTo(user.id) == 0);

                } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                    insertUser(contentResolver, user, currentUserId.compareTo(user.id) == 0);
                }
                cursor.close();
            }
        }

        // ---Make sure the database is up to date with this track info---
        public User resolveUserById(ContentResolver contentResolver, long userId) {

            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + userId + "'", null, null);

            if (cursor.getCount() != 0) {

                User user = new User(cursor);
                cursor.close();
                return user;
            }

            cursor.close();
            return null;

        }


     // adds a new note with a given title and text
        public void insertUser(ContentResolver contentResolver, User User, boolean isCurrentUser) {
            ContentValues contentValues = buildUserArgs(contentResolver,User, isCurrentUser);
            contentResolver.insert(Users.CONTENT_URI, contentValues);
        }

        // checks to see if a note with a given title is in our database
        public boolean isUserInDb(ContentResolver contentResolver, long id) {
            boolean ret = false;
            Cursor cursor = contentResolver.query(Users.CONTENT_URI, null, Users.ID + "='" + id + "'", null, null);
            if (null != cursor && cursor.moveToNext()) {
                ret = true;
            }
            cursor.close();
            return ret;
        }

        // adds a new note with a given title and text
        public void updateUser(ContentResolver contentResolver, User User, boolean isCurrentUser) {
            ContentValues contentValues = buildUserArgs(contentResolver,User, isCurrentUser);
            contentResolver.update(Users.CONTENT_URI, contentValues, Users.ID + "='" + User.id + "'", null);
        }


        private String[] getDBCols(ContentResolver contentResolver, Uri tableUri) {
            if (dbColumns.get(tableUri) == null)
                dbColumns.put(tableUri, GetColumnsArray(contentResolver, tableUri));
            return dbColumns.get(tableUri);
        }

        private ContentValues buildTrackArgs(ContentResolver contentResolver, Track track) {
            ContentValues args = new ContentValues();
            Field f;
            for (String key : getDBCols(contentResolver, Tracks.CONTENT_URI)) {
                try {
                    f = Track.class.getField(key.contentEquals("_id") ? "id" : key);
                    if (f != null) {
                        try {
                            if (f.getType() == String.class)
                                args.put(key, (String) f.get(track));
                            else if (f.getType() == Integer.TYPE || f.getType() == Integer.class)
                                args.put(key, f.getInt(track));
                            else if (f.getType() == Long.TYPE || f.getType() == Long.class){
                                args.put(key, f.getLong(track));
                            }else if (f.getType() == boolean.class)
                                args.put(key, ((Boolean) f.get(track)) ? 1 : 0);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

            }
            return args;

        }

        private ContentValues buildUserArgs(ContentResolver contentResolver, User User, boolean isCurrentUser) {
            ContentValues args = new ContentValues();
            Field f;
            for (String key : getDBCols(contentResolver, Users.CONTENT_URI)) {
                if (!isCurrentUser && key.equalsIgnoreCase("description"))
                    continue;

                try {
                    f = User.class.getField(key.contentEquals("_id") ? "id" : key);
                    if (f != null) {
                        try {
                            if (f.getType() == String.class)
                                args.put(key, (String) f.get(User));
                            else if (f.getType() == Integer.TYPE || f.getType() == Integer.class)
                                args.put(key, (Integer) f.get(User));
                            else if (f.getType() == Long.TYPE || f.getType() == Long.class)
                                args.put(key, (Long) f.get(User));
                            else if (f.getType() == boolean.class)
                                args.put(key, ((Boolean) f.get(User)) ? 1 : 0);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

            }
            return args;

        }

        public static String[] GetColumnsArray(ContentResolver contentResolver, Uri tableUri) {
            String[] ar = null;
            Cursor c = null;
            try {
                c = contentResolver.query(tableUri, null, null, null, "_id DESC limit 1");
                if (c != null) {
                    ar = c.getColumnNames();
                }
            } catch (Exception e) {
                Log.v(tableUri.toString(), e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
            }
            return ar;
        }

        public static String join(List<String> list, String delim) {
            StringBuilder buf = new StringBuilder();
            int num = list.size();
            for (int i = 0; i < num; i++) {
                if (i != 0)
                    buf.append(delim);
                buf.append(list.get(i));
            }
            return buf.toString();
        }

        public static String joinArray(String[] list, String delim) {
            StringBuilder buf = new StringBuilder();
            int num = list.length;
            for (int i = 0; i < num; i++) {
                if (i != 0)
                    buf.append(delim);
                buf.append(list[i]);
            }
            return buf.toString();
        }

        private String joinArray(long[] list, String delim) {
            StringBuilder buf = new StringBuilder();
            int num = list.length;
            for (int i = 0; i < num; i++) {
                if (i != 0)
                    buf.append(delim);
                buf.append(Long.toString(list[i]));
            }
            return buf.toString();
        }



    }