package com.soundcloud.android.service.record;

import static com.soundcloud.android.Consts.Notifications.PLAYBACK_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.RECORD_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.CloudRecorder;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.utils.IOUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


    public static final File RECORD_DIR = IOUtils.ensureUpdatedDirectory(
            new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
            new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".rec"));

    public static final String[] ALL_ACTIONS = {
      RECORD_STARTED, RECORD_ERROR, RECORD_SAMPLE, RECORD_PROGRESS, RECORD_FINISHED,
      PLAYBACK_STARTED, PLAYBACK_STOPPED, PLAYBACK_COMPLETE, PLAYBACK_PROGRESS, PLAYBACK_PROGRESS
    };

    // recorder/player
    private CloudRecorder mRecorder;

    // files
    private boolean mIsRecording;
    private Recording mRecording;

    // state
    private int mServiceStartId = -1;

    // notifications
    private PendingIntent mRecordPendingIntent;
    private NotificationManager nm;
    private Notification mRecordNotification;

    private LocalBroadcastManager mBroadcastManager;

    private WakeLock mWakeLock;
    private final IBinder mBinder = new LocalBinder<CloudCreateService>() {
        @Override public CloudCreateService getService() {
            return CloudCreateService.this;

        }
    };

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // cargoculted?
        if (!isRecording() && !isPlaying()) {
            // No active playlist, OK to stop the service right now
            stopSelf(mServiceStartId);
        }
        return false;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (PLAYBACK_STARTED.equals(action)) {

            } else if (PLAYBACK_COMPLETE.equals(action) || PLAYBACK_ERROR.equals(action)) {
                onPlaybackComplete();
            } else if (RECORD_STARTED.equals(action)) {

            } else if (RECORD_PROGRESS.equals(action)) {
                final long time = intent.getLongExtra(CloudCreateService.EXTRA_ELAPSEDTIME, -1l);
                if (mRecordNotification != null) {
                    updateRecordTicker(mRecordNotification, time);
                }
            } else if (RECORD_FINISHED.equals(action)) {

            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "create service started");

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mRecorder = CloudRecorder.getInstance(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(receiver, CloudRecorder.getIntentFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) mRecorder.onDestroy();

        mBroadcastManager.unregisterReceiver(receiver);
        // prevent any ongoing notifications that may get stuck
        nm.cancel(RECORD_NOTIFY_ID);

        gotoIdleState();
        releaseWakeLock();
        mWakeLock = null;
    }

    public void startRecording(Recording recording) {
        Log.v(TAG, "startRecording(" + recording + ")");
        acquireWakeLock();
        mRecording = recording;
        startForeground(RECORD_NOTIFY_ID, createRecordingNotification(recording));
        if (mRecorder.startRecording(mRecording.audio_path) == CloudRecorder.State.ERROR) {
            onRecordError();
        } else {
            mIsRecording = true;
        }
    }

    private Notification createRecordingNotification(Recording recording) {
        mRecordPendingIntent = PendingIntent.getActivity(this, 0, recording.getViewIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mRecordNotification = createOngoingNotification(getString(R.string.cloud_recorder_notification_ticker), mRecordPendingIntent);
        mRecordNotification.setLatestEventInfo(this, getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, 0),
                mRecordPendingIntent);

        return mRecordNotification;
    }

    public File getRecordingFile() {
        return mRecording.audio_path;
    }

    public Recording getRecording() {
        return mRecording;
    }

    public void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stopRecording();
        }
        mIsRecording = false;
        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
    }

    public void startReading() {
        mRecorder.startReading();
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void stopPlayback() {
        mRecorder.stopPlayback();
        onPlaybackComplete();
    }

    public void pausePlayback() {
        if (mRecorder.isPlaying()) mRecorder.togglePlayback();
        nm.cancel(PLAYBACK_NOTIFY_ID);
        gotoIdleState();
    }

    public void setPlaybackStart(double pos) {
        mRecorder.onNewStartPosition(pos);
    }

    public void setPlaybackEnd(double pos) {
        mRecorder.onNewEndPosition(pos);
    }

    public void startPlayback(Recording file) throws IOException {
        if (file == null || !file.exists()) throw new IOException("file "+ file +" does not exist");

        mRecording = file;
        mRecorder.play();

        Intent intent;
        if (!mRecording.isSaved()) {
            intent = (new Intent(Actions.RECORD))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent = (new Intent(this, ScCreate.class))
            .setData(mRecording.toUri())
            .setAction(Intent.ACTION_MAIN);
        }
        startForeground(PLAYBACK_NOTIFY_ID, createPlaynotification(intent, mRecording));
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

    public void seekTo(float position) {
        mRecorder.seekToPercentage(position);
    }

    public boolean isPlaying() {
        return mRecorder.isPlaying();
    }

    public long getPlaybackDuration() {
        return mRecorder.getDuration();
    }

    public File getCurrentPlaybackPath() {
        return mRecording.audio_path;
    }

    public long getCurrentPlaybackPosition() {
        return mRecorder.getCurrentPlaybackPosition();
    }

    /**
     * Revert edited file to original form
     */
    public void revertFile() {
        mRecorder.resetPlaybackBounds();
    }


    public void onPlaybackComplete() {
        nm.cancel(PLAYBACK_NOTIFY_ID);
        gotoIdleState();
    }

    private void gotoIdleState() {
        if (!isRecording() && !isPlaying()) {
            stopForeground(true);
        }
    }

    private void onRecordError() {
        sendBroadcast(new Intent(RECORD_ERROR));
        mIsRecording = false;

        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
    }

    /* package */ void updateRecordTicker(Notification notification, long recordTimeMs) {
        notification.setLatestEventInfo(this,
                getString(R.string.cloud_recorder_event_title),
                getString(R.string.cloud_recorder_event_message, recordTimeMs / 1000),
                mRecordPendingIntent);
        nm.notify(RECORD_NOTIFY_ID, notification);
    }

    public static Notification createOngoingNotification(CharSequence tickerText, PendingIntent pendingIntent) {
        int icon = R.drawable.ic_status;
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

    public File[] listRecordingFiles() {
        return RECORD_DIR.listFiles(new RecordingFilter());
    }

    public List<Recording> getUnsavedRecordings() {
        MediaPlayer mp = null;
        List<Recording> unsaved = new ArrayList<Recording>();
        for (File f : listRecordingFiles()) {
            if (getUserIdFromFile(f) != -1) continue; // ignore current file
            Recording r = SoundCloudDB.getRecordingByPath(getContentResolver(), f);
            if (r == null) {
                r = new Recording(f);
                try {
                    if (mp == null) {
                        mp = new MediaPlayer();
                    }
                    mp.reset();
                    mp.setDataSource(f.getAbsolutePath());
                    mp.prepare();
                    r.duration = mp.getDuration();
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                }
                unsaved.add(r);
            }
        }
        Collections.sort(unsaved, null);
        return unsaved;
    }


    public Recording checkForUnusedPrivateRecording(User user) {
        if (user == null) return null;
        for (File f : listRecordingFiles()) {
            if (getUserIdFromFile(f) == user.id) {
                return new Recording(f);
            }
        }
        return null;
    }

    public static Recording createRecording(User user) {
        File file = new File(RECORD_DIR,
                System.currentTimeMillis() + (user == null ? "" : "_" + user.id));
        return new Recording(file);
    }

    public static long getUserIdFromFile(File file) {
        final String path = file.getName();
        if (TextUtils.isEmpty(path) || !path.contains("_") || path.indexOf("_") + 1 >= path.length()) {
            return -1;
        } else try {
            return Long.valueOf(
                    path.substring(path.indexOf('_') + 1,
                    path.contains(".") ? path.indexOf('.') : path.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public class RecordingFilter implements FilenameFilter {
        @Override
        public boolean accept(File file, String name) {
            return Recording.isRawFilename(name) || Recording.isEncodedFilename(name) &&
                    (mRecording == null || !mRecording.audio_path.equals(file));
        }
    }
}