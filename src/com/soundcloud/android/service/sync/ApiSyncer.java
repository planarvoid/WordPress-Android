package com.soundcloud.android.service.sync;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiSyncer {
    public static final int MINIMUM_LOCAL_ITEMS_STORED = 100;
    private final AndroidCloudAPI mApi;
    private final ContentResolver mResolver;
    private final Context mContext;

    private Map<Content, ContentValues[]> collectionValues = new HashMap<Content, ContentValues[]>();
    private List<Long> trackAdditions = new ArrayList<Long>();
    private List<Long> userAdditions = new ArrayList<Long>();

    private static final int API_LOOKUP_BATCH_SIZE = 200;
    private static final int RESOLVER_BATCH_SIZE = 100;

    public ApiSyncer(SoundCloudApplication app) {
        mApi = app;
        mResolver = app.getContentResolver();
        mContext = app;
    }

    public boolean syncContent(Content c) throws IOException {
        boolean changed = false;
        if (c.remoteUri != null) {
            switch (c) {
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                case ME_SOUND_STREAM:
                    changed = syncActivities(c);
                    break;

                case ME_TRACKS:
                case ME_FAVORITES:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    changed = quickSync(c, SoundCloudApplication.getUserIdFromContext(mContext));
                    break;
            }
        } else {
            switch (c) {
                case TRACK_CLEANUP:
                case USERS_CLEANUP:
                    changed = mResolver.update(c.uri, null, null, null) > 0;
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong("lastSyncCleanup", System.currentTimeMillis());
                    break;
                default:
                    Log.w(ApiSyncService.LOG_TAG, "no remote URI defined for " + c);
            }

        }
        return changed;
    }

    /* package */ boolean syncActivities(Content content) throws IOException {
        LocalCollection collection = LocalCollection.fromContentUri(content.uri, mResolver);
        String future_href = null;
        if (collection != null) {
            future_href = collection.sync_state;
        }

        Request request = future_href == null ? content.request() : Request.to(future_href);
        Activities activities = Activities.fetch(mApi, request, null, -1);

        mResolver.bulkInsert(content.uri, activities.buildContentValues());
        mResolver.bulkInsert(Content.TRACKS.uri, activities.getTrackContentValues());
        mResolver.bulkInsert(Content.USERS.uri, activities.getUserContentValues());

        if (content == Content.ME_ACTIVITIES) {
            mResolver.bulkInsert(Content.COMMENTS.uri, activities.getCommentContentValues());
        }

        LocalCollection.insertLocalCollection(content.uri, activities.future_href, System.currentTimeMillis(), activities.size(), mResolver
        );

        return !activities.isEmpty();
    }

    private boolean quickSync(Content c, final long userId) throws IOException {
        final long start = System.currentTimeMillis();
        int size = 0;
        List<Long> local = idCursorToList(mResolver.query(
                Content.COLLECTION_ITEMS.uri,
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ?",
                new String[]{String.valueOf(c.collectionType)},
                DBHelper.CollectionItems.SORT_ORDER));

        List<Long> remote = getCollectionIds(mApi, c.remoteUri);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());


        if (local.equals(remote)){
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: no change in URI " + c.uri + ". Skipping sync.");
            return false;
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);

        Log.d(ApiSyncService.LOG_TAG, "Need to remove " + itemDeletions.size() + " items");

        int i = 0;
        while (i < itemDeletions.size()) {
            List<Long> batch = itemDeletions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, itemDeletions.size()));
            mResolver.delete(c.uri, CloudUtils.getWhereIds(DBHelper.CollectionItems.ITEM_ID, batch), CloudUtils.longListToStringArr(batch));
            i += RESOLVER_BATCH_SIZE;
        }

        ContentValues[] cv = new ContentValues[remote.size()];
        i = 0;
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i + 1);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }
        mResolver.bulkInsert(c.uri, cv);
        LocalCollection.insertLocalCollection(c.uri, null, System.currentTimeMillis(), remote.size(), mResolver);

        // ensure the first couple of pages of items for quick loading
        int added = SoundCloudDB.bulkInsertParcelables(mResolver,getAdditionsFromIds(mApi,mResolver,remote.subList(0, Math.min(remote.size(),MINIMUM_LOCAL_ITEMS_STORED)),
                    c.resourceType.equals(Track.class) ? Content.TRACKS : Content.USERS,false));

        Log.d(ApiSyncService.LOG_TAG, "Added " + added + " new items for this endpoint");
        return true;
    }


    public static List<Parcelable> getAdditionsFromIds(AndroidCloudAPI app,
                                                       ContentResolver resolver,
                                                       List<Long> additions,
                                                       Content content,
                                                       boolean ignoreStored) throws IOException {

        if (additions == null || additions.size() == 0) return new ArrayList<Parcelable>();

        if (!ignoreStored) {
            // remove anything that is already in the DB
            int i = 0;
            List<Long> storedIds = new ArrayList<Long>();
            while (i < additions.size()) {
                List<Long> batch = additions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, additions.size()));
                storedIds.addAll(idCursorToList(resolver.query(content.uri, new String[]{DBHelper.Tracks._ID},
                        CloudUtils.getWhereIds(DBHelper.Tracks._ID, batch), CloudUtils.longListToStringArr(batch), null)));
                i += RESOLVER_BATCH_SIZE;
            }
            additions.removeAll(storedIds);
        }

        // add new items from batch lookups
        List<Parcelable> items = new ArrayList<Parcelable>();
        int i = 0;
        while (i < additions.size()) {

            List<Long> batch = additions.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, additions.size()));
            InputStream is = validateResponse(app.get(Request.to(content.remoteUri)
                    .add("linked_partitioning", "1").add("limit", API_LOOKUP_BATCH_SIZE).add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            CollectionHolder holder;
            if (Track.class.equals(content.resourceType)) {
                holder = app.getMapper().readValue(is, TracklistItemHolder.class);
                for (TracklistItem t : (TracklistItemHolder) holder) {
                    items.add(new Track(t));
                }
            } else if (User.class.equals(content.resourceType)) {
                holder = app.getMapper().readValue(is, UserlistItemHolder.class);
                for (UserlistItem u : (UserlistItemHolder) holder) {
                    items.add(new User(u));
                }
            }
            i += API_LOOKUP_BATCH_SIZE;
        }
        return items;
    }

    public static List<Long> idCursorToList(Cursor c) {
        List<Long> ids = new ArrayList<Long>();
        if (c != null && c.moveToFirst()) {
            do {
                ids.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return ids;
    }

    private static List<Long> getCollectionIds(AndroidCloudAPI app, String endpoint) throws IOException {
        List<Long> items = new ArrayList<Long>();
        IdHolder holder = null;
        if (endpoint.contains("?")) endpoint = endpoint.substring(0,endpoint.indexOf("?"));
        do {
            Request request =  (holder == null) ? Request.to(endpoint + "/ids") : Request.to(holder.next_href);
            request.add("linked_partitioning", "1");
            holder = app.getMapper().readValue(validateResponse(app.get(request)).getEntity().getContent(), IdHolder.class);
            if (holder.collection != null) items.addAll(holder.collection);

        } while (holder.next_href != null);
        return items;
    }

    private static HttpResponse validateResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK
            && response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_MODIFIED) {
            throw new IOException("Invalid response: " + response.getStatusLine());
        }
        return response;
    }



    public static class IdHolder extends CollectionHolder<Long> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}
