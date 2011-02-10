
package com.soundcloud.android;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class DBAdapter {

    private static final String TAG = "SoundcloudDBAdapter";

    private static final String DATABASE_USER_TABLE = "Users";

    private static final String DATABASE_TRACK_TABLE = "Tracks";

    private static final String DATABASE_NAME = "SoundCloud";

    private static final int DATABASE_VERSION = 3;

    private static final String DATABASE_CREATE_TRACKS = "create table Tracks (id string primary key, "
            + "permalink string null, "
            + "duration string null, "
            + "tag_list string null, "
            + "track_type string null, "
            + "title string null, "
            + "permalink_url string null, "
            + "artwork_url string null, "
            + "waveform_url string null, "
            + "downloadable string null, "
            + "download_url string null, "
            + "stream_url string null, "
            + "streamable string null, "
            + "user_id string null, "
            + "user_favorite boolean false, "
            + "user_played boolean false, "
            + "filelength integer null);";

    private static final String DATABASE_CREATE_USERS = "create table Users (id string primary key, "
            + "username string null, "
            + "avatar_url string null, "
            + "permalink string null, "
            + "city string null, "
            + "country string null, "
            + "discogs_name string null, "
            + "followers_count string null, "
            + "followings_count string null, "
            + "full_name string null, "
            + "myspace_name string null, "
            + "track_count string null, "
            + "website string null, "
            + "website_title string null, "
            + "description text null);";

    private final SoundCloudApplication scApp;

    private DatabaseHelper DBHelper;

    private SQLiteDatabase db;

    public DBAdapter(SoundCloudApplication scApp) {
        this.scApp = scApp;
        DBHelper = new DatabaseHelper(scApp);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        DatabaseHelper(Context scApp) {
            super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(DATABASE_CREATE_TRACKS);
                db.execSQL(DATABASE_CREATE_USERS);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Log.w(TAG, "Upgrading database from version " + oldVersion +
            // " to " + newVersion);

            if (newVersion > oldVersion) {
                db.beginTransaction();

                boolean success = true;
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {

                        case 3:
                            upgradeTo3(db);
                            break;
                        // etc. for later versions.
                    }

                    if (!success) {
                        break;
                    }
                }

                if (success) {
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
            } else {
                db.execSQL("DROP TABLE IF EXISTS Tracks");
                db.execSQL("DROP TABLE IF EXISTS Users");
                db.execSQL("DROP TABLE IF EXISTS Followings");
                db.execSQL("DROP TABLE IF EXISTS Favorites");
                onCreate(db);
            }

        }

        private void upgradeTo3(SQLiteDatabase db) {
            // reformat table to remove unnecessary cols
            db.execSQL("DROP TABLE IF EXISTS TmpTracks");
            db.execSQL(DATABASE_CREATE_TRACKS.replace("create table Tracks",
                    "create table TmpTracks"));
            List<String> columns = GetColumns(db, "TmpTracks");
            columns.retainAll(GetColumns(db, "Tracks"));
            String cols = join(columns, ",");
            db.execSQL(String.format("INSERT INTO Tmp%s (%s) SELECT %s from %s",
                    DATABASE_TRACK_TABLE, cols, cols, DATABASE_TRACK_TABLE));
            db.execSQL("DROP table  '" + DATABASE_TRACK_TABLE + "'");
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from Tmp%s",
                    DATABASE_TRACK_TABLE, cols, cols, DATABASE_TRACK_TABLE));
            db.execSQL("DROP table Tmp" + DATABASE_TRACK_TABLE);

            // make sure booleans are formatted properly, as some were strings
            // before
            db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = '' ");
            db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = '0' ");
            db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = 'false' ");
            db.execSQL("UPDATE Tracks  set user_favorite = 1 where user_favorite = 'true' ");
            db.execSQL("UPDATE Tracks  set user_favorite = 1 where user_favorite = '1' ");
            db.execSQL("UPDATE Tracks  set user_played = 0 where user_played = '' ");
            db.execSQL("UPDATE Tracks  set user_played = 0 where user_played = '0' ");
            db.execSQL("UPDATE Tracks  set user_played = 0 where user_played = 'false' ");
            db.execSQL("UPDATE Tracks  set user_played = 1 where user_played = 'true' ");
            db.execSQL("UPDATE Tracks  set user_played = 1 where user_played = '1' ");
        }
    }

    // ---opens the database---
    public DBAdapter open() throws SQLException {

        db = DBHelper.getWritableDatabase();
        return this;
    }

    // ---closes the database---
    public void close() {
        db.close();
        DBHelper.close();
    }

    public void wipeDB() {
        db.execSQL("DROP TABLE " + DATABASE_USER_TABLE);
        db.execSQL("DROP TABLE " + DATABASE_TRACK_TABLE);
        db.execSQL(DATABASE_CREATE_USERS);
        db.execSQL(DATABASE_CREATE_TRACKS);

        // Log.i("WIPE", "Wiping Database");
    }

    private String[] getDBCols(String tablename) {
        if (this.scApp.getDBColumns().get(tablename) == null)
            this.scApp.getDBColumns().put(tablename, GetColumnsArray(db, tablename));
        return this.scApp.getDBColumns().get(tablename);
    }

    private ContentValues buildTrackArgs(Track track) {
        ContentValues args = new ContentValues();
        Field f;
        for (String key : getDBCols(DATABASE_TRACK_TABLE)) {
            try {
                f = Track.class.getField(key);
                if (f != null) {
                    try {
                        if (f.getType() == String.class)
                            args.put(key, (String) f.get(track));
                        else if (f.getType() == Integer.class)
                            args.put(key, (Integer) f.get(track));
                        else if (f.getType() == Long.class)
                            args.put(key, (Long) f.get(track));
                        else if (f.getType() == Boolean.class)
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
    
    private ContentValues buildUserArgs(User User, Boolean isCurrentUser) {
        ContentValues args = new ContentValues();
        Field f;
        for (String key : getDBCols(DATABASE_USER_TABLE)) {
            if (!isCurrentUser && key.equalsIgnoreCase("description"))
                continue;
            
            try {
                f = User.class.getField(key);
                if (f != null) {
                    try {
                        if (f.getType() == String.class)
                            args.put(key, (String) f.get(User));
                        else if (f.getType() == Integer.class)
                            args.put(key, (Integer) f.get(User));
                        else if (f.getType() == Long.class)
                            args.put(key, (Long) f.get(User));
                        else if (f.getType() == Boolean.class)
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

    // ---insert a title into the database---
    public void insertTrack(Track track) {
        long id = db.insert(DATABASE_TRACK_TABLE, null, buildTrackArgs(track));
    }

    public long insertTrack(HashMap<String, String> track) {
        ContentValues args = new ContentValues();
        for (String key : getDBCols(DATABASE_TRACK_TABLE)) {
            if (track.containsKey(key)) {
                args.put(key, track.get(key));
            }
        }
        return db.insert(DATABASE_TRACK_TABLE, null, args);
        
    }

    public long insertUser(User user, Boolean isCurrentUser) {
        return db.insert(DATABASE_USER_TABLE, null, buildUserArgs(user, isCurrentUser));
    }

    public int trimTracks(long[] currentPlaylist) {
        String[] whereArgs = new String[2];
        whereArgs[0] = whereArgs[1] = Boolean.toString(false);
        return db.delete(DATABASE_TRACK_TABLE,
                "(user_favorite = 0 AND user_played = 0) AND id NOT IN ("
                        + joinArray(currentPlaylist, ",") + ")", null);
    }

    public int test() {
        SQLiteStatement dbJournalCountQuery;
        dbJournalCountQuery = db.compileStatement("select count(*) from " + DATABASE_TRACK_TABLE);
        return (int) dbJournalCountQuery.simpleQueryForLong();
    }

    public int updateTrack(Track track) {
        return db.update(DATABASE_TRACK_TABLE, buildTrackArgs(track), "id='"
                + track.id + "'", null);
    }

    public int updateUser(User user, Boolean isCurrentUser) {
        return db.update(DATABASE_USER_TABLE, buildUserArgs(user, isCurrentUser),
                "id='" + user.id + "'", null);
    }


    public int markTrackPlayed(String id) {
        ContentValues args = new ContentValues();
        args.put("user_played", true);
        return db.update(DATABASE_TRACK_TABLE, args,"id='" + id + "'", null);
    }

    // ---retrieves all the titles---
    public Cursor getTrackById(long l, long currentUserId) {
        if (currentUserId != 0)
            return db.rawQuery("SELECT Tracks.* FROM Tracks WHERE Tracks.id = '" + l + "'", null);
        else
            return db.query(DATABASE_TRACK_TABLE, GetColumnsArray(db, DATABASE_TRACK_TABLE),
                    "id='" + l + "'", null, null, null, null);
    }

    public Cursor getTrackPlayedById(String id, String current_user_id) {
        return db.rawQuery(
                "SELECT Tracks.user_played as user_played from Tracks where Tracks.id = '" + id
                        + "'", null);
    }

    public Cursor getUserById(Long userId, Long currentUserId) {
        if (db == null) return null;
        if (currentUserId != 0)
            return db.rawQuery("SELECT Users.* FROM Users WHERE Users.id = '" + userId + "'", null);
        else
            return db.query(DATABASE_USER_TABLE, GetColumnsArray(db, DATABASE_USER_TABLE),
                    "id='" + userId + "'", null, null, null, null);
    }

    public static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String[] GetColumnsArray(SQLiteDatabase db, String tableName) {
        String[] ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = c.getColumnNames();
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
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
            buf.append((String) list.get(i));
        }
        return buf.toString();
    }

    public static String joinArray(String[] list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.length;
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list[i]);
        }
        return buf.toString();
    }

    private String joinArray(long[] list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.length;
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) Long.toString(list[i]));
        }
        return buf.toString();
    }

}
