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

    public static final String EXTRA_STATUS_RECEIVER =
            "com.soundcloud.android.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT =
            "com.soundcloud.android.extra.SYNC_RESULT";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_FINISHED = 0x3;


    public ApiSyncService() {
        super("ApiSyncService");
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
            ApiSyncer apiSyncer = new ApiSyncer((SoundCloudApplication) getApplication());
            ArrayList<String> contents = intent.getStringArrayListExtra("syncUris");
            if (contents == null) {
                contents = new ArrayList<String>();
            }
            if (intent.getData() != null) {
                contents.add(intent.getData().toString());
            }

            for (String c : contents) {
                apiSyncer.syncContent(Content.byUri(Uri.parse(c)));
            }

//            if (intent.getBooleanExtra(SyncExtras.INCOMING, false)) {
//                apiSyncer.syncActivities(Request.to(Endpoints.MY_ACTIVITIES), Content.ME_SOUND_STREAM);
//            }
//            if (intent.getBooleanExtra(SyncExtras.EXCLUSIVE, false)) {
//                apiSyncer.syncActivities(Request.to(Endpoints.MY_EXCLUSIVE_TRACKS), Content.ME_EXCLUSIVE_STREAM);
//            }
//            if (intent.getBooleanExtra(SyncExtras.ACTIVITY, false)) {
//                apiSyncer.syncActivities(Request.to(Endpoints.MY_NEWS), Content.ME_ACTIVITIES);
//            }


//            if (intent.getBooleanExtra(SyncExtras.TRACKS, false)) {
//                //apiSyncer.syncCollection(ScContentProvider.Content.ME_TRACKS, Endpoints.MY_TRACKS, Track.class);
//                slowSyncCollection(Content.ME_TRACKS,Endpoints.MY_TRACKS,Track.class);
//            }
//            if (intent.getBooleanExtra(SyncExtras.FAVORITES, false)) {
//                apiSyncer.syncCollection(Content.ME_FAVORITES, Endpoints.MY_FAVORITES, Track.class);
//                //slowSyncCollection(ScContentProvider.Content.ME_FAVORITES, Endpoints.MY_FAVORITES, Track.class);
//            }
//            if (intent.getBooleanExtra(SyncExtras.FOLLOWINGS, false)) {
//                //apiSyncer.syncCollection(ScContentProvider.Content.ME_FOLLOWINGS, Endpoints.MY_FOLLOWINGS, User.class);
//                slowSyncCollection(Content.ME_FOLLOWINGS, Endpoints.MY_FOLLOWINGS, User.class);
//            }


//            if (intent.getBooleanExtra(SyncExtras.FOLLOWERS, false)) {
//                //apiSyncer.syncCollection(ScContentProvider.Content.ME_FOLLOWERS, Endpoints.MY_FOLLOWERS, User.class);
//                slowSyncCollection(Content.ME_FOLLOWERS, Endpoints.MY_FOLLOWERS, User.class);
//            }

            apiSyncer.resolveDatabase();

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



    private void sendError(ResultReceiver receiver, SyncResult syncResult){
        if (receiver == null) return;
        final Bundle bundle = new Bundle();
        if (syncResult != null) bundle.putParcelable(EXTRA_SYNC_RESULT, syncResult);
        receiver.send(STATUS_ERROR, bundle);
    }
}
