package com.soundcloud.android.service.record;

import static com.soundcloud.android.Consts.Notifications.PLAYBACK_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.RECORD_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.record.CloudRecorder;
import com.soundcloud.android.service.LocalBinder;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CloudCreateService extends Service  {
    private static final String TAG = CloudCreateService.class.getSimpleName();

    public static final String RECORD_STARTED    = "com.soundcloud.android.recordstarted";
    public static final String RECORD_ERROR      = "com.soundcloud.android.recorderror";
    public static final String RECORD_SAMPLE     = "com.soundcloud.android.recordsample";
    public static final String RECORD_PROGRESS   = "com.soundcloud.android.recordprogress";
    public static final String RECORD_FINISHED   = "com.soundcloud.android.recordfinished";

    public static final String PLAYBACK_STARTED  = "com.soundcloud.android.playbackstarted";
    public static final String PLAYBACK_STOPPED  = "com.soundcloud.android.playbackstopped";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_PROGRESS = "com.soundcloud.android.playbackprogress";
    public static final String PLAYBACK_ERROR    = "com.soundcloud.android.playbackerror";

    public static final String EXTRA_AMPLITUDE   = "amplitude";
    public static final String EXTRA_ELAPSEDTIME = "elapsedTime";
    public static final String EXTRA_POSITION    = "position";
    public static final String EXTRA_STATE       = "state";
    public static final String EXTRA_PATH        = "path";

    public static final String[] ALL_ACTIONS = {
      RECORD_STARTED, RECORD_ERROR, RECORD_SAMPLE, RECORD_PROGRESS, RECORD_FINISHED,
      PLAYBACK_STARTED, PLAYBACK_STOPPED, PLAYBACK_COMPLETE, PLAYBACK_PROGRESS, PLAYBACK_PROGRESS
    };

    // recorder/player
    private CloudRecorder mRecorder;

    // state
    private int mServiceStartId = -1;

    // notifications
    private PendingIntent mRecordPendingIntent;
    private NotificationManager nm;
    private Notification mRecordNotification;

    private LocalBroadcastManager mBroadcastManager;

    private static final int IDLE_DELAY = 30*1000;  // interval after which we stop the service when idle

    private WakeLock mWakeLock;
    private final IBinder mBinder = new LocalBinder<CloudCreateService>() {
        @Override public CloudCreateService getService() {
            return CloudCreateService.this;
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

        mRecorder = CloudRecorder.getInstance(this);

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mRecorder = CloudRecorder.getInstance(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(receiver, CloudRecorder.getIntentFilter());

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

            if (PLAYBACK_STARTED.equals(action)) {
                onPlaybackStarted(mRecorder.getRecording());

            } else if (PLAYBACK_STOPPED.equals(action) || PLAYBACK_COMPLETE.equals(action) || PLAYBACK_ERROR.equals(action)) {
                gotoIdleState(PLAYBACK_NOTIFY_ID);

            } else if (RECORD_STARTED.equals(action)) {
                acquireWakeLock();
                startForeground(RECORD_NOTIFY_ID, createRecordingNotification(mRecorder.getRecording()));

            } else if (RECORD_PROGRESS.equals(action)) {
                final long time = intent.getLongExtra(CloudCreateService.EXTRA_ELAPSEDTIME, -1l);
                if (mRecordNotification != null) {
                    updateRecordTicker(mRecordNotification, time);
                }

            } else if (RECORD_FINISHED.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);

            } else if (RECORD_ERROR.equals(action)) {
                gotoIdleState(RECORD_NOTIFY_ID);
            }
        }
    };

    private void gotoIdleState(int cancelNotificationId) {
        nm.cancel(cancelNotificationId);
        scheduleServiceShutdownCheck();
        if (!mRecorder.isActive()) stopForeground(true);
    }


    private Notification createRecordingNotification(Recording recording) {
        mRecordPendingIntent = PendingIntent.getActivity(this, 0, recording.getViewIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mRecordNotification = createOngoingNotification(getString(R.string.cloud_recorder_notification_ticker), mRecordPendingIntent);
        mRecordNotification.setLatestEventInfo(this, getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, 0),
                mRecordPendingIntent);

        return mRecordNotification;
    }

    private void onPlaybackStarted(Recording recording) {
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

        Notification notification = createOngoingNotification(
                getString(R.string.cloud_recorder_playback_notification_ticker, title),
                pi);

        notification.setLatestEventInfo(getApplicationContext(), getApplicationContext()
                .getString(R.string.cloud_recorder_playback_event_title), title,
                pi);

        return notification;
    }

    /* package */ void updateRecordTicker(Notification notification, long recordTimeMs) {
        notification.setLatestEventInfo(this,
                getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, recordTimeMs / 1000),
                mRecordPendingIntent);
        nm.notify(RECORD_NOTIFY_ID, notification);
    }

    public static Notification createOngoingNotification(CharSequence tickerText, PendingIntent pendingIntent) {
        int icon = R.drawable.ic_notification_cloud;
        Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
        notification.contentIntent = pendingIntent;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
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
