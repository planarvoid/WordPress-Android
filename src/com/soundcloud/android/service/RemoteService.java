package com.soundcloud.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;

public class RemoteService extends IntentService{

    static final String LOG_TAG = RemoteService.class.getSimpleName();

    public interface Actions {
        String SYNC_FAVORITES = RemoteService.class.getName() + ".sync_favorites";
    }

    public RemoteService() {
        super("RemoteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Cloud Remote service started");

        Intent broadCastIntent = new Intent();

		String action = intent.getStringExtra("action");
        if (action == null) return;


		if (action.equals(Actions.SYNC_FAVORITES)) {
			syncFavorites();
		}

		broadCastIntent.setAction(action);
		this.sendBroadcast(broadCastIntent);
    }

    private void syncFavorites(){
        // go to lightweight endpoint, get state of favorites
        // check against local state
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }
}
