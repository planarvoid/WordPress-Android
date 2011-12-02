package com.soundcloud.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;

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
        String FAVORITES = ApiService.class.getName() + ".sync_favorites";
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

    private void sendError(ResultReceiver receiver, SyncResult syncResult){
        if (receiver == null) return;
        final Bundle bundle = new Bundle();
        if (syncResult != null) bundle.putParcelable(EXTRA_SYNC_RESULT, syncResult);
        receiver.send(STATUS_ERROR, bundle);

    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }
}
