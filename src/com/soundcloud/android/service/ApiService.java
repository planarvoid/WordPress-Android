package com.soundcloud.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ApiService extends IntentService{

    static final String LOG_TAG = ApiService.class.getSimpleName();

    public static final String EXTRA_STATUS_RECEIVER =
            "com.soundcloud.android.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT =
            "com.soundcloud.android.extra.SYNC_RESULT";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_FINISHED = 0x3;


    public interface SyncExtras {
        String INCOMING = ApiService.class.getName() + ".sync_incoming";
        String EXCLUSIVE = ApiService.class.getName() + ".sync_exclusive";
        String ACTIVITY = ApiService.class.getName() + ".sync_activities";
        String TRACKS = ApiService.class.getName() + ".sync_tracks";
        String FAVORITES = ApiService.class.getName() + ".sync_favorites";
        String FOLLOWINGS = ApiService.class.getName() + ".sync_followings";
        String FOLLOWERS = ApiService.class.getName() + ".sync_followers";
        String TEST = ApiService.class.getName() + ".sync_test";
    }

    public ApiService() {
        super("ApiService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Cloud Api service started");
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        final SyncResult syncResult = new SyncResult();

        if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);
        final long startSync = System.currentTimeMillis();
        try {
            long start;
            HashSet<Long> trackAdditions = new HashSet<Long>();
            HashSet<Long> userAdditions = new HashSet<Long>();
            HashMap<Uri, ContentValues[]> itemValues = new HashMap<Uri, ContentValues[]>();


            if (intent.getBooleanExtra(SyncExtras.INCOMING, false)) {
                syncActivities(Request.to(Endpoints.MY_ACTIVITIES), ScContentProvider.Content.ME_SOUND_STREAM);

            }
            if (intent.getBooleanExtra(SyncExtras.EXCLUSIVE, false)) {
                syncActivities(Request.to(Endpoints.MY_EXCLUSIVE_TRACKS), ScContentProvider.Content.ME_EXCLUSIVE_STREAM);
            }
            if (intent.getBooleanExtra(SyncExtras.ACTIVITY, false)) {
                syncActivities(Request.to(Endpoints.MY_NEWS), ScContentProvider.Content.ME_ACTIVITIES);
            }

            if (intent.getBooleanExtra(SyncExtras.TRACKS, false)) {
                syncCollection(ScContentProvider.Content.ME_TRACKS, Endpoints.MY_TRACKS, Track.class);
            }
            if (intent.getBooleanExtra(SyncExtras.FAVORITES, false)) {
                syncCollection(ScContentProvider.Content.ME_FAVORITES,Endpoints.MY_FAVORITES, Track.class);
            }
            if (intent.getBooleanExtra(SyncExtras.FOLLOWINGS, false)) {
                syncCollection(ScContentProvider.Content.ME_FOLLOWINGS, Endpoints.MY_FOLLOWINGS, User.class);
            }
            if (intent.getBooleanExtra(SyncExtras.FOLLOWERS, false)) {
                syncCollection(ScContentProvider.Content.ME_FOLLOWERS,Endpoints.MY_FOLLOWERS, User.class);
            }


            // our new tracks/users, compiled so we didn't do duplicate lookups
            final long addStart = System.currentTimeMillis();
            bulkAdd(trackAdditions, Track.class);
            bulkAdd(userAdditions, User.class);
            Log.d(LOG_TAG, "Cloud Api service: parcelables added in " + (System.currentTimeMillis() - addStart) + " ms");


            // do collection inserts
            final long itemStart = System.currentTimeMillis();
            for (Map.Entry<Uri, ContentValues[]> entry : itemValues.entrySet()){
                int added = getContentResolver().bulkInsert(entry.getKey(), entry.getValue());
                LocalCollection.insertLocalCollection(getContentResolver(),null,System.currentTimeMillis(),added);
            }
            Log.d(LOG_TAG, "Cloud Api service: items added in " + (System.currentTimeMillis() - addStart) + " ms");



            Log.d(LOG_TAG, "Cloud Api service: Done sync in " + (System.currentTimeMillis() - startSync) + " ms");
            if (receiver != null) receiver.send(STATUS_FINISHED, Bundle.EMPTY);

        } catch (CloudAPI.InvalidTokenException e) {
            Log.e(LOG_TAG, "Cloud Api service: Problem while syncing", e);
            if (syncResult != null) syncResult.stats.numAuthExceptions++;
            sendError(receiver, syncResult);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cloud Api service: Problem while syncing", e);
            if (syncResult != null) syncResult.stats.numIoExceptions++;
            sendError(receiver, syncResult);
        } catch (Exception e) {
            sendError(receiver, syncResult);
        }
    }

    private void syncActivities(Request request, Uri contentUri) throws IOException {
        final long start = System.currentTimeMillis();
        Activities a = ActivitiesCache.get(getApp(), getApp().getAccount(), request);
        LocalCollection.insertLocalCollection(getContentResolver(), contentUri, System.currentTimeMillis(), a.size());
        Log.d(LOG_TAG, "Cloud Api service: " + request.toUrl() + " synced in " + (System.currentTimeMillis() - start) + " ms");
    }

    private int syncCollection(Uri contentUri, String endpoint, Class<?> loadModel) throws IOException {
        final long start = System.currentTimeMillis();

        int i = 0;
        int page_size = 50;
        CollectionHolder holder = null;
        List<Parcelable> items = new ArrayList<Parcelable>();
        do {
            Request request = Request.to(endpoint);
            request.add("offset",i * 50);
            request.add("limit", page_size);
            request.add("linked_partitioning", "1");
            InputStream is = getApp().get(request).getEntity().getContent();
            if (Track.class.equals(loadModel)) {
                holder = getApp().getMapper().readValue(is, TracklistItemHolder.class);
                for (TracklistItem t : (TracklistItemHolder) holder) {
                    items.add(new Track(t));
                }
            } else if (User.class.equals(loadModel)) {
                holder = getApp().getMapper().readValue(is, UserlistItemHolder.class);
                for (UserlistItem u : (UserlistItemHolder) holder) {
                    items.add(new User(u));
                }
            }
            i++;
        } while (!TextUtils.isEmpty(holder.next_href));

        getContentResolver().delete(contentUri, null, null);
        SoundCloudDB.bulkInsertParcelables(getApp(), items, contentUri, getApp().getCurrentUserId(), 0);
        LocalCollection.insertLocalCollection(getContentResolver(),contentUri,System.currentTimeMillis(),items.size());

        Log.d(LOG_TAG, "Cloud Api service: synced " + contentUri + " in " + (System.currentTimeMillis() - start) + " ms");

        return items.size();
    }





    private ContentValues[] quickSync(Uri contentUri, String endpoint, Class<?> loadModel, Set<Long> additions) throws IOException {

        final long start = System.currentTimeMillis();
        int size = 0;

        try {
            List<Long> local = idCursorToList(getContentResolver().query(contentUri, new String[]{DBHelper.Users._ID}, null, null, null));
            List<Long> remote = getApp().getMapper().readValue(getApp().get(Request.to(endpoint + "/ids")).getEntity().getContent(), List.class);

            // do this here, has no impact
            Set<Long> newDeletions = new HashSet(local);
            newDeletions.removeAll(remote);
            bulkRemove(contentUri,newDeletions);


            // tracks/users that this collection depends on
            // store these to add shortly, they will depend on the tracks/users being there
            Set<Long> newAdditions = new HashSet(remote);
            newAdditions.removeAll(local);
            additions.addAll(newAdditions);

            // the new collection relationships, send these back, as they may depend on track/user additions
            ContentValues[] cv = new ContentValues[remote.size()];
            int i = 0;
            for (Long id : remote){
                cv[i] = new ContentValues();
                cv[i].put(DBHelper.CollectionItems.POSITION, i);
            }
            return cv;

        } catch (IOException e) {
            e.printStackTrace();
        }
        LocalCollection.insertLocalCollection(getContentResolver(),contentUri,System.currentTimeMillis(),size);
        Log.d(LOG_TAG, "Cloud Api service: synced " + contentUri + " in " + (System.currentTimeMillis() - start) + " ms");
        return new ContentValues[0];
    }





    private int bulkRemove(Uri contentUri, Set<Long> deletions) {

        StringBuilder sb = new StringBuilder(DBHelper.CollectionItems.ITEM_ID + " in (?");
        for (int i = 1; i < deletions.size(); i++) {
            sb.append(",?");
        }
        sb.append(")");

        int i = 0;
        String[] idList = new String[deletions.size()];
        for (Long id : deletions) {
            idList[i] = String.valueOf(id);
            i++;
        }
        return getContentResolver().delete(contentUri, sb.toString(), idList);
    }




    private void bulkAdd(Set<Long> additions, Class<?> loadModel) throws IOException {

        if (additions.size() == 0) return;

        CollectionHolder holder = null;
        List<Parcelable> items = new ArrayList<Parcelable>();

        // todo, add function to do select where id in (,,,) instead of 1 by 1
        if (Track.class.equals(loadModel)){
            for (Long addition : additions) {
                if (SoundCloudDB.isTrackInDb(getContentResolver(), addition)) additions.remove(addition);
            }
        } else if (User.class.equals(loadModel)){
            for (Long addition : additions) {
                if (SoundCloudDB.isUserInDb(getContentResolver(), addition)) additions.remove(addition);
            }
        }

        InputStream is = getApp().get(Request.to(Track.class.equals(loadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                .add("ids", TextUtils.join(",", additions))).getEntity().getContent();

        if (Track.class.equals(loadModel)) {
            holder = getApp().getMapper().readValue(is, TracklistItemHolder.class);
            for (TracklistItem t : (TracklistItemHolder) holder) {
                items.add(new Track(t));
            }
        } else if (User.class.equals(loadModel)) {
            holder = getApp().getMapper().readValue(is, UserlistItemHolder.class);
            for (UserlistItem u : (UserlistItemHolder) holder) {
                items.add(new User(u));
            }
        }

        SoundCloudDB.bulkInsertParcelables(getApp(), items, null, getApp().getCurrentUserId(), 0);
    }



    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private List<Long> idCursorToList(Cursor c){
        List<Long> ids = new ArrayList<Long>();
        if (c != null && c.moveToFirst()){
            do {
                ids.add(c.getLong(0));
            } while (c.moveToNext());
        }
        if (c != null) c.close();
        return ids;
    }

    private void sendError(ResultReceiver receiver, SyncResult syncResult){
        if (receiver == null) return;
        final Bundle bundle = new Bundle();
        if (syncResult != null) bundle.putParcelable(EXTRA_SYNC_RESULT, syncResult);
        receiver.send(STATUS_ERROR, bundle);
    }

    public static class CollectionState {
        public List<Long> ids;
    }

    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
}