package com.soundcloud.android.creators.record;

import static com.soundcloud.android.NotificationConstants.PLAYBACK_NOTIFY_ID;
import static com.soundcloud.android.NotificationConstants.RECORD_NOTIFY_ID;
import static com.soundcloud.android.creators.record.SoundRecorder.EXTRA_ELAPSEDTIME;
import static com.soundcloud.android.creators.record.SoundRecorder.EXTRA_SHOULD_NOTIFY;
import static com.soundcloud.android.creators.record.SoundRecorder.NOTIFICATION_STATE;
import static com.soundcloud.android.creators.record.SoundRecorder.PLAYBACK_COMPLETE;
import static com.soundcloud.android.creators.record.SoundRecorder.PLAYBACK_ERROR;
import static com.soundcloud.android.creators.record.SoundRecorder.PLAYBACK_STARTED;
import static com.soundcloud.android.creators.record.SoundRecorder.PLAYBACK_STOPPED;
import static com.soundcloud.android.creators.record.SoundRecorder.RECORD_ERROR;
import static com.soundcloud.android.creators.record.SoundRecorder.RECORD_FINISHED;
import static com.soundcloud.android.creators.record.SoundRecorder.RECORD_PROGRESS;
import static com.soundcloud.android.creators.record.SoundRecorder.RECORD_STARTED;
import static com.soundcloud.android.creators.record.SoundRecorder.getIntentFilter;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;

/**
 * In charge of lifecycle and notifications for the {@link SoundRecorder}
 */
public class SoundRecorderService extends Service {

    private static final String TAG = SoundRecorderService.class.getSimpleName();
    private static final int IDLE_DELAY = 30 * 1000;  // interval after which we stop the service when idle

    private final IBinder binder = new LocalBinder<SoundRecorderService>() {
        @Override
        public SoundRecorderService getService() {
            return SoundRecorderService.this;
        }
    };

    private SoundRecorder recorder;
    private WakeLock wakeLock;
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);

    // notifications
    private PendingIntent recordPendingIntent;
    private NotificationManager notificationManager;
    private LocalBroadcastManager broadcastManager;
    private long lastNotifiedTime;
    private int startId;

    private static final class DelayedStopHandler extends Handler {

        private final WeakReference<SoundRecorderService> service;

        DelayedStopHandler(SoundRecorderService soundRecorderService) {
            service = new WeakReference<>(soundRecorderService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            SoundRecorderService service = this.service.get();
            if (service != null && !service.recorder.isActive()) {
                Log.d(TAG, "DelayedStopHandler: stopping service");
                service.stopSelf(service.startId);
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "BroadcastReceiver#onReceive(" + action + ")");

            if (PLAYBACK_STARTED.equals(action)) {
                if (intent.getBooleanExtra(EXTRA_SHOULD_NOTIFY, true)) {
                    sendPlayingNotification(recorder.getRecording());
                }

            } else if (PLAYBACK_STOPPED.equals(action) ||
                    PLAYBACK_COMPLETE.equals(action) ||
                    PLAYBACK_ERROR.equals(action)) {
                gotoIdleState(PLAYBACK_NOTIFY_ID);

            } else if (RECORD_STARTED.equals(action)) {
                acquireWakeLock();
                if (intent.getBooleanExtra(EXTRA_SHOULD_NOTIFY, true)) {
                    sendRecordingNotification(recorder.getRecording());
                }

            } else if (RECORD_PROGRESS.equals(action)) {
                final long time = intent.getLongExtra(EXTRA_ELAPSEDTIME, -1L) / 1000;
                if (!ScTextUtils.usesSameTimeElapsedString(lastNotifiedTime, time)) {
                    lastNotifiedTime = time;
                    updateRecordTicker(time);
                }

            } else if (RECORD_FINISHED.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);

            } else if (RECORD_ERROR.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);

            } else if (NOTIFICATION_STATE.equals(action)) {
                if (intent.getBooleanExtra(EXTRA_SHOULD_NOTIFY, true)) {
                    if (recorder.isRecording()) {
                        sendRecordingNotification(recorder.getRecording());
                    } else if (recorder.isPlaying()) {
                        sendPlayingNotification(recorder.getRecording());
                    }
                } else {
                    lastNotifiedTime = -1;
                    killNotification(PLAYBACK_NOTIFY_ID);
                    killNotification(RECORD_NOTIFY_ID);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startId = startId;
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "create service started");

        recorder = SoundRecorder.getInstance(this);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        recorder = SoundRecorder.getInstance(this);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, getIntentFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        delayedStopHandler.removeCallbacksAndMessages(null);

        broadcastManager.unregisterReceiver(receiver);
        gotoIdleState(RECORD_NOTIFY_ID);
        gotoIdleState(PLAYBACK_NOTIFY_ID);

        releaseWakeLock();
        wakeLock = null;
    }

    private NotificationCompat.Builder ongoingNotificationBuilder(String title, String message,
                                                                  PendingIntent pendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }

    private void scheduleServiceShutdownCheck() {
        Log.d(TAG, "scheduleServiceShutdownCheck()");
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    private void gotoIdleState(int cancelNotificationId) {
        killNotification(cancelNotificationId);
        scheduleServiceShutdownCheck();
        if (!recorder.isActive()) {
            stopForeground(true);
        }
    }

    private void killNotification(int id) {
        notificationManager.cancel(id);
    }

    private Notification createRecordingNotification(Recording recording) {
        recordPendingIntent = PendingIntent.getActivity(this, 0, recording.getViewIntent(),
                                                        PendingIntent.FLAG_UPDATE_CURRENT);

        return ongoingNotificationBuilder(
                getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message_recordtime, "0"),
                recordPendingIntent)
                .build();
    }

    private void sendRecordingNotification(Recording recording) {
        startForeground(RECORD_NOTIFY_ID, createRecordingNotification(recording));
    }

    private void sendPlayingNotification(Recording recording) {
        Intent intent = (new Intent(Actions.RECORD))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startForeground(PLAYBACK_NOTIFY_ID, createPlaynotification(intent, recording));
    }

    private Notification createPlaynotification(Intent intent, Recording r) {
        String title = r.sharingNote(getApplicationContext());

        PendingIntent clickIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return ongoingNotificationBuilder(
                getString(R.string.cloud_recorder_playback_event_title), title, clickIntent)
                .build();
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void updateRecordTicker(long recordTime) {
        Notification notification = ongoingNotificationBuilder(
                getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message_recordtime,
                          ScTextUtils.formatTimeElapsed(getResources(), recordTime, false)),
                recordPendingIntent).build();
        notificationManager.notify(RECORD_NOTIFY_ID, notification);
    }
}
