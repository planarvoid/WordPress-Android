package com.soundcloud.android.provider;

import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated
public class SoundCloudDB {
    public static final int RESOLVER_BATCH_SIZE = 100;

    /**
     * Inserts a list of resources into the database
     * @param resolver
     * @param resources
     * @return
     */
    public static int bulkInsertResources(ContentResolver resolver, List<? extends ScResource> resources) {
        if (resources == null) return 0;
        BulkInsertMap map = new BulkInsertMap();
        for (ScResource resource : resources) {
            if (resource != null) resource.putFullContentValues(map);
        }
        return map.insert(resolver);
    }

    /**
     * Inserts a list of resources and their corresponding dependencies to a given URI.
     * Contains special handling based on Content requirements.
     * @param resolver
     * @param resources
     * @param collectionUri
     * @param ownerId
     * @return the number of insertsions
     */
    public static int insertCollection(ContentResolver resolver,
                                       @NotNull List<? extends ScResource> resources,
                                       @NotNull Uri collectionUri,
                                       long ownerId) {
        if (ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        BulkInsertMap map = new BulkInsertMap();
        for (int i=0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r != null) {
                r.putFullContentValues(map);
                long id = r.id;
                ContentValues contentValues = new ContentValues();
                switch (Content.match(collectionUri)) {
                    case PLAY_QUEUE:
                        contentValues.put(DBHelper.PlayQueue.POSITION, i);
                        contentValues.put(DBHelper.PlayQueue.TRACK_ID, id);
                        contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
                        break;
                    case PLAYLIST_TRACKS:
                        contentValues.put(DBHelper.PlaylistTracks.POSITION, i);
                        contentValues.put(DBHelper.PlaylistTracks.TRACK_ID, id);
                        break;
                    default:
                        contentValues.put(DBHelper.CollectionItems.POSITION, i);
                        contentValues.put(DBHelper.CollectionItems.ITEM_ID, id);
                        contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
                        break;
                }
                map.add(collectionUri,contentValues);
            }
        }
        return map.insert(resolver);
    }

    public static @NotNull List<Long> idCursorToList(Cursor c) {
        if (c == null) return Collections.emptyList();
        List<Long> ids = new ArrayList<Long>(c.getCount());
        while (c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        c.close();
        return ids;
    }

    public static Uri.Builder addPagingParams(Uri uri, int offset, int limit) {
        if (uri == null) return null;

        Uri.Builder b = uri.buildUpon();
        if (offset > 0) {
            b.appendQueryParameter("offset", String.valueOf(offset));
        }
        b.appendQueryParameter("limit", String.valueOf(limit));
        return b;
    }

    /**
     * @return a list of all ids for which objects are store in the database
     */
    public static List<Long> getStoredIdsBatched(ContentResolver resolver, List<Long> ids, Content content) {
        int i = 0;
        List<Long> storedIds = new ArrayList<Long>();
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, ids.size()));
            storedIds.addAll(idCursorToList(
                    resolver.query(content.uri, new String[]{BaseColumns._ID},
                            getWhereInClause(BaseColumns._ID, batch) + " AND " + DBHelper.ResourceTable.LAST_UPDATED + " > 0"
                            , ScModelManager.longListToStringArr(batch), null)
            ));
            i += RESOLVER_BATCH_SIZE;
        }
        return storedIds;
    }

    public static List<Long> getStoredIds(ContentResolver resolver, Uri uri, int offset, int limit) {
        return idCursorToList(resolver.query(SoundCloudDB.addPagingParams(uri, offset, limit)
                .appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build(),
                null, null, null, null));
    }

    public static String getWhereInClause(String column, List<Long> idSet){
        StringBuilder sb = new StringBuilder(column + " IN (?");
        for (int i = 1; i < idSet.size(); i++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }
}
