package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.Log;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class OfflineContentService extends IntentService {

    protected static final String ACTION_DOWNLOAD_TRACKS = "action_download_tracks";
    protected static final String TAG = "OfflineContent";

    @Inject DownloadController downloadController;

    public static void syncOfflineContent(Context context) {
        context.startService(getDownloadIntent(context));
    }

    private static Intent getDownloadIntent(Context context) {
        final Intent intent = new Intent(context, OfflineContentService.class);
        intent.setAction(ACTION_DOWNLOAD_TRACKS);
        return intent;
    }

    public OfflineContentService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OfflineContentService(DownloadController downloadController) {
        super(TAG);
        this.downloadController = downloadController;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Starting offlineContentService for action: " + action);

        if (ACTION_DOWNLOAD_TRACKS.equalsIgnoreCase(action)) {
            downloadController.downloadTracks();
        }
    }

}
