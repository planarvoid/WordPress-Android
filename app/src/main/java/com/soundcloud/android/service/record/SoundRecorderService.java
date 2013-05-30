package com.soundcloud.android.service.record;

import static com.soundcloud.android.Consts.Notifications.PLAYBACK_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.RECORD_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.LocalBinder;
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
import android.util.Log;

/**
 * In charge of lifecycle and notifications for the {@link com.soundcloud.android.record.SoundRecorder}
 */
public class SoundRecorderService extends Service  {
    private static final String TAG = SoundRecorderService.class.getSimpleName();

    // recorder/player
    private SoundRecorder mRecorder;

    // state
    private int mServiceStartId = -1;

    // notifications
    private PendingIntent mRecordPendingIntent;
    private NotificationManager nm;
    private Notification mRecordNotification;

    private LocalBroadcastManager mBroadcastManager;
    private static final int IDLE_DELAY = 30*1000;  // interval after which we stop the service when idle

    private long mLastNotifiedTime;

    private WakeLock mWakeLock;
    private final IBinder mBinder = new LocalBinder<SoundRecorderService>() {
        @Override public SoundRecorderService getService() {
            return SoundRecorderService.this;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        scheduleServiceShutdownCheck();
        return START_STICKY;
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mRecorder.isActive()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "DelayedStopHandler: stopping service");
                stopSelf(mServiceStartId);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "create service started");

        mRecorder = SoundRecorder.getInstance(this);

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mRecorder = SoundRecorder.getInstance(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(receiver, SoundRecorder.getIntentFilter());

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mBroadcastManager.unregisterReceiver(receiver);
        gotoIdleState(RECORD_NOTIFY_ID);
        gotoIdleState(PLAYBACK_NOTIFY_ID);

        releaseWakeLock();
        mWakeLock = null;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "BroadcastReceiver#onReceive(" + action + ")");

            if (SoundRecorder.PLAYBACK_STARTED.equals(action)) {
                if (intent.getBooleanExtra(SoundRecorder.EXTRA_SHOULD_NOTIFY, true)){
                    sendPlayingNotification(mRecorder.getRecording());
                }

            } else if (SoundRecorder.PLAYBACK_STOPPED.equals(action) ||
                       SoundRecorder.PLAYBACK_COMPLETE.equals(action) ||
                       SoundRecorder.PLAYBACK_ERROR.equals(action)) {
                gotoIdleState(PLAYBACK_NOTIFY_ID);

            } else if (SoundRecorder.RECORD_STARTED.equals(action)) {
                acquireWakeLock();
                if (intent.getBooleanExtra(SoundRecorder.EXTRA_SHOULD_NOTIFY, true)){
                    sendRecordingNotification(mRecorder.getRecording());
                }

            } else if (SoundRecorder.RECORD_PROGRESS.equals(action)) {
                final long time = intent.getLongExtra(SoundRecorder.EXTRA_ELAPSEDTIME, -1l) / 1000;
                if (!ScTextUtils.usesSameTimeElapsedString(mLastNotifiedTime,time) && mRecordNotification != null){
                    mLastNotifiedTime = time;
                    updateRecordTicker(mRecordNotification, time);
                }

            } else if (SoundRecorder.RECORD_FINISHED.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);

            } else if (SoundRecorder.RECORD_ERROR.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);

            } else if (SoundRecorder.NOTIFICATION_STATE.equals(action)) {
                if (intent.getBooleanExtra(SoundRecorder.EXTRA_SHOULD_NOTIFY, true)){
                    if (mRecorder.isRecording()) {
                        sendRecordingNotification(mRecorder.getRecording());
                    } else if (mRecorder.isPlaying()) {
                        sendPlayingNotification(mRecorder.getRecording());
                    }
                } else {
                    mLastNotifiedTime = -1;
                    killNotification(PLAYBACK_NOTIFY_ID);
                    killNotification(RECORD_NOTIFY_ID);
                }
            }
        }
    };


    private void gotoIdleState(int cancelNotificationId) {
        killNotification(cancelNotificationId);
        scheduleServiceShutdownCheck();
        if (!mRecorder.isActive()) stopForeground(true);
    }

    private void killNotification(int id) {
        if (id == RECORD_NOTIFY_ID) {
            mRecordNotification = null;
        }
        nm.cancel(id);
    }

    private Notification createRecordingNotification(Recording recording) {
        mRecordPendingIntent = PendingIntent.getActivity(this, 0, recording.getViewIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mRecordNotification = createOngoingNotification(this, mRecordPendingIntent);
        mRecordNotification.setLatestEventInfo(this, getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, 0),
                mRecordPendingIntent);

        return mRecordNotification;
    }

    private void sendRecordingNotification(Recording recording) {
        startForeground(RECORD_NOTIFY_ID, createRecordingNotification(recording));
    }

    private void sendPlayingNotification(Recording recording) {
        Intent intent;
        if (!recording.isSaved()) {
            intent = (new Intent(Actions.RECORD))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent = (new Intent(this, ScCreate.class))
            .setData(recording.toUri())
            .setAction(Intent.ACTION_MAIN);
        }
        startForeground(PLAYBACK_NOTIFY_ID, createPlaynotification(intent, recording));
    }

    private Notification createPlaynotification(Intent intent, Recording r) {
        String title = r.sharingNote(getResources());

        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = createOngoingNotification(this, pi);

        notification.setLatestEventInfo(getApplicationContext(), getApplicationContext()
                .getString(R.string.cloud_recorder_playback_event_title), title,
                pi);

        return notification;
    }

    /* package */ void updateRecordTicker(Notification notification, long recordTime) {
        notification.setLatestEventInfo(this,
                getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, ScTextUtils.getTimeString(getResources(),recordTime, false)),
                mRecordPendingIntent);
        nm.notify(RECORD_NOTIFY_ID, notification);
    }

    public static Notification createOngoingNotification(Context context, PendingIntent pendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        return builder.build();
    }

    private void acquireWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
