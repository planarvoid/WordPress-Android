package com.soundcloud.android.service.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activity.Activities;
import com.soundcloud.android.model.Activity.Activity;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.Request;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Performs the actual sync with the API. Used by {@link CollectionSyncRequest}.
 */
public class ApiSyncer {
    public static final String TAG = ApiSyncService.LOG_TAG;

    public static final int MAX_LOOKUP_COUNT = 100; // each time we sync, lookup a maximum of this number of items

    private final AndroidCloudAPI mApi;
    private final ContentResolver mResolver;
    private final Context mContext;

    public ApiSyncer(Context context) {
        mApi = (AndroidCloudAPI) context.getApplicationContext();
        mResolver = context.getContentResolver();
        mContext = context;
    }

    public Result syncContent(Uri uri, String action) throws IOException {
        Content c = Content.match(uri);
        Result result = null;
        if (c.remoteUri != null) {
            switch (c) {
                case ME:
                    result = syncMe(c);
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putLong(Consts.PrefKeys.LAST_USER_SYNC, System.currentTimeMillis())
                            .commit();

                    break;
                case ME_ALL_ACTIVITIES:
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                case ME_SOUND_STREAM:
                    result = syncActivities(uri, action);
                    result.success = true;
                    break;

                case ME_TRACKS:
                case ME_FAVORITES:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    result = syncContent(c, SoundCloudApplication.getUserIdFromContext(mContext));
                    result.success = true;
                    break;
            }
        } else {
            switch (c) {
                case TRACK_CLEANUP:
                case USERS_CLEANUP:
                    result = new Result(c.uri);
                    if (mResolver.update(c.uri, null, null, null) > 0) {
                        result.change = Result.CHANGED;
                    }
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putLong(Consts.PrefKeys.LAST_SYNC_CLEANUP, System.currentTimeMillis())
                            .commit();
                    break;
                default:
                    Log.w(TAG, "no remote URI defined for " + c);
            }

        }
        return result;
    }

    private Result syncActivities(Uri uri, String action) throws IOException {
        Result result = new Result(uri);
        log("syncActivities(" + uri + ")");

        final Content c = Content.match(uri);
        final int inserted;
        Activities activities;
        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            final Activity lastActivity = Activities.getLastActivity(c, mResolver);
            Request request = new Request(c.request()).add("limit", Consts.COLLECTION_PAGE_SIZE);
            if (lastActivity != null) request.add("cursor", lastActivity.toGUID());
            activities = Activities.fetch(mApi, request);
            if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(lastActivity))) {
                // this can happen at the end of the list
                inserted = 0;
            } else {
                inserted = activities.insert(c, mResolver);
            }
        } else {
            String future_href = LocalCollection.getExtraFromUri(uri, mResolver);
            Request request = future_href == null ? c.request() : Request.to(future_href);
            activities = Activities.fetchRecent(mApi, request, MAX_LOOKUP_COUNT);

            if (activities.hasMore()) {
                // delete all activities to avoid gaps in the data
                mResolver.delete(c.uri, null, null);
            }

            if (activities == null || activities.isEmpty() ||
                    (activities.size() == 1 && activities.get(0).equals(Activities.getFirstActivity(c, mResolver)))) {
                // this can happen at the beginning of the list if the api returns the first item incorrectly
                inserted = 0;
            } else {
                inserted = activities.insert(c, mResolver);
            }
            result.setSyncData(System.currentTimeMillis(), activities.size(), activities.future_href);
        }

        result.change = inserted > 0 ? Result.CHANGED : Result.UNCHANGED;
        log("activities: inserted " + inserted + " objects");
        return result;
    }

    private Result syncContent(Content content, final long userId) throws IOException {
        Result result = new Result(content.uri);
        List<Long> local = SoundCloudApplication.MODEL_MANAGER.getLocalIds(content, userId);
        List<Long> remote = getCollectionIds(mApi, content.remoteUri);

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size(), null);

        if (checkUnchanged(content, result, local, remote)) return result;
        handleDeletions(content, local, remote);

        int startPosition = 1;
        int added;
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:

                // load the first page of items to getSince proper last_seen ordering
                InputStream is = ScModelManager.validateResponse(mApi.get(Request.to(content.remoteUri)
                        .add("linked_partitioning", "1")
                        .add("limit", Consts.COLLECTION_PAGE_SIZE)))
                        .getEntity().getContent();

                // parse and add first items
                added = SoundCloudApplication.MODEL_MANAGER.writeCollectionFromStream(is, content.uri, userId, ScModel.CacheUpdateMode.FULL);
                break;

            case ME_TRACKS:
                // ensure the first couple of pages of items for quick loading
                added = SoundCloudApplication.MODEL_MANAGER.writeMissingCollectionItems(
                        mApi,
                        remote,
                        Content.TRACKS,
                        false
                );
                break;

            default:
                // ensure the first couple of pages of items for quick loading
                added = SoundCloudApplication.MODEL_MANAGER.writeMissingCollectionItems(
                        mApi,
                        remote,
                        Track.class.equals(content.modelType) ? Content.TRACKS : Content.USERS,
                        false,
                        MAX_LOOKUP_COUNT
                );
                break;
        }

        log("Added " + added + " new items for this endpoint");
        ContentValues[] cv = new ContentValues[remote.size()];
        int i = 0;
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, startPosition + i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }
        mResolver.bulkInsert(content.uri, cv);
        return result;
    }

    private boolean checkUnchanged(Content content, Result result, List<Long> local, List<Long> remote) {
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                Set<Long> localSet = new HashSet<Long>(local);
                Set<Long> remoteSet = new HashSet<Long>(remote);
                if (!localSet.equals(remoteSet)) {
                    result.change = Result.CHANGED;
                    result.extra = "0"; // reset sync misses
                } else {
                    result.change = local.equals(remote) ? Result.UNCHANGED : Result.REORDERED;
                }
                break;
            default:
                if (!local.equals(remote)) {
                    // items have been added or removed (not just ordering) so this is a sync hit
                    result.change = Result.CHANGED;
                    result.extra = "0"; // reset sync misses
                } else {
                    result.change = Result.UNCHANGED;
                    log("Cloud Api service: no change in URI " + content.uri + ". Skipping sync.");
                }
        }
        return result.change == Result.UNCHANGED;
    }

    private List<Long> handleDeletions(Content content, List<Long> local, List<Long> remote) {
        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);
        if (!itemDeletions.isEmpty()) {
            log("Need to remove " + itemDeletions.size() + " items");
            int i = 0;
            while (i < itemDeletions.size()) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + ScModelManager.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(content.uri, DBHelper.getWhereInClause(DBHelper.CollectionItems.ITEM_ID, batch), ScModelManager.longListToStringArr(batch));
                i += ScModelManager.RESOLVER_BATCH_SIZE;
            }
        }
        return itemDeletions;
    }

    private Result syncMe(Content c) throws IOException {
        Result result = new Result(c.uri);
        User user = new FetchUserTask(mApi, SoundCloudApplication.getUserIdFromContext(mContext))
                .doInBackground(c.request());

        result.change = Result.CHANGED;
        result.success = user != null;
        return result;
    }

    private static List<Long> getCollectionIds(AndroidCloudAPI app, String endpoint) throws IOException {
        List<Long> items = new ArrayList<Long>();
        IdHolder holder = null;
        if (endpoint.contains("?")) endpoint = endpoint.substring(0,endpoint.indexOf("?"));
        do {
            Request request =  (holder == null) ? Request.to(endpoint + "/ids") : Request.to(holder.next_href);
            request.add("linked_partitioning", "1");

            holder = app.getMapper().readValue(ScModelManager.validateResponse(app.get(request)).getEntity().getContent(), IdHolder.class);
            if (holder.collection != null) items.addAll(holder.collection);

        } while (holder.next_href != null);
        return items;
    }


    public static class IdHolder {
        @JsonProperty
        public List<Long> collection;

        @JsonProperty
        public String next_href;

        public IdHolder() {
        }

        public boolean hasMore() {
            return !TextUtils.isEmpty(next_href);
        }

        public Request getNextRequest() {
            if (!hasMore()) {
                throw new IllegalStateException("next_href is null");
            } else {
                return new Request(URI.create(next_href));
            }
        }

        public String getCursor() {
            if (next_href != null) {
                List<NameValuePair> params = URLEncodedUtils.parse(URI.create(next_href), "UTF-8");
                for (NameValuePair param : params) {
                    if (param.getName().equalsIgnoreCase("cursor")) {
                        return param.getValue();
                    }
                }
            }
            return null;
        }
    }

    public static class Result {
        public static final int UNCHANGED = 0;
        public static final int REORDERED = 1;
        public static final int CHANGED   = 2;

        public final Uri uri;
        public final SyncResult syncResult = new SyncResult();

        /** One of {@link #UNCHANGED}, {@link #REORDERED}, {@link #CHANGED}. */
        public int change;

        public boolean success;

        public long synced_at;
        public int new_size;
        public String extra;

        public Result(Uri uri) {
            this.uri = uri;
        }

        public void setSyncData(long synced_at, int new_size, @Nullable String extra){
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

        @Override
        public String toString() {
            return "Result{" +
                    "uri=" + uri +
                    ", syncResult=" + syncResult +
                    ", change=" + change +
                    ", success=" + success +
                    ", synced_at=" + synced_at +
                    ", new_size=" + new_size +
                    ", extra='" + extra + '\'' +
                    '}';
        }
    }

    private static void log(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }

}
