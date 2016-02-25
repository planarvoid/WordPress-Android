package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Log;
import rx.functions.Action1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

public class OfflineContentScheduler {

    @VisibleForTesting
    static final int RETRY_REQUEST_ID = R.id.offline_retry_request_id;
    static final int CLEANUP_REQUEST_ID = R.id.offline_cleanup_request_id;
    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;

    private final Context context;
    private final AlarmManager alarmManager;
    private final ResumeDownloadOnConnectedReceiver resumeOnConnectedReceiver;
    private final DownloadConnexionHelper downloadConnexionHelper;
    private final CurrentDateProvider dateProvider;

    @Inject
    public OfflineContentScheduler(Context context,
                                   AlarmManager alarmManager,
                                   ResumeDownloadOnConnectedReceiver resumeOnConnectedReceiver,
                                   DownloadConnexionHelper downloadConnexionHelper,
                                   CurrentDateProvider dateProvider) {
        this.context = context;
        this.alarmManager = alarmManager;
        this.resumeOnConnectedReceiver = resumeOnConnectedReceiver;
        this.downloadConnexionHelper = downloadConnexionHelper;
        this.dateProvider = dateProvider;
    }

    public void cancelPendingRetries() {
        alarmManager.cancel(getPendingIntent(context, RETRY_REQUEST_ID));
        resumeOnConnectedReceiver.unregister();
    }

    public void scheduleRetry() {
        if (!downloadConnexionHelper.isNetworkDownloadFriendly()) {
            resumeOnConnectedReceiver.register();
        }
        scheduleStartAt(dateProvider.getCurrentTime() + OfflineConstants.RETRY_DELAY, RETRY_REQUEST_ID);
    }

    public Action1<Object> scheduleCleanupAction() {
        return new Action1<Object>() {
            @Override
            public void call(Object ignored) {
                scheduleStartAt(dateProvider.getCurrentTime() + OfflineConstants.PENDING_REMOVAL_DELAY, CLEANUP_REQUEST_ID);
            }
        };
    }

    private void scheduleStartAt(long atTimeInMillis, int id) {
        Log.d(OfflineContentService.TAG, "Scheduling start of offline content service at " + atTimeInMillis);
        alarmManager.set(ALARM_TYPE, atTimeInMillis, getPendingIntent(context, id));
    }

    private PendingIntent getPendingIntent(Context context, int id) {
        Intent intent = new Intent(context, AlarmManagerReceiver.class);
        intent.setAction(OfflineContentService.ACTION_START);
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
