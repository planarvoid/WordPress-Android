package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


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
        String query = null;
        switch (content) {
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

            case ME_USERID:
                MatrixCursor c = new MatrixCursor(new String[] { BaseColumns._ID}, 1);
                c.addRow(new Object[] { SoundCloudApplication.fromContext(getContext()).getCurrentUserId() });
                return c;

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
            case TRACK:
                qb.setTables(Table.TRACK_VIEW.name);
                qb.appendWhere(Table.TRACK_VIEW.id + " = " + uri.getLastPathSegment());
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
                qb.appendWhere(DBHelper.Searches.USER_ID + " = "+ userId);
                break;

            case SEARCHES_USER:
                if (_columns == null) _columns = formatWithUser(fullUserColumns,userId);
                qb.setTables(makeCollectionJoin(Table.USERS));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case SEARCHES_TRACK:
                if (_columns == null) _columns = formatWithUser(fullTrackColumns,userId);
                qb.setTables(makeCollectionJoin(Table.TRACK_VIEW));
                makeCollectionSelection(qb, String.valueOf(userId), SEARCH);
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;

            case TRACK_PLAYS:
                qb.setTables(content.table.name);
                qb.appendWhere(DBHelper.TrackPlays.USER_ID + " = "+ userId);
                break;

            case TRACK_PLAYS_ITEM:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.TRACK_PLAYS.id + " = " + uri.getLastPathSegment());
                break;

            case RECORDINGS:
                qb.setTables(content.table.name +
                        " LEFT OUTER JOIN "+Table.USERS+
                        " ON "+content.table.field(DBHelper.Recordings.PRIVATE_USER_ID)+
                        "="+Table.USERS.field(DBHelper.Users._ID));
                String _selection = DBHelper.Recordings.USER_ID+"="+userId;
                if (selection != null) {
                    _selection += (" AND " +selection);
                }
                query = qb.buildQuery(
                        new String[] { content.table.allFields(), DBHelper.Users.USERNAME },
                        _selection,
                        null,
                        null,
                        _sortOrder, null);
                break;
            case RECORDING:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.RECORDINGS.id + " = "+ uri.getLastPathSegment());
                break;

            case ME_SOUND_STREAM:
            case ME_EXCLUSIVE_STREAM:
                if (_columns == null) _columns = formatWithUser(fullActivityColumns, userId);
            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
                qb.setTables(Table.ACTIVITY_VIEW.name);
                if (content != Content.ME_ALL_ACTIVITIES) {
                    // TODO prepared query
                    qb.appendWhere(DBHelper.ActivityView.CONTENT_ID + "=" + content.id);
                }
                _sortOrder = makeActivitiesSort(uri, sortOrder);
                break;
            case COMMENTS:
                qb.setTables(content.table.name);
                break;
            case PLAYLISTS:
                qb.setTables(content.table.name);
                qb.appendWhere("_id = "+ userId);
                break;

            case PLAYLIST:
                qb.setTables(Table.TRACK_VIEW + " INNER JOIN " + Table.PLAYLIST_ITEMS.name +
                        " ON (" + Table.TRACK_VIEW.id + " = " + DBHelper.PlaylistItems.TRACK_ID + ")");
                if (_columns == null) _columns = formatWithUser(fullTrackColumns, userId);
                qb.appendWhere(Table.PLAYLIST_ITEMS.name+"."+ DBHelper.PlaylistItems.USER_ID + " = " + userId);
                qb.appendWhere(" AND "+DBHelper.PlaylistItems.PLAYLIST_ID + " = " + uri.getLastPathSegment());
                _sortOrder = makeCollectionSort(uri, sortOrder);
                break;
            case ANDROID_SEARCH_SUGGEST:
            case ANDROID_SEARCH_SUGGEST_PATH:
                return suggest(uri, columns, selection, selectionArgs);

            case ANDROID_SEARCH_REFRESH:
            case ANDROID_SEARCH_REFRESH_PATH:
                return refresh(uri, columns, selection, selectionArgs, sortOrder);

            case UNKNOWN:
            default:
                throw new IllegalArgumentException("No query available for: " + uri);
        }

        if (query == null) {
            query = qb.buildQuery(_columns, selection, null, null, _sortOrder, null);
        }
        Log.d(TAG, "query: "+query);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(query, selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final long userId = SoundCloudApplication.getUserIdFromContext(getContext());
        long id;
        Uri result;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTIONS:
                id = dbInsertWithOnConflict(db, content.table, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case COLLECTION_PAGES:
                id = dbInsertWithOnConflict(db, content.table, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACKS:
                id = dbInsertWithOnConflict(db, content.table, values, SQLiteDatabase.CONFLICT_IGNORE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case TRACK_PLAYS:
                if (!values.containsKey(DBHelper.TrackPlays.USER_ID)) {
                    values.put(DBHelper.TrackPlays.USER_ID, userId);
                }
                id = db.insert(content.table.name, null, values);
                String counter = DBHelper.TrackPlays.PLAY_COUNT;
                db.execSQL("UPDATE "+content.table.name+
                        " SET "+counter+"="+counter+" + 1 WHERE "+content.table.id +"= ?",
                        new String[] {String.valueOf(id)});
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
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
                id = dbInsertWithOnConflict(db, content.table, values, SQLiteDatabase.CONFLICT_REPLACE);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case RECORDINGS:
                id = db.insert(content.table.name, null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case ME_FAVORITES:
                id = dbInsertWithOnConflict(db, Table.TRACKS, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id >= 0) {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CollectionItems.USER_ID, userId);
                    cv.put(DBHelper.CollectionItems.ITEM_ID, (Long) values.get(DBHelper.Tracks._ID));
                    cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, FAVORITE);
                    id = dbInsertWithOnConflict(db, Table.COLLECTION_ITEMS, cv, SQLiteDatabase.CONFLICT_ABORT);
                    result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                    getContext().getContentResolver().notifyChange(result, null, false);
                    return result;
                } else {
                    throw new SQLException("No insert available for: " + uri);
                }

            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
            case ME_EXCLUSIVE_STREAM:
                id = dbInsertWithOnConflict(db, content.table, values, SQLiteDatabase.CONFLICT_IGNORE);
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
        final Content content = Content.match(uri);

        switch (content) {
            case COLLECTIONS:
            case COLLECTION_PAGES:
            case SEARCHES:
                break;

            case TRACK:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;
            case USER:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;
            case PLAYLISTS:
                break;
            case PLAYLIST:
                where = TextUtils.isEmpty(where) ? DBHelper.PlaylistItems.PLAYLIST_ID + "=" + uri.getLastPathSegment()
                        : where + " AND " + DBHelper.PlaylistItems.PLAYLIST_ID + "=" + uri.getLastPathSegment();
                break;
            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
            case ME_SOUND_STREAM:
            case ME_EXCLUSIVE_STREAM:
                if (content != Content.ME_ALL_ACTIVITIES) {
                    where = DBHelper.Activities.CONTENT_ID+"= ?";
                    whereArgs = new String[] {String.valueOf(content.id) };
                }
                break;
            case ME_TRACKS:
            case ME_FAVORITES:
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
            case USER_TRACKS:
            case USER_FAVORITES:
            case USER_FOLLOWINGS:
            case USER_FOLLOWERS:
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
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        final Content content = Content.match(uri);
        switch (content) {
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
            case SEARCH:
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
                                    + "SELECT _id FROM "+ Table.TRACKS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.TRACK+ " ," + CollectionItemTypes.FAVORITE+ ") "
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " =  " + DBHelper.Tracks._ID
                                    + " UNION SELECT DISTINCT " + DBHelper.ActivityView.TRACK_ID + " FROM "+ Table.ACTIVITY_VIEW.name
                                    + " UNION SELECT DISTINCT " + DBHelper.PlaylistItems.TRACK_ID + " FROM "+ Table.PLAYLIST_ITEMS.name
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
                                        + " AND " + DBHelper.CollectionItems.USER_ID + " = " + userId
                                        + " AND  " + DBHelper.CollectionItems.ITEM_ID + " = " + Table.USERS.id
                                    + " UNION SELECT DISTINCT " + DBHelper.ActivityView.USER_ID + " FROM "+ Table.ACTIVITY_VIEW.name
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
        String[] extraCV = null;

        final Content content = Content.match(uri);
        final Table table;
        switch (content) {
            case TRACKS:
            case USERS:
                content.table.upsert(db, values);
                if (values.length != 0) getContext().getContentResolver().notifyChange(uri, null, false);
                return values.length;

            case COMMENTS:
            case PLAYLISTS:
            case ME_SOUND_STREAM:
            case ME_EXCLUSIVE_STREAM:
            case ME_ACTIVITIES:
                table = content.table;
                break;

            case ME_TRACKS:
            case USER_TRACKS:
            case ME_FAVORITES:
            case USER_FAVORITES:
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

            case PLAYLIST:
                table = content.table;
                extraCV = new String[]{DBHelper.PlaylistItems.PLAYLIST_ID, String.valueOf(uri.getLastPathSegment())};
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        try {
            db.beginTransaction();
            boolean failed = false;
            for (ContentValues v : values) {
                if (extraCV != null) v.put(extraCV[0], extraCV[1]);
                Log.d(TAG, "bulkInsert: " + v);

                if (db.replace(table.name, null, v) < 0) {
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


    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        switch (Content.match(uri)) {
            case TRACK_ARTWORK:
                String size = uri.getQueryParameter("size");
                List<String> segments = uri.getPathSegments();
                long trackId = Long.parseLong(segments.get(segments.size()-2));
                Cursor c = query(Content.TRACK.forId(trackId), null, null, null, null);
                if (c != null && c.moveToFirst()) {
                    Track track = new Track(c);
                    Consts.GraphicSize gs = (size == null || "list".equals(size)) ?
                            Consts.GraphicSize.getListItemGraphicSize(getContext()) :
                            Consts.GraphicSize.fromString(size);

                    final String artworkUri = gs.formatUri(track.getArtwork());
                    final File artworkFile = new File(getContext().getCacheDir(), CloudUtils.md5(artworkUri));
                    if (!artworkFile.exists()) {
                        OutputStream os = null;
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(artworkUri).openConnection();
                            conn.setUseCaches(true);
                            InputStream is = conn.getInputStream();
                            os = new BufferedOutputStream(new FileOutputStream(artworkFile));
                            final byte[] buffer = new byte[8192];
                            int n;
                            while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                                os.write(buffer, 0, n);
                            }
                            os.close();
                        } catch (MalformedURLException e) {
                            throw new FileNotFoundException();
                        } catch (IOException e) {
                            throw new FileNotFoundException();
                        } finally {
                            c.close();
                            if (os != null) try {
                                os.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    return ParcelFileDescriptor.open(artworkFile, ParcelFileDescriptor.MODE_READ_ONLY);
                } else {
                    throw new FileNotFoundException();
                }
            default:
                return super.openFile(uri, mode);
        }
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

    static String makeActivitiesSort(Uri uri, String sortCol) {
        String limit = uri.getQueryParameter("limit");
        String offset = uri.getQueryParameter("offset");
        if (TextUtils.isEmpty("limit") && TextUtils.isEmpty("offset")) {
            return null;
        } else {
            StringBuilder b = new StringBuilder();
            b.append(sortCol == null ? DBHelper.ActivityView.CREATED_AT + " DESC": sortCol);
            if (!TextUtils.isEmpty(limit)) b.append(" LIMIT ").append(limit);
            if (!TextUtils.isEmpty(offset)) b.append(" OFFSET ").append(offset);
            return b.toString();
        }
    }

    static String makeCollectionJoin(Table table){
        return table.name + " INNER JOIN " + Table.COLLECTION_ITEMS.name +
            " ON (" + table.id +" = " + DBHelper.CollectionItems.ITEM_ID+ ")";
    }

    static SCQueryBuilder makeCollectionSelection(SCQueryBuilder qb,
                                                      String userId, int collectionType) {

        qb.appendWhere(Table.COLLECTION_ITEMS.name+"."+ DBHelper.CollectionItems.USER_ID + " = " + userId);
        qb.appendWhere(" AND "+DBHelper.CollectionItems.COLLECTION_TYPE + " = " + collectionType);
        return qb;
    }

    @Override
    public String getType(Uri uri) {
        switch(Content.match(uri)) {
            case ANDROID_SEARCH_SUGGEST:
            case ANDROID_SEARCH_SUGGEST_PATH:
                return SearchManager.SUGGEST_MIME_TYPE;
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
        Log.d(TAG, "suggest("+uri+","+ Arrays.toString(columns)+","+selection+","+Arrays.toString(selectionArgs)+")");
        if (selectionArgs == null) {
            throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SCQueryBuilder qb = new SCQueryBuilder();
        qb.setTables(Table.TRACK_VIEW.name);
        String limit = uri.getQueryParameter("limit");

        qb.appendWhere( DBHelper.TrackView.TITLE+" LIKE '%"+selectionArgs[0]+"%'");
        String query = qb.buildQuery(
                new String[]{ DBHelper.TrackView._ID, DBHelper.TrackView.TITLE, DBHelper.TrackView.USERNAME },
                null, null, null, null, null, limit);
        Log.d(TAG, "suggest: query="+query);
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            MatrixCursor suggest = new MatrixCursor(
                    new String[] {BaseColumns._ID,
                                  SearchManager.SUGGEST_COLUMN_TEXT_1,
                                  SearchManager.SUGGEST_COLUMN_TEXT_2,
                                  SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                                  SearchManager.SUGGEST_COLUMN_ICON_1,
                                  SearchManager.SUGGEST_COLUMN_SHORTCUT_ID},
                    cursor.getCount());

            while (cursor.moveToNext()) {
                long trackId = cursor.getLong(0);
                String title = cursor.getString(1);
                String username = cursor.getString(2);
                String icon = Content.TRACK_ARTWORK.forId(trackId).toString();
                suggest.addRow(new Object[] {
                        trackId,
                        title,
                        username,
                        Track.getClientUri(trackId).toString(),
                        icon,
                        SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT});
            }
            cursor.close();
            return suggest;
        } else {
            Log.d(TAG, "suggest: cursor is null");
            // no results
            return new MatrixCursor(
                   new String[] {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1}, 0);
        }
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

    public static String[] fullActivityColumns = new String[]{
            Table.ACTIVITY_VIEW + ".*",
            "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS
                    + " WHERE " + DBHelper.ActivityView.TRACK_ID + " = " + DBHelper.CollectionItems.ITEM_ID
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


    private static long dbInsertWithOnConflict(SQLiteDatabase db, Table table,
                                              ContentValues values,
                                              int conflictAlgorithm) {
        if (Build.VERSION.SDK_INT > 7) {
            return db.insertWithOnConflict(table.name, null, values, conflictAlgorithm);
        }  else {
            // TODO: do something sensible here
            return db.insert(table.name, null, values);
        }
    }

    public interface CollectionItemTypes {
        int TRACK          = 0;
        int FAVORITE       = 1;
        int FOLLOWING      = 2;
        int FOLLOWER       = 3;
        int FRIEND         = 4;
        int SUGGESTED_USER = 5;
        int SEARCH         = 6;
    }
}
