package com.soundcloud.android.provider;

import com.soundcloud.android.provider.ScContentProvider.DbTable;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Database helper class for {@link SettingsProvider}.
 * Mostly just has a bit {@link #onCreate} to initialize the database.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ScContentProvider";
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 6;

    static final String DATABASE_CREATE_EVENTS = "create table Events (_id integer primary key AUTOINCREMENT, "
        + "created_at integer null, "
        + "user_id integer null, "
        + "type string null, "
        + "exclusive boolean false, "
        + "origin_id integer null, "
        + "tags string null, "
        + "label string null, "
        + "next_cursor string null);";


    static final String DATABASE_CREATE_TRACKS = "create table Tracks (_id integer primary key, "
            + "permalink string null, "
            + "duration integer null, "
            + "created_at integer null, "
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
            + "sharing string null, "
            + "user_id integer null, "
            + "user_favorite boolean false, "
            + "filelength integer null);";

    static final String DATABASE_CREATE_TRACK_PLAYS = "create table TrackPlays (_id integer primary key AUTOINCREMENT, "
            + "track_id integer null, " + "user_id integer null);";

    static final String DATABASE_CREATE_USERS = "create table Users (_id integer primary key, "
            + "username string null, "
            + "avatar_url string null, "
            + "permalink string null, "
            + "city string null, "
            + "country string null, "
            + "discogs_name string null, "
            + "followers_count integer null, "
            + "followings_count integer null, "
            + "full_name string null, "
            + "myspace_name string null, "
            + "track_count integer null, "
            + "website string null, "
            + "website_title string null, " + "description text null);";

    static final String DATABASE_CREATE_RECORDINGS = "create table Recordings (_id integer primary key AUTOINCREMENT, "
            + "user_id integer null, "
            + "timestamp integer null, "
            + "longitude string null, "
            + "latitude string null, "
            + "what_text string null, "
            + "where_text string null, "
            + "audio_path string null, "
            + "artwork_path string null, "
            + "duration integer null, "
            + "four_square_venue_id string null, "
            + "shared_emails text null, "
            + "service_ids string null, "
            + "is_private boolean false, "
            + "external_upload boolean false, "
            + "audio_profile integer null, "
            + "upload_status integer false, " + "upload_error boolean false);";

    private static final HashSet<String> mValidTables = new HashSet<String>();
    static {
        mValidTables.add("Tracks");
        mValidTables.add("TrackPlays");
        mValidTables.add("Recordings");
        mValidTables.add("Users");
        mValidTables.add("Events");
    }

    DatabaseHelper(Context scApp) {
        super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
    }




    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(DATABASE_CREATE_EVENTS);
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
            db.execSQL(DATABASE_CREATE_USERS);
            db.execSQL(DATABASE_CREATE_RECORDINGS);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion +
         " to " + newVersion);

        if (newVersion > oldVersion) {
            db.beginTransaction();

            boolean success = false;
            if (oldVersion >= 3){
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 4:
                            success = upgradeTo4(db);
                            break;
                        case 5:
                            success = upgradeTo5(db);
                            break;
                        case 6:
                            success = upgradeTo6(db);
                            break;
                        default:
                            break;
                    }

                    if (!success) {
                        break;
                    }
                }

            }

            if (success) {
                Log.i(TAG,"SUCCESSFUL UPGRADE");
                db.setTransactionSuccessful();
            } else {
                Log.i(TAG,"UPGRADE NOT SUCCESSFULL");
                /*db.execSQL("DROP TABLE IF EXISTS Tracks");
                db.execSQL("DROP TABLE IF EXISTS Users");
                db.execSQL("DROP TABLE IF EXISTS Recordings");
                db.execSQL("DROP TABLE IF EXISTS TrackPlays");
                onCreate(db);*/
            }
            db.endTransaction();
        } else {
            db.execSQL("DROP TABLE IF EXISTS Tracks");
            db.execSQL("DROP TABLE IF EXISTS Users");
            db.execSQL("DROP TABLE IF EXISTS Recordings");
            db.execSQL("DROP TABLE IF EXISTS TrackPlays");
            onCreate(db);
        }

    }

        /*
         * altered id naming for content resolver
         */
        private boolean upgradeTo4(SQLiteDatabase db) {
            try {
                alterTableColumns(db, DbTable.Tracks, new String[] {
                    "id"
                }, new String[] {
                    "_id"
                });
                alterTableColumns(db, DbTable.Users, new String[] {
                    "id"
                }, new String[] {
                    "_id"
                });
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /*
         * added sharing to database
         */
        private boolean upgradeTo5(SQLiteDatabase db) {
            try {
                alterTableColumns(db, DbTable.Tracks, null, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /*
         * added sharing to database
         */
        private boolean upgradeTo6(SQLiteDatabase db) {
            try {
                db.execSQL(DATABASE_CREATE_RECORDINGS);
                db.execSQL(DATABASE_CREATE_TRACK_PLAYS);
                alterTableColumns(db, DbTable.Tracks, null, null);
                alterTableColumns(db, DbTable.Users, null, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private String alterTableColumns(SQLiteDatabase db, DbTable tbl, String[] fromAppendCols,
                String[] toAppendCols) {
            db.execSQL("DROP TABLE IF EXISTS bck_" + tbl.tblName);
            db.execSQL(tbl.createString.replace("create table " + tbl.tblName, "create table bck_"
                    + tbl.tblName));
            List<String> columns = getColumnNames(db, "bck_" + tbl.tblName);
            columns.retainAll(getColumnNames(db, tbl.tblName));
            String cols = TextUtils.join(",",columns);
            String toCols = toAppendCols != null && toAppendCols.length > 0 ? cols + ","
                    + TextUtils.join(",",toAppendCols) : cols;
            String fromCols = fromAppendCols != null && fromAppendCols.length > 0 ? cols + ","
                    + TextUtils.join(",",fromAppendCols) : cols;
            db.execSQL(String.format("INSERT INTO bck_%s (%s) SELECT %s from %s", tbl.tblName,
                    toCols, fromCols, tbl.tblName));
            db.execSQL("DROP table  '" + tbl.tblName + "'");
            db.execSQL(tbl.createString);
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from bck_%s", tbl.tblName,
                    cols, cols, tbl.tblName));
            db.execSQL("DROP table bck_" + tbl.tblName);
            return cols;
        }


    public static boolean isValidTable(String name) {
        return mValidTables.contains(name);
    }



    public static List<String> getColumnNames(SQLiteDatabase db, String tableName){
        Cursor ti = db.rawQuery("pragma table_info ("+tableName+")",null);
        if ( ti.moveToFirst() ) {
            ArrayList<String> cols = new ArrayList<String>();
            int i = 0;
            do {
                cols.add(ti.getString(1));
                i++;
            } while (ti.moveToNext());
            return cols;
        }
        return null;
    }



}
