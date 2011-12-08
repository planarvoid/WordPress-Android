package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
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

    public void syncActivities(Request request, Uri contentUri) throws IOException {
        final long start = System.currentTimeMillis();
        Activities a = ActivitiesCache.get(mApp, mApp.getAccount(), request);
        LocalCollection.insertLocalCollection(mResolver, contentUri, System.currentTimeMillis(), a.size());
    }

    public void syncCollection(Uri contentUri, String endpoint, Class<?> loadModel) throws IOException {
        collectionValues.put(contentUri, quickSync(contentUri, endpoint, loadModel, loadModel == Track.class ? trackAdditions : userAdditions));
    }

    public void resolveDatabase() throws IOException {

        // our new tracks/users, compiled so we didn't do duplicate lookups
        final long addStart = System.currentTimeMillis();

        List<Parcelable> itemsToAdd = new ArrayList<Parcelable>();
        itemsToAdd.addAll(getAdditionsFromIds(trackAdditions, Track.class));
        itemsToAdd.addAll(getAdditionsFromIds(userAdditions, User.class));
        int added = SoundCloudDB.bulkInsertParcelables(mApp, itemsToAdd, null, 0, 0);

        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: " + added + " parcelables added in " + (System.currentTimeMillis() - addStart) + " ms");


        // do collection inserts
        final long itemStart = System.currentTimeMillis();
        for (Map.Entry<Uri, ContentValues[]> entry : collectionValues.entrySet()) {
            added = mResolver.bulkInsert(entry.getKey(), entry.getValue());
            LocalCollection.insertLocalCollection(mResolver, null, System.currentTimeMillis(), added);
        }
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: items added in " + (System.currentTimeMillis() - addStart) + " ms");
    }

    private ContentValues[] quickSync(Uri contentUri, String endpoint, Class<?> loadModel, ArrayList<Long> additions) throws IOException {

        final long start = System.currentTimeMillis();
        int size = 0;

        try {
            List<Long> local = idCursorToList(mResolver.query(contentUri, new String[]{DBHelper.Users._ID}, null, null, null));
            List<Long> remote = getCollectionIds(endpoint);

            // deletions can happen here, has no impact
            List<Long> itemDeletions = new ArrayList<Long>(local);
            itemDeletions.removeAll(remote);

            int i = 0;
            while (i < itemDeletions.size()){
                List<Long> batch = itemDeletions.subList(i,Math.min(i + RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(contentUri, CloudUtils.getWhereIds(batch), CloudUtils.longListToStringArr(batch));
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
            for (Long id : remote) {
                cv[i] = new ContentValues();
                cv[i].put(DBHelper.CollectionItems.POSITION, i);
            }
            return cv;

        } catch (IOException e) {
            e.printStackTrace();
        }
        LocalCollection.insertLocalCollection(mResolver, contentUri, System.currentTimeMillis(), size);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: synced " + contentUri + " in " + (System.currentTimeMillis() - start) + " ms");
        return new ContentValues[0];
    }

    private List<Parcelable> getAdditionsFromIds(List<Long> additions, Class<?> loadModel) throws IOException {

        if (additions.size() == 0) return new ArrayList<Parcelable>();

        // remove anything that is already in the DB
        Uri contentUri = (Track.class.equals(loadModel)) ? ScContentProvider.Content.TRACKS : ScContentProvider.Content.USERS;

        int i = 0;
        List<Long> storedIds = new ArrayList<Long>();
        while (i < additions.size()) {
            List<Long> batch = additions.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, additions.size()));
            storedIds.addAll(idCursorToList(mResolver.query(contentUri, new String[]{DBHelper.Tracks._ID},
                    CloudUtils.getWhereIds(batch),
                    CloudUtils.longListToStringArr(batch), null))
            );
            i += RESOLVER_BATCH_SIZE;
        }
        additions.removeAll(storedIds);

        // add new items from batch lookups
        List<Parcelable> items = new ArrayList<Parcelable>();
        i = 0;
        while (i < additions.size()) {

            List<Long> batch = additions.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, additions.size()));

            InputStream is = mApp.get(Request.to(Track.class.equals(loadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                    .add("ids", TextUtils.join(",", batch))).getEntity().getContent();


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
            i += RESOLVER_BATCH_SIZE;
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
            holder = mApp.getMapper().readValue(mApp.get(request).getEntity().getContent(), IdHolder.class);
            items.addAll(holder.collection);
        } while (holder.next_href != null);
        return items;
    }

    public static class IdHolder extends CollectionHolder<Long> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}
