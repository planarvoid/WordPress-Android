package com.soundcloud.android.service.record;

import static com.soundcloud.android.Consts.Notifications.*;
import static com.soundcloud.android.provider.DBHelper.Recordings;
import static com.soundcloud.android.utils.CloudUtils.isTaskFinished;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.activity.UploadMonitor;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.record.AudioConfig;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.record.CloudRecorder;
import com.soundcloud.android.view.create.CreateController;
import com.soundcloud.api.CloudAPI;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudCreateService extends Service  {
    private static final String TAG = CloudCreateService.class.getSimpleName();

    public static final String RECORD_STARTED    = "com.soundcloud.android.recordstarted";
    public static final String RECORD_ERROR      = "com.soundcloud.android.recorderror";
    public static final String RECORD_PROGRESS   = "com.soundcloud.android.recorderror";
    public static final String RECORD_FINISHED   = "com.soundcloud.android.recordfinished";

    public static final String PLAYBACK_STARTED  = "com.soundcloud.android.playbackstarted";
    public static final String PLAYBACK_STOPPED  = "com.soundcloud.android.playbackstopped";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_PROGRESS = "com.soundcloud.android.playbackprogress";
    public static final String PLAYBACK_ERROR    = "com.soundcloud.android.playbackerror";

    public static final String UPLOAD_STARTED    = "com.soundcloud.android.fileuploadstarted";
    public static final String UPLOAD_PROGRESS   = "com.soundcloud.android.fileuploadprogress";
    public static final String UPLOAD_SUCCESS    = "com.soundcloud.android.fileuploadsuccessful";
    public static final String UPLOAD_ERROR      = "com.soundcloud.android.fileuploaderror";
    public static final String UPLOAD_CANCELLED  = "com.soundcloud.android.fileuploadcancelled";

    public static final String EXTRA_AMPLITUDE   = "amplitude";
    public static final String EXTRA_ELAPSEDTIME = "elapsedTime";
    public static final String EXTRA_POSITION    = "position";
    public static final String EXTRA_STATE       = "state";
    public static final String EXTRA_PATH        = "path";


    public static AudioConfig DEFAULT_CONFIG = AudioConfig.PCM16_44100_1;

    // recorder/player
    private CloudRecorder mRecorder;

    // files
    private File mRecordingFile;

    // tasks
    private ImageResizeTask mResizeTask;
    private UploadTrackTask mUploadTask;

    // state
    private boolean mRecording;
    private Upload mCurrentUpload;
    private final Map<Long,Upload> mUploadMap = new HashMap<Long, Upload>();
    private int frameCount;
    private int mServiceStartId = -1;
    private Uri mPlaybackLocal;

    // notifications
    private PendingIntent mRecordPendingIntent;
    private RemoteViews mUploadNotificationView;
    private Notification mRecordNotification, mUploadNotification;
    private NotificationManager nm;
    private String mRecordEventTitle, mPlaybackTitle;

    private LocalBroadcastManager mBroadcastManager;

    private WakeLock mWakeLock;
    private final IBinder mBinder = new LocalBinder();


    public class LocalBinder extends Binder {
        public CloudCreateService getService() {
            return CloudCreateService.this;
        }
    }

    /** Accessor only for testing purposes */
    public IBinder getBinder() {
        return mBinder;
    }

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!isUploading() && !isRecording() && !isPlaying()) {
            // No active playlist, OK to stop the service right now
            stopSelf(mServiceStartId);
            return true;
        } else {
            // something is currently uploading so don't stop the service now.
            return true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // init the service here
        Log.i(TAG, "upload service started started");

        // get notification manager
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        mRecorder = CloudRecorder.getInstance(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        if (mBroadcastManager != null) // XXX robolectric, tmp
        // TODO unregister
        mBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (PLAYBACK_STARTED.equals(action)) {

                } else if (PLAYBACK_COMPLETE.equals(action) || PLAYBACK_ERROR.equals(action)) {
                    onPlaybackComplete();
                } else if (RECORD_STARTED.equals(action)) {

                } else if (RECORD_PROGRESS.equals(action)) {
                    final long recordTimeMs = intent.getLongExtra(CloudCreateService.EXTRA_ELAPSEDTIME, -1l);
                    if (mRecordNotification != null && recordTimeMs > -1 && frameCount++ % (1000 / CloudRecorder.TIMER_INTERVAL) == 0) {
                        updateRecordTicker(mRecordNotification, recordTimeMs);
                    }
                } else if (RECORD_FINISHED.equals(action)) {

                }
            }
        }, CloudRecorder.getIntentFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCurrentUpload != null) {
            Log.e(TAG, "Service being destroyed while still uploading.");
            Recording.updateStatus(getContentResolver(), mCurrentUpload);
        }

        // if we are getting shut down by the system, shut down recording/playback
        if (mRecorder != null) mRecorder.onDestroy();

        // prevent any ongoing notifications that may get stuck
        nm.cancel(RECORD_NOTIFY_ID);

        gotoIdleState();
        shutdownService();
        mWakeLock = null;
    }

    public void startRecording(File path) {
        Log.v(TAG, "startRecording(" + path + ")");

        acquireWakeLock();

        mRecordingFile = path;
        frameCount = 0;

        final long messageRecipient = CreateController.getPrivateUserIdFromPath(path);

        Intent intent = new Intent();
        if (messageRecipient != -1) {
            intent.setAction(Actions.MESSAGE)
                  .putExtra("recipient", messageRecipient);
        } else {
            intent.setAction(Actions.RECORD);
        }

        mRecordPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRecordNotification = createOngoingNotification(getString(R.string.cloud_recorder_notification_ticker), mRecordPendingIntent);
        mRecordEventTitle = getString(R.string.cloud_recorder_event_title);
        mRecordNotification.setLatestEventInfo(this, mRecordEventTitle,
                getString(R.string.cloud_recorder_event_message, 0),
                mRecordPendingIntent);

        startForeground(RECORD_NOTIFY_ID, mRecordNotification);
        mRecording = true;
        if (mRecorder.startRecording(mRecordingFile) == CloudRecorder.State.ERROR) {
                onRecordError();
        }
    }

    public File getRecordingFile() {
        return mRecordingFile;
    }


    public void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stopRecording();
        }
        mRecording = false;
        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
    }

    public void startReading() {
        mRecorder.startReading();
    }

    public boolean isRecording() {
        return mRecording;
    }

    public void loadPlaybackTrack(File file) throws IOException {
        if (file == null || !file.exists()) throw new IOException("file "+file+" does not exist");

        String[] columns = { Recordings._ID, Recordings.WHERE_TEXT, Recordings.WHAT_TEXT };
        Cursor cursor = getContentResolver().query(Content.RECORDINGS.uri,
                columns, Recordings.AUDIO_PATH + "= ?",new String[]{file.getAbsolutePath()}, null);

        if (cursor != null && cursor.moveToFirst()) {
            mPlaybackLocal = Content.RECORDINGS.buildUpon().appendPath(
                    String.valueOf(cursor.getLong(cursor.getColumnIndex(Recordings._ID)))).build();

            mPlaybackTitle = Recording.generateRecordingSharingNote(
                    getResources(),
                    cursor.getString(cursor.getColumnIndex(Recordings.WHAT_TEXT)),
                    cursor.getString(cursor.getColumnIndex(Recordings.WHERE_TEXT)),
                    mRecordingFile.lastModified());
        } else {
            mPlaybackTitle = Recording.generateRecordingSharingNote(getResources(), null, null, mRecordingFile.lastModified());
        }
        if (cursor != null) cursor.close();
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

    public void startPlayback(File file) throws IOException {
        loadPlaybackTrack(file);
        mRecorder.play();

        Intent i;
        if (mPlaybackLocal == null) {
            i = (new Intent(Actions.RECORD))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            i = (new Intent(this, ScCreate.class))
            .setData(mPlaybackLocal)
            .setAction(Intent.ACTION_MAIN);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification mPlaybackNotification = createOngoingNotification(
                getString(R.string.cloud_recorder_playback_notification_ticker,
                        mPlaybackTitle),
                pendingIntent);

        mPlaybackNotification.setLatestEventInfo(getApplicationContext(), getApplicationContext()
                .getString(R.string.cloud_recorder_playback_event_title), mPlaybackTitle,
                pendingIntent);

        startForeground(PLAYBACK_NOTIFY_ID, mPlaybackNotification);
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
        return mRecordingFile;
    }

    public long getCurrentPlaybackPosition() {
        return mRecorder.getCurrentPlaybackPosition();
    }

    /**
     * Process trimming, normalization, fades, etc
     */
    public void processFile() {
        // for now just revert trim
        mRecorder.resetPlaybackBounds();
    }

    /**
     * Revert edited file to original form
     */
    public void revertFile() {
        mRecorder.resetPlaybackBounds();
    }

    public boolean startUpload(final Upload upload) {
        if (mCurrentUpload != null && mCurrentUpload.status == Upload.Status.UPLOADING) return false;

        acquireWakeLock();

        mUploadMap.put(upload.id, upload);

        if (mRecordingFile != null && mRecordingFile.equals(upload.soundFile)) {
            stopPlayback();
        }

        upload.status = Upload.Status.UPLOADING;
        if (upload.local_recording_id != 0) {
            Recording.updateStatus(getContentResolver(), upload);
        }

        mCurrentUpload = upload;
        sendUploadingNotification();
        sendBroadcast(new Intent(UPLOAD_STARTED).putExtra("upload_id", upload.id));

        mUploadTask = new UploadTrackTask((CloudAPI) getApplication());
        if (upload.artworkFile == null) {
            mUploadTask.execute(upload);
        } else {
            mResizeTask = new ImageResizeTask(mUploadTask);
            mResizeTask.execute(upload);
        }
        return true;
    }

    public boolean isUploading() {
        return (mResizeTask != null || mUploadTask != null);
    }

    public void cancelUploadById(long id) {
        if (mCurrentUpload == null) return;
        // eventually add support for cancelling queued uploads, but not necessary yet
        if (id == mCurrentUpload.id) cancelUpload();
    }


    public void cancelUpload() {
        if (mCurrentUpload == null) return;

        if (mResizeTask != null && !isTaskFinished(mResizeTask)) {
            mResizeTask.cancel(true);
            mResizeTask = null;
        }

        if (mUploadTask != null && !isTaskFinished(mUploadTask)) {
            mUploadTask.cancel(true);
            mUploadTask = null;
        }

        if (mCurrentUpload != null){
            mCurrentUpload.status = Upload.Status.NOT_YET_UPLOADED;
            Recording.updateStatus(getContentResolver(), mCurrentUpload);
        }

        mCurrentUpload = null;
        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
        sendBroadcast(new Intent(UPLOAD_CANCELLED));
    }

    public long getUploadLocalId() {
        return (mCurrentUpload != null && mCurrentUpload.status == Upload.Status.UPLOADING)
                ? mCurrentUpload.local_recording_id : 0;
    }

    public Upload getUploadById(long id) {
        return mUploadMap.get(id);
    }

    public void onPlaybackComplete() {
        nm.cancel(PLAYBACK_NOTIFY_ID);
        gotoIdleState();
    }

    private void gotoIdleState() {
        if (!isUploading() && !isRecording() && !isPlaying()) {
            mPlaybackLocal = null;
            stopForeground(true);
        }
    }

    private void shutdownService() {
        Log.v(TAG, "upload Service stopped!!!");

        releaseWakeLock();

        if (mUploadTask != null) mUploadTask.cancel(true);
        if (mResizeTask != null) mResizeTask.cancel(true);

        Log.v(TAG, "upload Service shutdown complete.");
    }
    private void onRecordError() {
        sendBroadcast(new Intent(RECORD_ERROR));
        mRecording = false;

        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
    }

    /* package */ void updateRecordTicker(Notification notification, long recordTimeMs) {
        notification.setLatestEventInfo(this, mRecordEventTitle,
                getString(R.string.cloud_recorder_event_message, recordTimeMs / 1000), mRecordPendingIntent);
        nm.notify(RECORD_NOTIFY_ID, notification);
    }

    private boolean sendUploadingNotification(){
        if (mCurrentUpload == null) return false;

        Intent i = (new Intent(this, UploadMonitor.class))
                .putExtra("upload_id", mCurrentUpload.id);

        mUploadNotificationView = new RemoteViews(getPackageName(), R.layout.create_service_status_upload);
        mUploadNotificationView.setTextViewText(R.id.message, mCurrentUpload.title);
        mUploadNotificationView.setTextViewText(R.id.percentage, "0");
        mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mUploadNotification = createOngoingNotification(getString(R.string.cloud_uploader_notification_ticker),
                PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

        mUploadNotification.contentView = mUploadNotificationView;

        startForeground(UPLOAD_NOTIFY_ID, mUploadNotification);
        return true;
    }

    private class ImageResizeTask extends AsyncTask<Upload, Integer, Upload> {
        private static final int RECOMMENDED_SIZE = 800;
        private AsyncTask<Upload, ?, ?> nextTask;

        public ImageResizeTask(AsyncTask<Upload, ?, ?> nextTask) {
            this.nextTask = nextTask;
        }

        @Override
        protected void onPostExecute(Upload result) {
            mResizeTask = null;
            if (result.isSuccess() && !isCancelled() && !mCurrentUpload.isCancelled()) {
                nextTask.execute(result);
            } else if (!mCurrentUpload.isSuccess()) {
                notifyUploadCurrentUploadFinished(result);
            }
        }

        @Override
        protected Upload doInBackground(Upload... params) {
            final Upload upload = params[0];
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "resizing "+upload.artworkFile);
            try {
                File outFile = IOUtils.getCacheFile(CloudCreateService.this, "upload_tmp.png");
                if (ImageUtils.resizeImageFile(upload.artworkFile, outFile, RECOMMENDED_SIZE, RECOMMENDED_SIZE)) {
                    upload.resizedArtworkFile = outFile;
                } else {
                    Log.w(TAG, "did not resize image "+upload.artworkFile);
                }
            } catch (IOException e) {
                Log.e(TAG, "error resizing", e);
            }
            return upload;
        }
    }

    private class UploadTrackTask extends UploadTask {
        private String eventString;

        public UploadTrackTask(CloudAPI api) {
            super(api);
        }

        @Override
        protected void onPreExecute() {
            eventString = getString(R.string.cloud_uploader_event_uploading);
            mUploadNotificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected void onProgressUpdate(Long... progress) {
            if (!isCancelled()) {
                mUploadNotificationView.setProgressBar(R.id.progress_bar,
                        progress[1].intValue(), (int) Math.min(progress[1], progress[0]), false);

                final int currentProgress = (int) Math.min(100, (100 * progress[0]) / progress[1]);
                mUploadNotificationView.setTextViewText(R.id.percentage, String.format(eventString, currentProgress));
                nm.notify(UPLOAD_NOTIFY_ID, mUploadNotification);

                sendBroadcast(new Intent(UPLOAD_PROGRESS)
                    .putExtra("upload_id", mCurrentUpload.id)
                    .putExtra("progress", currentProgress));
            }
        }

        @Override
        protected void onPostExecute(Upload result) {
            mUploadTask = null;
            if (!result.isCancelled()) {
                notifyUploadCurrentUploadFinished(result);
            }
        }
    }

    private void notifyUploadCurrentUploadFinished(Upload upload) {
        if (upload.id != mCurrentUpload.id) {
            Log.w(TAG, "upload mismatch");
            return;
        }

        nm.cancel(UPLOAD_NOTIFY_ID);

        gotoIdleState();
        final CharSequence title;
        final CharSequence message;

        Intent userTracks = (new Intent(Actions.MY_PROFILE).putExtra("userBrowserTag", UserBrowser.Tab.tracks));

        if (upload.isSuccess()) {
            upload.status = Upload.Status.UPLOADED;

            title = getString(R.string.cloud_uploader_notification_finished_title);
            message = getString(R.string.cloud_uploader_notification_finished_message, upload.title);

            Intent broadcastIntent = new Intent(UPLOAD_SUCCESS);
            broadcastIntent.putExtra("upload_id", upload.id);
            broadcastIntent.putExtra("isPrivate", upload.isPrivate());
            sendBroadcast(broadcastIntent);
            Recording.updateStatus(getContentResolver(), upload);
            upload.onUploaded();
            mCurrentUpload = null;
        } else {
            upload.status = Upload.Status.NOT_YET_UPLOADED;
            title = getString(R.string.cloud_uploader_notification_error_title);
            message = getString(R.string.cloud_uploader_notification_error_message, upload.title);

            Intent broadcastIntent = new Intent(UPLOAD_ERROR);
            broadcastIntent.putExtra("upload_id", mCurrentUpload.id);
            sendBroadcast(broadcastIntent);
            Recording.updateStatus(getContentResolver(), upload);
        }

        int icon = R.drawable.ic_status;
        CharSequence tickerText = upload.isSuccess() ? getString(R.string.cloud_uploader_notification_finished_ticker)
                : getString(R.string.cloud_uploader_notification_error_ticker);
        long when = System.currentTimeMillis();

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, userTracks, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this,title ,message , contentIntent);
        nm.notify(UPLOAD_NOTIFY_ID, notification);
        releaseWakeLock();
    }


    private Notification createOngoingNotification(CharSequence tickerText, PendingIntent pendingIntent) {
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
}