
package com.soundcloud.android.service;

import static com.soundcloud.android.Consts.Notifications.PLAYBACK_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.RECORD_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.UPLOAD_NOTIFY_ID;
import static com.soundcloud.android.utils.CloudUtils.isTaskFinished;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ExternalUploadProgress;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.task.OggEncoderTask;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.task.UploadTask.Params;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.record.CloudRecorder;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;
import com.soundcloud.api.CloudAPI;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

public class CloudCreateService extends Service {
    private static final String TAG = "CloudUploaderService";

    public static final String RECORD_ERROR      = "com.soundcloud.android.recorderror";
    public static final String UPLOAD_PROGRESS    = "com.sound.android.fileuploadprogress";
    public static final String UPLOAD_SUCCESS    = "com.sound.android.fileuploadsuccessful";
    public static final String UPLOAD_ERROR      = "com.sound.android.fileuploaderror";
    public static final String UPLOAD_CANCELLED  = "com.sound.android.fileuploadcancelled";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR    = "com.soundcloud.android.playbackerror";


    private static WakeLock mWakeLock;

    private CloudRecorder mRecorder;

    private File mRecordFile;

    private boolean mRecording = false;

    private OggEncoderTask<Params, ?> mOggTask;
    private ImageResizeTask mResizeTask;
    private UploadTask mUploadTask;

    private PendingIntent mRecordPendingIntent;

    private RemoteViews mUploadNotificationView;

    private Notification mRecordNotification;
    private Notification mUploadNotification;

    private String mRecordEventTitle;
    private String mRecordEventMessage;

    private NotificationManager nm;

    private int mServiceStartId = -1;

    private boolean mCurrentUploadCancelled = false;

    private long mRecordStartTime;

    private int frameCount;

    private MediaPlayer mPlayer;
    private File mPlaybackFile;

    private Uri mPlaybackLocal;

    private String mPlaybackTitle;

    private Upload mCurrentUpload;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
    }

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
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnCompletionListener(completionListener);
        mPlayer.setOnErrorListener(errorListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCurrentUpload != null){
            Log.e(TAG, "Service being destroyed while still uploading.");
            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Upload.UploadStatus.NOT_YET_UPLOADED);
            cv.put(Recordings.UPLOAD_ERROR, true);
            int x = getContentResolver().update(Content.RECORDINGS,cv,Recordings.ID+"="+ mCurrentUpload.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
        }

        _shutdownService();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        mWakeLock = null;
        super.onDestroy();
    }

    private void gotoIdleState() {
        if (!isUploading() && !isRecording() && !isPlaying()) {
            mPlaybackLocal = null;
            stopForeground(true);
        }
    }

    private void _shutdownService() {
        Log.i(TAG, "upload Service stopped!!!");

        releaseWakeLock();

        if (mOggTask != null)    mOggTask.cancel(true);
        if (mUploadTask != null) mUploadTask.cancel(true);
        if (mResizeTask != null) mResizeTask.cancel(true);

        Log.i(TAG, "upload Service shutdown complete.");
    }

    private void startRecording(String path, int mode) {
        Log.v(TAG, "startRecording("+path+", "+mode+")");

        acquireWakeLock();

        mRecordFile = new File(path);
        frameCount = 0;

        mRecorder = new CloudRecorder(mode, MediaRecorder.AudioSource.MIC);
        mRecorder.setRecordService(CloudCreateService.this);
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());

        Thread t = new Thread() {
            @Override
            public void run() {
                mRecorder.prepare();
                mRecorder.start();

                if (mRecorder.getState() == CloudRecorder.State.ERROR){
                    onRecordError();
                } else {
                    mRecordStartTime = System.currentTimeMillis();
                }
            }
        };

        Intent i = (new Intent(this, Main.class)).addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", "record");

        mRecordPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        mRecordNotification = createOngoingNotification(getApplicationContext().getResources()
                .getString(R.string.cloud_recorder_notification_ticker), mRecordPendingIntent);

        mRecordEventTitle = getApplicationContext().getString(R.string.cloud_recorder_event_title);

        mRecordEventMessage = getApplicationContext().getString(
                R.string.cloud_recorder_event_message);

        mRecordNotification.setLatestEventInfo(getApplicationContext(), mRecordEventTitle,
                CloudUtils.formatString(mRecordEventMessage, 0), mRecordPendingIntent);

        startForeground(RECORD_NOTIFY_ID, mRecordNotification);

        mRecording = true;

        t.start();
    }

    public String getRecordingPath() {
        return mRecordFile.getAbsolutePath();
    }

    private void onRecordError(){
        sendBroadcast(new Intent(RECORD_ERROR));

        //already in an error state, so just call these in case
        mRecorder.stop();
        mRecorder.release();
        mRecording = false;

        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();

    }


    public void onRecordFrameUpdate(float maxAmplitude) {
        ((SoundCloudApplication) this.getApplication()).onFrameUpdate(maxAmplitude, System.currentTimeMillis() - mRecordStartTime);
        // this should happen every second
        if (frameCount++ % (1000 / CloudRecorder.TIMER_INTERVAL)  == 0) updateRecordTicker();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
        }
        mRecording = false;
        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
    }

    private boolean isRecording() {
        return mRecording;
    }

    private void updateRecordTicker() {
        mRecordNotification.setLatestEventInfo(getApplicationContext(), mRecordEventTitle, CloudUtils
                .formatString(mRecordEventMessage, (int)(System.currentTimeMillis() - mRecordStartTime)/1000), mRecordPendingIntent);

        nm.notify(RECORD_NOTIFY_ID, mRecordNotification);
    }


    public void loadPlaybackTrack(String playbackPath) {
        mPlayer.reset();
        mPlaybackFile = new File(playbackPath);

        String[] columns = { Recordings.ID, Recordings.WHERE_TEXT, Recordings.WHAT_TEXT };
        Cursor cursor = getContentResolver().query(Content.RECORDINGS,
                columns, Recordings.AUDIO_PATH + "= ?",new String[]{playbackPath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            mPlaybackLocal = Content.RECORDINGS.buildUpon().appendPath(
                    String.valueOf(cursor.getLong(cursor.getColumnIndex(Recordings.ID)))).build();

            mPlaybackTitle = CloudUtils.generateRecordingSharingNote(
                    cursor.getString(cursor.getColumnIndex(Recordings.WHAT_TEXT)),
                    cursor.getString(cursor.getColumnIndex(Recordings.WHERE_TEXT)),
                    mPlaybackFile.lastModified());
        } else {
            mPlaybackTitle = CloudUtils.generateRecordingSharingNote(null, null, mPlaybackFile.lastModified());
        }
        if (cursor != null) cursor.close();



        try {
            FileInputStream fis = new FileInputStream(playbackPath);
            mPlayer.setDataSource(fis.getFD());
            fis.close();
            mPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            sendBroadcast(new Intent(PLAYBACK_ERROR));
        }
    }

    public void stopPlayback() {
        try{
            mPlayer.stop();
        } catch (IllegalStateException e){
            Log.e(TAG,"error " + e.toString());
        }
        onPlaybackComplete();
    }

    public void pausePlayback() {
       mPlayer.pause();
       nm.cancel(PLAYBACK_NOTIFY_ID);
       gotoIdleState();
    }

    private void onPlaybackComplete(){
        mPlaybackFile = null;
        mPlaybackLocal = null;
        nm.cancel(PLAYBACK_NOTIFY_ID);
        gotoIdleState();
    }

    public void startPlayback() {
        mPlayer.start();

        Intent i;
        if (mPlaybackLocal == null){
            i = (new Intent(this, Main.class)).addCategory(Intent.CATEGORY_LAUNCHER)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("tabTag", "record");
        } else {
            i = (new Intent(this, ScCreate.class))
            .setData(mPlaybackLocal)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            .setAction(Intent.ACTION_MAIN);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification mPlaybackNotification = createOngoingNotification(CloudUtils.formatString(getApplicationContext().getResources()
                .getString(R.string.cloud_recorder_playback_notification_ticker), mPlaybackTitle),
                pendingIntent);

        mPlaybackNotification.setLatestEventInfo(getApplicationContext(), getApplicationContext()
                .getString(R.string.cloud_recorder_playback_event_title), mPlaybackTitle,
                pendingIntent);

        startForeground(PLAYBACK_NOTIFY_ID, mPlaybackNotification);
    }



    public void seekTo(int position) {
        mPlayer.seekTo(position);
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public int getPlaybackDuration() {
        return mPlayer.getDuration();
    }

    public String getCurrentPlaybackPath() {
        return mPlaybackFile == null ? "" : mPlaybackFile.getAbsolutePath();
    }

    public int getCurrentPlaybackPosition() {
        return mPlayer.getCurrentPosition();
    }

    @SuppressWarnings("unchecked")
    private void uploadTrack(final Map<String,?> trackdata) {
        startUpload(new Upload(trackdata));
    }



    @SuppressWarnings("unchecked")
    private void startUpload(final Upload upload) {
        acquireWakeLock();

        if (mPlaybackFile != null && mPlaybackFile.equals(upload.trackFile)) {
            stopPlayback();
        }

        upload.upload_id = System.currentTimeMillis();
        mCurrentUpload = upload;

        Log.i("asdf","Start Upload " + upload.title);

        mCurrentUploadCancelled = false;

        Intent i = (new Intent(this, ExternalUploadProgress.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("upload", upload);


        mUploadNotificationView = new RemoteViews(getPackageName(), R.layout.create_service_status_upload);
        mUploadNotificationView.setTextViewText(R.id.message, upload.title);
        mUploadNotificationView.setTextViewText(R.id.percentage, "0");
        mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mUploadNotification = createOngoingNotification( getString(R.string.cloud_uploader_notification_ticker),
                PendingIntent.getActivity(this, 0, i, 0));

        mUploadNotification.contentView = mUploadNotificationView;
        startForeground(UPLOAD_NOTIFY_ID, mUploadNotification);

        mOggTask = new EncodeOggTask();
        mOggTask.execute(new Params[] { new Params(upload) });
    }

    private class EncodeOggTask extends OggEncoderTask<UploadTask.Params, UploadTask.Params> {
        private String eventString;

        @Override
        protected void onPreExecute() {
            eventString = getString(R.string.cloud_uploader_event_encoding);
            mUploadNotificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected UploadTask.Params doInBackground(UploadTask.Params... params) {
            UploadTask.Params param = params[0];
            if (param.encode && !encode(param.trackFile, param.encodedFile)) {
                if (param.encodedFile.exists()) param.encodedFile.delete();
                param.fail();
            }
            return param;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (!isCancelled()) {
                mUploadNotificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
                mUploadNotificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                        100, (100 * progress[0]) / progress[1])));
                nm.notify(UPLOAD_NOTIFY_ID, mUploadNotification);
            }
        }

        @Override
        protected void onPostExecute(UploadTask.Params param) {
            mOggTask = null;
            if (!isCancelled() && !mCurrentUploadCancelled && param.isSuccess()) {
                if (param.encode && param.trackFile.exists()) {

                    //in case upload doesn't finish, maintain the timestamp (unnecessary now but might be if we change titling)
                    param.encodedFile.setLastModified(param.trackFile.lastModified());

                    File newEncodedFile = new File(param.trackFile.getParentFile(), param.encodedFile.getName());
                    if (param.encodedFile.renameTo(newEncodedFile)) {
                        param.encodedFile = newEncodedFile;
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(Recordings.AUDIO_PATH, param.encodedFile.getAbsolutePath());
                    cv.put(Recordings.AUDIO_PROFILE, Profile.ENCODED_HIGH);
                    int x = getContentResolver().update(Content.RECORDINGS,cv,Recordings.ID+"="+ mCurrentUpload.local_recording_id, null);
                    Log.d(TAG, x + " row(s) audio path updated.");

                    mCurrentUpload.trackFile = mCurrentUpload.encodedFile;
                    mCurrentUpload.encode = false;

                    // XXX always delete here?
                    param.trackFile.delete();
                }

                mUploadTask = new UploadTrackTask((CloudAPI) getApplication());

                if (param.artworkFile == null) {
                    mUploadTask.execute(param);
                } else {
                    mResizeTask = new ImageResizeTask(mUploadTask);
                    mResizeTask.execute(param);
                }
            } else if (!mCurrentUploadCancelled) notifyUploadCurrentUploadFinished(param);
        }
    }

    private class ImageResizeTask extends AsyncTask<UploadTask.Params, Integer, UploadTask.Params> {
        private static final int RECOMMENDED_SIZE = 800;
        private AsyncTask<UploadTask.Params, ?, ?> nextTask;


        public ImageResizeTask(AsyncTask<UploadTask.Params, ?, ?> nextTask) {
            this.nextTask = nextTask;
        }

        @Override
        protected void onPostExecute(UploadTask.Params result) {
            mResizeTask = null;
            if (result.isSuccess() && !isCancelled() && !mCurrentUploadCancelled) {
                nextTask.execute(result);
            } else if (!mCurrentUploadCancelled) {
                notifyUploadCurrentUploadFinished(result);
            }
        }

        @Override
        protected UploadTask.Params doInBackground(UploadTask.Params... params) {
            final UploadTask.Params param = params[0];
            try {
                File outFile = CloudUtils.getCacheFile(CloudCreateService.this, "upload_tmp.png");
                if (ImageUtils.resizeImageFile(param.artworkFile, outFile, RECOMMENDED_SIZE, RECOMMENDED_SIZE)) {
                    param.resizedFile = outFile;
                } else {
                    Log.w(TAG, "did not resize image "+param.artworkFile);
                }
                return param;
            } catch (IOException e) {
                Log.e(TAG, "error resizing", e);
                return param.fail();
            }
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

                mUploadNotificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                        100, (100 * progress[0]) / progress[1])));

                nm.notify(UPLOAD_NOTIFY_ID, mUploadNotification);

                Intent i = new Intent(UPLOAD_PROGRESS);
                i.putExtra("upload_id", mCurrentUpload.upload_id);
                i.putExtra("progress", Math.min(100, (100 * progress[0]) / progress[1]));
                sendBroadcast(i);
            }
        }

        @Override
        protected void onPostExecute(UploadTask.Params result) {
            if (isCancelled()) result.fail();
            mUploadTask = null;
            if (!mCurrentUploadCancelled) {
                notifyUploadCurrentUploadFinished(result);
            }
        }
    }

    private void notifyUploadCurrentUploadFinished(UploadTask.Params params) {
        nm.cancel(UPLOAD_NOTIFY_ID);

        gotoIdleState();

        CharSequence notificationTitle;
        CharSequence notificationMessage;

        if (params.isSuccess()) {
            mCurrentUpload.upload_status = Upload.UploadStatus.UPLOADED;
            mCurrentUpload.upload_error = false;
            notificationTitle = getString(R.string.cloud_uploader_notification_finished_title);
            notificationMessage = String.format(getString(R.string.cloud_uploader_notification_finished_message),
                    params.get(com.soundcloud.api.Params.Track.TITLE));

            // XXX make really, really sure 3rd party uploads don't get deleted
            if (mCurrentUpload.delete_after && params.encode && params.trackFile != null && params.trackFile.exists()) params.trackFile.delete();
            if (params.encodedFile != null && params.encodedFile.exists()) params.encodedFile.delete();

            Intent intent = new Intent(UPLOAD_SUCCESS);
            intent.putExtra("upload_id", mCurrentUpload.upload_id);
            intent.putExtra("isPrivate", params.get(com.soundcloud.api.Params.Track.SHARING).equals(com.soundcloud.api.Params.Track.PRIVATE));
            sendBroadcast(intent);

            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Upload.UploadStatus.UPLOADED);
            int x = getContentResolver().update(Content.RECORDINGS,cv,Recordings.ID+"="+ mCurrentUpload.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked as uploaded.");

            mCurrentUpload = null;

        } else {
            mCurrentUpload.upload_status = Upload.UploadStatus.NOT_YET_UPLOADED;
            mCurrentUpload.upload_error = true;
            notificationTitle = getString(R.string.cloud_uploader_notification_error_title);
            notificationMessage = String.format(getString(R.string.cloud_uploader_notification_error_message),
                    params.get(com.soundcloud.api.Params.Track.TITLE));

            Intent intent = new Intent(UPLOAD_ERROR);
            intent.putExtra("upload_id", mCurrentUpload.upload_id);
            sendBroadcast(intent);

            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_ERROR, true);
            cv.put(Recordings.UPLOAD_STATUS, Upload.UploadStatus.NOT_YET_UPLOADED);
            int x = getContentResolver().update(Content.RECORDINGS,cv,Recordings.ID+"="+ mCurrentUpload.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
        }

        int icon = R.drawable.statusbar;
        CharSequence tickerText = params.isSuccess() ? getString(R.string.cloud_uploader_notification_finished_ticker)
                : getString(R.string.cloud_uploader_notification_error_ticker);
        long when = System.currentTimeMillis();

        Intent i = (new Intent(this, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(Intent.ACTION_MAIN)
                .putExtra("upload", mCurrentUpload);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this,notificationTitle ,notificationMessage , contentIntent);
        nm.notify(UPLOAD_NOTIFY_ID, notification);
        releaseWakeLock();
    }


    public boolean isUploading() {
        return (mOggTask != null || mResizeTask != null || mUploadTask != null);
    }

    public void cancelUpload() {
        mCurrentUploadCancelled = true;

        if (mOggTask != null && !isTaskFinished(mOggTask)) {
            mOggTask.cancel(true);
            mOggTask = null;
        }

        if (mResizeTask != null && !isTaskFinished(mResizeTask)) {
            mResizeTask.cancel(true);
            mResizeTask = null;
        }

        if (mUploadTask != null && !isTaskFinished(mUploadTask)) {
            mUploadTask.cancel(true);
            mUploadTask = null;
        }

        if (mCurrentUpload != null){
            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Upload.UploadStatus.NOT_YET_UPLOADED);
            int x = getContentResolver().update(Content.RECORDINGS,cv,Recordings.ID+"="+ mCurrentUpload.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
            mCurrentUpload.upload_status = Upload.UploadStatus.NOT_YET_UPLOADED;
        }

        mCurrentUpload = null;


        nm.cancel(RECORD_NOTIFY_ID);
        gotoIdleState();
        sendBroadcast(new Intent(UPLOAD_CANCELLED));

    }

    public long getPlaybackLocalId() {
        return mPlaybackLocal == null ? 0 : Long.valueOf(mPlaybackLocal.getLastPathSegment());
    }

    public long getUploadLocalId() {
        return (mCurrentUpload != null && mCurrentUpload.upload_status == Upload.UploadStatus.NOT_YET_UPLOADED)
                ? mCurrentUpload.local_recording_id : 0;
    }

    private Notification createOngoingNotification(CharSequence tickerText, PendingIntent pendingIntent){
        int icon = R.drawable.statusbar;
        Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
        notification.contentIntent = pendingIntent;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private void acquireWakeLock() {
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            sendBroadcast(new Intent(PLAYBACK_COMPLETE));
            onPlaybackComplete();
        }
    };

    private MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            sendBroadcast(new Intent(PLAYBACK_ERROR));
            onPlaybackComplete();
            return true;
        }
    };

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still has
     * a remote reference to the stub.
     */
    static class ServiceStub extends ICloudCreateService.Stub {
        WeakReference<CloudCreateService> mService;

        public ServiceStub(CloudCreateService cloudUploaderService) {
            mService = new WeakReference<CloudCreateService>(cloudUploaderService);
        }

        @Override
        public void startRecording(String path, int mode) throws RemoteException {
            if (mService.get() != null) mService.get().startRecording(path, mode);
        }

        @Override
        public boolean isRecording() throws RemoteException {
            return mService.get() != null && mService.get().isRecording();
        }

        @Override
        public String getRecordingPath() throws RemoteException {
            return mService.get() != null ? mService.get().getRecordingPath() : null;
        }

        @Override
        public void stopRecording() throws RemoteException {
            if (mService.get() != null) mService.get().stopRecording();
        }

        @Override
        public void updateRecordTicker() throws RemoteException {
            if (mService.get() != null) mService.get().updateRecordTicker();
        }

        @Override
        public void loadPlaybackTrack(String playbackFile) {
            mService.get().loadPlaybackTrack(playbackFile);
        }

        @Override
        public boolean isPlayingBack() {
            return mService.get() != null ? mService.get().isPlaying() : null;
        }

        @Override
        public void startPlayback() {
            if (mService.get() != null) mService.get().startPlayback();
        }

        @Override
        public void pausePlayback() {
            if (mService.get() != null) mService.get().pausePlayback();
        }

        @Override
        public void stopPlayback() {
            if (mService.get() != null) mService.get().stopPlayback();
        }

        @Override
        public int getCurrentPlaybackPosition() {
            return mService.get() != null ? mService.get().getCurrentPlaybackPosition() : null;
        }

        @Override
        public int getPlaybackDuration() {
            return mService.get() != null ? mService.get().getPlaybackDuration() : null;
        }

        @Override
        public void seekTo(int position) {
            if (mService.get() != null) mService.get().seekTo(position);
        }


        @Override
        public void uploadTrack(Map trackdata) throws RemoteException {
            if (mService.get() != null) mService.get().uploadTrack(trackdata);
        }

        @Override
        public void startUpload(Upload upload) throws RemoteException {
            if (mService.get() != null) mService.get().startUpload(upload);
        }


        @Override
        public boolean isUploading() throws RemoteException {
            return mService.get() != null && mService.get().isUploading();
        }

        @Override
        public void cancelUpload() throws RemoteException {
            if (mService.get() != null) mService.get().cancelUpload();
        }

        @Override
        public String getPlaybackPath() throws RemoteException {
            return mService.get() != null ? mService.get().getCurrentPlaybackPath() : null;
        }

        @Override
        public long getUploadLocalId() throws RemoteException {
            return mService.get() != null ? mService.get().getUploadLocalId() : 0;
        }

        @Override
        public long getPlaybackLocalId() throws RemoteException {
            return mService.get() != null ? mService.get().getPlaybackLocalId() : 0;
        }

    }
    private final IBinder mBinder = new ServiceStub(this);


}

