package com.soundcloud.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

public class ApiService extends IntentService{

    static final String LOG_TAG = ApiService.class.getSimpleName();

    public static final String EXTRA_STATUS_RECEIVER =
            "com.soundcloud.android.extra.STATUS_RECEIVER";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_FINISHED = 0x3;


    public interface SyncExtras {
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
        Log.d(LOG_TAG, "Cloud Remote service started");
        Log.d("asdf", "Cloud Remote service started");
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);
        try {
            if (intent.getBooleanExtra(SyncExtras.INCOMING,false)){
                Log.i("asdf","SYNC INCOMING");
                ActivitiesCache.get(getApp(), getApp().getAccount(),Request.to(Endpoints.MY_ACTIVITIES));

            }
            if (intent.getBooleanExtra(SyncExtras.EXCLUSIVE,false)){
                Log.i("asdf","SYNC EXCLUSIVE");
                ActivitiesCache.get(getApp(), getApp().getAccount(),Request.to(Endpoints.MY_EXCLUSIVE_TRACKS));
            }
            if (intent.getBooleanExtra(SyncExtras.ACTIVITY,false)){
                Log.i("asdf","SYNC ACTIVITY");
                ActivitiesCache.get(getApp(), getApp().getAccount(),Request.to(Endpoints.MY_NEWS));
            }
            Log.i("asdf","SYNC DONE");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Problem while syncing", e);


            if (receiver != null) {
                // Pass back error to surface listener
                final Bundle bundle = new Bundle();
                bundle.putString(Intent.EXTRA_TEXT, e.toString());
                receiver.send(STATUS_ERROR, bundle);
            }
        }
        Log.i("asdf","Done Sync " + receiver);
        if (receiver != null) receiver.send(STATUS_FINISHED, Bundle.EMPTY);
    }

    private void syncFavorites(){
        // go to lightweight endpoint, get state of favorites
        // check against local state
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }
}
