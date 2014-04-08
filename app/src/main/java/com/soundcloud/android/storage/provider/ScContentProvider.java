package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.FOLLOWER;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.FOLLOWING;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.LIKE;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.image.ImageSize;
import org.jetbrains.annotations.Nullable;

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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.AsyncTask;
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
        String RANDOM           = "random";
        String CACHED           = "cached";
        String LIMIT            = "limit";
        String OFFSET           = "offset";
        String IDS_ONLY         = "idsOnly";
        String TYPE_IDS_ONLY    = "typeIdsOnly";
    }

    private DBHelper dbHelper;

    public ScContentProvider() {
        dbHelper = DBHelper.getInstance(getContext());
    }

    public ScContentProvider(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public boolean onCreate() {
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
            case COLLECTION_PAGES:
            case USER_ASSOCIATIONS:
                qb.setTables(content.table.name);
                break;
            case PLAY_QUEUE:
                qb.setTables(content.table.name);
                if (_sortOrder == null){
                    _sortOrder = DBHelper.PlayQueue._ID + " ASC";
                }
                break;

            case ME_SOUNDS :
                qb.setTables(Table.SOUND_ASSOCIATION_VIEW.name);
                if ("1".equals(uri.getQueryParameter(Parameter.TYPE_IDS_ONLY))) {
                    _columns = new String[]{DBHelper.SoundAssociationView._TYPE, DBHelper.SoundAssociationView._ID};
                } else if (_columns == null) _columns = formatWithUser(getSoundViewColumns(Table.SOUND_ASSOCIATION_VIEW), userId);

                makeSoundAssociationSelection(qb, String.valueOf(userId),
                        new int[]{CollectionItemTypes.TRACK, CollectionItemTypes.REPOST, CollectionItemTypes.PLAYLIST});


                _sortOrder = makeCollectionSort(uri, DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP + " DESC");

                break;

            case ME_PLAYLISTS :
                qb.setTables(Table.SOUND_ASSOCIATION_VIEW.name);
                if (_columns == null) _columns = formatWithUser(getSoundViewColumns(Table.SOUND_ASSOCIATION_VIEW), userId);

                makeSoundAssociationSelection(qb, String.valueOf(userId),new int[]{CollectionItemTypes.PLAYLIST});
                qb.appendWhere(" AND " + DBHelper.SoundView._TYPE + "= " + Playable.DB_TYPE_PLAYLIST);
                _sortOrder = makeCollectionSort(uri, DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP + " DESC");
                break;

            case ME_LIKES:
            case ME_REPOSTS:
                qb.setTables(Table.SOUND_ASSOCIATION_VIEW.name);
                if ("1".equals(uri.getQueryParameter(Parameter.TYPE_IDS_ONLY))) {
                    _columns = new String[]{DBHelper.SoundAssociationView._TYPE, DBHelper.SoundAssociationView._ID};
                } else if (_columns == null) _columns = formatWithUser(getSoundViewColumns(Table.SOUND_ASSOCIATION_VIEW), userId);

                makeSoundAssociationSelection(qb, String.valueOf(userId),
                        new int[]{content.collectionType});

                _sortOrder = makeCollectionSort(uri, DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP + " DESC");

                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND "+ DBHelper.SoundView.CACHED + "= 1");
                }
                break;

            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                /* XXX special case for now. we  need to not join in the users table on an id only request, because
                it is an inner join and will not return ids with missing users. Switching to a left join is possible
                but not 4 days before major release*/
                if ("1".equals(uri.getQueryParameter(Parameter.IDS_ONLY))) {
                    qb.setTables(Table.USER_ASSOCIATIONS.name);
                    qb.appendWhere(Table.USER_ASSOCIATIONS.name + "." + DBHelper.UserAssociations.OWNER_ID + " = " + String.valueOf(userId));
                    qb.appendWhere(" AND " + DBHelper.UserAssociations.ASSOCIATION_TYPE + " = " + content.collectionType);
                    _columns = new String[]{DBHelper.UserAssociations.TARGET_ID};
                    _sortOrder = makeCollectionSort(uri, sortOrder);

                } else {
                    qb.setTables(Table.USER_ASSOCIATION_VIEW.name);
                    if (_columns == null) {
                        _columns = formatWithUser(getUserViewColumns(Table.USER_ASSOCIATION_VIEW), userId);
                    }
                    qb.appendWhere(Table.USER_ASSOCIATION_VIEW.name + "." + DBHelper.UserAssociationView.USER_ASSOCIATION_OWNER_ID + " = " + String.valueOf(userId));
                    qb.appendWhere(" AND " + DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE + " = " + content.collectionType);

                    _sortOrder = makeCollectionSort(uri, sortOrder != null ?
                            sortOrder : DBHelper.UserAssociationView.USER_ASSOCIATION_POSITION);
                }
                break;


            case ME_USERID:
                MatrixCursor c = new MatrixCursor(new String[] { BaseColumns._ID}, 1);
                c.addRow(new Object[]{SoundCloudApplication.getUserId()});
                return c;

            case TRACKS:
            case PLAYLISTS:
                qb.setTables(Table.SOUND_VIEW.name);
                if (_columns == null) _columns = formatWithUser(getSoundViewColumns(Table.SOUND_VIEW),userId);
                if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
                    _sortOrder = "RANDOM()";
                }
                appendSoundType(qb, content);
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND " + DBHelper.SoundView.CACHED + "= 1");
                }
                break;

            case TRACK:
            case PLAYLIST:
                qb.setTables(Table.SOUND_VIEW.name);
                appendSoundType(qb, content);
                qb.appendWhere(" AND " + Table.SOUND_VIEW.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(getSoundViewColumns(Table.SOUND_VIEW),userId);
                break;

            case PLAYLIST_TRACKS:
                qb.setTables(Table.PLAYLIST_TRACKS_VIEW.name);
                // extract the playlist id from the second to last segment
                qb.appendWhere(DBHelper.PlaylistTracksView.PLAYLIST_ID + " = " + uri.getPathSegments().get(1));
                if (_columns == null) {
                    _columns = formatWithUser(
                            getSoundViewColumns(Table.PLAYLIST_TRACKS_VIEW),
                            userId);
                }
                if (_sortOrder == null){
                    _sortOrder = DBHelper.PlaylistTracksView.PLAYLIST_POSITION + " ASC, "
                            + DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " DESC";
                }
                break;

            case PLAYLIST_ALL_TRACKS:
                qb.setTables(Table.PLAYLIST_TRACKS.name);
                break;

            case USERS:
                qb.setTables(content.table.name);
                if (_columns == null) _columns = formatWithUser(getUserViewColumns(Table.USERS),userId);
                break;

            case USER:
                qb.setTables(content.table.name);
                qb.appendWhere(Table.USERS.id + " = " + uri.getLastPathSegment());
                if (_columns == null) _columns = formatWithUser(getUserViewColumns(Table.USERS),userId);
                break;

            case TRACK_PLAYS:
                qb.setTables(content.table.name);
                qb.appendWhere(DBHelper.TrackMetadata.USER_ID + " = " + userId);
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
                        " LEFT OUTER JOIN " + Table.USERS +
                        " ON " + content.table.field(DBHelper.Recordings.PRIVATE_USER_ID) +
                        "=" + Table.USERS.field(DBHelper.Users._ID));

                qb.appendWhere(Table.RECORDINGS.id + " = "+ uri.getLastPathSegment());
                query = qb.buildQuery(new String[] { content.table.allFields(), DBHelper.Users.USERNAME },
                                        selection,
                                        null,
                                        null,
                                        _sortOrder, null);
                break;

            case ME_SOUND_STREAM:
                if (_columns == null) {
                    final String[] rawColumns = getSoundViewColumns(Table.ACTIVITY_VIEW,
                            DBHelper.ActivityView.SOUND_ID, DBHelper.ActivityView.SOUND_TYPE);

                    _columns = formatWithUser(rawColumns, userId);
                }
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(DBHelper.SoundView.CACHED + "= 1 AND ");
                }

            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
                qb.setTables(Table.ACTIVITY_VIEW.name);
                if (content != Content.ME_ALL_ACTIVITIES) {
                    // filter out playlist
                    qb.appendWhere(DBHelper.ActivityView.CONTENT_ID + "=" + content.id + " ");
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
            query = qb.buildQuery(_columns, _selection, null /* selectionArgs passed further down */, null,_sortOrder, getRowLimit(uri));
        }
        log("query: "+query);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(query, _selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    private String getRowLimit(Uri uri) {
        String limit = uri.getQueryParameter(Parameter.LIMIT);
        String offset = uri.getQueryParameter(Parameter.OFFSET);
        if (limit != null && offset != null) {
            return offset + "," + limit;
        }
        return limit;
    }

    private void appendSoundType(SCQueryBuilder qb, Content content) {
        qb.appendWhere(" " + DBHelper.SoundView._TYPE + " = '" +
                (content.modelType == Track.class ? Playable.DB_TYPE_TRACK : Playable.DB_TYPE_PLAYLIST)
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
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // inserts based on collection URIs
            //////////////////////////////////////////////////////////////////////////////////////////////////
            case COLLECTION:
            case COLLECTIONS:
            case COLLECTION_PAGES:
            case COLLECTION_ITEMS:
            case USER_ASSOCIATIONS:
            case USERS:
            case RECORDINGS:
            case ME_SOUNDS:
            case ME_PLAYLISTS:
            case ME_SHORTCUTS:
            case TRACKS:
            case PLAYLISTS:
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
            case ME_LIKES:
            case ME_REPOSTS:
            case ME_FOLLOWINGS:
                id = content.table.insertOrReplace(db, values);
                if (id >= 0 && values.containsKey(BaseColumns._ID)) {
                    id = values.getAsLong(BaseColumns._ID);
                }
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            //////////////////////////////////////////////////////////////////////////////////////////////////
            // special cases
            //////////////////////////////////////////////////////////////////////////////////////////////////

            case TRACK_PLAYS:
                // TODO should be in update()
                if (!values.containsKey(DBHelper.TrackMetadata.USER_ID)) {
                    values.put(DBHelper.TrackMetadata.USER_ID, userId);
                }
                String trackId = values.getAsString(DBHelper.TrackMetadata._ID);
                content.table.insertWithOnConflict(db, values, SQLiteDatabase.CONFLICT_IGNORE);

                String counter = DBHelper.TrackMetadata.PLAY_COUNT;
                db.execSQL("UPDATE " + content.table.name +
                        " SET " + counter + "=" + counter + " + 1 WHERE " + content.table.id + "= ?",
                        new String[]{trackId}) ;
                result = uri.buildUpon().appendPath(trackId).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            case PLAYLIST_TRACKS:
                id = db.insert(content.table.name, null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);

                // update the track count in the playlist tables
                String playlistId = values.getAsString(DBHelper.PlaylistTracks.PLAYLIST_ID);
                final String trackCount = DBHelper.Sounds.TRACK_COUNT;
                db.execSQL("UPDATE " + Table.SOUNDS.name + " SET " + trackCount + "=" + trackCount + " + 1 WHERE " +
                        DBHelper.Sounds._ID + "=? AND " + DBHelper.Sounds._TYPE + "=?",
                        new String[]{playlistId, String.valueOf(Playable.DB_TYPE_PLAYLIST)}) ;
                return result;

            //////////////////////////////////////////////////////////////////////////////////////////////////
            // upserts for single-resource URIs
            //////////////////////////////////////////////////////////////////////////////////////////////////

            case TRACK:
            case USER:
            case PLAYLIST:
            case RECORDING:
                if (content.table.upsert(db, new ContentValues[]{values}) != -1){
                    getContext().getContentResolver().notifyChange(uri, null, false);
                } else {
                    log("Error inserting to uri " + uri.toString());
                }
                return uri;

            case TRACK_METADATA:
                if (!values.containsKey(DBHelper.TrackMetadata.USER_ID)) {
                    values.put(DBHelper.TrackMetadata.USER_ID, userId);
                }
                content.table.upsert(db, new ContentValues[] {values} );
                return uri.buildUpon().appendPath(
                        values.getAsString(DBHelper.TrackMetadata._ID)).build();

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

        final long userIdFromContext = SoundCloudApplication.getUserIdFromContext(getContext());
        switch (content) {
            case COLLECTIONS:
            case COLLECTION_PAGES:
            case RECORDINGS:
            case PLAY_QUEUE:
            case ME_CONNECTIONS:
            case PLAYLISTS:
            case ME_ALL_ACTIVITIES:
                break;

            case TRACK:
            case USER:
            case PLAYLIST:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;

            case PLAYLIST_TRACKS:
                where = (!TextUtils.isEmpty(where) ? where + " AND " : "") + DBHelper.PlaylistTracks.PLAYLIST_ID + "=" + uri.getPathSegments().get(1);
                break;


            case RECORDING:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;

            case ME_ACTIVITIES:
            case ME_SOUND_STREAM:
            where = DBHelper.Activities.CONTENT_ID+"= ?";
            whereArgs = new String[] {String.valueOf(content.id) };
            break;

            case ME_SOUNDS: // still used in com.soundcloud.android.dao.SoundAssociationStorage#syncToLocal
                // add userId
                String whereAppend = Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + userIdFromContext;
                // append possible types
                int[] collectionType = new int[]{CollectionItemTypes.TRACK, CollectionItemTypes.REPOST, CollectionItemTypes.PLAYLIST};
                for (int i = 0; i < collectionType.length; i++) {
                    whereAppend += (i == 0 ? " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" : ", ")
                            + collectionType[i]
                            + (i == collectionType.length - 1 ? ")" : "");
                }

                where = TextUtils.isEmpty(where) ? whereAppend : where + " AND " + whereAppend;
                break;

            case ME_LIKES:
            case ME_PLAYLISTS:
            case ME_REPOSTS:
                whereAppend = Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + userIdFromContext
                        + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + content.collectionType;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;

                break;

            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                whereAppend = Table.USER_ASSOCIATIONS.name + "." + DBHelper.UserAssociations.OWNER_ID + " = " + userIdFromContext
                        + " AND " + DBHelper.UserAssociations.ASSOCIATION_TYPE + " = " + content.collectionType;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;

                break;

            case COLLECTION_ITEMS:
                whereAppend = Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + userIdFromContext;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;
                break;

            case USER_ASSOCIATIONS:
                whereAppend = Table.USER_ASSOCIATIONS.name + "." + DBHelper.UserAssociations.OWNER_ID + " = " + userIdFromContext;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;
                break;

            case ME_PLAYLIST:
                whereAppend = Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + userIdFromContext
                + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " +CollectionItemTypes.PLAYLIST + " " ;
                where += " AND " + DBHelper.CollectionItems.ITEM_ID + " = " + uri.getLastPathSegment();
                where += " AND " + whereAppend;
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
            case USER_ASSOCIATIONS:
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case COLLECTION:
            case TRACK:
            case USER:
            case PLAYLIST:
            case RECORDING:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;

            case RECORDINGS:
                count = db.update(content.table.name, values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;

            case PLAYABLE_CLEANUP:
                long userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0){
                    final long start = System.currentTimeMillis();

                    // remove unassociated playlists
                    where = "_id NOT IN ("
                            + "SELECT _id FROM " + Table.SOUNDS.name + " WHERE EXISTS("
                            + getPlaylistAssociationsSelect(userId)
                            + ") AND " + DBHelper.SoundView._TYPE + " = " + Playable.DB_TYPE_PLAYLIST;
                    count = db.delete(Table.SOUNDS.name, where, null);

                    // delete stale playlist associations
                    where = DBHelper.PlaylistTracks.PLAYLIST_ID + " NOT IN ("
                            + "SELECT _id FROM " + Table.SOUNDS.name + " WHERE " + DBHelper.Sounds._TYPE
                            + " = " + Playable.DB_TYPE_PLAYLIST
                        + ")";
                    count += db.delete(Table.PLAYLIST_TRACKS.name, where, null);

                    // finally, remove tracks
                    where = "_id NOT IN ("
                                    + "SELECT _id FROM " + Table.SOUNDS.name + " WHERE EXISTS("
                                    + getTrackAssociationsSelect(userId)
                                    + " UNION SELECT DISTINCT " + DBHelper.PlayQueue.TRACK_ID + " FROM "+ Table.PLAY_QUEUE.name
                                    + " UNION SELECT DISTINCT " + DBHelper.PlaylistTracks.TRACK_ID + " FROM "+ Table.PLAYLIST_TRACKS.name
                                + ") AND " + DBHelper.SoundView._TYPE + " = " + Playable.DB_TYPE_TRACK;

                    count += db.delete(Table.SOUNDS.name,where, null);
                    log("Track cleanup done: deleted " + count + " items in " + (System.currentTimeMillis() - start) + " ms");
                    return count;
                }
                return 0;

            case USERS_CLEANUP:
                userId = SoundCloudApplication.getUserIdFromContext(getContext());
                if (userId > 0) {
                    where = "_id NOT IN (SELECT DISTINCT " + DBHelper.Sounds.USER_ID + " FROM "+ Table.SOUNDS.name + " UNION "
                                    + "SELECT _id FROM "+ Table.USERS.name + " WHERE EXISTS("
                                        + "SELECT 1 FROM CollectionItems WHERE "
                                        + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (" + CollectionItemTypes.FOLLOWER+ " ," + CollectionItemTypes.FOLLOWING+ " ," + CollectionItemTypes.FRIEND+ ") "
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

            case SOUND_STREAM_CLEANUP:
                String limit = uri.getQueryParameter(Parameter.LIMIT);
                long start = System.currentTimeMillis();
                count = cleanupActivities(Content.ME_SOUND_STREAM, db, limit);
                log("SoundStream cleanup done: deleted " + count + " stream items in " + (System.currentTimeMillis() - start) + " ms");
                getContext().getContentResolver().notifyChange(Content.ME_SOUND_STREAM.uri, null, false);
                return count;

            case ACTIVITIES_CLEANUP:
                limit = uri.getQueryParameter(Parameter.LIMIT);
                start = System.currentTimeMillis();
                count = cleanupActivities(Content.ME_ACTIVITIES, db, limit);
                log("Activities cleanup done: deleted " + count + " activities in " + (System.currentTimeMillis() - start) + " ms");
                getContext().getContentResolver().notifyChange(Content.ME_ACTIVITIES.uri, null, false);
                return count;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private String getTrackAssociationsSelect(long userId) {
        return String.format(selectAssociationsAndActivities, String.valueOf(CollectionItemTypes.TRACK),
                userId, Playable.DB_TYPE_TRACK, Playable.DB_TYPE_TRACK);
    }

    private String getPlaylistAssociationsSelect(long userId) {
        return String.format(selectAssociationsAndActivities, String.valueOf(CollectionItemTypes.PLAYLIST),
                userId, Playable.DB_TYPE_PLAYLIST, Playable.DB_TYPE_PLAYLIST);
    }

    private int cleanupActivities(Content content, SQLiteDatabase db, String limit) {
        int count;
        String where = DBHelper.ActivityView.CONTENT_ID + "=" + content.id + " AND _id NOT IN ("
                + "SELECT _id FROM " + Table.ACTIVITY_VIEW.name + " WHERE "
                + DBHelper.ActivityView.CONTENT_ID + "=" + content.id
                + " LIMIT " + (limit == null ? 200 : limit) + ")";

        count = db.delete(Table.ACTIVITIES.name, where, null);
        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (values == null || values.length == 0) return 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String[] extraCV = null;
        boolean recreateTable = false;
        boolean deleteUri = false;

        final Content content = Content.match(uri);
        final Table table;
        switch (content) {
            case TRACKS:
            case USERS:
            case RECORDINGS:
            case SOUNDS:
            case PLAYLISTS:
                content.table.upsert(db, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return values.length;

            case PLAY_QUEUE:
                recreateTable = true;
                // fall through
            case COMMENTS:
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
                table = content.table;
                break;

            case ME_SOUNDS:
                table = Table.COLLECTION_ITEMS;
                break;

            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                table = Table.USER_ASSOCIATIONS;
                extraCV = new String[]{DBHelper.UserAssociations.ASSOCIATION_TYPE, String.valueOf(content.collectionType)};
                break;

            case ME_LIKES:
            case ME_REPOSTS:
                table = Table.COLLECTION_ITEMS;
                extraCV = new String[]{DBHelper.CollectionItems.COLLECTION_TYPE, String.valueOf(content.collectionType)};
                break;

            case PLAYLIST_TRACKS:
                deleteUri = true; // clean out table first
                table = Table.PLAYLIST_TRACKS;
                extraCV = new String[]{DBHelper.PlaylistTracks.PLAYLIST_ID, uri.getPathSegments().get(1)};
                break;

            case ME_SHORTCUTS:
                recreateTable = true;
                table = content.table;
                break;

            default:
                table = content.table;
        }

        if (table == null) throw new IllegalArgumentException("No table for URI "+uri);

        db.beginTransaction();
        try {
            boolean failed = false;

            if (recreateTable) {
                db.delete(table.name, null, null);
            }

            if (deleteUri){
                delete(uri,null,null);
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
                db.execSQL("INSERT OR IGNORE INTO " + Table.SOUNDS.name + " (_id, title, artwork_url, permalink_url, _type) " +
                        " SELECT id, text, icon_url, permalink_url, 0 FROM " + Table.SUGGESTIONS.name + " where kind = 'like'");
            }

            if (!failed) db.setTransactionSuccessful();
        } finally {
            // We had crashes on Samsung devices where the transaction was not open at this point.
            // Let's keep this in and see if this fixes it.
            // https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/533f1439fabb27481b264056
            // Otherwise, feel free to remove again.
            if (db.inTransaction()) {
                db.endTransaction();
            }
        }
        getContext().getContentResolver().notifyChange(uri, null, false);
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
                            final String listUrl = ImageSize.getSearchSuggestionsListItemImageSize(getContext()).formatUri(url);
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

    static String makeCollectionSort(Uri uri, @Nullable String sortCol) {
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        }  else {
            b.append(TextUtils.isEmpty(sortCol) ? DBHelper.CollectionItems.POSITION : sortCol);
        }
        return b.toString();
    }

    static String makeActivitiesSort(Uri uri, String sortCol) {
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        } else {
            b.append(sortCol == null ? DBHelper.ActivityView.CREATED_AT + " DESC" : sortCol);
        }
        return b.toString();
    }

    // TODO, move this logic out of here and into Storage classes
    static SCQueryBuilder makeSoundAssociationSelection(SCQueryBuilder qb, String userId, int[] collectionType) {
        qb.appendWhere(Table.SOUND_ASSOCIATION_VIEW.name + "." + DBHelper.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID + " = " + userId);
        for (int i = 0; i < collectionType.length; i++) {
            qb.appendWhere((i == 0 ? " AND " + DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE + " IN (" : ", ")
                    + collectionType[i]
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
                return "vnd.soundcloud.playable/track";

            case PLAYLIST:
                return "vnd.soundcloud.playable/playlist";

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
                    DBHelper.Suggestions.ID,
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

    private static String[] getSoundViewColumns(Table table) {
        return getSoundViewColumns(table, table.id, table.type);
    }

    private static String[] getSoundViewColumns(Table table, String idCol, String typeCol) {
        return new String[]{
                table.name + ".*",
                "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS + ", " + Table.SOUNDS.name
                        + " WHERE " + idCol + " = " + DBHelper.CollectionItems.ITEM_ID
                        + " AND " + typeCol + " = " + DBHelper.CollectionItems.RESOURCE_TYPE
                        + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + LIKE
                        + " AND " + Table.COLLECTION_ITEMS.name + "." +  DBHelper.CollectionItems.USER_ID + " = $$$)"
                        + " AS " + DBHelper.SoundView.USER_LIKE,

                "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS + ", " + Table.SOUNDS.name
                        + " WHERE " + idCol + " = " + DBHelper.CollectionItems.ITEM_ID
                        + " AND " + typeCol + " = " + DBHelper.CollectionItems.RESOURCE_TYPE
                        + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + CollectionItemTypes.REPOST
                        + " AND " + Table.COLLECTION_ITEMS.name + "." +  DBHelper.CollectionItems.USER_ID + " = $$$)"
                        + " AS " + DBHelper.SoundView.USER_REPOST
        };
    }

    public static String[] getUserViewColumns(Table table) {
        return new String[]{
                table + ".*",
                "EXISTS (SELECT 1 FROM " + Table.USER_ASSOCIATIONS + ", " + Table.USERS.name
                        + " WHERE " + DBHelper.Users._ID + " = " + DBHelper.UserAssociations.TARGET_ID
                        + " AND " + DBHelper.UserAssociations.ASSOCIATION_TYPE + " = " + FOLLOWING
                        + " AND " + DBHelper.UserAssociations.OWNER_ID + " = $$$) AS " + DBHelper.Users.USER_FOLLOWING,
                "EXISTS (SELECT 1 FROM " + Table.USER_ASSOCIATIONS + ", " + Table.USERS.name
                        + " WHERE " + DBHelper.Users._ID + " = " + DBHelper.UserAssociations.TARGET_ID
                        + " AND " + DBHelper.UserAssociations.ASSOCIATION_TYPE + " = " + FOLLOWER
                        + " AND " + DBHelper.UserAssociations.OWNER_ID + " = $$$) AS " + DBHelper.Users.USER_FOLLOWER
        };
    }

    private static String selectAssociationsAndActivities =
        "SELECT 1 FROM CollectionItems WHERE "
            + DBHelper.CollectionItems.COLLECTION_TYPE + " IN (%s,"
                + CollectionItemTypes.LIKE + " ," + CollectionItemTypes.REPOST + ") "
            + " AND " + DBHelper.CollectionItems.USER_ID + " = %s"
            + " AND  " + DBHelper.CollectionItems.ITEM_ID + " =  " + DBHelper.Sounds._ID
            + " AND  " + DBHelper.CollectionItems.RESOURCE_TYPE + " =  %s"
        + ")"
        + " UNION SELECT DISTINCT " + DBHelper.Activities.SOUND_ID + " FROM " + Table.ACTIVITIES.name
        + " WHERE " + DBHelper.Activities.SOUND_TYPE + " = %s";

    /**
     * Roughly corresponds to locally synced collections.
     */
    public interface CollectionItemTypes {
        int TRACK           = 0;
        int LIKE            = 1;
        int FOLLOWING       = 2;
        int FOLLOWER        = 3;
        int FRIEND          = 4;
        //int SUGGESTED_USER  = 5; //unused
        //int SEARCH          = 6; //unused
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
            // only delete tracks from other users - needs proper state checking
            final long trackId = intent.getLongExtra(PlaybackService.BroadcastExtras.ID, 0);
            final long userId = intent.getLongExtra(PlaybackService.BroadcastExtras.USER_ID, 0);
            if (trackId > 0 && userId != SoundCloudApplication.getUserIdFromContext(context)) {
                removeTrack(context).execute(trackId);
            }
        }

        public static AsyncTask<Long,Void,Void> removeTrack(final Context context) {
            return new AsyncTask<Long,Void,Void>(){
                @Override
                protected Void doInBackground(Long... params) {
                    context.getContentResolver().delete(Content.TRACK.forId(params[0]), null, null);
                    context.getContentResolver().delete(Content.ME_ALL_ACTIVITIES.uri,
                            DBHelper.Activities.SOUND_ID + " = " + params[0] + " AND " +
                                    DBHelper.ActivityView.TYPE + " NOT IN ( " + Activity.getDbPlaylistTypesForQuery() + " ) ", null);
                    return null;
                }
            };
        }
    }
}
