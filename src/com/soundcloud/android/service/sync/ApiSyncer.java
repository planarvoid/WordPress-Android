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
    private HashSet<Long> trackAdditions = new HashSet<Long>();
    private HashSet<Long> userAdditions = new HashSet<Long>();

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
        bulkAdd(trackAdditions, Track.class);
        bulkAdd(userAdditions, User.class);
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: parcelables added in " + (System.currentTimeMillis() - addStart) + " ms");

        // do collection inserts
        final long itemStart = System.currentTimeMillis();
        for (Map.Entry<Uri, ContentValues[]> entry : collectionValues.entrySet()) {
            int added = mResolver.bulkInsert(entry.getKey(), entry.getValue());
            LocalCollection.insertLocalCollection(mResolver, null, System.currentTimeMillis(), added);
        }
        Log.d(ApiSyncService.LOG_TAG, "Cloud Api service: items added in " + (System.currentTimeMillis() - addStart) + " ms");
    }

    private ContentValues[] quickSync(Uri contentUri, String endpoint, Class<?> loadModel, Set<Long> additions) throws IOException {

        final long start = System.currentTimeMillis();
        int size = 0;

        try {
            List<Long> local = idCursorToList(mResolver.query(contentUri, new String[]{DBHelper.Users._ID}, null, null, null));
            List<Long> remote = mApp.getMapper().readValue(mApp.get(Request.to(endpoint + "/ids")).getEntity().getContent(), List.class);

            // deletions can happen here, has no impact
            Set<Long> itemDeletions = new HashSet(local);
            itemDeletions.removeAll(remote);
            mResolver.delete(contentUri, CloudUtils.getWhereIds(itemDeletions), CloudUtils.longArrToStringArr(itemDeletions));

            // tracks/users that this collection depends on
            // store these to add shortly, they will depend on the tracks/users being there
            Set<Long> newAdditions = new HashSet(remote);
            newAdditions.removeAll(local);
            additions.addAll(newAdditions);

            // the new collection relationships, send these back, as they may depend on track/user additions
            ContentValues[] cv = new ContentValues[remote.size()];
            int i = 0;
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

    private void bulkAdd(Set<Long> additions, Class<?> loadModel) throws IOException {

        if (additions.size() == 0) return;

        CollectionHolder holder = null;
        List<Parcelable> items = new ArrayList<Parcelable>();

        // todo, add function to do select where id in (,,,) instead of 1 by 1
        if (Track.class.equals(loadModel)) {
            for (Long addition : additions) {
                if (SoundCloudDB.isTrackInDb(mResolver, addition)) additions.remove(addition);
            }
        } else if (User.class.equals(loadModel)) {
            for (Long addition : additions) {
                if (SoundCloudDB.isUserInDb(mResolver, addition)) additions.remove(addition);
            }
        }

        InputStream is = mApp.get(Request.to(Track.class.equals(loadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                .add("ids", TextUtils.join(",", additions))).getEntity().getContent();

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

        SoundCloudDB.bulkInsertParcelables(mApp, items, null, mApp.getCurrentUserId(), 0);
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

    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}
