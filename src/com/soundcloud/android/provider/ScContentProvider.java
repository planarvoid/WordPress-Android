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
import android.text.TextUtils;
import com.soundcloud.android.SoundCloudApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScContentProvider extends ContentProvider {
    private static final String LOG_TAG = ScContentProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";
    public static final Pattern URL_PATTERN = Pattern.compile("^content://" + AUTHORITY + "/(\\w+)(?:/(-?\\d+))?$");
    public static final long DEFAULT_POLL_FREQUENCY = 3600l; // 1h

    private static final UriMatcher sUriMatcher = buildMatcher();

    private DBHelper dbHelper;


    static class TableInfo {
        DBHelper.Tables table;
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
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        SCQueryBuilder qb = new SCQueryBuilder();
        String whereAppend;

        switch (sUriMatcher.match(uri)) {
            case COLLECTIONS:
                qb.setTables(DBHelper.Tables.COLLECTIONS.tableName);
                break;
            case COLLECTION_PAGES:
                qb.setTables(DBHelper.Tables.COLLECTION_PAGES.tableName);
                break;

            case ME_TRACKS:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.TRACK);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case USER_TRACKS:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection,uri.getPathSegments().get(1), CollectionItemTypes.TRACK);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case ME_FAVORITES:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.FAVORITE);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case USER_FAVORITES:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection,uri.getPathSegments().get(1), CollectionItemTypes.FAVORITE);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case ME_FOLLOWERS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.FOLLOWER);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case USER_FOLLOWERS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection,uri.getPathSegments().get(1), CollectionItemTypes.FOLLOWER);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case ME_FOLLOWINGS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.FOLLOWING);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case USER_FOLLOWINGS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection,uri.getPathSegments().get(1), CollectionItemTypes.FOLLOWING);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

             case ME_FRIENDS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.FRIEND);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case SUGGESTED_USERS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.SUGGESTED_USER);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case TRACKS:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(DBHelper.Tables.TRACKVIEW.tableName);
                break;
            case TRACK_ITEM:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(DBHelper.Tables.TRACKVIEW.tableName);
                whereAppend = DBHelper.TrackView.CONCRETE_ID + " = " + uri.getLastPathSegment();
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;


            case USERS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(DBHelper.Tables.USERS.tableName);
                break;

            case USER_ITEM:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(DBHelper.Tables.USERS.tableName);
                whereAppend = DBHelper.Users.CONCRETE_ID + " = " + uri.getLastPathSegment();
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;


            case SEARCHES:
                qb.setTables(DBHelper.Tables.SEARCHES.tableName);
                whereAppend = DBHelper.Searches.CONCRETE_USER_ID + " = "+ userId;
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;

            case SEARCHES_USERS_ITEM:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.SEARCH);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case SEARCHES_TRACKS_ITEM:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), CollectionItemTypes.SEARCH);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case TRACK_PLAYS:
                qb.setTables(DBHelper.Tables.TRACK_PLAYS.tableName);
                whereAppend = DBHelper.TrackPlays.CONCRETE_USER_ID + " = "+ userId;
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;

            case TRACK_PLAYS_ITEM:
                qb.setTables(DBHelper.Tables.TRACK_PLAYS.tableName);
                whereAppend = DBHelper.TrackPlays.CONCRETE_USER_ID + " = "+ userId + " AND "
                        + DBHelper.TrackPlays.CONCRETE_TRACK_ID + " = " + uri.getLastPathSegment();
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;

            case RECORDINGS:
                qb.setTables(DBHelper.Tables.RECORDINGS.tableName);
                whereAppend = DBHelper.Recordings.CONCRETE_USER_ID + " = "+ userId;
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;

            case RECORDING_ITEM:
                qb.setTables(DBHelper.Tables.RECORDINGS.tableName);
                whereAppend = DBHelper.Recordings.CONCRETE_ID + " = "+ uri.getLastPathSegment();
                selection = selection == null ? whereAppend : selection + " AND " + whereAppend;
                break;

            default:
                throw new IllegalArgumentException("No query available for: " + uri);
        }

        String q = qb.buildQuery(columns, selection, selectionArgs, null, null, sortOrder, null);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(q, selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        long id;
        Uri result;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {

            case COLLECTIONS:
                id = db.insertWithOnConflict(DBHelper.Tables.COLLECTIONS.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case COLLECTION_PAGES:
                id = db.insertWithOnConflict(DBHelper.Tables.COLLECTION_PAGES.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACKS:
                id = db.insertWithOnConflict(DBHelper.Tables.TRACKS.tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACK_PLAYS:
                id = db.insertWithOnConflict(DBHelper.Tables.TRACK_PLAYS.tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case SEARCHES:
                id = db.insertWithOnConflict(DBHelper.Tables.SEARCHES.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case USERS:
                System.out.println("Inserting id " + values.get("_id"));
                id = db.insertWithOnConflict(DBHelper.Tables.USERS.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                System.out.println("Insert got " + result);
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case ME_FAVORITES:
                id = db.insertWithOnConflict(DBHelper.Tables.TRACKS.tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id >= 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, userId);
                    cv.put(DBHelper.CollectionItems.ITEM_ID, (Long) values.get(DBHelper.Tracks._ID));
                    cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, CollectionItemTypes.FAVORITE);
                    id = db.insertWithOnConflict(DBHelper.Tables.COLLECTION_ITEMS.tableName, null, cv, SQLiteDatabase.CONFLICT_ABORT);
                    result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                    getContext().getContentResolver().notifyChange(result, null, false);
                    return result;
                } else {
                    throw new SQLException("No insert available for: " + uri);
                }

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        String tableName;
        switch (sUriMatcher.match(uri)) {
            case COLLECTIONS:
                tableName = DBHelper.Tables.COLLECTIONS.tableName;
                break;
            case COLLECTION_PAGES:
                tableName = DBHelper.Tables.COLLECTION_PAGES.tableName;
                break;
            case TRACK_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                tableName = DBHelper.Tables.TRACKS.tableName;
                break;
            case USER_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                tableName = DBHelper.Tables.USERS.tableName;
                break;
            case SEARCHES:
                tableName = DBHelper.Tables.SEARCHES.tableName;
                break;

            case ME_TRACKS:
                where = makeCollectionSelection(where, String.valueOf(userId), CollectionItemTypes.TRACK);
                tableName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                break;
            case ME_FAVORITES:
                where = makeCollectionSelection(where, String.valueOf(userId), CollectionItemTypes.FAVORITE);
                tableName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                break;
            case ME_FOLLOWINGS:
                where = makeCollectionSelection(where, String.valueOf(userId), CollectionItemTypes.FOLLOWING);
                tableName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                break;
            case ME_FOLLOWERS:
                where = makeCollectionSelection(where, String.valueOf(userId), CollectionItemTypes.FOLLOWER);
                tableName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        count = db.delete(tableName, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;

    }


    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TRACK_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(DBHelper.Tables.TRACKS.tableName, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case USER_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(DBHelper.Tables.USERS.tableName, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case SEARCHES_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(DBHelper.Tables.SEARCHES.tableName, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    // to replace rows
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        String[] extraCV = null;

        try {
            String tblName;
            switch (sUriMatcher.match(uri)) {
                case TRACKS:
                    tblName = DBHelper.Tables.TRACKS.tableName;
                    break;
                case USERS:
                    tblName = DBHelper.Tables.USERS.tableName;
                    break;
                case ME_TRACKS:
                case USER_TRACKS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.TRACK)};
                    break;
                case ME_FAVORITES:
                case USER_FAVORITES:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.FAVORITE)};
                    break;
                case ME_FOLLOWERS:
                case USER_FOLLOWERS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.FOLLOWER)};
                    break;
                case ME_FOLLOWINGS:
                case USER_FOLLOWINGS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.FOLLOWING)};
                    break;
                case ME_FRIENDS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.FRIEND)};
                    break;
                case SUGGESTED_USERS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.SUGGESTED_USER)};
                    break;
                case SEARCHES_USERS_ITEM:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.SEARCH)};
                    break;

                case SEARCHES_TRACKS_ITEM:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(CollectionItemTypes.SEARCH)};
                    break;
                default:

                    throw new IllegalArgumentException("Unknown URI " + uri);
            }


            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (extraCV != null) values[i].put(extraCV[0],extraCV[1]);
                if (db.replace(tblName, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(uri, null, false);
        return values.length;
    }

    static DBHelper.Tables getTable(String s) {
        DBHelper.Tables table = null;
        Matcher m = URL_PATTERN.matcher(s);
        if (m.matches()) {
            table = DBHelper.Tables.get(m.group(1));
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

    static String makeCollectionSort(Uri uri, String sortCol) {
        StringBuilder b = new StringBuilder();
        b.append(sortCol == null ? DBHelper.CollectionItems.POSITION : sortCol);
        if (!TextUtils.isEmpty(uri.getQueryParameter("limit"))) b.append(" LIMIT " + uri.getQueryParameter("limit"));
        if (!TextUtils.isEmpty(uri.getQueryParameter("offset"))) b.append(" OFFSET " + uri.getQueryParameter("offset"));
        return b.toString();
    }

    static String makeCollectionJoin(String modelTable, String modelIdCol){
        return modelTable + " INNER JOIN " + DBHelper.Tables.COLLECTION_ITEMS.tableName +
                        " ON (" + modelIdCol + " = " + DBHelper.CollectionItems.ITEM_ID+ ")";
    }

    static String makeCollectionSelection(String selection, String userId, int collectionType) {
        final String whereAppend = DBHelper.CollectionItems.CONCRETE_USER_ID + " = " + userId
                + " AND " + DBHelper.CollectionItems.CONCRETE_COLLECTION_TYPE + " = " + collectionType;
        return selection == null ? whereAppend : selection + " AND " + whereAppend;
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

    public static String[] formatWithUser(String[] columns, long userId){

        for (int i = 0; i < columns.length; i++){
            columns[i] = columns[i].replace("$$$",String.valueOf(userId));
        }
        return columns;
    }

    public static String[] fullTrackColumns = new String[]{
            DBHelper.Tables.TRACKVIEW.tableName + ".*",
            "EXISTS (SELECT 1 FROM " + DBHelper.Tables.COLLECTION_ITEMS.tableName
                    + " where " + DBHelper.TrackView.CONCRETE_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.FAVORITE
                    + " and " + DBHelper.CollectionItems.USER_ID + " = $$$) as " + DBHelper.TrackView.USER_FAVORITE,
    };

    public static String[] fullUserColumns = new String[]{
            DBHelper.Tables.USERS.tableName + ".*",
            "EXISTS (SELECT 1 FROM " + DBHelper.Tables.COLLECTION_ITEMS.tableName
                    + " where " + DBHelper.Users.CONCRETE_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.FOLLOWING
                    + " and " + DBHelper.CollectionItems.USER_ID + " = $$$) as "  + DBHelper.Users.USER_FOLLOWING,
            "EXISTS (SELECT 1 FROM " + DBHelper.Tables.COLLECTION_ITEMS.tableName
                    + " where " + DBHelper.Users.CONCRETE_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.FOLLOWER
                    + " and " + DBHelper.CollectionItems.USER_ID + " = $$$) as " + DBHelper.Users.USER_FOLLOWER
    };


    public interface CollectionItemTypes {
        int TRACK = 1;
        int FAVORITE = 2;
        int FOLLOWING = 3;
        int FOLLOWER = 4;
        int FRIEND = 5;
        int SUGGESTED_USER = 6;
        int SEARCH = 7;
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


        Uri ME_FRIENDS                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/connections/friends");
        Uri SUGGESTED_USERS             = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/suggested");

        Uri ME_SOUND_STREAM             = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/activities/tracks");
        Uri ME_EXCLUSIVE_STREAM         = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/activities/tracks/exclusive");
        Uri ME_ACTIVITIES               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/me/activities/all/own");

        Uri TRACKS                      = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks");
        Uri TRACK_ITEM                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/#");
        Uri TRACK_COMMENTS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/#/comments");
        Uri TRACK_PERMISSIONS           = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/#/permissions");
        Uri TRACK_SECRET_TOKEN          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/tracks/#/secret-token");

        Uri USERS                       = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users");
        Uri USER_ITEM                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#");
        Uri USER_TRACKS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/tracks");
        Uri USER_FAVORITES              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/favorites");
        Uri USER_FOLLOWERS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/followers");
        Uri USER_FOLLOWINGS             = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/followings");
        Uri USER_COMMENTS               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/comments");
        Uri USER_GROUPS                 = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/groups");
        Uri USER_PLAYLISTS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/users/#/playlists");

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
        Uri COLLECTIONS                   = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/COLLECTIONS");
        Uri COLLECTION_PAGES              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/COLLECTION_PAGES");

        Uri RECORDINGS                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/recordings");
        Uri RECORDING_ITEM              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/recordings/#");
        Uri EVENTS                      = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/events");
        Uri EVENT_ITEM                  = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/events/#");
        Uri SEARCHES                    = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches");
        Uri SEARCHES_ITEM               = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches/#");
        Uri SEARCHES_TRACKS             = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches/tracks");
        Uri SEARCHES_USERS              = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches/users");
        Uri SEARCHES_TRACK_ITEM         = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches/tracks/*");
        Uri SEARCHES_USER_ITEM          = Uri.parse("content://" + ScContentProvider.AUTHORITY +"/searches/users/*");
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
    private static final int ME_SOUND_STREAM        = 111;
    private static final int ME_EXCLUSIVE_STREAM    = 112;
    private static final int ME_ACTIVITIES          = 113;

    private static final int ME_FRIENDS             = 150;
    private static final int SUGGESTED_USERS        = 151;

    private static final int TRACKS                 = 201;
    private static final int TRACK_ITEM             = 202;
    private static final int TRACK_COMMENTS         = 203;
    private static final int TRACK_PERMISSIONS      = 204;
    private static final int TRACK_SECRET_TOKEN     = 205;

    private static final int USERS                  = 301;
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

    private static final int COLLECTIONS            = 1000;
    private static final int COLLECTION_PAGES       = 1001;

    private static final int RECORDINGS             = 1100;
    private static final int RECORDING_ITEM         = 1101;
    private static final int EVENTS                 = 1200;
    private static final int EVENT_ITEM             = 1201;
    private static final int TRACK_PLAYS            = 1300;
    private static final int TRACK_PLAYS_ITEM       = 1301;
    private static final int SEARCHES               = 1400;
    private static final int SEARCHES_ITEM          = 1401;
    private static final int SEARCHES_TRACKS        = 1402;
    private static final int SEARCHES_USERS         = 1403;
    private static final int SEARCHES_TRACKS_ITEM   = 1404;
    private static final int SEARCHES_USERS_ITEM    = 1405;



	public static UriMatcher buildMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(ScContentProvider.AUTHORITY, "me",ME);
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

        matcher.addURI(ScContentProvider.AUTHORITY, "me/connections/friends", ME_FRIENDS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/suggested", SUGGESTED_USERS);

        matcher.addURI(ScContentProvider.AUTHORITY, "me/activities/tracks", ME_SOUND_STREAM);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/activities/tracks/exclusive", ME_EXCLUSIVE_STREAM);
        matcher.addURI(ScContentProvider.AUTHORITY, "me/activities/all/own", ME_ACTIVITIES);

        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/#/comments", TRACK_COMMENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/#/permissions", TRACK_PERMISSIONS);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/#/secret-token", TRACK_SECRET_TOKEN);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks/#", TRACK_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "tracks", TRACKS);


        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/tracks", USER_TRACKS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/favorites", USER_FAVORITES);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/followers", USER_FOLLOWERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/followings", USER_FOLLOWINGS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/comments", USER_COMMENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/groups", USER_GROUPS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#/playlists", USER_PLAYLISTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "users/#", USER_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "users", USERS);

        matcher.addURI(ScContentProvider.AUTHORITY, "comments/#", COMMENT_ITEM);

        matcher.addURI(ScContentProvider.AUTHORITY, "playlists/#", PLAYLIST_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "playlists", PLAYLISTS);

        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/users", GROUP_USERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/moderators", GROUP_MODERATORS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/members", GROUP_MEMBERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/contributors", GROUP_CONTRIBUTORS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#/tracks", GROUP_TRACKS);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups/#", GROUP_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "groups", GROUPS);

        matcher.addURI(ScContentProvider.AUTHORITY, "COLLECTIONS", COLLECTIONS);
        matcher.addURI(ScContentProvider.AUTHORITY, "COLLECTION_PAGES", COLLECTION_PAGES);

        matcher.addURI(ScContentProvider.AUTHORITY, "recordings/#", RECORDING_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "recordings", RECORDINGS);
        matcher.addURI(ScContentProvider.AUTHORITY, "events/#", EVENT_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "events", EVENTS);
        matcher.addURI(ScContentProvider.AUTHORITY, "track_plays/#", TRACK_PLAYS_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "track_plays", TRACK_PLAYS);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches", SEARCHES);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches/#", SEARCHES_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches/tracks", SEARCHES_TRACKS);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches/users", SEARCHES_USERS);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches/tracks/*", SEARCHES_TRACKS_ITEM);
        matcher.addURI(ScContentProvider.AUTHORITY, "searches/users/*", SEARCHES_USERS_ITEM);

		return matcher;

	}

    /*static String TRACKVIEW_FAVORITE_JOIN = DBHelper.Tables.TRACKVIEW.tableName + " INNER JOIN " + DBHelper.Tables.COLLECTION_ITEMS.tableName +
                        " ON (" + DBHelper.TrackView.CONCRETE_ID + " = " + DBHelper.UserFavorites.CONCRETE_FAVORITE_ID+ ")";

    static String USER_FOLLOWING_JOIN = DBHelper.Tables.USERS.tableName + " INNER JOIN " + DBHelper.Tables.USER_FOLLOWING.tableName +
                        " ON (" + DBHelper.Users.CONCRETE_ID + " = " + DBHelper.UserFollowing.CONCRETE_FOLLOWING_ID+ ")";
      */


}
