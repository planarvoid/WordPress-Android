package com.soundcloud.android.service.sync;

import android.content.SyncResult;
import android.net.Uri;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.SoundCloudDB;
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

    public Result syncContent(Uri uri, String action) throws IOException {
        Content c = Content.match(uri);
        Result result = null;
        if (c.remoteUri != null) {
            switch (c) {
                case ME_ALL_ACTIVITIES:
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                case ME_SOUND_STREAM:
                    result = syncActivities(uri, action);
                    break;

                case ME_TRACKS:
                case ME_FAVORITES:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    result = quickSync(c, SoundCloudApplication.getUserIdFromContext(mContext));
                    break;
            }
        } else {
            switch (c) {
                case TRACK_CLEANUP:
                case USERS_CLEANUP:
                    result = new Result(c.uri);
                    result.wasChanged = mResolver.update(c.uri, null, null, null) > 0;
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putLong(SyncAdapterService.PREF_LAST_SYNC_CLEANUP, System.currentTimeMillis())
                            .commit();
                    break;
                default:
                    Log.w(ApiSyncService.LOG_TAG, "no remote URI defined for " + c);
            }

        }
        return result;
    }

    /* package */ Result syncActivities(Uri uri, String action) throws IOException {
        Result result = new Result(uri);
        Log.d(ApiSyncService.LOG_TAG, "syncActivities(" + uri + ")");

        final Content c = Content.match(uri);
        final int inserted;
        final Activities activities;
        if (!TextUtils.isEmpty(action) && action.equals(ApiSyncService.ACTION_APPEND)) {
            final Activity lastActivity = Activities.getLastActivity(c, mResolver);
            Request request = new Request(c.request()).add("limit", Consts.COLLECTION_PAGE_SIZE);
            if (lastActivity != null) request.add("cursor", lastActivity.toGUID());
            activities = Activities.fetch(mApi, request);
            inserted = activities.insert(c, mResolver);
        } else {
            String future_href = LocalCollection.getExtraFromUri(uri, mResolver);
            Request request = future_href == null ? c.request() : Request.to(future_href);
            activities = Activities.fetchRecent(mApi, request, MINIMUM_LOCAL_ITEMS_STORED);
            inserted = activities.insert(c, mResolver);
            result.setSyncData(System.currentTimeMillis(), activities.size(), activities.future_href);
        }
        result.wasChanged = !activities.isEmpty();
        Log.d(ApiSyncService.LOG_TAG, "activities: inserted " + inserted + " objects");
        return result;
    }

    private Result quickSync(Content c, final long userId) throws IOException {
        Result result = new Result(c.uri);

        List<Long> local = idCursorToList(mResolver.query(
                Content.COLLECTION_ITEMS.uri,
                new String[] { DBHelper.CollectionItems.ITEM_ID },
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[] { String.valueOf(c.collectionType), String.valueOf(userId)},
                DBHelper.CollectionItems.SORT_ORDER));

        List<Long> remote = getCollectionIds(mApi, c.remoteUri);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(),remote.size(),null);

        if (local.equals(remote) && !(c == Content.ME_FOLLOWERS || c == Content.ME_FOLLOWINGS)){
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: no change in URI " + c.uri + ". Skipping sync.");
            return result;
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

        int startPosition = 1;
        int added;
        switch (c) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // load the first page of items to getSince proper last_seen ordering
                InputStream is = validateResponse(mApi.get(Request.to(c.remoteUri)
                        .add("linked_partitioning", "1").add("limit", Consts.COLLECTION_PAGE_SIZE)))
                        .getEntity().getContent();

                // parse and add first items
                List<Parcelable> firstUsers = new ArrayList<Parcelable>();
                ScModel.getCollectionFromStream(is, mApi.getMapper(), User.class, firstUsers);
                added = SoundCloudDB.bulkInsertParcelables(mResolver, firstUsers, c.uri, userId, 1);

                // remove items from master remote list and adjust start index
                for (Parcelable u : firstUsers) {
                    remote.remove(((User) u).id);
                }
                startPosition = firstUsers.size();
                break;

            default:
                // ensure the first couple of pages of items for quick loading
                added = SoundCloudDB.bulkInsertParcelables(mResolver, getAdditionsFromIds(
                        mApi,
                        mResolver,
                        new ArrayList<Long>(remote.subList(0, Math.min(remote.size(), MINIMUM_LOCAL_ITEMS_STORED))),
                        c.resourceType.equals(Track.class) ? Content.TRACKS : Content.USERS,
                        false
                ));
                break;
        }

        Log.d(ApiSyncService.LOG_TAG, "Added " + added + " new items for this endpoint");
        ContentValues[] cv = new ContentValues[remote.size()];
        i = 0;
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, startPosition + i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }
        mResolver.bulkInsert(c.uri, cv);
        return result;
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
            InputStream is = validateResponse(
               app.get(
                Request.to(content.remoteUri)
                    .add("linked_partitioning", "1")
                    .add("limit", API_LOOKUP_BATCH_SIZE)
                    .add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            ScModel.getCollectionFromStream(is,
                    app.getMapper(),
                    content.resourceType,
                    items
            );
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

    public static class Result {
        public Uri uri;
        public boolean wasChanged;
        public SyncResult syncResult;
        public boolean success;

        public long synced_at;
        public int new_size;
        public String extra;

        public Result(Uri uri) {
            this.uri = uri;
            syncResult = new SyncResult();
        }

        public void setSyncData(long synced_at, int new_size, String extra){
            this.synced_at = synced_at;
            this.new_size = new_size;
            this.extra = extra;
        }

        public static Result fromAuthException(Uri uri) {
            Result r = new Result(uri);
            r.syncResult.stats.numAuthExceptions++;
            return r;
        }

        public static Result fromIOException(Uri uri) {
            Result r = new Result(uri);
            r.syncResult.stats.numIoExceptions++;
            return r;
        }
    }
}
