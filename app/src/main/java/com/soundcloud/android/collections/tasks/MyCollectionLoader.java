package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.Friend;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.api.legacy.InvalidTokenException;
import com.soundcloud.android.api.legacy.Request;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads collection from local storage. Model objects which are not in the database yet will
 * be resolved and stored automatically.
 * <p/>
 * This is used to potentially complete partially synced collections. If the collection is
 * fully cached in the database no remote lookups are performed.
 */
@Deprecated
public class MyCollectionLoader<T extends ScModel> implements CollectionLoader<T> {

    @Override @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    public ReturnData<T> load(PublicApi api, CollectionParams<T> params) {
        final Context context = SoundCloudApplication.instance;
        final ContentResolver resolver = context.getContentResolver();
        boolean keepGoing = true;
        int responseCode = HttpStatus.SC_OK;

        switch (params.getContent()) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // these don't sync with mini representations. we might only have ids
                List<Long> storedIds = new UserAssociationStorage(context).getStoredIds(params.getPagedUri());

                // if we already have all the data, this is a NOP
                try {
                    fetchAndStoreMissingCollectionItems(resolver, api, storedIds, params.getContent(), false);
                } catch (InvalidTokenException e) {
                    // TODO, move this once we centralize our error handling
                    // InvalidTokenException should expose the response code so we don't have to hardcode it here
                    responseCode = HttpStatus.SC_UNAUTHORIZED;
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    keepGoing = false;
                }
        }

        List<T> newItems = loadLocalContent(resolver,
                params.loadModel,
                params.getPagedUri());


        if (keepGoing) {
            keepGoing = newItems.size() > 0;
        }

        return new ReturnData<>(newItems, params, null, responseCode, keepGoing, true);
    }

    // TODO: this is horrible, leftover from ScModelManager
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private static <T extends ScModel> List<T> loadLocalContent(ContentResolver resolver,
                                                                Class<T> resourceType,
                                                                Uri localUri) {
        Cursor itemsCursor = resolver.query(localUri, null, null, null, null);
        List<ScModel> items = new ArrayList<>();
        if (itemsCursor != null) {
            while (itemsCursor.moveToNext())
                if (PublicApiTrack.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedTrackFromCursor(itemsCursor));
                } else if (PublicApiUser.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedUserFromCursor(itemsCursor));
                } else if (Friend.class.equals(resourceType)) {
                    items.add(new Friend(SoundCloudApplication.sModelManager.getCachedUserFromCursor(itemsCursor)));
                } else if (SoundAssociation.class.equals(resourceType)) {
                    items.add(new SoundAssociation(itemsCursor));
                } else if (UserAssociation.class.equals(resourceType)) {
                    items.add(new UserAssociation(itemsCursor));
                } else if (PublicApiPlaylist.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedPlaylistFromCursor(itemsCursor));
                } else {
                    throw new IllegalArgumentException("NOT HANDLED YET " + resourceType);
                }
        }
        if (itemsCursor != null) {
            itemsCursor.close();
        }
        //noinspection unchecked
        return (List<T>) items;
    }



    /**
     *
     * @param resolver
     * @param api          the api
     * @param modelIds     a list of model ids
     * @param content      the content to fetch for
     * @param ignoreStored if it should ignore stored ids
     * @return how many entries where stored in the db
     * @throws java.io.IOException
     */
    // TODO really pass in api as parameter?
    @Deprecated
    public int fetchAndStoreMissingCollectionItems(ContentResolver resolver, PublicApi api,
                                                   @NotNull List<Long> modelIds,
                                                   final Content content,
                                                   boolean ignoreStored) throws IOException {
        if (modelIds.isEmpty()) {
            return 0;
        }

        return getDaoForContent(resolver, content).createCollection(
                fetchMissingCollectionItems(resolver, api, modelIds, content, ignoreStored)
        );
    }

    // TODO really pass in api as parameter?
    @Deprecated
    private List<PublicApiResource> fetchMissingCollectionItems(ContentResolver resolver, PublicApi api,
                                                                @NotNull List<Long> modelIds,
                                                                final Content content,
                                                                boolean ignoreStored) throws IOException {

        if (modelIds.isEmpty()) {
            return Collections.emptyList();
        }

        // copy so we don't modify the original
        List<Long> ids = new ArrayList<>(modelIds);
        if (!ignoreStored) {
            ids.removeAll(getStoredIds(resolver, content, modelIds));
        }
        // TODO this has to be abstracted more. Hesitant to do so until the api is more final
        Request request = PublicApiTrack.class.equals(content.modelType) ||
                SoundAssociation.class.equals(content.modelType) ? Content.TRACKS.request() : Content.USERS.request();

        return api.readListFromIds(request, ids);
    }

    private BaseDAO<PublicApiResource> getDaoForContent(ContentResolver resolver, final Content content) {
        return new BaseDAO<PublicApiResource>(resolver) {
            @Override
            public Content getContent() {
                return content;
            }
        };
    }

    /**
     * @return a list of all ids for which objects are stored in the db.
     * DO NOT REMOVE BATCHING, SQlite has a variable limit that may vary per device
     * http://www.sqlite.org/limits.html
     */
    @Deprecated
    public Set<Long> getStoredIds(ContentResolver resolver, final Content content, List<Long> ids) {
        BaseDAO<PublicApiResource> dao = getDaoForContent(resolver, content);
        Set<Long> storedIds = new HashSet<>();
        for (int i = 0; i < ids.size(); i += BaseDAO.RESOLVER_BATCH_SIZE) {
            List<Long> batch = ids.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, ids.size()));
            List<Long> newIds = dao.buildQuery()
                    .select(BaseColumns._ID)
                    .whereIn(BaseColumns._ID, Lists.transform(batch, Functions.toStringFunction()))
                    .where("AND " + TableColumns.ResourceTable.LAST_UPDATED + " > ?", "0")
                    .queryIds();
            storedIds.addAll(newIds);
        }
        return storedIds;
    }
}
