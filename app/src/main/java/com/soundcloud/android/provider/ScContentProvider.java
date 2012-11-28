package com.soundcloud.android.provider;

import static com.soundcloud.android.Consts.GraphicSize;
import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.IOUtils;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;


public class ScContentProvider extends ContentProvider {
    private static final String TAG = ScContentProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";

    public static interface Parameter {
        String RANDOM = "random";
        String CACHED = "cached";
        String LIMIT  = "limit";
        String OFFSET = "offset";
    }

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

        return safeExecute(new DbOperation<Cursor>() {
            @Override
            public Cursor execute() {
                return doQuery(uri, columns, selection, selectionArgs, sortOrder);
            }
        }, null);
    }

    private Cursor doQuery(final Uri uri,
                        final String[] columns,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        final SCQueryBuilder qb = new SCQueryBuilder();
        String[] _columns = columns;
        String _selection = selection;
        String[] _selectionArgs = selectionArgs;
        String _sortOrder = sortOrder;
        final Content content = Content.match(uri);
        String query = null;
        switch (content) {

            case ME:
                qb.setTables(content.table.name);
                _selection = "_id = ?";
                _selectionArgs = new String[] {String.valueOf(userId)};
                break;
            case COLLECTION_ITEMS:
                qb.setTables(content.table.name);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case COLLECTIONS:
                qb.setTables(content.table.name);
                break;
            case COLLECTION_PAGES:
                qb.setTables(content.table.name);
                break;

            case ME_SOUNDS :
                qb.setTables(makeCollectionJoin(Table.SOUND_ASSOCIATION_VIEW));

                if (_columns == null) _columns = formatWithUser(fullSoundAssociationColumns, userId);
                makeSoundAssociationSelection(qb, String.valueOf(userId),
                        new int[]{CollectionItemTypes.TRACK, CollectionItemTypes.REPOST});
                makeCollectionSort(uri, "");// send empty string instead of null, we want the default order

                break;

            case ME_TRACKS:
            case ME_LIKES:
            case ME_REPOSTS:
                qb.setTables(makeCollectionJoin(Table.SOUND_VIEW));
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                makeCollectionSelection(qb, String.valueOf(userId), content.collectionType);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND "+ DBHelper.SoundView.CACHED + "= 1");
                }
                break;

            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
            case ME_FRIENDS:
            case SUGGESTED_USERS:
                qb.setTables(makeCollectionJoin(Table.USERS));
                if (_columns == null){
                    _columns = formatWithUser(fullUserColumns, userId);
                    if (content == Content.ME_FRIENDS) {
                        _sortOrder = makeCollectionSort(uri, sortOrder == null ?
                                DBHelper.Users.USER_FOLLOWING + " ASC, " + DBHelper.Users._ID + " ASC" : sortOrder);
                    } else {
                        _sortOrder = makeCollectionSort(uri, sortOrder);
                    }
                } else {
                    _sortOrder = makeCollectionSort(uri, sortOrder);
                }
                makeCollectionSelection(qb, String.valueOf(userId), content.collectionType);
                break;

            case ME_USERID:
                MatrixCursor c = new MatrixCursor(new String[] { BaseColumns._ID}, 1);
                c.addRow(new Object[]{SoundCloudApplication.getUserId()});
                return c;

            case USER_TRACKS:
            case USER_LIKES:
            case USER_REPOSTS:
                qb.setTables(makeCollectionJoin(Table.SOUND_VIEW));
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
            case PLAYLISTS:
                qb.setTables(Table.SOUND_VIEW.name);
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
                    _sortOrder = "RANDOM()";
                }
                appendSoundType(qb, content);
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND " + DBHelper.SoundView.CACHED + "= 1");
                }
                break;

            case TRACK:
                qb.setTables(Table.SOUND_VIEW.name);
                appendSoundType(qb, content);
                qb.appendWhere(" AND " + Table.SOUND_VIEW.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                break;

            case USERS:
                qb.setTables(content.table.name);
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                break;

            case USER:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.USERS.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                break;

            case SEARCHES:
                qb.setTables(content.table.name);
                qb.appendWhere(DBHelper.Searches.USER_ID + " = " + userId);
                break;

            case SEARCHES_USER:
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(Table.USERS));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case SEARCHES_TRACK:
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(Table.SOUND_VIEW));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case TRACK_PLAYS:
                qb.setTables(content.table.name);
                qb.appendWhere(DBHelper.TrackMetadata.USER_ID + " = "+ userId);
                break;

            case TRACK_PLAYS_ITEM:
                qb.setTables(content.table.name);
                qb.appendWhere(content.table.id + " = " + uri.getLastPathSegment());
                break;

            case TRACK_METADATA:
                qb.setTables(content.table.name);
                qb.appendWhere(DBHelper.TrackMetadata.USER_ID + " = "+ userId);
                break;

            case RECORDINGS:
                qb.setTables(content.table.name +
                        " LEFT OUTER JOIN "+Table.USERS+
                        " ON "+content.table.field(DBHelper.Recordings.PRIVATE_USER_ID)+
                        "="+Table.USERS.field(DBHelper.Users._ID));
                String user_selection = DBHelper.Recordings.USER_ID+"="+userId;
                if (selection != null) {
                    user_selection += (" AND " +selection);
                }
                query = qb.buildQuery(
                        new String[] { content.table.allFields(), DBHelper.Users.USERNAME },
                        user_selection,
                        null,
                        null,
                        _sortOrder, null);
                break;
            case RECORDING:
                qb.setTables(content.table.name +
                                        " LEFT OUTER JOIN "+Table.USERS+
                                        " ON "+content.table.field(DBHelper.Recordings.PRIVATE_USER_ID)+
                                        "="+Table.USERS.field(DBHelper.Users._ID));

                qb.appendWhere(Table.RECORDINGS.id + " = "+ uri.getLastPathSegment());
                query = qb.buildQuery(new String[] { content.table.allFields(), DBHelper.Users.USERNAME },
                                        selection,
                                        null,
                                        null,
                                        _sortOrder, null);
                break;

            case ME_SOUND_STREAM:
                if (_columns == null) _columns = formatWithUser(fullActivityColumns, userId);
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(DBHelper.SoundView.CACHED + "= 1 AND ");
                }

            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
                qb.setTables(Table.ACTIVITY_VIEW.name);
                if (content != Content.ME_ALL_ACTIVITIES) {
                    // filter out playlist
                    String types = "";
                    for (int i=0; i<Activity.Type.PLAYLIST_TYPES.length;i++) {
                        types += "'"+Activity.Type.PLAYLIST_TYPES[i].type+"'";
                        if (i < Activity.Type.PLAYLIST_TYPES.length-1) {
                            types += ",";
                        }
                    }
                    qb.appendWhere(DBHelper.ActivityView.CONTENT_ID + "=" + content.id + " AND " +
                            DBHelper.ActivityView.TYPE + " NOT IN ( " +types +" ) ");
                }
                _sortOrder = makeActivitiesSort(uri, sortOrder);
                break;
            case COMMENTS:
                qb.setTables(content.table.name);
                break;
            case PLAY_QUEUE_ITEM:
                qb.setTables(content.table.name);
                qb.appendWhere("_id = " + userId);
                break;

            case PLAY_QUEUE:
                qb.setTables(Table.SOUND_VIEW + " INNER JOIN " + Table.PLAY_QUEUE.name +
                        " ON (" + Table.SOUND_VIEW.id + " = " + DBHelper.PlayQueue.TRACK_ID + ")");
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                qb.appendWhere(Table.PLAY_QUEUE.name+"."+ DBHelper.PlayQueue.USER_ID + " = " + userId);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case ANDROID_SEARCH_SUGGEST:
            case ANDROID_SEARCH_SUGGEST_PATH:
                return suggest(uri, columns, selection, selectionArgs);

            case ANDROID_SEARCH_REFRESH:
            case ANDROID_SEARCH_REFRESH_PATH:
                return refresh(uri, columns, selection, selectionArgs, sortOrder);

            case ME_SHORTCUT:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.SUGGESTIONS.id + " = " + uri.getLastPathSegment());
                break;

            case ME_CONNECTION:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.CONNECTIONS.id + " = " + uri.getLastPathSegment());
                break;

            case ME_SHORTCUTS:
            case ME_CONNECTIONS:
                qb.setTables(content.table.name);


                break;

            case UNKNOWN:
            default:
                throw new IllegalArgumentException("No query available for: " + uri);
        }

        if (query == null) {
            query = qb.buildQuery(_columns, _selection, null /* selectionArgs passed further down */, null,_sortOrder, null);
        }
        log("query: "+query);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(query, _selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void appendSoundType(SCQueryBuilder qb, Content content) {
        qb.appendWhere(" " + DBHelper.SoundView._TYPE + " = '" +
                (content.modelType == Track.class ? Sound.DB_TYPE_TRACK : Sound.DB_TYPE_PLAYLIST)
                + "'");
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        return safeExecute(new DbOperation<Uri>() {
            @Override
            public Uri execute() {
                return doInsert(uri, values);
            }
        }, null);
    }

    private Uri doInsert(final Uri uri, final ContentValues values) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        long id;
        Uri result;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTIONS:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case COLLECTION_PAGES:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACKS:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;
            case TRACK_METADATA:
                if (!values.containsKey(DBHelper.TrackMetadata.USER_ID)) {
                    values.put(DBHelper.TrackMetadata.USER_ID, userId);
                }
                content.table.upsert(db, new ContentValues[] {values} );
                return uri.buildUpon().appendPath(
                        values.getAsString(DBHelper.TrackMetadata._ID)).build();
            case TRACK_PLAYS:
                // TODO should be in update()
                if (!values.containsKey(DBHelper.TrackMetadata.USER_ID)) {
                    values.put(DBHelper.TrackMetadata.USER_ID, userId);
                }
                String trackId = values.getAsString(DBHelper.TrackMetadata._ID);
                content.table.insertWithOnConflict(db,values,SQLiteDatabase.CONFLICT_IGNORE);

                String counter = DBHelper.TrackMetadata.PLAY_COUNT;
                db.execSQL("UPDATE "+content.table.name+
                        " SET "+counter+"="+counter+" + 1 WHERE "+content.table.id +"= ?",
                        new String[] {trackId}) ;
                result = uri.buildUpon().appendPath(trackId).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;
            case SEARCHES:
                if (!values.containsKey(DBHelper.Searches.USER_ID)) {
                    values.put(DBHelper.Searches.USER_ID, userId);
                }
                id = db.insert(content.table.name, null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;
            case USERS:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case RECORDINGS:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_REPLACE);
                if (id >= 0) {
                    result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                    getContext().getContentResolver().notifyChange(result, null, false);
                    return result;
                } else {
                    return null;
                }

            case ME_LIKES:
            case ME_REPOSTS:
                id = Table.SOUNDS.upsertSingle(db, values);
                if (id >= 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, userId);
                    cv.put(DBHelper.CollectionItems.POSITION, System.currentTimeMillis());
                    cv.put(DBHelper.CollectionItems.ITEM_ID, (Long) values.get(DBHelper.Sounds._ID));
                    cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, content.collectionType);
                    id = Table.COLLECTION_ITEMS.insertWithOnConflict(db, cv, SQLiteDatabase.CONFLICT_ABORT);
                    result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                    getContext().getContentResolver().notifyChange(result, null, false);
                    return result;
                } else {
                    throw new SQLException("No insert available for: " + uri);
                }

            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
                id = content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                return result;

            case ME_SHORTCUTS:
                id = content.table.insertOrReplace(db, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                return result;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(final Uri uri, final String where, final String[] whereArgs) {
        return safeExecute(new DbOperation<Integer>() {
            @Override
            public Integer execute() {
                return doDelete(uri, where, whereArgs);
            }
        }, 0);
    }

    private int doDelete(Uri uri, String where, String[] whereArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        final Content content = Content.match(uri);

        switch (content) {
            case COLLECTIONS:
            case COLLECTION_PAGES:
            case SEARCHES:
            case RECORDINGS:
            case PLAY_QUEUE:
            case ME_CONNECTIONS:
            case PLAYLISTS:
            case ME_ALL_ACTIVITIES:
                break;

            case TRACK:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;
            case USER:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;
            case ME_ACTIVITIES:
            case ME_SOUND_STREAM:
                where = DBHelper.Activities.CONTENT_ID+"= ?";
                whereArgs = new String[] {String.valueOf(content.id) };
                break;
            case RECORDING:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;

            case ME_TRACKS:
            case ME_LIKES:
            case ME_REPOSTS:
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
            case USER_TRACKS:
            case USER_LIKES:
            case USER_REPOSTS:
            case USER_FOLLOWINGS:
            case USER_FOLLOWERS:
            case ME_FRIENDS:
                final String whereAppend = Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + SoundCloudApplication.getUserIdFromContext(getContext())
                        + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + content.collectionType;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;

                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.delete(content.table.name , where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String where, final String[] whereArgs) {
        return safeExecute(new DbOperation<Integer>() {
            @Override
            public Integer execute() {
                return doUpdate(uri, values, where, whereArgs);
            }
        }, 0);
    }

    private int doUpdate(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTIONS:
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case COLLECTION:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case TRACK:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case USER:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case SEARCHES_ITEM:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case RECORDINGS:
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case RECORDING:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;

            case TRACK_CLEANUP:
                long userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0){
                    where = "_id NOT IN ("
                                    + "SELECT _id FROM "+ Table.SOUNDS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.TRACK+
                                            " ," + CollectionItemTypes.LIKE + ") "
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " =  " + DBHelper.Sounds._ID
                                    + " UNION SELECT DISTINCT " + DBHelper.ActivityView.SOUND_ID + " FROM "+ Table.ACTIVITIES.name
                                    + " UNION SELECT DISTINCT " + DBHelper.PlayQueue.TRACK_ID + " FROM "+ Table.PLAY_QUEUE.name
                                    + ")"
                                + ")";

                    final long start = System.currentTimeMillis();
                    count = db.delete(Table.SOUNDS.name,where,null);
                    log("Track cleanup done: deleted " + count + " tracks in " + (System.currentTimeMillis() - start) + " ms");
                    getContext().getContentResolver().notifyChange(Content.TRACKS.uri, null, false);
                    return count;
                }
                return 0;

            case USERS_CLEANUP:
                userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0) {
                    where = "_id NOT IN (SELECT DISTINCT " + DBHelper.Sounds.USER_ID + " FROM "+ Table.SOUNDS.name + " UNION "
                                    + "SELECT _id FROM "+ Table.USERS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.FOLLOWER+ " ," + CollectionItemTypes.FOLLOWING+ ") "
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " = " + Table.USERS.id
                                    + " UNION SELECT DISTINCT " + DBHelper.ActivityView.USER_ID + " FROM "+ Table.ACTIVITIES.name
                                    + ")"
                                + ") AND _id <> " + userId;
                    final long start = System.currentTimeMillis();
                    count = db.delete(Table.USERS.name, where, null);
                    log("User cleanup done: deleted " + count + " users in " + (System.currentTimeMillis() - start) + " ms");
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
        String[] extraCV = null;
        boolean recreateTable = false;

        final Content content = Content.match(uri);
        final Table table;
        switch (content) {
            case TRACKS:
            case USERS:
            case RECORDINGS:
            case SOUNDS:
            case PLAYLISTS:
                content.table.upsert(db, values);
                if (values.length != 0) getContext().getContentResolver().notifyChange(uri, null, false);
                return values.length;

            case COMMENTS:
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
            case PLAY_QUEUE:
                table = content.table;
                break;

            case ME_SOUNDS:
                table = Table.COLLECTION_ITEMS;
                break;

            case ME_TRACKS:
            case USER_TRACKS:
            case ME_LIKES:
            case USER_LIKES:
            case ME_REPOSTS:
            case USER_REPOSTS:
            case ME_FOLLOWERS:
            case USER_FOLLOWERS:
            case ME_FOLLOWINGS:
            case USER_FOLLOWINGS:
            case ME_FRIENDS:
            case SUGGESTED_USERS:
            case SEARCHES_USER:
            case SEARCHES_TRACK:
                table = Table.COLLECTION_ITEMS;
                extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(content.collectionType)};
                break;


            case ME_SHORTCUTS:
                recreateTable = true;
                table = content.table;
                break;

            default:
                table = content.table;
        }

        if (table == null) throw new IllegalArgumentException("No table for URI "+uri);

        try {
            db.beginTransaction();
            boolean failed = false;

            if (recreateTable) {
                db.delete(table.name, null, null);
            }

            for (ContentValues v : values) {
                if (v != null){
                    if (extraCV != null) v.put(extraCV[0], extraCV[1]);
                    log("bulkInsert: " + v);

                    if (db.insertWithOnConflict(table.name, null, v, SQLiteDatabase.CONFLICT_REPLACE) < 0) {
                        Log.w(TAG, "replace returned failure");
                        failed = true;
                        break;
                    }
                }
            }

            if (content == Content.ME_SHORTCUTS) {
                db.execSQL("INSERT OR IGNORE INTO " + Table.USERS.name + " (_id, username, avatar_url, permalink_url) " +
                        " SELECT id, text, icon_url, permalink_url FROM " + Table.SUGGESTIONS.name + " where kind = 'following'");
                db.execSQL("INSERT OR IGNORE INTO " + Table.SOUNDS.name + " (_id, title, artwork_url, permalink_url) " +
                        " SELECT id, text, icon_url, permalink_url FROM " + Table.SUGGESTIONS.name + " where kind = 'like'");
            }


            if (!failed) db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (values.length != 0) getContext().getContentResolver().notifyChange(uri, null, false);
        return values.length;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        switch (Content.match(uri)) {
            case ME_SHORTCUTS_ICON:
                List<String> segments = uri.getPathSegments();
                long suggestId = Long.parseLong(segments.get(segments.size() - 1));

                Cursor c = query(Content.ME_SHORTCUT.forId(suggestId), null, null, null, null);
                try {
                    if (c != null && c.moveToFirst()) {
                        String url = c.getString(c.getColumnIndex(DBHelper.Suggestions.ICON_URL));
                        if (url != null) {
                            final String listUrl = GraphicSize.getSearchSuggestionsListItemGraphicSize(getContext()).formatUri(url);
                            final File iconFile = IOUtils.getCacheFile(getContext(), IOUtils.md5(listUrl));
                            if (!iconFile.exists()) {
                                HttpUtils.fetchUriToFile(listUrl, iconFile, false);
                            }
                            return ParcelFileDescriptor.open(iconFile, ParcelFileDescriptor.MODE_READ_ONLY);
                        } else throw new FileNotFoundException();
                    } else {
                        throw new FileNotFoundException();
                    }
                } finally {
                    if (c != null) c.close();
                }
            default:
                return super.openFile(uri, mode);
        }
    }

    static String makeCollectionSort(Uri uri, String sortCol) {
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        }  else {
            b.append(sortCol == null ? DBHelper.CollectionItems.POSITION : sortCol);
        }
        String limit = uri.getQueryParameter(Parameter.LIMIT);
        if (!TextUtils.isEmpty(limit)) b.append(" LIMIT ").append(limit);
        String offset = uri.getQueryParameter(Parameter.OFFSET);
        if (!TextUtils.isEmpty(offset)) b.append(" OFFSET ").append(offset);
        return b.toString();
    }

    static String makeActivitiesSort(Uri uri, String sortCol) {
        String limit  = uri.getQueryParameter(Parameter.LIMIT);
        String offset = uri.getQueryParameter(Parameter.OFFSET);
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        } else {
            b.append(sortCol == null ? DBHelper.ActivityView.CREATED_AT + " DESC" : sortCol);
        }
        if (!TextUtils.isEmpty(limit)) b.append(" LIMIT ").append(limit);
        if (!TextUtils.isEmpty(offset)) b.append(" OFFSET ").append(offset);
        return b.toString();
    }

    static String makeCollectionJoin(Table table) {
        String join = table.name + " INNER JOIN " + Table.COLLECTION_ITEMS.name +
                " ON (" + table.id + " = " + DBHelper.CollectionItems.ITEM_ID;
        // XXX hackz
        if (table == Table.SOUNDS) {
            join += "AND " + table.type + " = " + DBHelper.CollectionItems.RESOURCE_TYPE;
        }
        join += ")";
        return join;
    }

    static SCQueryBuilder makeCollectionSelection(SCQueryBuilder qb, String userId, int collectionType) {
        qb.appendWhere(Table.COLLECTION_ITEMS.name+"."+ DBHelper.CollectionItems.USER_ID + " = " + userId);
        qb.appendWhere(" AND "+DBHelper.CollectionItems.COLLECTION_TYPE + " = " + collectionType);
        return qb;
    }

    static SCQueryBuilder makeSoundAssociationSelection(SCQueryBuilder qb, String userId, int[] collectionType) {
        qb.appendWhere(Table.SOUND_ASSOCIATION_VIEW.name + "." + DBHelper.SoundAssociationView.SOUND_ASSOCIATION_USER_ID + " = " + userId);
        for (int i = 0; i < collectionType.length; i++) {
            qb.appendWhere((i == 0 ? " AND (" : " OR ")
                    + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + collectionType[i]
                    + (i == collectionType.length - 1 ? ")" : ""));
        }
        return qb;
    }

    @Override
    public String getType(Uri uri) {
        switch (Content.match(uri)) {
            case ANDROID_SEARCH_SUGGEST:
            case ANDROID_SEARCH_SUGGEST_PATH:
                return SearchManager.SUGGEST_MIME_TYPE;

            case USER:
                return "vnd.soundcloud/user";

            case TRACK:
                return "vnd.soundcloud/track";

            case SEARCH_ITEM:
                return "vnd.soundcloud/search_item";


            case RECORDING:
            case RECORDINGS:
                return "vnd.soundcloud/recording";

            default:
                return null;
        }
    }

    /**
     * Suggest tracks and users based on partial user input.
     * @return a cursor with search suggestions. See {@link SearchManager} for documentation
     *         on schema etc.
     * @see <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     *     Building a suggestion table</a>
     */
    private Cursor suggest(Uri uri, String[] columns, String selection, String[] selectionArgs) {
        log("suggest(" + uri + "," + Arrays.toString(columns) + "," + selection + "," + Arrays.toString(selectionArgs) + ")");
        if (selectionArgs == null) {
            throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SCQueryBuilder qb = new SCQueryBuilder();
        qb.setTables(Table.SUGGESTIONS.name);

        qb.appendWhere( DBHelper.Suggestions.TEXT+" LIKE '"+selectionArgs[0]+"%' OR "+DBHelper.Suggestions.TEXT +
                " LIKE '% "+selectionArgs[0]+"%'");

        final String limit = uri.getQueryParameter(Parameter.LIMIT);
        final String query = qb.buildQuery(
                new String[] {
                    BaseColumns._ID,
                    SearchManager.SUGGEST_COLUMN_TEXT_1,
                    SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                    DBHelper.Suggestions.ICON_URL,
                    "'content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/' || _id" + " AS "
                            + SearchManager.SUGGEST_COLUMN_ICON_1,
                    "'"+SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT + "' AS "  + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
                },
                null, null, null, null, null, limit);

        log("suggest: query="+query);
        return db.rawQuery(query, null);
    }

    /**
     * Called when suggest returns {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID}.
     * Currently not used.
     */
    @SuppressWarnings("UnusedParameters")
    private Cursor refresh(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    public static void enableSyncing(Account account, long pollFrequency) {
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), pollFrequency);
    }

    public static void disableSyncing(Account account) {
        ContentResolver.setSyncAutomatically(account, AUTHORITY, false);
        ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
    }

    // XXX ghetto, use prepared statements
    public static String[] formatWithUser(String[] columns, long userId){
        for (int i = 0; i < columns.length; i++){
            columns[i] = columns[i].replace("$$$",String.valueOf(userId));
        }
        return columns;
    }


    public static String[] fullTrackColumns = new String[]{
            Table.SOUND_VIEW + ".*",

            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + Table.SOUND_VIEW.id + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + LIKE
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_LIKE,


            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                                + " WHERE " + Table.SOUND_VIEW.id + " = " + DBHelper.CollectionItems.ITEM_ID
                                + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.REPOST
                                + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_REPOST
    };
    public static String[] fullSoundAssociationColumns = new String[]{
                Table.SOUND_ASSOCIATION_VIEW + ".*",
                "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                        + " WHERE " + Table.SOUND_ASSOCIATION_VIEW.id + " = " + DBHelper.CollectionItems.ITEM_ID
                        + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + LIKE
                        + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_LIKE,
                "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                                    + " WHERE " + Table.SOUND_ASSOCIATION_VIEW.id + " = " + DBHelper.CollectionItems.ITEM_ID
                                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.REPOST
                                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_REPOST
        };




    public static String[] fullActivityColumns = new String[]{
            Table.ACTIVITY_VIEW + ".*",
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + DBHelper.ActivityView.SOUND_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + LIKE
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_LIKE,
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + DBHelper.ActivityView.SOUND_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                    + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + REPOST
                    + " AND " + DBHelper.CollectionItems.USER_ID + " = $$$) AS " + DBHelper.SoundView.USER_REPOST
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


    /**
     * Roughly corresponds to locally synced collections.
     */
    public interface CollectionItemTypes {
        int TRACK           = 0;
        int LIKE            = 1;
        int FOLLOWING       = 2;
        int FOLLOWER        = 3;
        int FRIEND          = 4;
        int SUGGESTED_USER  = 5;
        int SEARCH          = 6;
        int REPOST          = 7;
        int PLAYLIST        = 8;
    }

    private static void log(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }

    private static interface DbOperation<V> {
        V execute();
    }

    // don't die on disk i/o problems
    private <V> V safeExecute(DbOperation<V> r, V def) {
        try {
            return r.execute();
        } catch (SQLiteDiskIOException e) {
            final String msg = "sqlite disk I/O:" + SQLiteErrors.convertToErrorMessage(e);
            Log.w(TAG, msg, e);
            SoundCloudApplication.handleSilentException(msg,  e);
            return def;
        }
    }

    /**
     * Handles deletion of tracks which are no longer available (have been marked private / deleted).
     */
    public static class TrackUnavailableListener extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            final long id = intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, 0);
            final long userId = intent.getLongExtra(CloudPlaybackService.BroadcastExtras.user_id, 0);
            // only delete tracks from other users - needs proper state checking
            if (id > 0 && userId != SoundCloudApplication.getUserIdFromContext(context)) {
                new Thread() {
                    @Override
                    public void run() {
                        Log.d(TAG, "deleting unavailable track "+id);
                        context.getContentResolver().delete(Content.TRACK.forId(id), null, null);
                    }
                }.start();
            }
        }
    }
}
