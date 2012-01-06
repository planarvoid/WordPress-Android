package com.soundcloud.android.service.sync;

import android.preference.PreferenceManager;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class ApiSyncer {

    static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    static final Long WIFI_STALE_TIME = 1000l;//10*60*1000

    private SoundCloudApplication mApp;
    private ContentResolver mResolver;

    private HashMap<Uri, ContentValues[]> collectionValues = new HashMap<Uri, ContentValues[]>();
    private ArrayList<Long> trackAdditions = new ArrayList<Long>();
    private ArrayList<Long> userAdditions = new ArrayList<Long>();

    private static int API_LOOKUP_BATCH_SIZE = 200;
    private static int RESOLVER_BATCH_SIZE = 100;

    public ApiSyncer(SoundCloudApplication app) {
        mApp = app;
        mResolver = app.getContentResolver();
    }

    public void syncContent(Content c, boolean manualRefresh) throws IOException {
        if (c.remoteUri != null) {
            switch (c) {
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                case ME_SOUND_STREAM:
                    syncActivities(Request.to(c.remoteUri), c.uri);
                    break;

                case ME_TRACKS:
                case ME_FAVORITES:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    syncCollection(c.uri, c.remoteUri, getLoadModelFromContent(c));
                    break;
            }
        } else {

            switch (c) {
                case TRACK_CLEANUP:
                case USERS_CLEANUP:
                    mResolver.update(c.uri, null, null, null);
                    PreferenceManager.getDefaultSharedPreferences(mApp).edit().putLong("lastSyncCleanup", System.currentTimeMillis());
                    break;
                default:
                    Log.w(ApiSyncService.LOG_TAG, "no remote URI defined for " + c);
            }

        }
    }

    public void syncActivities(Request request, Uri contentUri) throws IOException {
        final long start = System.currentTimeMillis();
        Activities a = ActivitiesCache.get(mApp, mApp.getAccount(), request);
        LocalCollection.insertLocalCollection(mResolver, contentUri, System.currentTimeMillis(), a.size());
    }

    public void syncCollection(Uri contentUri, String endpoint, Class<?> loadModel) throws IOException {
        collectionValues.put(contentUri, quickSync(contentUri, endpoint, loadModel == Track.class ? trackAdditions : userAdditions));
    }

    private long getStaleTime() {
        return CloudUtils.isWifiConnected(mApp) ? WIFI_STALE_TIME : Consts.SYNC_STALE_TIME;
    }

    public void performDbAdditions() throws IOException {
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: Resolving Database");

        // our new tracks/users, compiled so we didn't do duplicate lookups
        final long addStart = System.currentTimeMillis();

        List<Parcelable> itemsToAdd = new ArrayList<Parcelable>();
        itemsToAdd.addAll(getAdditionsFromIds(trackAdditions, Track.class, false));
        itemsToAdd.addAll(getAdditionsFromIds(userAdditions, User.class, false));
        int added = SoundCloudDB.bulkInsertParcelables(mApp, itemsToAdd, null, 0, 0);

        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: " + added + " parcelables added in " + (System.currentTimeMillis() - addStart) + " ms");

        // do collection inserts
        final long itemStart = System.currentTimeMillis();
        for (Map.Entry<Uri, ContentValues[]> entry : collectionValues.entrySet()) {
            if (entry.getValue().length > 0) {
                Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: Upserting " + entry.getValue().length + " new collection items");
                added = mResolver.bulkInsert(entry.getKey(), entry.getValue());
            } else {
                added = 0;
            }
            LocalCollection.insertLocalCollection(mResolver, entry.getKey(), System.currentTimeMillis(), added);
        }
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: " + added + " items added in " + (System.currentTimeMillis() - itemStart) + " ms");
    }

    public int refreshCollectionIds(Uri uri) throws IOException {

        final long itemStart = System.currentTimeMillis();
        // get remote collection
        List<Long> local = idCursorToList(mResolver.query(uri, new String[]{DBHelper.CollectionItems.ITEM_ID},
                null,null, DBHelper.CollectionItems.POSITION + " ASC"));

        Content c = Content.match(uri);
        List<Long> remote = getCollectionIds(c.remoteUri);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());

        if (local.equals(remote)){
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: no change in URI " + uri + ". Skipping id refresh.");
            return local.size();
        }

        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);
        int i = 0;
        while (i < itemDeletions.size()) {
            List<Long> batch = itemDeletions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, itemDeletions.size()));
            mResolver.delete(uri, CloudUtils.getWhereIds(DBHelper.CollectionItems.ITEM_ID, batch), CloudUtils.longListToStringArr(batch));
            i += RESOLVER_BATCH_SIZE;
        }

        ContentValues[] cv = new ContentValues[remote.size()];
        i = 0;
        final long userId = mApp.getCurrentUserId();
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i + 1);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }

        int added = mResolver.bulkInsert(uri, cv);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: Refreshed collection ids in " + (System.currentTimeMillis() - itemStart) + " ms");
        return added;
    }

    /**
     * @param uri : a URI representing what is being requested, e.g. content://com.soundcloud.android/me/favorites?offset=100&limit=50
     */
    public int loadContent(Uri uri) throws IOException {

        // get local collection

        // get last update time

        //  not exists or stale


        Content c = Content.match(uri);
        List<Long> pageIds = idCursorToList(mResolver.query(uri, new String[]{DBHelper.CollectionItems.ITEM_ID},
                null, null, DBHelper.CollectionItems.POSITION + " ASC"));
        final int itemCount = pageIds.size();
        SoundCloudDB.bulkInsertParcelables(mApp, getAdditionsFromIds(pageIds, c.resourceType, false));
        return itemCount;
    }

    private ContentValues[] quickSync(Uri contentUri, String endpoint,ArrayList<Long> additions) throws IOException {

        final long start = System.currentTimeMillis();
        int size = 0;
        List<Long> local = idCursorToList(mResolver.query(contentUri, new String[]{DBHelper.CollectionItems.ITEM_ID},
                null,null, DBHelper.CollectionItems.POSITION + " ASC"));

        List<Long> remote = getCollectionIds(endpoint);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());

        if (local.equals(remote)){
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: no change in URI " + contentUri + ". Skipping sync.");
            return new ContentValues[0];
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);

        Log.d(ApiSyncService.LOG_TAG, "Need to remove " + itemDeletions.size() + " items");

        int i = 0;
        while (i < itemDeletions.size()) {
            List<Long> batch = itemDeletions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, itemDeletions.size()));
            mResolver.delete(contentUri, CloudUtils.getWhereIds(DBHelper.CollectionItems.ITEM_ID, batch), CloudUtils.longListToStringArr(batch));
            i += RESOLVER_BATCH_SIZE;
        }


        // tracks/users that this collection depends on
        // store these to add shortly, they will depend on the tracks/users being there
        Set<Long> newAdditions = new HashSet(remote);
        newAdditions.removeAll(local);
        additions.addAll(newAdditions);

        // the new collection relationships, send these back, as they may depend on track/user additions
        ContentValues[] cv = new ContentValues[remote.size()];
        i = 0;
        final long userId = mApp.getCurrentUserId();
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i + 1);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }

        return cv;
    }

    private List<Parcelable> getAdditionsFromIds(List<Long> additions, Class<?> loadModel, boolean ignoreStored) throws IOException {

        if (additions.size() == 0) return new ArrayList<Parcelable>();

        if (!ignoreStored) {
            // remove anything that is already in the DB
            Uri contentUri = Content.forModel(loadModel).uri;
            int i = 0;
            List<Long> storedIds = new ArrayList<Long>();
            while (i < additions.size()) {
                List<Long> batch = additions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, additions.size()));
                storedIds.addAll(idCursorToList(mResolver.query(contentUri, new String[]{DBHelper.Tracks._ID},
                        CloudUtils.getWhereIds(DBHelper.Tracks.ID, batch), CloudUtils.longListToStringArr(batch), null)));
                i += RESOLVER_BATCH_SIZE;
            }
            additions.removeAll(storedIds);
        }

        // add new items from batch lookups
        List<Parcelable> items = new ArrayList<Parcelable>();
        int i = 0;
        while (i < additions.size()) {

            List<Long> batch = additions.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, additions.size()));
            InputStream is = validateResponse(mApp.get(Request.to(Track.class.equals(loadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                    .add("linked_partitioning", "1").add("limit", API_LOOKUP_BATCH_SIZE).add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            CollectionHolder holder = null;
            if (Track.class.equals(loadModel)) {
                holder = mApp.getMapper().readValue(is, TracklistItemHolder.class);
                for (TracklistItem t : (TracklistItemHolder) holder) {
                    items.add(new Track(t));
                }
            } else if (User.class.equals(loadModel)) {
                holder = mApp.getMapper().readValue(is, UserlistItemHolder.class);
                for (UserlistItem u : (UserlistItemHolder) holder) {
                    items.add(new User(u));
                }
            }
            i += API_LOOKUP_BATCH_SIZE;
        }
        return items;
    }

    private List<Long> idCursorToList(Cursor c) {
        List<Long> ids = new ArrayList<Long>();
        if (c != null && c.moveToFirst()) {
            do {
                ids.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return ids;
    }

    private List<Long> getCollectionIds(String endpoint) throws IOException {
        List<Long> items = new ArrayList<Long>();
        IdHolder holder = null;
        do {
            Request request =  (holder == null) ? Request.to(endpoint + "/ids") : Request.to(holder.next_href);
            request.add("linked_partitioning", "1");
            holder = mApp.getMapper().readValue(validateResponse(mApp.get(request)).getEntity().getContent(), IdHolder.class);
            if (holder.collection != null) items.addAll(holder.collection);

        } while (holder.next_href != null);
        return items;
    }

    private HttpResponse validateResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK
                && response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_MODIFIED) {
            throw new IOException("Invalid response: " + response.getStatusLine());
        }
        return response;
    }

    public void refreshPage(Content content, int pageIndex) throws IOException {
        Log.d(LOG_TAG, "Refreshing page items " + content.uri);
        final Uri pagedUri = content.uri.buildUpon().appendQueryParameter("offset", String.valueOf(pageIndex * Consts.COLLECTION_PAGE_SIZE))
                    .appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build();

        Cursor c = mResolver.query(pagedUri, new String[]{DBHelper.CollectionItems.ITEM_ID, DBHelper.TrackView.LAST_UPDATED},
                null,null,DBHelper.CollectionItems.POSITION + " ASC");
        List<Long> staleItems = new ArrayList<Long>();
        final long cutoff = System.currentTimeMillis() - getStaleTime();
        if (c != null && c.moveToFirst()) {
            do {
                if (c.getLong(1) < cutoff) staleItems.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();

        int updated = SoundCloudDB.bulkInsertParcelables(mApp,
                getAdditionsFromIds(staleItems,getLoadModelFromContent(content), true), null, 0, 0);

        Log.d(LOG_TAG, "Updated " + updated + " items");
    }

    private Class<?> getLoadModelFromContent(Content c){
        switch (c) {
                case ME_TRACKS:
                case ME_FAVORITES:
                    return Track.class;
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    return User.class;
                default:
                    throw new IllegalArgumentException("Load model not recognized from content");
            }
    }

    public static class IdHolder extends CollectionHolder<Long> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}
