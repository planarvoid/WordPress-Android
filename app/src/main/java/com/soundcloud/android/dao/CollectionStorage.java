package com.soundcloud.android.dao;

import static com.soundcloud.android.dao.ResolverHelper.idCursorToList;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
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
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionStorage {
    private final ContentResolver mResolver;

    public CollectionStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
    }

    public CollectionStorage(ContentResolver resolver) {
        mResolver = resolver;
    }

    /**
     * @return a list of all ids for which objects are stored in the db.
     * DO NOT REMOVE BATCHING, SQlite has a variable limit that may vary per device
     * http://www.sqlite.org/limits.html
     */
    @Deprecated
    public Set<Long> getStoredIds(final Content content, List<Long> ids) {
        BaseDAO<ScResource> dao = getDaoForContent(content);
        Set<Long> storedIds = new HashSet<Long>();
        for (int i=0; i < ids.size(); i += BaseDAO.RESOLVER_BATCH_SIZE) {
            List<Long> batch = ids.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, ids.size()));
            List<Long> newIds = dao.buildQuery()
                    .select(BaseColumns._ID)
                    .whereIn(BaseColumns._ID, Lists.transform(batch, Functions.toStringFunction()))
                    .where("AND " + DBHelper.ResourceTable.LAST_UPDATED + " > ?", "0")
                    .queryIds();
            storedIds.addAll(newIds);
        }
        return storedIds;
    }

    public void clear() {
        getDaoForContent(Content.COLLECTION_ITEMS).deleteAll();
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
    @Deprecated
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
    @Deprecated
    private List<ScResource> fetchMissingCollectionItems(AndroidCloudAPI api,
                                                        @NotNull List<Long> modelIds,
                                                        final Content content,
                                                        boolean ignoreStored) throws IOException {

        if (modelIds.isEmpty()) return Collections.emptyList();

        // copy so we don't modify the original
        List<Long> ids = new ArrayList<Long>(modelIds);
        if (!ignoreStored) {
            ids.removeAll(getStoredIds(content, modelIds));
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
