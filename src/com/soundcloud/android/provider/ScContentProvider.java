package com.soundcloud.android.provider;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";
    public static final Pattern URL_PATTERN = Pattern.compile("^content://" + AUTHORITY + "/(\\w+)(?:/(-?\\d+))?$");
    public static final long DEFAULT_POLL_FREQUENCY = 3600l; // 1h

    private static final UriMatcher sUriMatcher = buildMatcher();

    private DatabaseHelper dbHelper;

    static class TableInfo {
        DatabaseHelper.Tables table;
        long id = -1;

        public String where(String where) {
            if (id != -1){
                return TextUtils.isEmpty(where) ? "_id=" + id : where + " AND _id=" + id;
            } else {
                return where;
            }
        }

        public String[] whereArgs(String[] whereArgs) {
            return whereArgs;
        }
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case ME:
                getContext()
                qb.setTables("Tracks INNER JOIN UserFavorites ON (Tracks._id = UserFavorites.track_id)");
                selection = selection == null ? "UserFavorites.user_id=";
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;

        /*

        TableInfo info = getTableInfo(uri);
        if (info.id != -1) {
            selection = selection == null ? "_id=" + info.id : selection + " AND _id=" + info.id;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(info.table.tableName, columns, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
        */
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final DatabaseHelper.Tables table = getTable(uri);
        long id = db.insert(table.tableName, null, values);
        if (id >= 0) {
            final Uri result = uri.buildUpon().appendPath(String.valueOf(id)).build();
            getContext().getContentResolver().notifyChange(result, null);
            return result;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        TableInfo tableInfo = getTableInfo(uri);
        int count = db.delete(tableInfo.table.tableName, tableInfo.where(where), tableInfo.whereArgs(whereArgs));
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        TableInfo tableInfo = getTableInfo(uri);
        int count = db.update(tableInfo.table.tableName, values, tableInfo.where(where), tableInfo.whereArgs(whereArgs));
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    // to replace rows
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        String tblName = getTableInfo(uri).table.tableName;
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (db.replace(tblName, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    static DatabaseHelper.Tables getTable(Uri u) {
        return getTable(u.toString());
    }

    static DatabaseHelper.Tables getTable(String s) {
        DatabaseHelper.Tables table = null;
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches()) {
            table = DatabaseHelper.Tables.get(m.group(1));
        }
        if (table != null) {
            return table;
        } else {
            throw new IllegalArgumentException("unknown uri " + s);
        }
    }

    static TableInfo getTableInfo(Uri uri) {
        return getTableInfo(uri.toString());
    }

    static TableInfo getTableInfo(String s) {
        TableInfo result = new TableInfo();
        result.table = getTable(s);
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches() && m.group(2) != null) {
            result.id = Long.parseLong(m.group(2));
        }
        return result;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static void enableSyncing(Account account, long pollFrequency) {
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

        if (Build.VERSION.SDK_INT >= 8) {
            ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), pollFrequency);
        }
    }

    public static void disableSyncing(Account account) {
        ContentResolver.setSyncAutomatically(account, AUTHORITY, false);
          if (Build.VERSION.SDK_INT >= 8) {
            ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
        }
    }


    public interface Content {

        /** LOCAL + REMOTE API URIS **/
        Uri ME                          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me");
        Uri ME_TRACKS                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/tracks");
        Uri ME_COMMENTS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/comments");
        Uri ME_FOLLOWINGS               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/followings");
        Uri ME_FOLLOWINGS_ITEM          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/followings/#");
        Uri ME_FOLLOWERS                = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/followers");
        Uri ME_FOLLOWERS_ITEM           = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/followers/#");
        Uri ME_FAVORITES                = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/favorites");
        Uri ME_FAVORITES_ITEM           = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/favorites/#");
        Uri ME_GROUPS                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/groups");
        Uri ME_PLAYLISTS                = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/playlists");

        Uri TRACK_ITEM                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/*");
        Uri TRACK_COMMENTS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/*/comments");
        Uri TRACK_PERMISSIONS           = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/*/permissions");
        Uri TRACK_SECRET_TOKEN          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/*/secret-token");

        Uri USER_ITEM                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*");
        Uri USER_TRACKS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/tracks");
        Uri USER_FAVORITES              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/favorites");
        Uri USER_FOLLOWERS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/followers");
        Uri USER_FOLLOWINGS             = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/followings");
        Uri USER_COMMENTS               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/comments");
        Uri USER_GROUPS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/groups");
        Uri USER_PLAYLISTS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/*/playlists");

        Uri COMMENT_ITEM                = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/comments/#");

        Uri PLAYLISTS                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/playlists");
        Uri PLAYLIST_ITEM               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/playlists/#");

        Uri GROUPS                      = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups");
        Uri GROUP_ITEM                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#");
        Uri GROUP_USERS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#/users");
        Uri GROUP_MODERATORS            = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#/moderators");
        Uri GROUP_MEMBERS               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#/members");
        Uri GROUP_CONTRIBUTORS          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#/contributors");
        Uri GROUP_TRACKS                = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/groups/#/tracks");

        /** LOCAL URIS **/
        Uri RECORDINGS                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/recordings");
        Uri RECORDING_ITEM              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/recordings/#");
        Uri EVENTS                      = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/events");
        Uri EVENT_ITEM                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/events/#");
        Uri SEARCHES                    = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches");
        Uri TRACK_PLAYS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/track_plays");

    }

    private static final int ME                     = 100;
    private static final int ME_TRACKS              = 101;
    private static final int ME_COMMENTS            = 102;
    private static final int ME_FOLLOWINGS          = 103;
    private static final int ME_FOLLOWINGS_ITEM     = 104;
    private static final int ME_FOLLOWERS           = 105;
    private static final int ME_FOLLOWERS_ITEM      = 106;
    private static final int ME_FAVORITES           = 107;
    private static final int ME_FAVORITES_ITEM      = 108;
    private static final int ME_GROUPS              = 109;
    private static final int ME_PLAYLISTS           = 110;

    private static final int TRACK_ITEM             = 202;
    private static final int TRACK_COMMENTS         = 203;
    private static final int TRACK_PERMISSIONS      = 204;
    private static final int TRACK_SECRET_TOKEN     = 205;

    private static final int USER_ITEM              = 302;
    private static final int USER_TRACKS            = 303;
    private static final int USER_FAVORITES         = 304;
    private static final int USER_FOLLOWERS         = 305;
    private static final int USER_FOLLOWINGS        = 306;
    private static final int USER_COMMENTS          = 307;
    private static final int USER_GROUPS            = 308;
    private static final int USER_PLAYLISTS         = 309;

    private static final int COMMENT_ITEM           = 400;

    private static final int PLAYLISTS              = 501;
    private static final int PLAYLIST_ITEM          = 502;

    private static final int GROUPS                 = 601;
    private static final int GROUP_ITEM             = 602;
    private static final int GROUP_USERS            = 603;
    private static final int GROUP_MODERATORS       = 604;
    private static final int GROUP_MEMBERS          = 605;
    private static final int GROUP_CONTRIBUTORS     = 606;
    private static final int GROUP_TRACKS           = 607;

    private static final int RECORDINGS             = 1000;
    private static final int RECORDING_ITEM         = 1001;
    private static final int EVENTS                 = 1002;
    private static final int EVENT_ITEM             = 1003;
    private static final int SEARCHES               = 1004;
    private static final int TRACK_PLAYS            = 1005;


	private static UriMatcher buildMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(ScContentProvider.AUTHORITY, "me",ME)
        matcher.addURI(ScContentProvider.AUTHORITY, "me/tracks", ME_TRACKS);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/comments", ME_COMMENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/followings", ME_FOLLOWINGS);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/followings/#", ME_FOLLOWINGS_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/followers", ME_FOLLOWERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/followers/#", ME_FOLLOWERS_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/favorites", ME_FAVORITES);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/favorites/#", ME_FAVORITES_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/groups", ME_GROUPS);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/playlists", ME_PLAYLISTS);

        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/*", TRACK_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/*/comments", TRACK_COMMENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/*/permissions", TRACK_PERMISSIONS);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/*/secret-token", TRACK_SECRET_TOKEN);

        matcher.addURI(ScContentProvider.AUTHORITY, "users/*", USER_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/tracks", USER_TRACKS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/favorites", USER_FAVORITES);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/followers", USER_FOLLOWERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/followings", USER_FOLLOWINGS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/comments", USER_COMMENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/groups", USER_GROUPS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/*/playlists", USER_PLAYLISTS);

        matcher.addURI(ScContentProvider.AUTHORITY, "comments/#", COMMENT_ITEM);

        matcher.addURI(ScContentProvider.AUTHORITY, "playlists", PLAYLISTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "playlists/#", PLAYLIST_ITEM);

        matcher.addURI(ScContentProvider.AUTHORITY, "groups", GROUPS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#", GROUP_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/users", GROUP_USERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/moderators", GROUP_MODERATORS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/members", GROUP_MEMBERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/contributors", GROUP_CONTRIBUTORS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/tracks", GROUP_TRACKS);


        matcher.addURI(ScContentProvider.AUTHORITY, "recordings", RECORDINGS);
        matcher.addURI(ScContentProvider.AUTHORITY, "recordings/#", RECORDING_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "events", EVENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "events/#", EVENT_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches", SEARCHES);
        matcher.addURI(ScContentProvider.AUTHORITY, "track_plays", TRACK_PLAYS);
        
		return matcher;

	}

}