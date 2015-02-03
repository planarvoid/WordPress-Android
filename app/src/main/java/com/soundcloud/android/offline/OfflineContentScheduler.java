package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class OfflineContentScheduler {

    @VisibleForTesting
    static final int REQUEST_ID = R.id.action_syncing; // do these have to be unique over the app??
    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
    private static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(60);

    private final Context context;
    private final AlarmManager alarmManager;

    @Inject
    public OfflineContentScheduler(Context context, AlarmManager alarmManager) {
        this.context = context;
        this.alarmManager = alarmManager;
    }

    public void cancelPendingRetries(){
        alarmManager.cancel(getPendingIntent(context));
    }

    public void scheduleRetry(){
        scheduleRetry(System.currentTimeMillis() + RETRY_DELAY);
    }

    @VisibleForTesting
    void scheduleRetry(long atTimeInMillis){
        Log.d(OfflineContentService.TAG, "Scheduling retry of offline content service");
        alarmManager.set(ALARM_TYPE, atTimeInMillis, getPendingIntent(context));
    }

    private PendingIntent getPendingIntent(Context context) {
        Intent intent =  new Intent(context, OfflineSyncStartReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
