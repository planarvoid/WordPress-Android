package com.soundcloud.android.activity;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.utils.record.CloudRecorder.Profile;
import com.soundcloud.utils.record.PowerGauge;
import com.soundcloud.utils.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class ScCreate extends ScActivity {
    private static final String TAG = "ScCreate";

    private TextView txtInstructions, txtRecordStatus;

    private LinearLayout mFileLayout;

    private PowerGauge mPowerGauge;

    private SeekBar mProgressBar;

    private ImageButton btnAction;

    private File mRecordFile, mRecordDir;

    private CreateState mLastState, mCurrentState;

    private long mLastDisplayedTime = 0;

    private TextView mChrono;

    private long mLastSeekEventTime;

    private int mAudioProfile;

    private String mRecordErrorMessage;

    private String mDurationFormatLong;

    private String mDurationFormatShort;

    private String mCurrentDurationString;

    private boolean mSampleInterrupted = false;

    private RemainingTimeCalculator mRemainingTimeCalculator;

    public enum CreateState {
        IDLE_RECORD, RECORD, IDLE_PLAYBACK, PLAYBACK
    }

    public static int REC_SAMPLE_RATE = 44100;

    public static int PCM_REC_CHANNELS = 1;

    public static int PCM_REC_BITS_PER_SAMPLE = 16;

    public static int PCM_REC_MAX_FILE_SIZE = -1;

    // public static int PCM_REC_MAX_FILE_SIZE = 158760000; // 15 mins at
    // 44100x16bitx2channels

    private static final int PLAYBACK_REFRESH = 1001;

    private static final int PLAYBACK_REFRESH_INTERVAL = 200;

    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");

    private static final Pattern COMPRESSED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentState = CreateState.IDLE_RECORD;

        setContentView(R.layout.sc_record);

        initResourceRefs();

        updateUi(false);

        mRecordDir = CloudUtils.ensureUpdatedDirectory(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/recordings/unsaved", CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/.rec/");
        if (!mRecordDir.exists()) mRecordDir.mkdirs();

        mRecordErrorMessage = "";

        // XXX do in manifest
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_COMPLETE);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_ERROR);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeMessages(PLAYBACK_REFRESH);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getSoundCloudApplication().setRecordListener(null);
        this.unregisterReceiver(mUploadStatusListener);
    }

    /*
    * Whenever the UI is re-created (due f.ex. to orientation change) we have
    * to reinitialize references to the views.
    */
    private void initResourceRefs() {
        mDurationFormatLong = getString(R.string.durationformatlong);
        mDurationFormatShort = getString(R.string.durationformatshort);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);

        FrameLayout mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
        txtInstructions = (TextView) findViewById(R.id.txt_instructions);

        mProgressBar = (SeekBar) findViewById(R.id.progress_bar);
        mProgressBar.setOnSeekBarChangeListener(mSeekListener);

        txtRecordStatus = (TextView) findViewById(R.id.txt_record_status);

        mChrono = (TextView) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.GONE);

        RelativeLayout mProgressFrame = (RelativeLayout) findViewById(R.id.progress_frame);
        mProgressFrame.setVisibility(View.GONE);

        mFileLayout = (LinearLayout) findViewById(R.id.file_layout);

        btnAction = (ImageButton) findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAction();
            }
        });

        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(CloudUtils.Dialogs.DIALOG_RESET_RECORDING);
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // reset and send intent to upload
                Intent i = new Intent(ScCreate.this,ScUpload.class);
                i.putExtra("uploadFilePath", mRecordFile.getAbsolutePath());
                startActivity(i);
            }
        });

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mPowerGauge = new PowerGauge(this);
        mGaugeHolder.addView(mPowerGauge);
    }

    @Override
    public void onCreateServiceBound() {
        super.onCreateServiceBound();
        boolean takeAction = false;
        try {
            if (mCreateService.isRecording()) {
                mCurrentState = CreateState.RECORD;
                setRecordFile(new File(mCreateService.getRecordingPath()));
                getSoundCloudApplication().setRecordListener(recListener);
                setRequestedOrientation(getResources().getConfiguration().orientation);
            } else if (mCreateService.isPlayingBack()) {
                mCurrentState = CreateState.PLAYBACK;
                setRecordFile(new File(mCreateService.getPlaybackPath()));
                configurePlaybackInfo();
                queueNextPlaybackRefresh(refreshPlaybackInfo());
                takeAction = true;
            } else if (!mRecordDir.exists()) {
                // can happen when there's no mounted sdcard
                btnAction.setEnabled(false);
            } else {
                // in this case, state should be based on what is in the recording directory
                setRecordFile();
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

        updateUi(takeAction);
    }


    @Override
    public void onReauthenticate() {
        onRefresh();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
            mCurrentState = CreateState.valueOf(state.getString("createCurrentCreateState"));
            updateUi(false);
        }
        super.onRestoreInstanceState(state);
    }

    public void onRecordingError() {
        mSampleInterrupted = true;
        mRecordErrorMessage = getResources().getString(R.string.error_recording_message);
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }


    /*** State Handling ***/
    private void onAction() {
        switch (mCurrentState) {
            case IDLE_RECORD:   mCurrentState = CreateState.RECORD; break;
            case RECORD:        mCurrentState = CreateState.IDLE_PLAYBACK; break;
            case IDLE_PLAYBACK: mCurrentState = CreateState.PLAYBACK; break;
            case PLAYBACK:      mCurrentState = CreateState.IDLE_PLAYBACK; break;
        }
        updateUi(true);
    }

    private void updateUi(boolean takeAction) {
        Log.i(TAG, "Update Soundcloud Create state: " + mCurrentState + " | take action: "
                + takeAction);
        switch (mCurrentState) {
            case IDLE_RECORD:
                if (takeAction) {
                    stopPlayback();
                    for (File f : mRecordDir.listFiles()) f.delete();
                }
                if (!TextUtils.isEmpty(mRecordErrorMessage)) txtRecordStatus.setText(mRecordErrorMessage);

                btnAction.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.btn_rec_states));
                txtRecordStatus.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.GONE);
                mChrono.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);
                break;

            case RECORD:
                btnAction.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.btn_rec_stop_states));
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
                                if (mCreateService.isPlayingBack()) mCreateService.pausePlayback();
                            } catch (RemoteException e) {
                                Log.e(TAG, "error", e);
                            }
                            break;
                    }
                }

                mChrono.setText(mCurrentDurationString);

                btnAction.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.btn_rec_play_states));
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
                btnAction.setBackgroundDrawable(getResources()
                        .getDrawable(R.drawable.btn_rec_stop_states));

                if (takeAction) startPlayback();
                break;
        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }



    private void startRecording() {
        pause(true);

        mRecordErrorMessage = "";
        mSampleInterrupted = false;

        mLastDisplayedTime = 0;

        mRemainingTimeCalculator.reset();
        mPowerGauge.clear();

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
        }

        final boolean hiQ = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("defaultRecordingQuality", "high")
            .contentEquals("high");

        mAudioProfile = hiQ ? Profile.best() : Profile.low();

        mRecordFile = new File(mRecordDir, System.currentTimeMillis() + "." + mAudioProfile);

        if (mSampleInterrupted) {
            mCurrentState = CreateState.IDLE_RECORD;
            updateUi(true);
        } else {
            mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);
            if (PCM_REC_MAX_FILE_SIZE != -1) {
                mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, PCM_REC_MAX_FILE_SIZE);
            }

            setRequestedOrientation(getResources().getConfiguration().orientation);
            try {
                mCreateService.startRecording(mRecordFile.getAbsolutePath(), mAudioProfile);
                mRecordFile.setLastModified(System.currentTimeMillis());
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }

            getSoundCloudApplication().setRecordListener(recListener);
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
                    mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
                    break;
                case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                    mRecordErrorMessage = getResources().getString(
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

        Resources res = getResources();
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
        getSoundCloudApplication().setRecordListener(null);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        try {
            mCreateService.stopRecording();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        // disable actions during processing and playback preparation
        btnAction.setEnabled(false);
        loadPlaybackTrack();
    }

    private void loadPlaybackTrack(){
        try {
            // might be loaded and paused already
            if (TextUtils.isEmpty(mCreateService.getPlaybackPath()) || !mCreateService.getPlaybackPath().contentEquals(mRecordFile.getAbsolutePath()))
                mCreateService.loadPlaybackTrack(mRecordFile.getAbsolutePath());

            configurePlaybackInfo();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

    }

    private void configurePlaybackInfo(){
        try {
            mCurrentDurationString =  CloudUtils.makeTimeString(mDurationFormatShort,
                    mCreateService.getPlaybackDuration() / 1000);
            mProgressBar.setMax(mCreateService.getPlaybackDuration());

            if (mCreateService.getCurrentPlaybackPosition() > 0 && mCreateService.getCurrentPlaybackPosition() < mCreateService.getPlaybackDuration())
                mProgressBar.setProgress(mCreateService.getCurrentPlaybackPosition());

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }


    private void onPlaybackComplete(){
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
            queueNextPlaybackRefresh(refreshPlaybackInfo());
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

    }

    private long refreshPlaybackInfo() {
        try {
            if (mCreateService == null || mCurrentState != CreateState.PLAYBACK)
                return PLAYBACK_REFRESH_INTERVAL;

            long pos = mCreateService.getCurrentPlaybackPosition();
            long remaining = PLAYBACK_REFRESH_INTERVAL - pos % PLAYBACK_REFRESH_INTERVAL;
            mChrono.setText(CloudUtils.makeTimeString(pos < 3600 * 1000 ? mDurationFormatShort
                    : mDurationFormatLong, pos / 1000)
                    + " / " + mCurrentDurationString);
            mProgressBar.setProgress((int) pos);

            return remaining;

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        return PLAYBACK_REFRESH_INTERVAL;
    }

    private void queueNextPlaybackRefresh(long delay) {
        if (mCurrentState == CreateState.PLAYBACK) {
            Message msg = mHandler.obtainMessage(PLAYBACK_REFRESH);
            mHandler.removeMessages(PLAYBACK_REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PLAYBACK_REFRESH :
                    queueNextPlaybackRefresh(refreshPlaybackInfo());
                    break;
                default:
                    break;
            }
        }
    };

    private void stopPlayback() {
        try {
            mCreateService.stopPlayback();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        mProgressBar.setProgress(0);
    }



    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
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

    private void setRecordFile(){
        setRecordFile(getValidOldestFile());
    }

    private File getValidOldestFile() {
        File file = null;
        for (File f : mRecordDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return isRawFilename(name) || isCompressedFilename(name);
            }
           })) {
            if (file == null || f.lastModified() < file.lastModified()) file = f;
        }
        return file;
    }

    /* package */ void setRecordFile(File f) {
        mRecordFile = f;
        if (f != null) mAudioProfile = isRawFilename(f.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
    }

    private boolean isRawFilename(String filename){
        return RAW_PATTERN.matcher(filename).matches();
    }

    private boolean isCompressedFilename(String filename){
        return COMPRESSED_PATTERN.matcher(filename).matches();
    }

    public void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.makeTimeString(
                elapsed < 3600000 ? mDurationFormatShort : mDurationFormatLong, elapsed/1000));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
        }
    }

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudCreateService.RECORD_ERROR))
                onRecordingError();
            else if (action.equals(CloudCreateService.PLAYBACK_COMPLETE))
                onPlaybackComplete();
            else if (action.equals(CloudCreateService.PLAYBACK_ERROR))
                onPlaybackComplete(); // might be unknown errors, meriting proper error handling
        }
    };


    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case CloudUtils.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_reset_recording_title)
                        .setMessage(R.string.dialog_reset_recording_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mCurrentState = CreateState.IDLE_RECORD;
                                        updateUi(true);
                                        removeDialog(CloudUtils.Dialogs.DIALOG_RESET_RECORDING);
                                    }
                                }).setNegativeButton(getString(R.string.btn_no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_RESET_RECORDING);
                                    }
                                }).create();
            default:
                return super.onCreateDialog(which);
        }
    }
}
