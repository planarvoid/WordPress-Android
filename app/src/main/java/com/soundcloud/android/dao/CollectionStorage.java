package com.soundcloud.android.dao;

import static com.soundcloud.android.dao.ResolverHelper.idCursorToList;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionStorage {
    private final ContentResolver mResolver;

    public CollectionStorage(Context context) {
        mResolver = context.getContentResolver();
    }

    public int insertCollection(@NotNull List<? extends ScResource> resources,
                                @NotNull Uri collectionUri,
                                long ownerId) {
        if (ownerId < 0) throw new IllegalArgumentException("need valid ownerId for collection");

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();

            contentValues.put(DBHelper.CollectionItems.POSITION, i);
            contentValues.put(DBHelper.CollectionItems.ITEM_ID, r.id);
            contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
            map.add(collectionUri, contentValues);
        }
        return map.insert(mResolver);
    }

    public List<Long> getLocalIds(Content content, long userId) {
        return idCursorToList(mResolver.query(
                Content.COLLECTION_ITEMS.uri,
                new String[]{ DBHelper.CollectionItems.ITEM_ID },
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{ String.valueOf(content.collectionType), String.valueOf(userId) },
                DBHelper.CollectionItems.SORT_ORDER
            )
        );
    }

    /**
     * @param api           the api
     * @param modelIds      a list of model ids
     * @param content       the content to fetch for
     * @param ignoreStored  if it should ignore stored ids
     * @return how many entries where stored in the db
     * @throws java.io.IOException
     */
    // TODO really pass in api as parameter?
    public int fetchAndStoreMissingCollectionItems(AndroidCloudAPI api,
                                                   @NotNull List<Long> modelIds,
                                                   final Content content,
                                                   boolean ignoreStored) throws IOException {
        if (modelIds.isEmpty()) return 0;

        return getDaoForContent(content).createCollection(
            fetchMissingCollectionItems(api, modelIds, content, ignoreStored)
        );
    }

    // TODO really pass in api as parameter?
    private List<ScResource> fetchMissingCollectionItems(AndroidCloudAPI api,
                                                        @NotNull List<Long> modelIds,
                                                        final Content content,
                                                        boolean ignoreStored) throws IOException {

        if (modelIds.isEmpty()) return Collections.emptyList();

        BaseDAO<ScResource> dao = getDaoForContent(content);
        // copy so we don't modify the original
        List<Long> ids = new ArrayList<Long>(modelIds);
        if (!ignoreStored) {
            ids.removeAll(dao.getStoredIds(modelIds));
        }
        // TODO this has to be abstracted more. Hesitant to do so until the api is more final
        Request request = Track.class.equals(content.modelType) ||
               SoundAssociation.class.equals(content.modelType) ? Content.TRACKS.request() : Content.USERS.request();

        return api.readListFromIds(request, ids);
    }

    private BaseDAO<ScResource> getDaoForContent(final Content content) {
        return new BaseDAO<ScResource>(mResolver) {
            @Override
            public Content getContent() {
                return content;
            }
        };
    }
}
