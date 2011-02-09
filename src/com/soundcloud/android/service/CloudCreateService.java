
package com.soundcloud.android.service;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.task.VorbisEncoderTask;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.utils.record.CloudRecorder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class CloudCreateService extends Service {

    // public static final String ServletUri = "http://" +
    // AppUtils.EmulatorLocalhost + ":8080/Ping";
    private static final String TAG = "CloudUploaderService";

    private static ArrayBlockingQueue<Track> _uploadlist = new ArrayBlockingQueue<Track>(20);

    public static final String RECORD_STARTED = "com.soundcloud.android.recordstarted";
    
    public static final String RECORD_ERROR = "com.soundcloud.android.recorderror";
    
    public static final String RECORD_STOPPED = "com.soundcloud.android.recordstopped";

    public static final String UPLOAD_SUCCESS = "com.sound.android.fileuploadsuccessful";

    public static final String UPLOAD_ERROR = "com.sound.android.fileuploaderror";

    public static final String UPLOAD_CANCELLED = "com.sound.android.fileuploadcancelled";
    
    public static final String SERVICECMD = "com.soundcloud.android.createservicecommand";
    
    public static final String CMDNAME = "command";

    private static final int CREATE_NOTIFY_ID = R.layout.sc_create;

    private static final int UPLOAD_NOTIFY_END_ID = R.layout.sc_upload;

    private static PowerManager mPowerManager;

    private static WakeLock mWakeLock;

    private com.soundcloud.utils.record.CloudRecorder mRecorder;

    private File mRecordFile;

    private Boolean mRecording = false;

    // private WavEncoderTask mWavTask;
    private VorbisEncoderTask mOggTask;

    private ImageResizeTask mResizeTask;

    private UploadTask mUploadTask;

    private PendingIntent mPendingIntent;

    private RemoteViews notificationView;

    private Notification mNotification;

    private String mCreateEventTitle;

    private String mCreateEventMessage;

    private HashMap<String, String> mUploadingData;

    private int _lastuploadPercentage;

    private NotificationManager nm;

    private DBAdapter db;

    private int mServiceStartId = -1;

    private boolean mServiceInUse = false;

    private boolean mResumeAfterCall = false;

    private boolean mIsSupposedToBePlaying = false;

    private boolean mQuietMode = false;

    private String mOggFilePath;

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

    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    // lifecycle methods
    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    @Override
    public IBinder onBind(Intent intent) {
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

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
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);

        // mRecorder.setInputBlockSize(16);
        // mRecorder.setSampleRate(ScCreate.REC_SAMPLE_RATE);
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
        Log.i(getClass().getSimpleName(), "upload Service stopped!!!");

        releaseWakeLock();

        if (mOggTask != null)
            mOggTask.cancel(true);

        if (mUploadTask != null)
            mUploadTask.cancel(true);

        Log.i(getClass().getSimpleName(), "upload Service shutdown complete.");

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

        Intent i = new Intent(what);
        sendBroadcast(i);

    }

    @SuppressWarnings("unchecked")
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

        if (mRecorder.getState() == CloudRecorder.State.ERROR) {
            notifyChange(RECORD_ERROR);
            return;
        }

        mNotification = new Notification(R.drawable.statusbar, getApplicationContext()
                .getResources().getString(R.string.cloud_recorder_notification_ticker), System
                .currentTimeMillis());

        Intent i = new Intent(this, Dashboard.class);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        // i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("tabIndex", Dashboard.TabIndexes.TAB_RECORD);
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
        if (frameCount % 10 == 0) // this should happen every second, as the
            // frame updates are every 100 ms
            updateRecordTicker();
        frameCount++;

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
    private void startUpload(Map trackdata) {

        acquireWakeLock();

        mCurrentUploadCancelled = false;

        /*
         * Iterator it = trackdata.entrySet().iterator(); while (it.hasNext()) {
         * Map.Entry pairs = (Map.Entry)it.next();
         * System.out.println(pairs.getKey() + " = " + pairs.getValue()); }
         */

        mUploadingData = (HashMap<String, String>) trackdata;

        int icon = R.drawable.statusbar;
        CharSequence tickerText = getString(R.string.cloud_uploader_notification_ticker);
        long when = System.currentTimeMillis();
        mNotification = new Notification(icon, tickerText, when);

        Intent i = new Intent(this, Dashboard.class);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction(Intent.ACTION_MAIN);

        notificationView = new RemoteViews(getPackageName(), R.layout.status_upload);

        // CharSequence titleText =
        // getString(R.string.cloud_uploader_event_title_track);
        CharSequence trackText = mUploadingData.get("track[title]").toString();
        notificationView.setTextViewText(R.id.message, trackText);
        notificationView.setTextViewText(R.id.percentage, "0");
        notificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mNotification.contentView = notificationView;
        mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

        startForeground(CREATE_NOTIFY_ID, mNotification);

        mOggFilePath = CloudUtils.getCacheDirPath(this) + "/"
                + mUploadingData.get("ogg_filename").toString();
        ;
        mOggTask = new EncodeOggTask();
        mOggTask.execute(mUploadingData.get("pcm_path"), mOggFilePath);

    }

    private class EncodeOggTask extends VorbisEncoderTask {

        private String eventString;

        @Override
        protected void onPreExecute() {
            eventString = getApplicationContext().getString(R.string.cloud_uploader_event_encoding);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            // Log.i(TAG, "Progress " + progress[0] + " " + progress[1] + " " +
            // isCancelled());

            if (isCancelled())
                return;

            notificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                    100, (100 * progress[0]) / progress[1])));
            nm.notify(CREATE_NOTIFY_ID, mNotification);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isCancelled())
                result = false;

            onOggEncodeComplete(result);
        }
    }

    public void onOggEncodeComplete(Boolean result) {
        mOggTask = null;

        if (result && !mCurrentUploadCancelled) {

            File pcmFile = new File(mUploadingData.get("pcm_path"));
            pcmFile.delete();

            if (!TextUtils.isEmpty(mUploadingData.get("artwork_path"))) {

                Options opt;
                try {
                    opt = CloudUtils.determineResizeOptions(mUploadingData.get("artwork_path"),
                            500, 500, true);
                    if (opt.inSampleSize > 1) {
                        mResizeTask = new ImageResizeTask();
                        mResizeTask.execute(opt.inSampleSize);
                        return;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            startUpload();

        } else {
            if (!mCurrentUploadCancelled) {
                notifyUploadCurrentUploadFinished(result);
            }
        }
    }

    public class ImageResizeTask extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled())
                return;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isCancelled())
                result = false;
            onImageResizeComplete(result);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = params[0];
                InputStream is = null;
                is = new FileInputStream(mUploadingData.get("artwork_path"));

                Bitmap newBitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();

                FileOutputStream out = new FileOutputStream(CloudUtils
                        .getCacheDirPath(CloudCreateService.this)
                        + "/upload_tmp.png");
                newBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                CloudUtils.clearBitmap(newBitmap);
                mUploadingData.put("artwork_path", CloudUtils
                        .getCacheDirPath(CloudCreateService.this)
                        + "/upload_tmp.png");

                return true;

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return false;
        }

    }

    public void onImageResizeComplete(Boolean result) {
        mResizeTask = null;

        if (result && !mCurrentUploadCancelled) {
            startUpload();
        } else {
            if (!mCurrentUploadCancelled) {
                notifyUploadCurrentUploadFinished(result);
            }
        }
    }

    private void startUpload() {
        final List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();

        Iterator it = mUploadingData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (!(pairs.getKey().toString().contentEquals("pcm_path") || pairs.getKey().toString()
                    .contentEquals("image_path")))
                params.add(new BasicNameValuePair(pairs.getKey().toString(), pairs.getValue()
                        .toString()));

        }

        mUploadTask = new UploadOggTask();
        mUploadTask.trackFile = new File(mOggFilePath);
        mUploadTask.trackParams = params;
        mUploadTask.scApplication = (SoundCloudApplication) this.getApplication();
        if (!TextUtils.isEmpty(mUploadingData.get("artwork_path")))
            mUploadTask.artworkFile = new File(mUploadingData.get("artwork_path"));
        mUploadTask.execute();
    }

    private class UploadOggTask extends UploadTask {

        private String eventString;

        @Override
        protected void onPreExecute() {
            eventString = getApplicationContext()
                    .getString(R.string.cloud_uploader_event_uploading);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled())
                return;
            notificationView.setProgressBar(R.id.progress_bar, progress[1], Math.min(progress[1],
                    progress[0]), false);
            notificationView.setTextViewText(R.id.percentage, String.format(eventString, Math.min(
                    100, (100 * progress[0]) / progress[1])));
            nm.notify(CREATE_NOTIFY_ID, mNotification);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isCancelled())
                result = false;
            onOggUploadComplete(result);
        }
    }

    public void onOggUploadComplete(Boolean result) {
        mUploadTask = null;
        if (!mCurrentUploadCancelled) {
            notifyUploadCurrentUploadFinished(result);
        }
    }

    private void notifyUploadCurrentUploadFinished(boolean success) {

        nm.cancel(CREATE_NOTIFY_ID);

        gotoIdleState();

        int icon = R.drawable.statusbar;
        CharSequence tickerText = success ? getString(R.string.cloud_uploader_notification_finished_ticker)
                : getString(R.string.cloud_uploader_notification_error_ticker);
        long when = System.currentTimeMillis();
        mNotification = new Notification(icon, tickerText, when);
        mNotification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

        Intent i = new Intent(this, Dashboard.class);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction(Intent.ACTION_MAIN);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // the next two lines initialize the Notification, using the
        // configurations above
        Notification notification = new Notification(icon, tickerText, when);
        if (success) {
            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_finished_title), String.format(
                            getString(R.string.cloud_uploader_notification_finished_message),
                            mUploadingData.get("track[title]").toString()), contentIntent);

            notifyChange(UPLOAD_SUCCESS);

        } else {
            notification.setLatestEventInfo(this,
                    getString(R.string.cloud_uploader_notification_error_title), String.format(
                            getString(R.string.cloud_uploader_notification_error_message),
                            mUploadingData.get("track[title]").toString()), contentIntent);

            notifyChange(UPLOAD_ERROR);
        }

        nm.notify(UPLOAD_NOTIFY_END_ID, notification);
        releaseWakeLock();
    }

    private void cleanUp() {
        File tmp = new File(mOggFilePath);
        if (tmp.exists()) {
            tmp.delete();
            tmp = null;
        }

        tmp = new File(mUploadingData.get("artwork_path"));
        if (tmp.exists()) {
            tmp.delete();
            tmp = null;
        }
    }

    public Boolean isUploading() {
        return (mOggTask != null || mResizeTask != null || mUploadTask != null);
    }

    public void cancelUpload() {
        if (mOggTask != null && !CloudUtils.isTaskFinished(mOggTask)) {
            mOggTask.cancel(true);
            mOggTask = null;
        }

        if (mResizeTask != null && !CloudUtils.isTaskFinished(mResizeTask)) {
            mResizeTask.cancel(true);
            mResizeTask = null;
        }

        if (mUploadTask != null && !CloudUtils.isTaskFinished(mUploadTask)) {
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
                mService.get().startUpload(trackdata);
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

    

}// end class MyService
