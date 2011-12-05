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
import com.google.android.imageloader.ContentURLStreamHandlerFactory;
import com.soundcloud.android.Consts;
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
import java.lang.reflect.Array;
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
        String DASHBOARD = ApiService.class.getName() + ".sync_dashboard";
        String INCOMING = ApiService.class.getName() + ".sync_incoming";
        String EXCLUSIVE = ApiService.class.getName() + ".sync_exclusive";
        String ACTIVITY = ApiService.class.getName() + ".sync_activities";
        String TRACKS = ApiService.class.getName() + ".sync_tracks";
        String FAVORITES = ApiService.class.getName() + ".sync_favorites";
        String FOLLOWINGS = ApiService.class.getName() + ".sync_followings";
        String FOLLOWERS = ApiService.class.getName() + ".sync_followers";
    }

    public ApiService() {
        super("ApiService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Cloud Api service started");
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        final SyncResult syncResult = intent.getParcelableExtra(EXTRA_SYNC_RESULT);

        if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);
        final long startSync = System.currentTimeMillis();
        try {
            long start;
            if (intent.getBooleanExtra(SyncExtras.INCOMING, false) || intent.getBooleanExtra(SyncExtras.DASHBOARD, false)) {
                start = System.currentTimeMillis();
                ActivitiesCache.get(getApp(), getApp().getAccount(), Request.to(Endpoints.MY_ACTIVITIES));
                Log.d(LOG_TAG, "Cloud Api service: INCOMING synced in " + (System.currentTimeMillis() - start) + " ms");
            }
            if (intent.getBooleanExtra(SyncExtras.EXCLUSIVE, false) || intent.getBooleanExtra(SyncExtras.DASHBOARD, false)) {
                start = System.currentTimeMillis();
                ActivitiesCache.get(getApp(), getApp().getAccount(), Request.to(Endpoints.MY_EXCLUSIVE_TRACKS));
                Log.d(LOG_TAG, "Cloud Api service: EXCLUSIVE synced in " + (System.currentTimeMillis() - start) + " ms");
            }
            if (intent.getBooleanExtra(SyncExtras.ACTIVITY, false) || intent.getBooleanExtra(SyncExtras.DASHBOARD, false)) {
                start = System.currentTimeMillis();
                ActivitiesCache.get(getApp(), getApp().getAccount(), Request.to(Endpoints.MY_NEWS));
                Log.d(LOG_TAG, "Cloud Api service: ACTIVITIY synced in " + (System.currentTimeMillis() - start) + " ms");
            }

            if (intent.getBooleanExtra(SyncExtras.TRACKS, false)) {
                start = System.currentTimeMillis();
                syncCollection(ScContentProvider.Content.ME_TRACKS, Endpoints.MY_TRACKS, TracklistItemHolder.class);
                Log.d(LOG_TAG, "Cloud Api service: TRACKS synced in " + (System.currentTimeMillis() - start) + " ms");
            }

            if (intent.getBooleanExtra(SyncExtras.FAVORITES, false)) {
                start = System.currentTimeMillis();
                syncCollection(ScContentProvider.Content.ME_FAVORITES,Endpoints.MY_FAVORITES, TracklistItemHolder.class);
                Log.d(LOG_TAG, "Cloud Api service: FAVORITES synced in " + (System.currentTimeMillis() - start) + " ms");
            }

            if (intent.getBooleanExtra(SyncExtras.FOLLOWINGS, false)) {
                start = System.currentTimeMillis();
                syncCollection(ScContentProvider.Content.ME_FOLLOWINGS, Endpoints.MY_FOLLOWINGS, UserlistItemHolder.class);
                Log.d(LOG_TAG, "Cloud Api service: FOLLOWINGS synced in " + (System.currentTimeMillis() - start) + " ms");
            }

            if (intent.getBooleanExtra(SyncExtras.FOLLOWERS, false)) {
                start = System.currentTimeMillis();
                syncCollection(ScContentProvider.Content.ME_FOLLOWERS,Endpoints.MY_FOLLOWERS, UserlistItemHolder.class);
                Log.d(LOG_TAG, "Cloud Api service: FOLLOWERS synced in " + (System.currentTimeMillis() - start) + " ms");
            }



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

    private void syncCollection(Uri contentUri, String endpoint, Class<?> loadModel) throws IOException {


        int i = 0;
        int page_size = 50;
        CollectionHolder holder = null;
        List<Parcelable> items = new ArrayList<Parcelable>();
        do {
            Request request = Request.to(endpoint);
            request.add("offset",i * 50);
            request.add("limit",page_size);
            request.add("linked_partitioning", "1");
            holder = (CollectionHolder) getApp().getMapper().readValue(getApp().get(request).getEntity().getContent(), loadModel);
            items.addAll(holder.collection);
            i++;
        } while (!TextUtils.isEmpty(holder.next_href));

        getContentResolver().delete(contentUri,null,null);
        SoundCloudDB.bulkInsertParcelables(getApp(),items,contentUri,getApp().getCurrentUserId(),0);

        Log.i("asdf","Total Items are " + items.size());
    }

    private void syncFollowings(){
        try {
            List<Long> local = idCursorToList(getContentResolver().query(ScContentProvider.Content.ME_FOLLOWINGS,new String[]{DBHelper.Users._ID},null,null,null));
            List<Long> remote = getApp().getMapper().readValue(getApp().get(Request.to(Endpoints.MY_FOLLOWINGS + "/ids")).getEntity().getContent(), List.class);

            if (local.size() == 0 && remote.size() != 0){
                // paging

            }

            Set<Long> deletions = new HashSet(local);
            deletions.removeAll(remote);

            Set<Long> additions = new HashSet(remote);
            additions.removeAll(local);

        } catch (IOException e) {
            e.printStackTrace();
        }

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
