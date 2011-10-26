package com.soundcloud.android.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScUpload;
import com.soundcloud.android.activity.tour.Record;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder;
import com.soundcloud.android.utils.record.PowerGauge;
import com.soundcloud.android.utils.record.RemainingTimeCalculator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.CloudUtils.mkdirs;
import static com.soundcloud.android.utils.CloudUtils.showToast;

public class CreateController {

    private ScActivity mActivity;
    private ICloudCreateService mCreateService;
    private Recording mRecording;
    private User mPrivateUser;

    private TextView txtInstructions, txtRecordStatus, mChrono;
    private ViewGroup mFileLayout;
    private PowerGauge mPowerGauge;
    private SeekBar mProgressBar;
    private ImageButton btnAction;
    private File mRecordFile, mRecordDir;
    private CreateState mLastState, mCurrentState;
    private long mLastDisplayedTime,mDuration, mLastSeekEventTime;
    private int mAudioProfile;
    private String mRecordErrorMessage, mCurrentDurationString;
    private boolean mSampleInterrupted, mActive;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private Thread mProgressThread;
    private List<Recording> mUnsavedRecordings;

    private CreateListener mCreateListener;

    private Drawable
            btn_rec_states_drawable,
            btn_rec_stop_states_drawable,
            btn_rec_play_states_drawable;

    public enum CreateState {
        IDLE_STANDBY_REC, IDLE_STANDBY_PLAY, IDLE_RECORD, RECORD, IDLE_PLAYBACK, PLAYBACK
    }

    public static int REC_SAMPLE_RATE = 44100;
    public static int PCM_REC_CHANNELS = 1;
    public static int PCM_REC_BITS_PER_SAMPLE = 16;
    public static int PCM_REC_MAX_FILE_SIZE = -1;

    public CreateController(ScActivity c, ViewGroup vg, Uri recordingUri) {
        this(c,vg,recordingUri, null);
    }

    public CreateController(ScActivity c, ViewGroup vg, Uri recordingUri, User privateUser) {
        this(c, vg, recordingUri == null ? null : Recording.fromUri(recordingUri, c.getContentResolver()), privateUser);
        if (recordingUri != null && mRecording == null){
            mActivity.showToast(R.string.error_getting_recording);
        }
    }

    public CreateController(ScActivity c, ViewGroup vg, Recording recording, User privateUser) {

        mActivity = c;
        mRecording = recording;
        mPrivateUser = privateUser;

        btn_rec_states_drawable = c.getResources().getDrawable(R.drawable.btn_rec_states);
        btn_rec_stop_states_drawable = c.getResources().getDrawable(R.drawable.btn_rec_stop_states);
        btn_rec_play_states_drawable = c.getResources().getDrawable(R.drawable.btn_rec_play_states);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);

        txtInstructions = (TextView) vg.findViewById(R.id.txt_instructions);

        mProgressBar = (SeekBar) vg.findViewById(R.id.progress_bar);
        mProgressBar.setOnSeekBarChangeListener(mSeekListener);

        txtRecordStatus = (TextView) vg.findViewById(R.id.txt_record_status);

        mChrono = (TextView) vg.findViewById(R.id.chronometer);
        mChrono.setVisibility(View.GONE);

        RelativeLayout mProgressFrame = (RelativeLayout) vg.findViewById(R.id.progress_frame);
        mProgressFrame.setVisibility(View.GONE);

        mFileLayout = (ViewGroup) vg.findViewById(R.id.file_layout);

        btnAction = (ImageButton) vg.findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAction();
            }
        });

        ((Button) vg.findViewById(R.id.btn_reset)).setText(c.getString(mRecording == null ? R.string.reset : R.string.delete));
        vg.findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecording == null)
                    mActivity.showDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                else {
                    mActivity.showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
                }
            }
        });

        ((Button) vg.findViewById(R.id.btn_save)).setText(c.getString(mRecording == null ? R.string.btn_save : R.string.btn_next));
        vg.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecording == null){
                    Recording r = new Recording(mRecordFile);
                    r.audio_profile = mAudioProfile;
                    r.user_id = mActivity.getCurrentUserId();
                    if (mPrivateUser != null){
                        SoundCloudDB.writeUser(mActivity.getContentResolver(),mPrivateUser, SoundCloudDB.WriteState.all,mActivity.getCurrentUserId());
                        r.private_user_id = mPrivateUser.id;
                        r.is_private = true;
                    }

                    try { // set duration because ogg files report incorrect
                          // duration in mediaplayer if playback is attempted
                          // after encoding
                        r.duration = mActivity.getCreateService().getPlaybackDuration();
                    } catch (RemoteException ignored) {
                    }

                    Uri newRecordingUri = mActivity.getContentResolver().insert(DatabaseHelper.Content.RECORDINGS, r.buildContentValues());
                    mRecording = r;
                    mRecording.id = Long.parseLong(newRecordingUri.getLastPathSegment());

                    if (mCreateListener != null){
                        mCreateListener.onSave(newRecordingUri, mRecording, true);
                    }
                } else {
                    //start for result, because if an upload starts, finish, playback should not longer be possible
                    if (mCreateListener != null){
                        mCreateListener.onSave(mRecording.toUri(), mRecording, false);
                    }
                }
            }
        });

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mPowerGauge = new PowerGauge(mActivity);
        ((FrameLayout) vg.findViewById(R.id.gauge_holder)).addView(mPowerGauge);

        mCurrentState = CreateState.IDLE_RECORD;
        mRecordDir = CloudUtils.ensureUpdatedDirectory(
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".rec"));
        mkdirs(mRecordDir);
        mRecordErrorMessage = "";

        updateUi(false);
    }

    public void reset() {
        mRecordFile = null;
        mRecording = null;
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }

    public void setInstructionsText(String s){
        txtInstructions.setText(s);
    }

    public void onCreateServiceBound(ICloudCreateService createService) {
        mCreateService = createService;
        if (mActive) configureState();

    }

    public void setListener(CreateListener listener){
        mCreateListener = listener;
    }

    public void setRecording(Recording recording){
        mRecording = recording;
        configureState();
    }

    private void configureState(){
        if (mCreateService == null) return;

        boolean takeAction = false;
        try {

            long recordingId = 0;
            if (mRecording != null) {
                recordingId = mRecording.id;
                setRecordFile(mRecording.audio_path);
                mAudioProfile = mRecording.audio_profile;
                mDuration = mRecording.duration;
            }

            if (mCreateService.isRecording() && mRecording == null){
                if (shouldReactToRecording()) {
                    mCurrentState = CreateState.RECORD;
                    setRecordFile(new File(mCreateService.getRecordingPath()));
                    mActivity.getApp().setRecordListener(recListener);
                    mActivity.setRequestedOrientation(mActivity.getResources().getConfiguration().orientation);
                } else {
                    mCurrentState = CreateState.IDLE_STANDBY_REC;
                }
            } else if ( mCreateService.isPlayingBack()) {
                //if (recordingId == mCreateService.getPlaybackLocalId())
                if (shouldReactToPlayback()) {
                    mCurrentState = CreateState.PLAYBACK;
                    setRecordFile(new File(mCreateService.getPlaybackPath()));
                    configurePlaybackInfo();
                    startProgressThread();
                    takeAction = true;
                } else {
                    mCurrentState = CreateState.IDLE_STANDBY_PLAY;
                }
            } else if (!mRecordDir.exists()) {
                // can happen when there's no mounted sd card
                btnAction.setEnabled(false);
            } else {

                if (mRecordFile == null && mPrivateUser != null) {
                    checkForUnusedPrivateFile();
                }

                if (mRecordFile != null) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    loadPlaybackTrack();
                } else {
                    mCurrentState = CreateState.IDLE_RECORD;
                    takeAction = true;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            mCurrentState = CreateState.IDLE_RECORD;
        }

        if (!(mCurrentState == CreateState.RECORD || mCurrentState == CreateState.IDLE_STANDBY_REC || mCurrentState == CreateState.IDLE_STANDBY_PLAY)
                && mRecordDir != null && mRecordDir.exists() && mPrivateUser == null) {
                checkUnsavedFiles();
        }
        updateUi(takeAction);
    }

    private boolean shouldReactToRecording(){
        boolean react = false;
        try {
            react = shouldReactToPath(mCreateService.getRecordingPath());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return react;
    }

    private boolean shouldReactToPlayback(){
        boolean react = false;
        try {
            react = shouldReactToPath(mCreateService.getPlaybackPath());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return react;
    }

     private boolean shouldReactToPath(String path){
         if (TextUtils.isEmpty(path)) return false;
        long userIdFromPath = userIdFromPath = getPrivateUserIdFromPath(path);
        return ((userIdFromPath == -1 && mPrivateUser == null) || (mPrivateUser != null && userIdFromPath == mPrivateUser.id));
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        state.putString("createCurrentRecordFilePath", mRecordFile != null ? mRecordFile.getAbsolutePath() : "");
    }

    public void onRestoreInstanceState(Bundle state) {
        if (!TextUtils.isEmpty(state.getString("createCurrentRecordFilePath"))) {
            setRecordFile(new File(state.getString("createCurrentRecordFilePath")));
        }
        if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
            mCurrentState = CreateState.valueOf(state.getString("createCurrentCreateState"));
            updateUi(false);
        }
    }

    public void onRecordingError() {
        mSampleInterrupted = true;
        mRecordErrorMessage = mActivity.getResources().getString(R.string.error_recording_message);
        if (mRecordFile.exists()) mRecordFile.delete();
        mRecordFile = null;
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }


    /*** State Handling ***/
    private void onAction() {
        if (mCurrentState == CreateState.IDLE_STANDBY_REC) {
            stopRecording();
            configureState();
        } else if (mCurrentState == CreateState.IDLE_STANDBY_PLAY) {
            stopPlayback();
            onCreateServiceBound(mCreateService);
        } else {
            switch (mCurrentState) {
                case IDLE_RECORD:   mCurrentState = CreateState.RECORD; break;
                case RECORD:        mCurrentState = CreateState.IDLE_PLAYBACK; break;
                case IDLE_PLAYBACK: mCurrentState = CreateState.PLAYBACK; break;
                case PLAYBACK:      mCurrentState = CreateState.IDLE_PLAYBACK; break;
            }
            updateUi(true);
        }
    }

    void updateUi(boolean takeAction) {
        switch (mCurrentState) {
            case IDLE_RECORD:
                if (takeAction) {
                    stopPlayback();
                }
                if (!TextUtils.isEmpty(mRecordErrorMessage)) {
                    txtRecordStatus.setText(mRecordErrorMessage);
                } else {
                    txtRecordStatus.setText(null);
                }

                btnAction.setImageDrawable(btn_rec_states_drawable);
                txtRecordStatus.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.GONE);
                mChrono.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);
                break;

            case IDLE_STANDBY_REC:
            case IDLE_STANDBY_PLAY:
                btnAction.setImageDrawable(btn_rec_stop_states_drawable);
                txtRecordStatus.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.GONE);
                mChrono.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);
                txtRecordStatus.setText(mCurrentState == CreateState.IDLE_STANDBY_REC ?
                        mActivity.getString(R.string.recording_in_progress) : mActivity.getString(R.string.playback_in_progress));
                break;

            case RECORD:
                btnAction.setImageDrawable(btn_rec_stop_states_drawable);
                txtInstructions.setVisibility(View.GONE);
                txtRecordStatus.setText("");
                txtRecordStatus.setVisibility(View.VISIBLE);
                mChrono.setText("0.00");
                mChrono.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.VISIBLE);

                if (takeAction) startRecording();
                break;

            case IDLE_PLAYBACK:
                if (takeAction) {
                    switch (mLastState) {
                        case RECORD: stopRecording(); break;
                        case PLAYBACK:
                            try {
                                if (mCreateService != null && mCreateService.isPlayingBack())  {
                                    mCreateService.pausePlayback();
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "error", e);
                            }
                            stopProgressThread();
                            break;
                    }
                }

                mChrono.setText(mCurrentDurationString);
                btnAction.setImageDrawable(btn_rec_play_states_drawable);
                txtInstructions.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.VISIBLE);
                txtRecordStatus.setVisibility(View.GONE);

                break;

            case PLAYBACK:
                txtRecordStatus.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.VISIBLE);
                btnAction.setImageDrawable(btn_rec_stop_states_drawable);

                if (takeAction) startPlayback();
                break;
        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    private void startRecording() {
        if (mPrivateUser == null){
            mActivity.trackPage(Consts.Tracking.RECORD_RECORDING);
            mActivity.trackEvent(Consts.Tracking.Categories.RECORDING, "start");
        } else {
            mActivity.trackPage(Consts.Tracking.AUDIO_MESSAGE_RECORDING);
            mActivity.trackEvent(Consts.Tracking.Categories.AUDIO_MESSAGE, "start");
        }


        mActivity.pause(true);

        mRecordErrorMessage = "";
        mSampleInterrupted = false;

        mLastDisplayedTime = 0;

        mRemainingTimeCalculator.reset();
        mPowerGauge.clear();

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_storage_is_full);
        }

        final boolean hiQ = PreferenceManager.getDefaultSharedPreferences(mActivity)
            .getString("defaultRecordingQuality", "high")
            .contentEquals("high");

        if (hiQ && SoundCloudApplication.DEV_MODE
                && !PreferenceManager.getDefaultSharedPreferences(mActivity)
                        .getString("dev.defaultRecordingHighQualityType", "compressed")
                        .contentEquals("compressed")) {
            //force raw for developer mode
            mAudioProfile = CloudRecorder.Profile.RAW;
        } else  {
            mAudioProfile = hiQ ? CloudRecorder.Profile.best() : CloudRecorder.Profile.low();
        }

        if (mPrivateUser != null) {
            mRecordFile = new File(mRecordDir, System.currentTimeMillis() + "_" + mPrivateUser.id + "." + mAudioProfile);
        } else {
            mRecordFile = new File(mRecordDir, System.currentTimeMillis() + "." + mAudioProfile);
        }
        if (mSampleInterrupted) {
            mCurrentState = CreateState.IDLE_RECORD;
            updateUi(true);
        } else {
            mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);
            if (PCM_REC_MAX_FILE_SIZE != -1) {
                mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, PCM_REC_MAX_FILE_SIZE);
            }

            mActivity.setRequestedOrientation(mActivity.getResources().getConfiguration().orientation);
            try {
                mCreateService.startRecording(mRecordFile.getAbsolutePath(), mAudioProfile);
                //noinspection ResultOfMethodCallIgnored
                mRecordFile.setLastModified(System.currentTimeMillis());
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }

            mActivity.getApp().setRecordListener(recListener);
        }
    }

    /*
     * Called when we're in recording state. Find out how much longer we can go
     * on recording. If it's under 5 minutes, we display a count-down in the UI.
     * If we've run out of time, stop the recording.
     */
    private void updateTimeRemaining() {
        // adding 2 seconds to make up for lag
        final long t = mRemainingTimeCalculator.timeRemaining() + 2;
        if (t <= 1) {
            mSampleInterrupted = true;

            int limit = mRemainingTimeCalculator.currentLowerLimit();
            switch (limit) {
                case RemainingTimeCalculator.DISK_SPACE_LIMIT:
                    mRecordErrorMessage = mActivity.getResources().getString(R.string.record_storage_is_full);
                    break;
                case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                    mRecordErrorMessage = mActivity.getResources().getString(
                            R.string.record_max_length_reached);
                    break;
                default:
                    mRecordErrorMessage = null;
                    break;
            }

            mCurrentState = CreateState.IDLE_PLAYBACK;
            updateUi(true);
            return;
        }

        Resources res = mActivity.getResources();
        String timeStr = "";

        if (t < 60) {
            timeStr = res.getQuantityString(R.plurals.seconds_available, (int) t, t);
        } else if (t < 300) {
            timeStr = res.getQuantityString(R.plurals.minutes_available, (int) (t / 60 + 1),
                    (t / 60 + 1));
        }
        txtRecordStatus.setText(timeStr);

    }

    public SoundCloudApplication.RecordListener recListener = new SoundCloudApplication.RecordListener() {
        @Override
        public void onFrameUpdate(float maxAmplitude, long elapsed) {
            synchronized (this) {
                mPowerGauge.updateAmplitude(maxAmplitude);
                mPowerGauge.postInvalidate();
                onRecProgressUpdate(elapsed);
            }
        }
    };

    private void stopRecording() {
         if (mPrivateUser == null){
             mActivity.trackPage(Consts.Tracking.RECORD_COMPLETE);
             mActivity.trackEvent(Consts.Tracking.Categories.RECORDING, "stop");
         } else {
             mActivity.trackPage(Consts.Tracking.AUDIO_MESSAGE_COMPLETE);
             mActivity.trackEvent(Consts.Tracking.Categories.AUDIO_MESSAGE, "stop");
         }

        if (mActivity.getApp().getRecordListener() == recListener) {
            mActivity.getApp().setRecordListener(null);
        }
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        try {
            if (mCreateService != null) {
                mCreateService.stopRecording();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        // disable actions during processing and playback preparation
        btnAction.setEnabled(false);
        loadPlaybackTrack();
    }

    private void loadPlaybackTrack() {
        try {
            if (mCreateService != null && mRecordFile != null) {
                // might be loaded and paused already
                if (TextUtils.isEmpty(mCreateService.getPlaybackPath()) ||
                    !mCreateService.getPlaybackPath().contentEquals(mRecordFile.getAbsolutePath())) {
                    mCreateService.loadPlaybackTrack(mRecordFile.getAbsolutePath());
                }
                configurePlaybackInfo();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void configurePlaybackInfo(){
        try {
            mCurrentDurationString =  CloudUtils.formatTimestamp(getDuration());
            mProgressBar.setMax((int) (getDuration()));

            if (mCreateService.getCurrentPlaybackPosition() > 0
                    && mCreateService.getCurrentPlaybackPosition() < getDuration()) {
                mProgressBar.setProgress(mCreateService.getCurrentPlaybackPosition());
            } else {
                mProgressBar.setProgress(0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private long getDuration() throws RemoteException{
        return mDuration == 0 ? mCreateService.getPlaybackDuration() : mDuration;
    }


    private void onPlaybackComplete(){
        stopProgressThread();
        mProgressBar.setProgress(0);
        if (mCurrentState == CreateState.PLAYBACK) {
            mCurrentState = CreateState.IDLE_PLAYBACK;
            loadPlaybackTrack();
            updateUi(true);
        }
    }

    private void startPlayback() {
        try {
            if (!mCreateService.isPlayingBack()) mCreateService.startPlayback(); //might already be playing back if activity just created
            startProgressThread();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

    }

    private void startProgressThread(){
        if (mProgressThread != null)
            return;

        mProgressThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (mCurrentState == CreateState.PLAYBACK) {
                        final long pos = mCreateService.getCurrentPlaybackPosition();

                        // Update the progress bar
                        mActivity.getHandler().post(new Runnable() {
                            public void run() {
                                if (mCurrentState == CreateState.PLAYBACK) {
                                    mChrono.setText(new StringBuilder()
                                            .append(CloudUtils.formatTimestamp(pos))
                                            .append(" / ")
                                            .append(mCurrentDurationString));
                                    mProgressBar.setProgress((int) pos);
                                }
                            }
                        });
                        Thread.sleep(50);
                    }
                } catch (RemoteException ignored) {
                } catch (InterruptedException ignored) {}
            }
        });
        mProgressThread.start();
    }

    private void stopProgressThread(){
        if (mProgressThread == null)
            return;

        if (!mProgressThread.isInterrupted())
            mProgressThread.interrupt();

        mProgressThread = null;
    }

    private void stopPlayback() {
        try {
            mCreateService.stopPlayback();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        stopProgressThread();
        mProgressBar.setProgress(0);
    }



    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                try {
                    mCreateService.seekTo(progress);
                } catch (RemoteException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
        public void onStopTrackingTouch(SeekBar bar) { }
    };

    private void checkUnsavedFiles() {
        String[] columns = { DatabaseHelper.Recordings.ID };
        Cursor cursor;

        // XXX background thread?
        MediaPlayer mp = null;
        mUnsavedRecordings = new ArrayList<Recording>();

        for (File f : mRecordDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Recording.isRawFilename(name) || Recording.isCompressedFilename(name);
            }
        })) {
            if (f.equals(mRecordFile) || getPrivateUserIdFromPath(f.getAbsolutePath()) != -1) continue; // ignore current file

            cursor = mActivity.getContentResolver().query(DatabaseHelper.Content.RECORDINGS,
                    columns,
                    DatabaseHelper.Recordings.AUDIO_PATH + "='" + f.getAbsolutePath() + "'",
                    null, null);

            // XXX TODO exclude currently uploading file!
            if ((cursor == null || cursor.getCount() == 0)) {
                Recording r = new Recording(f);
                r.audio_profile = Recording.isRawFilename(f.getName()) ? CloudRecorder.Profile.RAW : CloudRecorder.Profile.ENCODED_LOW;
                r.user_id = mActivity.getCurrentUserId();

                try {
                    mp = mp == null ? new MediaPlayer() : mp;
                    mp.reset();
                    mp.setDataSource(f.getAbsolutePath());
                    mp.prepare();
                    r.duration = mp.getDuration();
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                }
                mUnsavedRecordings.add(r);
            }
            if (cursor != null) cursor.close();
        }

        if (mUnsavedRecordings.size() > 0){
            Collections.sort(mUnsavedRecordings, new Comparator<Recording>() {
                public int compare(Recording r1, Recording r2) {
                    return Long.valueOf(r1.timestamp).compareTo(r2.timestamp);
                }
            });
            mActivity.showDialog(Consts.Dialogs.DIALOG_UNSAVED_RECORDING);
        }
    }

    private void checkForUnusedPrivateFile() {
        for (File f : mRecordDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Recording.isRawFilename(name) || Recording.isCompressedFilename(name);
            }
        })) {
            if (f.equals(mRecordFile)) continue; // ignore current file

            final long filePrivateUserId = getPrivateUserIdFromPath(f.getAbsolutePath());
            if (mPrivateUser != null && filePrivateUserId == mPrivateUser.id) {
                setRecordFile(f);
                break;
            };


        }
    }



    /* package */ void setRecordFile(File f) {
        mRecordFile = f;
        if (f != null) mAudioProfile = Recording.isRawFilename(f.getName()) ? CloudRecorder.Profile.RAW : CloudRecorder.Profile.ENCODED_LOW;
    }


    public void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.formatTimestamp(elapsed));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
        }
    }

    public void onStart(){
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_STARTED);
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_COMPLETE);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_ERROR);
        mActivity.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));
    }

    public void onPause() {
        mActive = false;
    }

    public void onResume() {
        if (mCreateService != null){
            configureState();
        }
        mActive = true;
    }

    public void onStop() {
        stopProgressThread();
        mActivity.unregisterReceiver(mUploadStatusListener);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void onDestroy() {
        if (mActivity.getApp().getRecordListener() == recListener) {
            mActivity.getApp().setRecordListener(null);
        }
    }

    public static long getPrivateUserIdFromPath(String path){
        if (path.indexOf("_") == -1){
            return -1;
        } else {
            return Long.valueOf(path.substring(path.indexOf("_")+1,path.indexOf(".")));
        }
    }

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudCreateService.RECORD_STARTED) &&
                    (mCurrentState == CreateState.IDLE_PLAYBACK || mCurrentState == CreateState.PLAYBACK)) {
                // this will happen if recording starts from somewhere else. just reset as the player will have to be reloaded anyway
                stopPlayback();
                reset();

            } else if (action.equals(CloudCreateService.RECORD_ERROR)) {
                onRecordingError();
            } else if (action.equals(CloudCreateService.PLAYBACK_COMPLETE) || action.equals(CloudCreateService.PLAYBACK_ERROR)) {
                if (shouldReactToPath(intent.getStringExtra("path"))) {
                    onPlaybackComplete();
                } else if (mCurrentState == CreateState.IDLE_STANDBY_PLAY) {
                    configureState();
                }
            }


        }
    };

    public Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNSAVED_RECORDING:
                if (mUnsavedRecordings == null) return null;

                final CharSequence[] fileIds = new CharSequence[mUnsavedRecordings.size()];
                final boolean[] checked = new boolean[mUnsavedRecordings.size()];
                for (int i=0; i < mUnsavedRecordings.size(); i++) {
                    fileIds[i] = new Date(mUnsavedRecordings.get(i).timestamp).toLocaleString() + ", " + mUnsavedRecordings.get(i).formattedDuration();
                    checked[i] = true;
                }

                return new AlertDialog.Builder(mActivity).setTitle(
                                R.string.dialog_unsaved_recordings_message).setMultiChoiceItems(
                                fileIds, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                                checked[whichButton] = isChecked;
                            }
                        }).setPositiveButton(R.string.btn_save,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mUnsavedRecordings != null) {
                                    for (int i = 0; i < mUnsavedRecordings.size(); i++){
                                        if (checked[i]){
                                            mActivity.getContentResolver().insert(DatabaseHelper.Content.RECORDINGS,mUnsavedRecordings.get(i).buildContentValues());
                                        } else {
                                            mUnsavedRecordings.get(i).delete(null);
                                        }
                                    }
                                }
                                mUnsavedRecordings = null;
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(mActivity).setTitle(R.string.dialog_reset_recording_title)
                        .setMessage(R.string.dialog_reset_recording_message).setPositiveButton(
                                mActivity.getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                CloudUtils.deleteFile(mRecordFile);
                                mActivity.removeDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                                if (mCreateListener != null) mCreateListener.onCancel();
                            }
                        }).setNegativeButton(mActivity.getString(R.string.btn_no), null)
                        .create();

            case Consts.Dialogs.DIALOG_DELETE_RECORDING:
             return new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.dialog_confirm_delete_recording_title)
                            .setMessage(R.string.dialog_confirm_delete_recording_message)
                            .setPositiveButton(mActivity.getString(R.string.btn_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            if (mRecording != null) mRecording.delete(mActivity.getContentResolver());
                                            if (mCreateListener != null) mCreateListener.onDelete();
                                        }
                                    })
                            .setNegativeButton(mActivity.getString(R.string.btn_no), null)
                            .create();

            default:
                return null;
        }
    }

    public interface CreateListener {
        void onSave(Uri recordingUri, Recording recording, boolean newRecording);
        void onCancel();
        void onDelete();
    }


}
