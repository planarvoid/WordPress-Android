
package com.soundcloud.android.service;

import static com.soundcloud.android.utils.CloudUtils.isTaskFinished;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB.Recordings;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.task.OggEncoderTask;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.utils.CloudUtils;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;

public class CloudCreateService extends Service {
    private static final String TAG = "CloudUploaderService";

    public static final String RECORD_ERROR      = "com.soundcloud.android.recorderror";
    public static final String UPLOAD_SUCCESS    = "com.sound.android.fileuploadsuccessful";
    public static final String UPLOAD_ERROR      = "com.sound.android.fileuploaderror";
    public static final String RECORD_STARTED    = "com.soundcloud.android.recordstarted";
    public static final String RECORD_STOPPED    = "com.soundcloud.android.recordstopped";
    public static final String UPLOAD_CANCELLED  = "com.sound.android.fileuploadcancelled";
    public static final String SERVICECMD        = "com.soundcloud.android.createservicecommand";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR    = "com.soundcloud.android.playbackerror";

    public static final String CMDNAME = "command";

    private static final int CREATE_NOTIFY_ID = R.layout.sc_create;

    private static final int UPLOAD_NOTIFY_END_ID = R.layout.sc_upload;

    private static WakeLock mWakeLock;

    private CloudRecorder mRecorder;

    private File mRecordFile;

    private boolean mRecording = false;

    private OggEncoderTask mOggTask;
    private ImageResizeTask mResizeTask;
    private UploadTask mUploadTask;

    private PendingIntent mPendingIntent;
    private RemoteViews notificationView;

    private Notification mNotification;

    private String mCreateEventTitle;
    private String mCreateEventMessage;

    private NotificationManager nm;

    private int mServiceStartId = -1;

    private boolean mCurrentUploadCancelled = false;

    private long mRecordStartTime;

    private int mCurrentState = 0;
    private int frameCount;

    private MediaPlayer mPlayer;
    private String mPlaybackPath;

    private long mUploadLocalId;


    public interface States {
        int IDLE_RECORDING = 0;
    }


    protected void acquireWakeLock() {
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    protected void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }


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

        if (mUploadLocalId != 0){
            Log.e(TAG, "Service being destroyed while still playing.");
            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Recording.UploadStatus.NOT_YET_UPLOADED);
            cv.put(Recordings.UPLOAD_ERROR, true);
            int x = getContentResolver().update(Recordings.CONTENT_URI,cv,Recordings.ID+"="+mUploadLocalId, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
        }

        _shutdownService();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        mWakeLock = null;
        super.onDestroy();
    }

    private void gotoIdleState() {
        mUploadLocalId = 0;
        stopForeground(true);
    }

    private void _shutdownService() {
        Log.i(TAG, "upload Service stopped!!!");

        releaseWakeLock();

        if (mOggTask != null)    mOggTask.cancel(true);
        if (mUploadTask != null) mUploadTask.cancel(true);
        if (mResizeTask != null) mResizeTask.cancel(true);

        Log.i(TAG, "upload Service shutdown complete.");
    }

    /**
     * Notify the change-receivers that something has changed. The intent that
     * is sent contains the following data for the currently playing track: "id"
     * - Integer: the database row ID "artist" - String: the name of the artist
     * "album" - String: the name of the album "track" - String: the name of the
     * track The intent has an action that is one of
     * "com.dubmoon.overcast.metachanged" "com.dubmoon.overcast.queuechanged",
     * "com.dubmoon.overcast.playbackcomplete"
     * "com.dubmoon.overcast.playstatechanged" respectively indicating that a
     * new track has started playing, that the playback queue has changed, that
     * playback has stopped because the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {
        sendBroadcast(new Intent(what));
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

                if (mRecorder.getState() == CloudRecorder.State.ERROR) onRecordError();
            }
        };


        mNotification = new Notification(R.drawable.statusbar, getApplicationContext()
                .getResources().getString(R.string.cloud_recorder_notification_ticker), System
                .currentTimeMillis());

        Intent i = (new Intent(this, Main.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("tabTag", "record");

        mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

        mCreateEventTitle = getApplicationContext().getString(R.string.cloud_recorder_event_title);
        mCreateEventMessage = getApplicationContext().getString(
                R.string.cloud_recorder_event_message);
        mNotification.setLatestEventInfo(getApplicationContext(), mCreateEventTitle, CloudUtils
                .formatString(mCreateEventMessage, 0), mPendingIntent);

        startForeground(CREATE_NOTIFY_ID, mNotification);
        mRecording = true;
        mRecordStartTime = System.currentTimeMillis();

        t.start();
    }

    public String getRecordingPath() {
        return mRecordFile.getAbsolutePath();
    }

    private void onRecordError(){
        notifyChange(RECORD_ERROR);

        //alread in an error state, so just call these in case
        mRecorder.stop();
        mRecorder.release();
        mRecording = false;

        nm.cancel(CREATE_NOTIFY_ID);
        gotoIdleState();

    }


    public void onRecordFrameUpdate(float maxAmplitude) {
        ((SoundCloudApplication) this.getApplication()).onFrameUpdate(maxAmplitude, System.currentTimeMillis() - mRecordStartTime);
        // this should happen every second
        if (frameCount++ % (1000 / CloudRecorder.TIMER_INTERVAL)  == 0) updateRecordTicker();
    }

    private void stopRecording() {

        mRecorder.stop();
        mRecorder.release();
        mRecording = false;

        nm.cancel(CREATE_NOTIFY_ID);
        gotoIdleState();
    }

    private boolean isRecording() {
        return mRecording;
    }

    private void updateRecordTicker() {
        mNotification.setLatestEventInfo(getApplicationContext(), mCreateEventTitle, CloudUtils
                .formatString(mCreateEventMessage, CloudUtils.getPCMTime(mRecordFile,
                        ScCreate.REC_SAMPLE_RATE, ScCreate.PCM_REC_CHANNELS,
                        ScCreate.PCM_REC_BITS_PER_SAMPLE)), mPendingIntent);

        nm.notify(CREATE_NOTIFY_ID, mNotification);
    }


    public void loadPlaybackTrack(String playbackPath) {
        mPlayer.reset();
        mPlaybackPath = playbackPath;
        try {
            FileInputStream fis = new FileInputStream(playbackPath);
            mPlayer.setDataSource(fis.getFD());
            fis.close();
            mPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            notifyChange(PLAYBACK_ERROR);
        }
    }

    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            notifyChange(PLAYBACK_COMPLETE);
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            notifyChange(PLAYBACK_ERROR);
            return true;
        }
    };

    public void stopPlayback() {
        mPlaybackPath = null;
        try{
            mPlayer.stop();
        } catch (IllegalStateException e){
            Log.e(TAG,"error " + e.toString());
        }
    }

    public void pausePlayback() {
       mPlayer.pause();
    }

    public void startPlayback() {
        mPlayer.start();
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
        return mPlaybackPath;
    }

    public int getCurrentPlaybackPosition() {
        return mPlayer.getCurrentPosition();
    }

    @SuppressWarnings("unchecked")
    private void uploadTrack(final Map<String,?> trackdata) {
        acquireWakeLock();

        mCurrentUploadCancelled = false;
        int icon = R.drawable.statusbar;
        CharSequence tickerText = getString(R.string.cloud_uploader_notification_ticker);
        mNotification = new Notification(icon, tickerText, System.currentTimeMillis());

        Intent i = (new Intent(this, Main.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setAction(Intent.ACTION_MAIN);

        notificationView = new RemoteViews(getPackageName(), R.layout.status_upload);

        CharSequence trackText = (CharSequence) trackdata.get("track[title]");
        notificationView.setTextViewText(R.id.message, trackText);
        notificationView.setTextViewText(R.id.percentage, "0");
        notificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mNotification.contentView = notificationView;
        mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

        if (!TextUtils.isEmpty(mPlaybackPath) && mPlaybackPath.contentEquals(String.valueOf(trackdata.get(UploadTask.Params.SOURCE_PATH))))
            stopPlayback();

        startForeground(CREATE_NOTIFY_ID, mNotification);
        mUploadLocalId = (Long) trackdata.get(UploadTask.Params.LOCAL_RECORDING_ID);
        mOggTask = new EncodeOggTask();
        mOggTask.execute(new UploadTask.Params[] { new UploadTask.Params(trackdata) });
    }

    private class EncodeOggTask extends OggEncoderTask<UploadTask.Params, UploadTask.Params> {
        private String eventString;

        @Override
        protected void onPreExecute() {
            eventString = getString(R.string.cloud_uploader_event_encoding);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
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
                notificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
                notificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                        100, (100 * progress[0]) / progress[1])));
                nm.notify(CREATE_NOTIFY_ID, mNotification);
            }
        }

        @Override
        protected void onPostExecute(UploadTask.Params param) {
            mOggTask = null;
            if (!isCancelled() && !mCurrentUploadCancelled && param.isSuccess()) {


                if (param.encode && param.trackFile.exists()) {
                    //in case upload doesn't finish, maintain the timestamp (unnecessary now but might be if we change titling)
                    param.encodedFile.setLastModified(param.trackFile.lastModified());
                    param.trackFile.delete();

                    ContentValues cv = new ContentValues();
                    cv.put(Recordings.AUDIO_PATH, param.encodedFile.getAbsolutePath());
                    cv.put(Recordings.AUDIO_PROFILE, Profile.ENCODED_HIGH);
                    int x = getContentResolver().update(Recordings.CONTENT_URI,cv,Recordings.ID+"="+param.local_recording_id, null);
                    Log.d(TAG, x+" row(s) audio path updated.");
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

    public class ImageResizeTask extends AsyncTask<UploadTask.Params, Integer, UploadTask.Params> {
        static final int RECOMMENDED_SIZE = 500;
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
                final long start = System.currentTimeMillis();
                BitmapFactory.Options options =
                        CloudUtils.determineResizeOptions(param.artworkFile,
                            RECOMMENDED_SIZE, RECOMMENDED_SIZE);

                int sampleSize = options.inSampleSize;
                int degree = 0;
                    ExifInterface exif = new ExifInterface(param.artworkFile.getAbsolutePath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                    if (orientation != -1) {
                        // We only recognize a subset of orientation tag values.
                        switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            degree = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            degree = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            degree = 270;
                            break;
                        default:
                            degree = 0;
                            break;
                        }
                    }


                if (sampleSize > 1 || degree > 0) {
                    InputStream is = new FileInputStream(param.artworkFile);

                    options = new BitmapFactory.Options();
                    options.inSampleSize = sampleSize;
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                    is.close();

                    if (degree != 0){
                        Bitmap preRotate = bitmap;
                        Matrix mat = new Matrix();
                        mat.postRotate(degree);
                        bitmap = Bitmap.createBitmap(preRotate, 0, 0, preRotate.getWidth(), preRotate.getHeight(), mat, true);
                        preRotate.recycle();
                    }

                    if (bitmap == null) throw new IOException("error decoding bitmap (bitmap == null)");

                    File resized = CloudUtils.getCacheFile(CloudCreateService.this, "upload_tmp.png");
                    FileOutputStream out = new FileOutputStream(resized);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

                    out.close();
                    CloudUtils.clearBitmap(bitmap);
                    param.resizedFile = resized;

                    Log.v(TAG, String.format("resized image in %d ms", System.currentTimeMillis() - start));
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
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected void onProgressUpdate(Long... progress) {
            if (!isCancelled()) {
                notificationView.setProgressBar(R.id.progress_bar,
                        progress[1].intValue(), (int) Math.min(progress[1], progress[0]),
                        false);

                notificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                        100, (100 * progress[0]) / progress[1])));

                nm.notify(CREATE_NOTIFY_ID, mNotification);
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
        nm.cancel(CREATE_NOTIFY_ID);

        gotoIdleState();

        int icon = R.drawable.statusbar;
        CharSequence tickerText = params.isSuccess() ? getString(R.string.cloud_uploader_notification_finished_ticker)
                : getString(R.string.cloud_uploader_notification_error_ticker);
        long when = System.currentTimeMillis();

        Intent i = (new Intent(this, Main.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setAction(Intent.ACTION_MAIN);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // the next two lines initialize the Notification, using the
        // configurations above
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

        if (params.isSuccess()) {
            if (params.trackFile != null && params.trackFile.exists()) params.trackFile.delete();
            if (params.encodedFile != null && params.encodedFile.exists()) params.encodedFile.delete();

            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_finished_title), String.format(
                            getString(R.string.cloud_uploader_notification_finished_message),
                            params.get("track[title]")), contentIntent);

            notifyChange(UPLOAD_SUCCESS);

            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Recording.UploadStatus.UPLOADED);
            int x = getContentResolver().update(Recordings.CONTENT_URI,cv,Recordings.ID+"="+params.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked as uploaded.");

        } else {
            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_error_title), String.format(
                            getString(R.string.cloud_uploader_notification_error_message),
                            params.get("track[title]")), contentIntent);

            notifyChange(UPLOAD_ERROR);

            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_ERROR, true);
            cv.put(Recordings.UPLOAD_STATUS, Recording.UploadStatus.NOT_YET_UPLOADED);
            int x = getContentResolver().update(Recordings.CONTENT_URI,cv,Recordings.ID+"="+params.local_recording_id, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
        }

        nm.notify(UPLOAD_NOTIFY_END_ID, notification);
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

        if (mUploadLocalId != 0){
            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, Recording.UploadStatus.NOT_YET_UPLOADED);
            int x = getContentResolver().update(Recordings.CONTENT_URI,cv,Recordings.ID+"="+mUploadLocalId, null);
            Log.d(TAG, x+" row(s) marked with upload error.");
        }


        nm.cancel(CREATE_NOTIFY_ID);
        gotoIdleState();
        notifyChange(UPLOAD_CANCELLED);

    }

    public long getUploadLocalId() {
        return mUploadLocalId;
    }

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

    }
    private final IBinder mBinder = new ServiceStub(this);

}

