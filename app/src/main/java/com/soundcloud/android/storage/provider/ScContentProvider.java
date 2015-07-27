package com.soundcloud.android.storage.provider;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.SQLiteErrors;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Lists;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"PMD.ExcessiveClassLength", "PMD.ExcessiveMethodLength", "PMD.NPathComplexity"})
public class ScContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.soundcloud.android.provider.ScContentProvider";
    private static final String TAG = ScContentProvider.class.getSimpleName();
    private DatabaseManager databaseManager;

    public ScContentProvider() {
    }

    public ScContentProvider(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCreate() {
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance(getContext());
        }
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

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        return safeExecute(new DbOperation<Uri>() {
            @Override
            public Uri execute() {
                return doInsert(uri, values);
            }
        }, null);
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

    @Override
    public int update(final Uri uri, final ContentValues values, final String where, final String[] whereArgs) {
        return safeExecute(new DbOperation<Integer>() {
            @Override
            public Integer execute() {
                return doUpdate(uri, values, where, whereArgs);
            }
        }, 0);
    }

    @Override
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (values == null || values.length == 0) {
            return 0;
        }

        SQLiteDatabase db = databaseManager.getWritableDatabase();
        String[] extraCV = null;
        boolean recreateTable = false;
        boolean deleteUri = false;

        final Content content = Content.match(uri);
        final Table table;
        switch (content) {
            case TRACKS:
            case USERS:
            case SOUNDS:
            case PLAYLISTS:
                content.table.upsert(db, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return values.length;

            case COMMENTS:
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
                table = content.table;
                break;

            case ME_SOUNDS:
                table = Table.Posts;
                break;

            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                table = Table.UserAssociations;
                extraCV = new String[]{TableColumns.UserAssociations.ASSOCIATION_TYPE, String.valueOf(content.collectionType)};
                break;

            case ME_REPOSTS:
                throw new IllegalArgumentException("Trying to bulkInsert reposts, and should not");

            case PLAYLIST_TRACKS:
                deleteUri = true; // clean out table first
                table = Table.PlaylistTracks;
                extraCV = new String[]{TableColumns.PlaylistTracks.PLAYLIST_ID, uri.getPathSegments().get(1)};
                break;

            case ME_SHORTCUTS:
                recreateTable = true;
                table = content.table;
                break;

            default:
                table = content.table;
        }

        if (table == null) {
            throw new IllegalArgumentException("No table for URI " + uri);
        }

        db.beginTransaction();
        try {
            boolean failed = false;

            if (recreateTable) {
                db.delete(table.name(), null, null);
            }

            if (deleteUri) {
                delete(uri, null, null);
            }

            for (ContentValues v : values) {
                if (v != null) {
                    if (extraCV != null) {
                        v.put(extraCV[0], extraCV[1]);
                    }
                    log("bulkInsert: " + v);
                    if (db.insertWithOnConflict(table.name(), null, v, SQLiteDatabase.CONFLICT_REPLACE) < 0) {
                        Log.w(TAG, "replace returned failure");
                        failed = true;
                        break;
                    }
                }
            }

            if (content == Content.ME_SHORTCUTS) {
                db.execSQL("INSERT OR IGNORE INTO " + Table.Users.name() + " (_id, username, avatar_url, permalink_url) " +
                        " SELECT id, text, icon_url, permalink_url FROM " + Table.Suggestions.name() + " where kind = 'following'");
                db.execSQL("INSERT OR IGNORE INTO " + Table.Sounds.name() + " (_id, title, artwork_url, permalink_url, _type) " +
                        " SELECT id, text, icon_url, permalink_url, 0 FROM " + Table.Suggestions.name() + " where kind = 'like'");
            }

            if (!failed) {
                db.setTransactionSuccessful();
            }
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
            default:
                return null;
        }
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
    public static String[] formatWithUser(String[] columns, long userId) {
        for (int i = 0; i < columns.length; i++) {
            columns[i] = columns[i].replace("$$$", String.valueOf(userId));
        }
        return columns;
    }

    public static String[] getUserViewColumns(Table table) {
        return new String[]{
                table + ".*",
                "EXISTS (SELECT 1 FROM " + Table.UserAssociations + ", " + Table.Users.name()
                        + " WHERE " + TableColumns.Users._ID + " = " + TableColumns.UserAssociations.TARGET_ID
                        + " AND " + TableColumns.UserAssociations.ASSOCIATION_TYPE + " = " + CollectionItemTypes.FOLLOWING
                        + " AND " + TableColumns.UserAssociations.OWNER_ID + " = $$$) AS " + TableColumns.Users.USER_FOLLOWING,
                "EXISTS (SELECT 1 FROM " + Table.UserAssociations + ", " + Table.Users.name()
                        + " WHERE " + TableColumns.Users._ID + " = " + TableColumns.UserAssociations.TARGET_ID
                        + " AND " + TableColumns.UserAssociations.ASSOCIATION_TYPE + " = " + CollectionItemTypes.FOLLOWER
                        + " AND " + TableColumns.UserAssociations.OWNER_ID + " = $$$) AS " + TableColumns.Users.USER_FOLLOWER
        };
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private Cursor doQuery(final Uri uri,
                           final String[] columns,
                           final String selection,
                           final String[] selectionArgs,
                           final String sortOrder) {
        final long userId = SoundCloudApplication.fromContext(getContext()).getAccountOperations().getLoggedInUserId();
        final SCQueryBuilder qb = new SCQueryBuilder();
        String[] _columns = columns;
        String _selection = selection;
        String[] _selectionArgs = selectionArgs;
        String _sortOrder = sortOrder;
        final Content content = Content.match(uri);
        String query = null;
        switch (content) {

            case ME:
                qb.setTables(content.table.name());
                _selection = "_id = ?";
                _selectionArgs = new String[]{String.valueOf(userId)};
                break;

            case COLLECTIONS:
            case USER_ASSOCIATIONS:
                qb.setTables(content.table.name());
                break;

            case ME_SOUNDS:
                joinPostsAndSoundView(qb);

                if (_columns == null) {
                    _columns = getPostsColumns() ;
                }
                _sortOrder = makeCollectionSort(uri, Table.Posts.field(TableColumns.Posts.CREATED_AT) + " DESC");

                break;

            case ME_PLAYLISTS:
                joinPostsAndSoundView(qb);
                if (_columns == null) {
                    _columns = getPostsColumns() ;
                }
                qb.appendWhere(" AND " + Table.Posts.field(TableColumns.Posts.TYPE) + " = '" + TableColumns.Posts.TYPE_POST + "' AND "
                        + Table.Posts.field(TableColumns.Posts.TARGET_TYPE) + " = '" + TableColumns.Sounds.TYPE_PLAYLIST + "'");

                _sortOrder = makeCollectionSort(uri, Table.Posts.field(TableColumns.Posts.CREATED_AT) + " DESC");
                break;

            case ME_LIKES:
                qb.setTables(Table.SoundView.name() + " INNER JOIN " + Table.Likes.name()
                        + " ON " + Table.SoundView + "." + TableColumns.SoundView._ID + " = " + Table.Likes.name() + "." + TableColumns.Likes._ID);
                if ("1".equals(uri.getQueryParameter(Parameter.TYPE_IDS_ONLY))) {
                    _columns = new String[]{Table.Likes + "." + TableColumns.Likes._TYPE, Table.Likes + "." + TableColumns.Likes._ID};
                } else if (_columns == null) {
                    _columns = addFakeLikeAssociationColumns(getSoundViewColumns(Table.SoundView), userId);
                }

                _sortOrder = Table.Likes + "." + TableColumns.Likes.CREATED_AT + " DESC";

                qb.appendWhere(Table.Likes + "." + TableColumns.Likes._TYPE + " = " + Table.SoundView + "." + TableColumns.SoundView._TYPE);
                qb.appendWhere(" AND " + TableColumns.Likes.REMOVED_AT + " IS NULL");
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND " + TableColumns.SoundView.CACHED + "= 1");
                }

                break;

            case ME_REPOSTS:
                joinPostsAndSoundView(qb);
                if (_columns == null) {
                    _columns = getPostsColumns() ;
                }

                qb.appendWhere(" AND " + Table.Posts.field(TableColumns.Posts.TYPE) + " = '" + TableColumns.Posts.TYPE_REPOST + "'");

                _sortOrder = makeCollectionSort(uri, TableColumns.Posts.CREATED_AT + " DESC");

                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND " + TableColumns.SoundView.CACHED + "= 1");
                }
                break;

            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                /* XXX special case for now. we  need to not join in the users table on an id only request, because
                it is an inner join and will not return ids with missing users. Switching to a left join is possible
                but not 4 days before major release*/
                if ("1".equals(uri.getQueryParameter(Parameter.IDS_ONLY))) {
                    qb.setTables(Table.UserAssociations.name());
                    qb.appendWhere(Table.UserAssociations.name() + "." + TableColumns.UserAssociations.OWNER_ID + " = " + userId);
                    qb.appendWhere(" AND " + TableColumns.UserAssociations.ASSOCIATION_TYPE + " = " + content.collectionType);
                    _columns = new String[]{TableColumns.UserAssociations.TARGET_ID};
                    _sortOrder = makeCollectionSort(uri, sortOrder);

                } else {
                    qb.setTables(Table.UserAssociationView.name());
                    if (_columns == null) {
                        _columns = formatWithUser(getUserViewColumns(Table.UserAssociationView), userId);
                    }
                    qb.appendWhere(Table.UserAssociationView.name() + "." + TableColumns.UserAssociationView.USER_ASSOCIATION_OWNER_ID + " = " + userId);
                    qb.appendWhere(" AND " + TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = " + content.collectionType);

                    _sortOrder = makeCollectionSort(uri, sortOrder != null ?
                            sortOrder : TableColumns.UserAssociationView.USER_ASSOCIATION_POSITION);
                }
                break;


            case ME_USERID:
                MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID}, 1);
                c.addRow(new Object[]{userId});
                return c;

            case TRACKS:
            case PLAYLISTS:
                qb.setTables(Table.SoundView.name());
                if (_columns == null) {
                    _columns = getSoundViewColumns(Table.SoundView);
                }
                if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
                    _sortOrder = "RANDOM()";
                }
                appendSoundType(qb, content);
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(" AND " + TableColumns.SoundView.CACHED + "= 1");
                }
                break;

            case TRACK:
            case PLAYLIST:
                qb.setTables(Table.SoundView.name());
                appendSoundType(qb, content);
                qb.appendWhere(" AND " + Table.SoundView.id + " = " + uri.getLastPathSegment());
                if (_columns == null) {
                    _columns = getSoundViewColumns(Table.SoundView);
                }
                break;

            case PLAYLIST_TRACKS:
                qb.setTables(Table.PlaylistTracksView.name());
                // extract the playlist id from the second to last segment
                qb.appendWhere(TableColumns.PlaylistTracksView.PLAYLIST_ID + " = " + uri.getPathSegments().get(1));
                if (_columns == null) {
                    _columns = getSoundViewColumns(Table.PlaylistTracksView);
                }
                if (_sortOrder == null) {
                    _sortOrder = TableColumns.PlaylistTracksView.PLAYLIST_POSITION + " ASC, "
                            + TableColumns.PlaylistTracksView.PLAYLIST_ADDED_AT + " DESC";
                }
                break;

            case PLAYLIST_ALL_TRACKS:
                qb.setTables(Table.PlaylistTracks.name());
                break;

            case USERS:
                qb.setTables(content.table.name());
                if (_columns == null) {
                    _columns = formatWithUser(getUserViewColumns(Table.Users), userId);
                }
                break;

            case USER:
                qb.setTables(content.table.name());
                qb.appendWhere(Table.Users.id + " = " + uri.getLastPathSegment());
                if (_columns == null) {
                    _columns = formatWithUser(getUserViewColumns(Table.Users), userId);
                }
                break;

            case TRACK_METADATA:
                qb.setTables(content.table.name());
                qb.appendWhere(TableColumns.TrackMetadata.USER_ID + " = " + userId);
                break;

            case ME_SOUND_STREAM:
                if (_columns == null) {
                    _columns = getSoundViewColumns(Table.ActivityView,
                            TableColumns.ActivityView.SOUND_ID, TableColumns.ActivityView.SOUND_TYPE);
                }
                if ("1".equals(uri.getQueryParameter(Parameter.CACHED))) {
                    qb.appendWhere(TableColumns.SoundView.CACHED + "= 1 AND ");
                }

            case ME_ALL_ACTIVITIES:
            case ME_ACTIVITIES:
                qb.setTables(Table.ActivityView.name());
                if (content != Content.ME_ALL_ACTIVITIES) {
                    // filter out playlist
                    qb.appendWhere(TableColumns.ActivityView.CONTENT_ID + "=" + content.id + " ");
                }
                _sortOrder = makeActivitiesSort(uri, sortOrder);
                break;
            case COMMENTS:
                qb.setTables(content.table.name());
                break;

            case ANDROID_SEARCH_SUGGEST:
            case ANDROID_SEARCH_SUGGEST_PATH:
                return suggest(uri, columns, selection, selectionArgs);

            case ANDROID_SEARCH_REFRESH:
            case ANDROID_SEARCH_REFRESH_PATH:
                return refresh(uri, columns, selection, selectionArgs, sortOrder);

            case ME_SHORTCUT:
                qb.setTables(content.table.name());
                qb.appendWhere(Table.Suggestions.id + " = " + uri.getLastPathSegment());
                break;

            case ME_SHORTCUTS:
                qb.setTables(content.table.name());
                break;

            case UNKNOWN:
            default:
                throw new IllegalArgumentException("No query available for: " + uri);
        }

        if (query == null) {
            query = qb.buildQuery(_columns, _selection, null /* selectionArgs passed further down */, null, _sortOrder, getRowLimit(uri));
        }
        log("query: " + query);
        SQLiteDatabase db = databaseManager.getReadableDatabase();
        Cursor c = db.rawQuery(query, _selectionArgs);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void joinPostsAndSoundView(SCQueryBuilder qb) {
        qb.setTables(Table.Posts.name() + "," + Table.SoundView.name());
        qb.appendWhere(Table.Posts.field(TableColumns.Posts.TARGET_TYPE) + " = " + Table.SoundView.field(TableColumns.SoundView._TYPE) + " AND " +
                Table.Posts.field(TableColumns.Posts.TARGET_ID) + " = " + Table.SoundView.field(TableColumns.SoundView._ID));
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
        qb.appendWhere(" " + TableColumns.SoundView._TYPE + " = '" +
                (content.modelType == PublicApiTrack.class ? Playable.DB_TYPE_TRACK : Playable.DB_TYPE_PLAYLIST)
                + "'");
    }

    private Uri doInsert(final Uri uri, final ContentValues values) {
        final long userId = SoundCloudApplication.fromContext(getContext()).getAccountOperations().getLoggedInUserId();
        long id;
        Uri result;
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        final Content content = Content.match(uri);
        switch (content) {
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // inserts based on collection URIs
            //////////////////////////////////////////////////////////////////////////////////////////////////
            case COLLECTION:
            case COLLECTIONS:
            case USER_ASSOCIATIONS:
            case ME_SOUNDS:
            case ME_PLAYLISTS:
            case ME_SHORTCUTS:
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
            // upserts to account for not overwriting extra data retrieved from api-mobile
            //////////////////////////////////////////////////////////////////////////////////////////////////
            case TRACKS:
            case PLAYLISTS:
            case USERS:
                id = content.table.upsert(db, new ContentValues[]{values});
                if (id >= 0 && values.containsKey(BaseColumns._ID)) {
                    id = values.getAsLong(BaseColumns._ID);
                }
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);
                return result;

            //////////////////////////////////////////////////////////////////////////////////////////////////
            // special cases
            //////////////////////////////////////////////////////////////////////////////////////////////////

            case PLAYLIST_TRACKS:
                id = db.insert(content.table.name(), null, values);
                result = uri.buildUpon().appendPath(String.valueOf(id)).build();
                getContext().getContentResolver().notifyChange(result, null, false);

                // update the track count in the playlist tables
                String playlistId = values.getAsString(TableColumns.PlaylistTracks.PLAYLIST_ID);
                final String trackCount = TableColumns.Sounds.TRACK_COUNT;
                db.execSQL("UPDATE " + Table.Sounds.name() + " SET " + trackCount + "=" + trackCount + " + 1 WHERE " +
                                TableColumns.Sounds._ID + "=? AND " + TableColumns.Sounds._TYPE + "=?",
                        new String[]{playlistId, String.valueOf(Playable.DB_TYPE_PLAYLIST)});
                return result;

            //////////////////////////////////////////////////////////////////////////////////////////////////
            // upserts for single-resource URIs
            //////////////////////////////////////////////////////////////////////////////////////////////////

            case TRACK:
            case USER:
            case PLAYLIST:
                if (content.table.upsert(db, new ContentValues[]{values}) != -1) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                } else {
                    log("Error inserting to uri " + uri.toString());
                }
                return uri;

            case TRACK_METADATA:
                if (!values.containsKey(TableColumns.TrackMetadata.USER_ID)) {
                    values.put(TableColumns.TrackMetadata.USER_ID, userId);
                }
                content.table.upsert(db, new ContentValues[]{values});
                return uri.buildUpon().appendPath(
                        values.getAsString(TableColumns.TrackMetadata._ID)).build();

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private int doDelete(Uri uri, String where, String[] whereArgs) {
        final SQLiteDatabase db = databaseManager.getWritableDatabase();
        int count;
        final Content content = Content.match(uri);

        final long userId = SoundCloudApplication.fromContext(getContext()).getAccountOperations().getLoggedInUserId();
        switch (content) {
            case COLLECTIONS:
            case PLAYLISTS:
            case ME_ALL_ACTIVITIES:
            case ME_SOUNDS:
                break;

            case TRACK:
            case USER:
            case PLAYLIST:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                break;

            case PLAYLIST_TRACKS:
                where = (!TextUtils.isEmpty(where) ? where + " AND " : "") + TableColumns.PlaylistTracks.PLAYLIST_ID + "=" + uri.getPathSegments().get(1);
                break;

            case ME_ACTIVITIES:
            case ME_SOUND_STREAM:
                where = TableColumns.Activities.CONTENT_ID + "= ?";
                whereArgs = new String[]{String.valueOf(content.id)};
                break;

            case ME_PLAYLISTS:
                String whereAppend = Table.Posts.field(TableColumns.Posts.TYPE) + " = '" + TableColumns.Posts.TYPE_POST + "' AND "
                        + Table.Posts.field(TableColumns.Posts.TARGET_TYPE) + " = " + TableColumns.Sounds.TYPE_PLAYLIST;
                where = TextUtils.isEmpty(where) ? whereAppend : where + " AND " + whereAppend;
                break;

            case ME_REPOSTS:
                whereAppend = TableColumns.Posts.TYPE + " = '" + TableColumns.Posts.TYPE_REPOST + "'";
                where = TextUtils.isEmpty(where) ? whereAppend : where + " AND " + whereAppend;
                break;

            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                whereAppend = Table.UserAssociations.name() + "." + TableColumns.UserAssociations.OWNER_ID + " = " + userId
                        + " AND " + TableColumns.UserAssociations.ASSOCIATION_TYPE + " = " + content.collectionType;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;

                break;

            case USER_ASSOCIATIONS:
                whereAppend = Table.UserAssociations.name() + "." + TableColumns.UserAssociations.OWNER_ID + " = " + userId;
                where = TextUtils.isEmpty(where) ? whereAppend
                        : where + " AND " + whereAppend;
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.delete(content.table.name(), where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    private int doUpdate(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        int count;
        final Content content = Content.match(uri);
        switch (content) {
            case COLLECTIONS:
            case USER_ASSOCIATIONS:
                count = db.update(content.table.name(), values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            case COLLECTION:
            case TRACK:
            case USER:
            case PLAYLIST:
                where = TextUtils.isEmpty(where) ? "_id=" + uri.getLastPathSegment() : where + " AND _id=" + uri.getLastPathSegment();
                count = db.update(content.table.name(), values, where, whereArgs);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return count;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Suggest tracks and users based on partial user input.
     *
     * @return a cursor with search suggestions. See {@link SearchManager} for documentation
     * on schema etc.
     * @see <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     * Building a suggestion table</a>
     */
    private Cursor suggest(Uri uri, String[] columns, String selection, String[] selectionArgs) {
        log("suggest(" + uri + "," + Arrays.toString(columns) + "," + selection + "," + Arrays.toString(selectionArgs) + ")");
        if (selectionArgs == null) {
            throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
        }
        SQLiteDatabase db = databaseManager.getReadableDatabase();
        SCQueryBuilder qb = new SCQueryBuilder();
        qb.setTables(Table.Suggestions.name());

        qb.appendWhere(TableColumns.Suggestions.TEXT + " LIKE '" + selectionArgs[0] + "%' OR " + TableColumns.Suggestions.TEXT +
                " LIKE '% " + selectionArgs[0] + "%'");

        final String limit = uri.getQueryParameter(Parameter.LIMIT);
        final String query = qb.buildQuery(
                new String[]{
                        BaseColumns._ID,
                        TableColumns.Suggestions.ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                        TableColumns.Suggestions.ICON_URL,
                        "'content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/' || _id" + " AS "
                                + SearchManager.SUGGEST_COLUMN_ICON_1,
                        "'" + SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT + "' AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
                },
                null, null, null, null, null, limit);

        log("suggest: query=" + query);
        return db.rawQuery(query, null);
    }

    /**
     * Called when suggest returns {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID}.
     * Currently not used.
     */
    @SuppressWarnings({"UnusedParameters", "PMD.UnusedFormalParameter"})
    private Cursor refresh(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    // New likes don't have all the fields expected to load into SoundAssociation in legacy ScListFragments etc.
    private static String[] addFakeLikeAssociationColumns(String[] columns, long userId) {
        final String[] arr = new String[columns.length + 3];
        final List<String> cols = Lists.newArrayList(columns);
        cols.add(Table.Likes + "." + TableColumns.Likes.CREATED_AT + " AS " + TableColumns.AssociationView.ASSOCIATION_TIMESTAMP);
        cols.toArray(arr);

        return arr;
    }

    private static String[] getSoundViewColumns(Table table) {
        return getSoundViewColumns(table, table.id, table.type);
    }

    private static String[] getSoundViewColumns(Table table, String idCol, String typeCol) {
        return new String[]{
                table.name() + ".*",
                "EXISTS (SELECT 1 FROM " + Table.Likes + ", " + Table.Sounds.name()
                        + " WHERE " + idCol + " = " + Table.Likes.name() + "." + TableColumns.Likes._ID
                        + " AND " + typeCol + " = " + Table.Likes.name() + "." + TableColumns.Likes._TYPE
                        + " AND " + TableColumns.Likes.REMOVED_AT + " IS NULL)"
                        + " AS " + TableColumns.SoundView.USER_LIKE,

                "EXISTS (SELECT 1 FROM " + Table.Posts + ", " + Table.Sounds.name()
                        + " WHERE " + idCol + " = " + TableColumns.Posts.TARGET_ID
                        + " AND " + typeCol + " = " + TableColumns.Posts.TARGET_TYPE
                        + " AND " + TableColumns.Posts.TYPE + " = '" + TableColumns.Posts.TYPE_REPOST + "')"
                        + " AS " + TableColumns.SoundView.USER_REPOST
        };
    }

    public static String POST_TYPE = "post_type";
    private static String[] getPostsColumns() {
        return new String[]{
                Table.SoundView.name() + ".*",
                Table.Posts.field(TableColumns.Posts.CREATED_AT) + " AS " + TableColumns.AssociationView.ASSOCIATION_TIMESTAMP,
                Table.Posts.field(TableColumns.Posts.TYPE) + " AS " + POST_TYPE,
                "EXISTS (SELECT 1 FROM " + Table.Likes + ", " + Table.Sounds.name()
                        + " WHERE Sounds._id = " + Table.Likes.name() + "." + TableColumns.Likes._ID
                        + " AND Sounds._type = " + Table.Likes.name() + "." + TableColumns.Likes._TYPE
                        + " AND " + TableColumns.Likes.REMOVED_AT + " IS NULL)"
                        + " AS " + TableColumns.SoundView.USER_LIKE,

                "EXISTS (SELECT 1 FROM " + Table.Posts + ", " + Table.Sounds.name()
                        + " WHERE Sounds._id = " + TableColumns.Posts.TARGET_ID
                        + " AND Sounds._type = " + TableColumns.Posts.TARGET_TYPE
                        + " AND " + TableColumns.Posts.TYPE + " = '" + TableColumns.Posts.TYPE_REPOST + "')"
                        + " AS " + TableColumns.SoundView.USER_REPOST
        };
    }

    private static void log(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }

    // don't die on disk i/o problems
    private <V> V safeExecute(DbOperation<V> r, V def) {
        try {
            return r.execute();
        } catch (SQLiteDiskIOException e) {
            final String msg = "sqlite disk I/O:" + SQLiteErrors.convertToErrorMessage(e);
            Log.w(TAG, msg, e);
            ErrorUtils.handleSilentException(msg, e);
            return def;
        }
    }

    static String makeCollectionSort(Uri uri, @Nullable String sortCol) {
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        } else {
            b.append(TextUtils.isEmpty(sortCol) ? TableColumns.CollectionItems.POSITION : sortCol);
        }
        return b.toString();
    }

    static String makeActivitiesSort(Uri uri, String sortCol) {
        StringBuilder b = new StringBuilder();
        if ("1".equals(uri.getQueryParameter(Parameter.RANDOM))) {
            b.append("RANDOM()");
        } else {
            b.append(sortCol == null ? TableColumns.ActivityView.CREATED_AT + " DESC" : sortCol);
        }
        return b.toString();
    }

    public interface Parameter {
        String RANDOM = "random";
        String CACHED = "cached";
        String LIMIT = "limit";
        String OFFSET = "offset";
        String IDS_ONLY = "idsOnly";
        String TYPE_IDS_ONLY = "typeIdsOnly";
    }

    private interface DbOperation<V> {
        V execute();
    }

    /**
     * Roughly corresponds to locally synced collections.
     */
    public interface CollectionItemTypes {
        int TRACK = 0;
        int LIKE = 1;
        int FOLLOWING = 2;
        int FOLLOWER = 3;
        //int FRIEND = 4;
        //int SUGGESTED_USER  = 5;
        //int SEARCH          = 6;
        int REPOST = 7;
        int PLAYLIST = 8;
    }
}
