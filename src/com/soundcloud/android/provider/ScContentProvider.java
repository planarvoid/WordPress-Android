package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.SoundCloudApplication;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScContentProvider extends ContentProvider {
    private static final String LOG_TAG = ScContentProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";
    public static final Pattern URL_PATTERN = Pattern.compile("^content://" + AUTHORITY + "/(\\w+)(?:/(-?\\d+))?$");
    public static final long DEFAULT_POLL_FREQUENCY = 3600l; // 1h

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

        Content content = Content.match(uri);
        switch (content) {
            case COLLECTION_ITEMS:
                qb.setTables(DBHelper.Tables.COLLECTION_ITEMS.tableName);
                break;
            case COLLECTIONS:
                qb.setTables(DBHelper.Tables.COLLECTIONS.tableName);
                break;
            case COLLECTION_PAGES:
                qb.setTables(DBHelper.Tables.COLLECTION_PAGES.tableName);
                break;

            case ME_TRACKS:
            case ME_FAVORITES:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), content.collectionType);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
            case ME_FRIENDS:
            case SUGGESTED_USERS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), content.collectionType);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case USER_TRACKS:
            case USER_FAVORITES:
                if (columns == null) columns = formatWithUser(fullTrackColumns, userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName, DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, uri.getPathSegments().get(1), content.collectionType);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;


            case USER_FOLLOWERS:
            case USER_FOLLOWINGS:
                if (columns == null) columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.USERS.tableName,DBHelper.Users.CONCRETE_ID));
                selection = makeCollectionSelection(selection,uri.getPathSegments().get(1), content.collectionType);
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
                selection = makeCollectionSelection(selection, String.valueOf(userId), SEARCH);
                sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case SEARCHES_TRACKS_ITEM:
                if (columns == null) columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(DBHelper.Tables.TRACKVIEW.tableName,DBHelper.TrackView.CONCRETE_ID));
                selection = makeCollectionSelection(selection, String.valueOf(userId), SEARCH);
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

            case UNKNOWN:
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
        switch (Content.match(uri)) {

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
                id = db.insertWithOnConflict(DBHelper.Tables.USERS.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case RECORDINGS:
                id = db.insert(DBHelper.Tables.RECORDINGS.tableName, null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case ME_FAVORITES:
                id = db.insertWithOnConflict(DBHelper.Tables.TRACKS.tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id >= 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, userId);
                    cv.put(DBHelper.CollectionItems.ITEM_ID, (Long) values.get(DBHelper.Tracks._ID));
                    cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, FAVORITE);
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
        final Content content = Content.match(uri);
        switch (content) {
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
            case ME_FAVORITES:
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
            case USER_TRACKS:
            case USER_FAVORITES:
            case USER_FOLLOWINGS:
            case USER_FOLLOWERS:
                where = makeCollectionSelection(where, String.valueOf(userId), content.collectionType);
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
        switch (Content.match(uri)) {
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
            case RECORDING_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(DBHelper.Tables.RECORDINGS.tableName, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case TRACK_CLEANUP:
                long userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0){
                    where = "_id NOT IN ("
                                    + "SELECT _id FROM "+ DBHelper.Tables.TRACKS.tableName + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.TRACK+ " ," + CollectionItemTypes.FAVORITE+ ") "
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " =  " + DBHelper.Tracks.ID
                                    + ")"
                                + ")";

                    final long start = System.currentTimeMillis();
                    count = db.delete(DBHelper.Tables.TRACKS.tableName,where,null);
                    Log.i(LOG_TAG,"Track cleanup done: deleted " + count + " tracks in " + (System.currentTimeMillis() - start) + " ms");
                    getContext().getContentResolver().notifyChange(Content.TRACKS.uri, null, false);
                    return count;
                }
                return 0;

            case USERS_CLEANUP:
                userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0) {
                    where = "_id NOT IN (SELECT DISTINCT " + DBHelper.Tracks.USER_ID + " FROM "+ DBHelper.Tables.TRACKS.tableName + " UNION "
                                    + "SELECT _id FROM "+ DBHelper.Tables.USERS.tableName + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.FOLLOWER+ " ," + CollectionItemTypes.FOLLOWING+ ") "
                                        + " AND " + DBHelper.CollectionItems.CONCRETE_USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " = " + DBHelper.Users.CONCRETE_ID
                                    + ")"
                                + ") AND _id <> " + userId;
                    final long start = System.currentTimeMillis();
                    count = db.delete(DBHelper.Tables.USERS.tableName,where,null);
                    Log.i(LOG_TAG,"User cleanup done: deleted " + count + " users in " + (System.currentTimeMillis() - start) + " ms");
                    getContext().getContentResolver().notifyChange(Content.USERS.uri, null, false);
                    return count;
                }
                return 0;

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
            final Content content = Content.match(uri);
            switch (content) {
                case ME_TRACKS:
                case USER_TRACKS:
                case ME_FAVORITES:
                case USER_FAVORITES:
                case ME_FOLLOWERS:
                case USER_FOLLOWERS:
                case ME_FOLLOWINGS:
                case USER_FOLLOWINGS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(content.collectionType)};
                    break;

                case TRACKS:
                    tblName = DBHelper.Tables.TRACKS.tableName;
                    break;
                case USERS:
                    tblName = DBHelper.Tables.USERS.tableName;
                    break;
                case ME_FRIENDS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(FRIEND)};
                    break;
                case SUGGESTED_USERS:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SUGGESTED_USER)};
                    break;
                case SEARCHES_USERS_ITEM:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SEARCH)};
                    break;

                case SEARCHES_TRACKS_ITEM:
                    tblName = DBHelper.Tables.COLLECTION_ITEMS.tableName;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SEARCH)};
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
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FAVORITE
                    + " and " + DBHelper.CollectionItems.USER_ID + " = $$$) as " + DBHelper.TrackView.USER_FAVORITE,
    };

    public static String[] fullUserColumns = new String[]{
            DBHelper.Tables.USERS.tableName + ".*",
            "EXISTS (SELECT 1 FROM " + DBHelper.Tables.COLLECTION_ITEMS.tableName
                    + " where " + DBHelper.Users.CONCRETE_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FOLLOWING
                    + " and " + DBHelper.CollectionItems.USER_ID + " = $$$) as "  + DBHelper.Users.USER_FOLLOWING,
            "EXISTS (SELECT 1 FROM " + DBHelper.Tables.COLLECTION_ITEMS.tableName
                    + " where " + DBHelper.Users.CONCRETE_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " and " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FOLLOWER
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
}
