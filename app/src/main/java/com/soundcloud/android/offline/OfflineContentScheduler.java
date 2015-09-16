package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class OfflineContentScheduler {

    @VisibleForTesting
    static final int REQUEST_ID = R.id.offline_syncing_request_id;
    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
    private static final long RETRY_DELAY = TimeUnit.MINUTES.toMillis(10);

    private final Context context;
    private final AlarmManager alarmManager;
    private final ResumeDownloadOnConnectedReceiver resumeOnConnectedReceiver;
    private final DownloadOperations downloadOperations;

    @Inject
    public OfflineContentScheduler(Context context, AlarmManager alarmManager,
                                   ResumeDownloadOnConnectedReceiver resumeOnConnectedReceiver,
                                   DownloadOperations downloadOperations) {
        this.context = context;
        this.alarmManager = alarmManager;
        this.resumeOnConnectedReceiver = resumeOnConnectedReceiver;
        this.downloadOperations = downloadOperations;
    }

    public void cancelPendingRetries() {
        alarmManager.cancel(getPendingIntent(context));
        resumeOnConnectedReceiver.unregister();
    }

    public void scheduleRetry() {
        if (!downloadOperations.isValidNetwork()) {
            resumeOnConnectedReceiver.register();
        }
        scheduleDelayedRetry(System.currentTimeMillis() + RETRY_DELAY);
    }

    @VisibleForTesting
    void scheduleDelayedRetry(long atTimeInMillis) {
        Log.d(OfflineContentService.TAG, "Scheduling retry of offline content service");
        alarmManager.set(ALARM_TYPE, atTimeInMillis, getPendingIntent(context));
    }

    private PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmManagerReceiver.class);
        intent.setAction(OfflineContentService.ACTION_START);
        return PendingIntent.getBroadcast(context, REQUEST_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
