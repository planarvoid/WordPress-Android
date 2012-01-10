package com.soundcloud.android.service.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ApiSyncService extends IntentService{

    public static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_CHECK_PERFORM_LOOKUPS = "com.soundcloud.android.sync.extra.PERFORM_LOOKUPS";

    public static final String SYNC_ACTION = "com.soundcloud.android.sync.action.SYNC";
    public static final String REFRESH_ACTION = "com.soundcloud.android.sync.action.REFRESH";
    public static final String APPEND_ACTION = "com.soundcloud.android.sync.action.APPEND";


    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_SYNC_ERROR = 0x2;
    public static final int STATUS_SYNC_FINISHED = 0x3;
    public static final int STATUS_REFRESH_ERROR = 0x4;
    public static final int STATUS_REFRESH_FINISHED = 0x5;
    public static final int STATUS_APPEND_ERROR = 0x6;
    public static final int STATUS_APPEND_FINISHED = 0x7;

    public ApiSyncService() {
        super("ApiSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Cloud Api service started");
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        final SyncResult syncResult = new SyncResult();
        final String action = intent.getAction();
        final Bundle resultData = new Bundle();

        if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        ApiSyncer apiSyncer = new ApiSyncer((SoundCloudApplication) getApplication());
        if (action == null || action.equals(SYNC_ACTION)) {
            try {

                final long startSync = System.currentTimeMillis();

                ArrayList<String> contents = intent.getStringArrayListExtra("syncUris");
                boolean manualRefresh = intent.getBooleanExtra("manualRefresh", false);
                if (contents == null) {
                    contents = new ArrayList<String>();
                }
                if (intent.getData() != null) {
                    contents.add(intent.getData().toString());
                }
                for (String c : contents) {
                    resultData.putBoolean(c, apiSyncer.syncContent(Content.byUri(Uri.parse(c)), manualRefresh));
                }
                apiSyncer.performDbAdditions(intent.getBooleanExtra(EXTRA_CHECK_PERFORM_LOOKUPS,true));
                Log.d(LOG_TAG, "Cloud Api service: Done sync in " + (System.currentTimeMillis() - startSync) + " ms");
                if (receiver != null) receiver.send(STATUS_SYNC_FINISHED, resultData);

            } catch (CloudAPI.InvalidTokenException e) {
                Log.e(LOG_TAG, "Cloud Api service: Problem while syncing", e);
                if (syncResult != null) syncResult.stats.numAuthExceptions++;
                sendSyncError(receiver, syncResult);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cloud Api service: Problem while syncing", e);
                if (syncResult != null) syncResult.stats.numIoExceptions++;
                sendSyncError(receiver, syncResult);
            } catch (Exception e) {
                sendSyncError(receiver, syncResult);
            }

        } else if (action.equals(REFRESH_ACTION)) {

            try {
                int totalItems = apiSyncer.refreshCollectionIds(intent.getData());
                Bundle b = new Bundle();
                b.putInt("itemCount", totalItems);
                if (receiver != null) receiver.send(STATUS_REFRESH_FINISHED, b);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cloud Api service: Problem while refreshing", e);
                if (receiver != null) receiver.send(STATUS_REFRESH_ERROR, null);
            }

        } else if (action.equals(APPEND_ACTION)) {
            try {
                int totalItems = apiSyncer.loadContent(intent.getData());
                Bundle b = new Bundle();
                b.putInt("itemCount",totalItems);
                if (receiver != null) receiver.send(STATUS_APPEND_FINISHED, b);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cloud Api service: Problem while appending", e);
                if (receiver != null) receiver.send(STATUS_APPEND_ERROR, null);
            }


        }
    }

     private int slowSyncCollection(Uri contentUri, String endpoint, Class<?> loadModel) throws IOException {
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
                holder = getApp().getMapper().readValue(is, ApiSyncer.TracklistItemHolder.class);
                for (TracklistItem t : (ApiSyncer.TracklistItemHolder) holder) {
                    items.add(new Track(t));
                }
            } else if (User.class.equals(loadModel)) {
                holder = getApp().getMapper().readValue(is, ApiSyncer.UserlistItemHolder.class);
                for (UserlistItem u : (ApiSyncer.UserlistItemHolder) holder) {
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

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }



    private void sendSyncError(ResultReceiver receiver, SyncResult syncResult){
        if (receiver == null) return;
        final Bundle bundle = new Bundle();
        if (syncResult != null) bundle.putParcelable(EXTRA_SYNC_RESULT, syncResult);
        receiver.send(STATUS_SYNC_ERROR, bundle);
    }
}
