package com.soundcloud.android.service.sync;

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
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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



    public boolean syncContent(Content c, boolean manualRefresh) throws IOException {
        boolean changed = false;
        if (c.remoteUri != null) {
            switch (c) {
                case ME_ACTIVITIES:
                case ME_EXCLUSIVE_STREAM:
                case ME_SOUND_STREAM:
                    changed = syncActivities(Request.to(c.remoteUri), c.uri);
                    break;

                case ME_TRACKS:
                case ME_FAVORITES:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                    changed = syncCollection(c);
                    break;
            }
        } else {

            switch (c) {
                case TRACK_CLEANUP:
                case USERS_CLEANUP:
                    changed = mResolver.update(c.uri, null, null, null) > 0;
                    PreferenceManager.getDefaultSharedPreferences(mApp).edit().putLong("lastSyncCleanup", System.currentTimeMillis());
                    break;
                default:
                    Log.w(ApiSyncService.LOG_TAG, "no remote URI defined for " + c);
            }

        }
        return changed;
    }

    /* package */ boolean syncActivities(Request request, Uri contentUri) throws IOException {
        final long start = System.currentTimeMillis();
        Activities a = ActivitiesCache.get(mApp, mApp.getAccount(), request);
        LocalCollection.insertLocalCollection(mResolver, contentUri, System.currentTimeMillis(), a.size());
        return true; // TODO, make this an actual result (true if something changed). not bothering now cause this is going to be changed
    }

    /* package */ boolean syncCollection(Content c) throws IOException {
        ContentValues[] cv = quickSync(c, c.resourceType == Track.class ? trackAdditions : userAdditions);
        collectionValues.put(c.uri, cv);
        return cv.length > 0;
    }

    private long getStaleTime() {
        return CloudUtils.isWifiConnected(mApp) ? WIFI_STALE_TIME : Consts.SYNC_STALE_TIME;
    }

    /* package */ void performDbAdditions(boolean doLookups) throws IOException {
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: Resolving Database");

        // our new tracks/users, compiled so we didn't do duplicate lookups
        final long addStart = System.currentTimeMillis();

        if (doLookups) {
            List<Parcelable> itemsToAdd = new ArrayList<Parcelable>();
            itemsToAdd.addAll(getAdditionsFromIds(mApp, trackAdditions, Content.TRACKS, false));
            itemsToAdd.addAll(getAdditionsFromIds(mApp, userAdditions, Content.USERS, false));
            int added = SoundCloudDB.bulkInsertParcelables(mApp, itemsToAdd, null, 0, 0);
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: " + added + " parcelables added in " + (System.currentTimeMillis() - addStart) + " ms");
        }

        // do collection inserts
        final long itemStart = System.currentTimeMillis();
        int added = 0;
        for (Map.Entry<Uri, ContentValues[]> entry : collectionValues.entrySet()) {
            if (entry.getValue().length > 0) {
                Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: Upserting to " + entry.getKey() + " " + entry.getValue().length + " new collection items");
                added += mResolver.bulkInsert(entry.getKey(), entry.getValue());
            }
            LocalCollection.insertLocalCollection(mResolver, entry.getKey(), System.currentTimeMillis(), added);
        }
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: " + added + " items added in " + (System.currentTimeMillis() - itemStart) + " ms");
    }

    public int refreshCollectionIds(Uri uri) throws IOException {

        final long itemStart = System.currentTimeMillis();
        // get remote collection
        List<Long> local = idCursorToList(mResolver.query(uri,
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                null, null,
                DBHelper.CollectionItems.SORT_ORDER));

        Content c = Content.match(uri);
        List<Long> remote = getCollectionIds(mApp, c.remoteUri);
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
        List<Long> pageIds = idCursorToList(mResolver.query(uri,
                new String[] { DBHelper.CollectionItems.ITEM_ID },
                null, null,
                DBHelper.CollectionItems.SORT_ORDER));

        final int itemCount = pageIds.size();
        SoundCloudDB.bulkInsertParcelables(mApp, getAdditionsFromIds(mApp, pageIds, c, false));
        return itemCount;
    }

    private ContentValues[] quickSync(Content c, ArrayList<Long> additions) throws IOException {



        final long start = System.currentTimeMillis();
        int size = 0;
        List<Long> local = idCursorToList(mResolver.query(
                Content.COLLECTION_ITEMS.uri,
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ?",
                new String[]{String.valueOf(c.collectionType)},
                DBHelper.CollectionItems.SORT_ORDER));

        List<Long> remote = getCollectionIds(mApp, c.remoteUri);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());


        if (local.equals(remote)){
            Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: no change in URI " + c.uri + ". Skipping sync.");
            return new ContentValues[0];
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

    public static List<Parcelable> getAdditionsFromIds(SoundCloudApplication app,
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
                storedIds.addAll(idCursorToList(app.getContentResolver().query(content.uri, new String[]{DBHelper.Tracks._ID},
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
            InputStream is = validateResponse(app.get(Request.to(content.remoteUri)
                    .add("linked_partitioning", "1").add("limit", API_LOOKUP_BATCH_SIZE).add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            CollectionHolder holder = null;
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

    public static  List<Long> getCollectionIds(SoundCloudApplication app, String endpoint) throws IOException {
        List<Long> items = new ArrayList<Long>();
        IdHolder holder = null;
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

    public void refreshPage(Content content, int pageIndex) throws IOException {
        Log.d(LOG_TAG, "Refreshing page items " + content.uri);
        final Uri pagedUri = content.uri.buildUpon().appendQueryParameter("offset", String.valueOf(pageIndex * Consts.COLLECTION_PAGE_SIZE))
                    .appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build();

        Cursor c = mResolver.query(pagedUri,
                new String[] {DBHelper.CollectionItems.ITEM_ID, DBHelper.TrackView.LAST_UPDATED },
                null, null,
                DBHelper.CollectionItems.SORT_ORDER);

        List<Long> staleItems = new ArrayList<Long>();
        final long cutoff = System.currentTimeMillis() - getStaleTime();
        if (c != null && c.moveToFirst()) {
            do {
                if (c.getLong(1) < cutoff) staleItems.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();

        int updated = SoundCloudDB.bulkInsertParcelables(mApp,
                getAdditionsFromIds(mApp, staleItems, content, true), null, 0, 0);

        Log.d(LOG_TAG, "Updated " + updated + " items");
    }


    public static class IdHolder extends CollectionHolder<Long> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}
