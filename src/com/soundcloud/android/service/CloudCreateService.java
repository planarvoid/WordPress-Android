
package com.soundcloud.android.service;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.task.VorbisEncoderTask;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.utils.record.CloudRecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import static com.soundcloud.android.CloudUtils.isTaskFinished;

public class CloudCreateService extends Service {
    private static final String TAG = "CloudUploaderService";

    public static final String RECORD_ERROR     = "com.soundcloud.android.recorderror";
    public static final String UPLOAD_SUCCESS   = "com.sound.android.fileuploadsuccessful";
    public static final String UPLOAD_ERROR     = "com.sound.android.fileuploaderror";
    public static final String RECORD_STARTED   = "com.soundcloud.android.recordstarted";
    public static final String RECORD_STOPPED   = "com.soundcloud.android.recordstopped";
    public static final String UPLOAD_CANCELLED = "com.sound.android.fileuploadcancelled";
    public static final String SERVICECMD       = "com.soundcloud.android.createservicecommand";
    
    public static final String CMDNAME = "command";

    private static final int CREATE_NOTIFY_ID = R.layout.sc_create;

    private static final int UPLOAD_NOTIFY_END_ID = R.layout.sc_upload;

    private static WakeLock mWakeLock;

    private com.soundcloud.utils.record.CloudRecorder mRecorder;

    private File mRecordFile;

    private boolean mRecording = false;

    private VorbisEncoderTask mOggTask;
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

    private int mCurrentState = 0;
    
    public interface States {
        
        int IDLE_RECORDING = 0;
        
        int RECORDING = 1;
        
        int IDLE_PLAYBACK = 2;
        
        int PLAYBACK = 3;
        
        int PRE_UPLOAD = 4;
        
        int UPLOAD = 5;
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
        if (isUploading() || isRecording()) {
            // something is currently uploading so don't stop the service now.
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // init the service here
        _startService();

        // get notification manager
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
    }

    private void _startService() {
        Log.i(getClass().getSimpleName(), "upload Service Started started!!!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (isUploading()) {
            Log.e(getClass().getSimpleName(), "Service being destroyed while still playing.");
        }

        _shutdownService();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        mWakeLock = null;
        super.onDestroy();
    }

    private void gotoIdleState() {
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


    private void startRecording(String path) {
        acquireWakeLock();

        mRecordFile = new File(path);
        frameCount = 0;

        mRecorder = new CloudRecorder(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultRecordingQuality", "high").contentEquals("high"),
                MediaRecorder.AudioSource.MIC, ScCreate.REC_SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mRecorder.setRecordService(this);
        mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        mRecorder.prepare();
        mRecorder.start();

        Log.i(TAG,"RECORD GET STATE " + mRecorder.getState());
        
        if (mRecorder.getState() == CloudRecorder.State.ERROR) {
            notifyChange(RECORD_ERROR);
            return;
        }

        mNotification = new Notification(R.drawable.statusbar, getApplicationContext()
                .getResources().getString(R.string.cloud_recorder_notification_ticker), System
                .currentTimeMillis());

        Intent i = (new Intent(this, Dashboard.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("tabIndex", Dashboard.TabIndexes.TAB_RECORD);

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

    }

    private int frameCount;

    public void onRecordFrameUpdate(float maxAmplitude) {
        ((SoundCloudApplication) this.getApplication()).onFrameUpdate(maxAmplitude);
        // this should happen every second
        if (frameCount++ % (1000 / CloudRecorder.TIMER_INTERVAL)  == 0) updateRecordTicker();
    }

    private void stopRecording() {

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mRecording = false;

        nm.cancel(CREATE_NOTIFY_ID);
        gotoIdleState();
    }

    private Boolean isRecording() {
        return mRecording;
    }

    private void updateRecordTicker() {
        mNotification.setLatestEventInfo(getApplicationContext(), mCreateEventTitle, CloudUtils
                .formatString(mCreateEventMessage, CloudUtils.getPCMTime(mRecordFile,
                        ScCreate.REC_SAMPLE_RATE, ScCreate.REC_CHANNELS,
                        ScCreate.REC_BITS_PER_SAMPLE)), mPendingIntent);

        nm.notify(CREATE_NOTIFY_ID, mNotification);
    }

    @SuppressWarnings("unchecked")
    private void uploadTrack(final Map<String,String> trackdata) {
        acquireWakeLock();

        mCurrentUploadCancelled = false;
        int icon = R.drawable.statusbar;
        CharSequence tickerText = getString(R.string.cloud_uploader_notification_ticker);
        mNotification = new Notification(icon, tickerText, System.currentTimeMillis());

        Intent i = (new Intent(this, Dashboard.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setAction(Intent.ACTION_MAIN);

        notificationView = new RemoteViews(getPackageName(), R.layout.status_upload);

        CharSequence trackText = trackdata.get("track[title]");
        notificationView.setTextViewText(R.id.message, trackText);
        notificationView.setTextViewText(R.id.percentage, "0");
        notificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mNotification.contentView = notificationView;
        mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

        startForeground(CREATE_NOTIFY_ID, mNotification);

        mOggTask = new EncodeOggTask();
        mOggTask.execute(new UploadTask.Params[]{new UploadTask.Params(trackdata)});
    }

    private class EncodeOggTask extends VorbisEncoderTask<UploadTask.Params, UploadTask.Params> {
        private String eventString;

        @Override
        protected void onPreExecute() {
            eventString = getString(R.string.cloud_uploader_event_encoding);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected UploadTask.Params doInBackground(UploadTask.Params... params) {
            UploadTask.Params param = params[0];
            if (!encode(param.trackFile, param.encodedFile)) param.fail();
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
                if (param.trackFile.delete()) Log.v(TAG, "deleted file " + param.trackFile);

                mUploadTask = new UploadOggTask((CloudAPI) getApplication());

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

                if (sampleSize > 1) {
                    Log.v(TAG, "resizing " + param.artworkFile);
                    InputStream is = new FileInputStream(param.artworkFile);

                    options = new BitmapFactory.Options();
                    options.inSampleSize = sampleSize;
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                    is.close();

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

    private class UploadOggTask extends UploadTask {
        private String eventString;

        public UploadOggTask(CloudAPI api) {
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
        mNotification = new Notification(icon, tickerText, when);
        mNotification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

        Intent i = (new Intent(this, Dashboard.class))
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setAction(Intent.ACTION_MAIN);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // the next two lines initialize the Notification, using the
        // configurations above
        Notification notification = new Notification(icon, tickerText, when);
        if (params.isSuccess()) {
            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_finished_title), String.format(
                            getString(R.string.cloud_uploader_notification_finished_message),
                            params.get("track[title]")), contentIntent);

            notifyChange(UPLOAD_SUCCESS);

        } else {
            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_error_title), String.format(
                            getString(R.string.cloud_uploader_notification_error_message),
                            params.get("track[title]")), contentIntent);

            notifyChange(UPLOAD_ERROR);
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

        nm.cancel(CREATE_NOTIFY_ID);
        gotoIdleState();
        notifyChange(UPLOAD_CANCELLED);
    }
    
    public void setCurrentState(int currentState) {
        mCurrentState = currentState;
    }
    
    public int getCurrentState() {
        return mCurrentState;
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
        public void startRecording(String path) throws RemoteException {
            if (mService.get() != null)
                mService.get().startRecording(path);
        }

        @Override
        public boolean isRecording() throws RemoteException {
            if (mService.get() != null)
                return mService.get().isRecording();
            return false;
        }

        @Override
        public void stopRecording() throws RemoteException {
            if (mService.get() != null)
                mService.get().stopRecording();
        }

        @Override
        public void updateRecordTicker() throws RemoteException {
            if (mService.get() != null)
                mService.get().updateRecordTicker();
        }

        @Override
        public void uploadTrack(Map trackdata) throws RemoteException {
            if (mService.get() != null)
                mService.get().uploadTrack(trackdata);
        }

        @Override
        public boolean isUploading() throws RemoteException {
            if (mService.get() != null)
                return mService.get().isUploading();
            return false;
        }

        @Override
        public void cancelUpload() throws RemoteException {
            if (mService.get() != null)
                mService.get().cancelUpload();
        }
        
        @Override
        public int getCurrentState() throws RemoteException {
            if (mService.get() != null)
                return mService.get().getCurrentState();
            
            return 0;
        }

        @Override
        public void setCurrentState(int newState) throws RemoteException {
            if (mService.get() != null)
                mService.get().setCurrentState(newState);
        }

    }
    private final IBinder mBinder = new ServiceStub(this);
}

