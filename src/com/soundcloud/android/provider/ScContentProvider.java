package com.soundcloud.android.provider;

import static com.soundcloud.android.model.Activity.Type;
import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.SoundCloudApplication;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;


public class ScContentProvider extends ContentProvider {
    private static final String TAG = ScContentProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(final Uri uri,
                        final String[] columns,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        final SCQueryBuilder qb = new SCQueryBuilder();
        String[] _columns = columns;
        String _sortOrder = sortOrder;
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTION_ITEMS:
                qb.setTables(Table.COLLECTION_ITEMS.name);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case COLLECTIONS:
                qb.setTables(Table.COLLECTIONS.name);
                break;
            case COLLECTION_PAGES:
                qb.setTables(Table.COLLECTION_PAGES.name);
                break;

            case ME_TRACKS:
            case ME_FAVORITES:
                qb.setTables(makeCollectionJoin(Table.TRACK_VIEW));
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                makeCollectionSelection(qb, String.valueOf(userId), content.collectionType);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
            case ME_FRIENDS:
            case SUGGESTED_USERS:
                qb.setTables(makeCollectionJoin(Table.USERS));
                if (_columns == null) _columns = formatWithUser(fullUserColumns, userId);
                makeCollectionSelection(qb, String.valueOf(userId), content.collectionType);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case USER_TRACKS:
            case USER_FAVORITES:
                qb.setTables(makeCollectionJoin(Table.TRACK_VIEW));
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                makeCollectionSelection(qb, uri.getPathSegments().get(1), content.collectionType);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case USER_FOLLOWERS:
            case USER_FOLLOWINGS:
                qb.setTables(makeCollectionJoin(Table.USERS));
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                makeCollectionSelection(qb, uri.getPathSegments().get(1), content.collectionType);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;


            case TRACKS:
                qb.setTables(Table.TRACK_VIEW.name);
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                break;
            case TRACK_ITEM:
                qb.setTables(Table.TRACK_VIEW.name);
                qb.appendWhere(Table.TRACK_VIEW.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                break;

            case USERS:
                qb.setTables(Table.USERS.name);
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                break;

            case USER_ITEM:
                qb.setTables(Table.USERS.name);
                qb.appendWhere(Table.USERS.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                break;

            case SEARCHES:
                qb.setTables(Table.SEARCHES.name);
                qb.appendWhere(Table.SEARCHES.id + " = "+ userId);
                break;

            case SEARCHES_USERS_ITEM:
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(Table.USERS));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case SEARCHES_TRACKS_ITEM:
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(Table.TRACK_VIEW));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case TRACK_PLAYS:
                qb.setTables(Table.TRACK_PLAYS.name);
                qb.appendWhere(Table.TRACK_PLAYS.id + " = "+ userId);
                break;

            case TRACK_PLAYS_ITEM:
                qb.setTables(Table.TRACK_PLAYS.name);
                qb.appendWhere(Table.TRACK_PLAYS.id + " = " + uri.getLastPathSegment());
                break;

            case RECORDINGS:
                qb.setTables(Table.RECORDINGS.name);
                qb.appendWhere(Table.RECORDINGS.id + " = "+ userId);
                break;

            case RECORDING_ITEM:
                qb.setTables(Table.RECORDINGS.name);
                qb.appendWhere(Table.RECORDINGS.id + " = "+ uri.getLastPathSegment());
                break;

            case ME_ALL_ACTIVITIES:
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
            case ME_EXCLUSIVE_STREAM:
                qb.setTables(Table.ACTIVITY_VIEW.name);
                switch (content) {
                    case ME_SOUND_STREAM:
                        selectActivityTypes(qb, Type.TRACK, Type.TRACK_SHARING);
                        break;
                    case ME_EXCLUSIVE_STREAM:
                        selectActivityTypes(qb, Type.TRACK, Type.TRACK_SHARING)
                            .appendWhere(" AND "+DBHelper.ActivityView.TAGS+" LIKE '%exclusive%'");
                        break;
                    case ME_ACTIVITIES:
                        selectActivityTypes(qb, Type.FAVORITING, Type.COMMENT);
                        break;
                }
                break;
            case COMMENTS:
                qb.setTables(Table.COMMENTS.name);
                break;
            case PLAYLISTS:
                qb.setTables(Table.PLAYLIST.name);
                qb.appendWhere("_id = "+ userId);
                break;

            case PLAYLIST_ITEMS:
                qb.setTables(Table.TRACK_VIEW + " INNER JOIN " + Table.PLAYLIST_ITEMS.name +
                        " ON (" + Table.TRACK_VIEW + "._id" + " = " + DBHelper.PlaylistItems.ITEM_ID + ")");
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                qb.appendWhere(Table.PLAYLIST_ITEMS.name+"."+ DBHelper.PlaylistItems.USER_ID + " = " + userId);
                qb.appendWhere(" AND "+DBHelper.PlaylistItems.PLAYLIST_ID + " = " + uri.getLastPathSegment());
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case UNKNOWN:
            default:
                throw new IllegalArgumentException("No query available for: " + uri);
        }

        final String q = qb.buildQuery(_columns, selection, selectionArgs /* ignored, see below */,
                null, null, _sortOrder, null);
        Log.d(TAG, "query: "+q);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(q, selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        long id;
        Uri result;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (Content.match(uri)) {
            case COLLECTIONS:
                id = db.insertWithOnConflict(Table.COLLECTIONS.name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case COLLECTION_PAGES:
                id = db.insertWithOnConflict(Table.COLLECTION_PAGES.name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACKS:
                id = db.insertWithOnConflict(Table.TRACKS.name, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACK_PLAYS:
                id = db.insertWithOnConflict(Table.TRACK_PLAYS.name, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case SEARCHES:
                id = db.insertWithOnConflict(Table.SEARCHES.name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case USERS:
                id = db.insertWithOnConflict(Table.USERS.name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case RECORDINGS:
                id = db.insert(Table.RECORDINGS.name, null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case ME_FAVORITES:
                id = db.insertWithOnConflict(Table.TRACKS.name, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id >= 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, userId);
                    cv.put(DBHelper.CollectionItems.ITEM_ID, (Long) values.get(DBHelper.Tracks._ID));
                    cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, FAVORITE);
                    id = db.insertWithOnConflict(Table.COLLECTION_ITEMS.name, null, cv, SQLiteDatabase.CONFLICT_ABORT);
                    result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                    getContext().getContentResolver().notifyChange(result, null, false);
                    return result;
                } else {
                    throw new SQLException("No insert available for: " + uri);
                }

            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
            case ME_EXCLUSIVE_STREAM:
                id = db.insertWithOnConflict(Table.ACTIVITIES.name(),  null, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                return result;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        String tableName;
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTIONS:
                tableName = Table.COLLECTIONS.name;
                break;
            case COLLECTION_PAGES:
                tableName = Table.COLLECTION_PAGES.name;
                break;
            case TRACK_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                tableName = Table.TRACKS.name;
                break;
            case USER_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                tableName = Table.USERS.name;
                break;
            case SEARCHES:
                tableName = Table.SEARCHES.name;
                break;
            case PLAYLIST_ITEMS:

                where = TextUtils.isEmpty(where) ? DBHelper.PlaylistItems.PLAYLIST_ID + "=" + uri.getLastPathSegment()
                        : where + " AND " + DBHelper.PlaylistItems.PLAYLIST_ID + "=" + uri.getLastPathSegment();
                tableName = Table.PLAYLIST_ITEMS.name;
                break;

            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
            case ME_SOUND_STREAM:
            case ME_EXCLUSIVE_STREAM:
                tableName = Table.ACTIVITIES.name;
                break;

            case ME_TRACKS:
            case ME_FAVORITES:
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
            case USER_TRACKS:
            case USER_FAVORITES:
            case USER_FOLLOWINGS:
            case USER_FOLLOWERS:
                tableName = Table.COLLECTION_ITEMS.name;
                //TODO
                //makeCollectionSelection(qb, String.valueOf(userId), content.collectionType);
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
                count = db.update(Table.TRACKS.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case USER_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(Table.USERS.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case SEARCHES_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(Table.SEARCHES.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case RECORDINGS:
                count = db.update(Table.RECORDINGS.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case RECORDING_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(Table.RECORDINGS.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;

            case TRACK_CLEANUP:
                long userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0){
                    where = "_id NOT IN ("
                                    + "SELECT _id FROM "+ Table.TRACKS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.TRACK+ " ," + CollectionItemTypes.FAVORITE+ ") "
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " =  " + DBHelper.Tracks._ID
                                    + ")"
                                + ")";

                    final long start = System.currentTimeMillis();
                    count = db.delete(Table.TRACKS.name,where,null);
                    Log.d(TAG,"Track cleanup done: deleted " + count + " tracks in " + (System.currentTimeMillis() - start) + " ms");
                    getContext().getContentResolver().notifyChange(Content.TRACKS.uri, null, false);
                    return count;
                }
                return 0;

            case USERS_CLEANUP:
                userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0) {
                    where = "_id NOT IN (SELECT DISTINCT " + DBHelper.Tracks.USER_ID + " FROM "+ Table.TRACKS.name + " UNION "
                                    + "SELECT _id FROM "+ Table.USERS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.FOLLOWER+ " ," + CollectionItemTypes.FOLLOWING+ ") "
                                        + " AND " + Table.COLLECTION_ITEMS.id + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " = " + Table.USERS.id
                                    + ")"
                                + ") AND _id <> " + userId;
                    final long start = System.currentTimeMillis();
                    count = db.delete(Table.USERS.name, where, null);
                    Log.d(TAG,"User cleanup done: deleted " + count + " users in " + (System.currentTimeMillis() - start) + " ms");
                    getContext().getContentResolver().notifyChange(Content.USERS.uri, null, false);
                    return count;
                }
                return 0;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (values == null || values.length == 0) return 0;

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
                    tblName = Table.COLLECTION_ITEMS.name;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(content.collectionType)};
                    break;

                case TRACKS:
                    tblName = Table.TRACKS.name;
                    break;
                case USERS:
                    tblName = Table.USERS.name;
                    break;
                case ME_FRIENDS:
                    tblName = Table.COLLECTION_ITEMS.name;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(FRIEND)};
                    break;
                case SUGGESTED_USERS:
                    tblName = Table.COLLECTION_ITEMS.name;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SUGGESTED_USER)};
                    break;
                case SEARCHES_USERS_ITEM:
                    tblName = Table.COLLECTION_ITEMS.name;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SEARCH)};
                    break;

                case SEARCHES_TRACKS_ITEM:
                    tblName = Table.COLLECTION_ITEMS.name;
                    extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(SEARCH)};
                    break;
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                    tblName = Table.ACTIVITIES.name;
                    break;

                case COMMENTS:
                    tblName = Table.COMMENTS.name;
                    break;
                case PLAYLISTS:
                    tblName = Table.PLAYLIST.name;
                    break;
                case PLAYLIST_ITEMS:
                    tblName = Table.PLAYLIST_ITEMS.name;
                    extraCV = new String[]{DBHelper.PlaylistItems.PLAYLIST_ID, String.valueOf(uri.getLastPathSegment())};
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }
            boolean failed = false;
            for (ContentValues v : values) {
                if (extraCV != null) v.put(extraCV[0],extraCV[1]);
                Log.d(TAG, "bulkInsert: "+v);

                if (db.replace(tblName, null, v) < 0) {
                    Log.w(TAG, "replace returned failure");
                    failed = true;
                    break;
                }
            }
            if (!failed) db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (values.length != 0) getContext().getContentResolver().notifyChange(uri, null, false);
        return values.length;
    }

    static String makeCollectionSort(Uri uri, String sortCol) {
        StringBuilder b = new StringBuilder();
        b.append(sortCol == null ? DBHelper.CollectionItems.POSITION : sortCol);
        String limit = uri.getQueryParameter("limit");
        if (!TextUtils.isEmpty(limit)) b.append(" LIMIT ").append(limit);
        String offset = uri.getQueryParameter("offset");
        if (!TextUtils.isEmpty(offset)) b.append(" OFFSET ").append(offset);
        return b.toString();
    }

    static String makeCollectionJoin(Table table){
        return table.name + " INNER JOIN " + Table.COLLECTION_ITEMS.name +
            " ON (" + table.name+"._id"+" = " + DBHelper.CollectionItems.ITEM_ID+ ")";
    }

    static SCQueryBuilder makeCollectionSelection(SCQueryBuilder qb,
                                                      String userId, int collectionType) {

        qb.appendWhere(Table.COLLECTION_ITEMS.name+"."+ DBHelper.CollectionItems.USER_ID + " = " + userId);
        qb.appendWhere(" AND "+DBHelper.CollectionItems.COLLECTION_TYPE + " = " + collectionType);
        return qb;
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

    // XXX ghetto, use prepared statements
    public static String[] formatWithUser(String[] columns, long userId){
        for (int i = 0; i < columns.length; i++){
            columns[i] = columns[i].replace("$$$",String.valueOf(userId));
        }
        return columns;
    }

    public static String[] fullTrackColumns = new String[]{
            Table.TRACK_VIEW + ".*",
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + Table.TRACK_VIEW.id + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FAVORITE
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.TrackView.USER_FAVORITE,
    };

    public static String[] fullUserColumns = new String[]{
            Table.USERS + ".*",
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " +  Table.USERS.id + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FOLLOWING
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS "  + DBHelper.Users.USER_FOLLOWING,
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + Table.USERS.id + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + FOLLOWER
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.Users.USER_FOLLOWER
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

    private static SCQueryBuilder selectActivityTypes(SCQueryBuilder qb, Type... types) {
        if (types.length > 0) {
            StringBuilder sb = new StringBuilder("type in(");
            for (int i=0; i<types.length; i++) {
                sb.append("'").append(types[i].type).append("'");
                if (i<types.length -1 ) sb.append(',');
            }
            sb.append(")");

            qb.appendWhere(sb);
        }
        return qb;
    }
}
