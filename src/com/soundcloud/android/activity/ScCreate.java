package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;
import com.soundcloud.android.utils.record.PowerGauge;
import com.soundcloud.android.utils.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ScCreate extends ScActivity {
    private TextView txtInstructions, txtRecordStatus;
    private LinearLayout mFileLayout;
    private PowerGauge mPowerGauge;
    private SeekBar mProgressBar;
    private ImageButton btnAction;
    private File mRecordFile, mRecordDir;
    private CreateState mLastState, mCurrentState;
    private long mLastDisplayedTime;
    private TextView mChrono;
    private long mDuration, mLastSeekEventTime;
    private int mAudioProfile;
    private String mRecordErrorMessage;
    private String mDurationFormatLong;
    private String mDurationFormatShort;
    private String mCurrentDurationString;
    private long mRecordingId;
    private boolean mSampleInterrupted;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private Thread mProgressThread;
    private List<Recording> mUnsavedRecordings;

    public enum CreateState {
        IDLE_RECORD, RECORD, IDLE_PLAYBACK, PLAYBACK
    }

    public static int REC_SAMPLE_RATE = 44100;
    public static int PCM_REC_CHANNELS = 1;
    public static int PCM_REC_BITS_PER_SAMPLE = 16;
    public static int PCM_REC_MAX_FILE_SIZE = -1;

    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");
    private static final Pattern COMPRESSED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentState = CreateState.IDLE_RECORD;

        setContentView(R.layout.sc_record);
        if (getIntent().hasExtra("recordingId") && getIntent().getLongExtra("recordingId",0) != 0) {
            mRecordingId = getIntent().getLongExtra("recordingId",0);
        }

        initResourceRefs();
        updateUi(false);

        mRecordDir = CloudUtils.ensureUpdatedDirectory(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/recordings/", CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/.rec/");
        mkdirs(mRecordDir);
        mRecordErrorMessage = "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        pageTrack("/record");
    }

    @Override
    protected void onStart(){
        super.onStart();

        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_COMPLETE);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_ERROR);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));

        checkUnsavedFiles();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(mUploadStatusListener);
        stopProgressThread();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getSoundCloudApplication().getRecordListener() == recListener) {
            getSoundCloudApplication().setRecordListener(null);
        }
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

        ((Button) findViewById(R.id.btn_reset))
                .setText(getString(mRecordingId == 0 ? R.string.reset : R.string.delete));
        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecordingId == 0)
                    showDialog(CloudUtils.Dialogs.DIALOG_RESET_RECORDING);
                else {
                    new AlertDialog.Builder(ScCreate.this)
                            .setTitle(R.string.dialog_confirm_delete_recording_title)
                            .setMessage(R.string.dialog_confirm_delete_recording_message)
                            .setPositiveButton(getString(R.string.btn_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            getContentResolver().delete(Content.RECORDINGS,
                                                    Recordings.ID + " = " + mRecordingId, null);
                                            finish();
                                        }
                                    })
                            .setNegativeButton(getString(R.string.btn_no),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                        }
                                    }).create().show();
                }
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecordingId == 0){

                    Recording r = new Recording();
                    r.audio_path = mRecordFile.getAbsolutePath();
                    r.audio_profile = mAudioProfile;
                    r.timestamp = mRecordFile.lastModified();
                    r.user_id = getUserId();

                    try { // set duration because ogg files report incorrect
                          // duration in mediaplayer if playback is attempted
                          // after encoding
                        r.duration = mCreateService.getPlaybackDuration();
                    } catch (RemoteException ignored) {
                    }



                    Uri newRecordingUri = getContentResolver().insert(Content.RECORDINGS, r.buildContentValues());
                    Intent i = new Intent(ScCreate.this,ScUpload.class);
                    i.putExtra("recordingId", Long.valueOf(newRecordingUri.getPathSegments().get(newRecordingUri.getPathSegments().size()-1)));
                    startActivity(i);

                    mRecordingId = 0;
                    mRecordFile = null;
                    mCurrentState = CreateState.IDLE_RECORD;
                } else {
                    Intent i = new Intent(ScCreate.this, ScUpload.class);
                    i.putExtra("recordingId", mRecordingId);
                    //start for result, because if an upload starts, finish, playback should not longer be possible
                    startActivityForResult(i, 0);
                }
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
            if (getIntent().hasExtra("recordingId") && getIntent().getLongExtra("recordingId",0) != 0){
                String[] columns = { Recordings.ID, Recordings.AUDIO_PATH,Recordings.AUDIO_PROFILE, Recordings.DURATION };
                Cursor cursor = getContentResolver().query(Content.RECORDINGS,
                        columns, Recordings.ID + "= ?",new String[] {Long.toString(getIntent().getLongExtra("recordingId",0))}, null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    mRecordingId = cursor.getLong(cursor.getColumnIndex(Recordings.ID));
                    setRecordFile(new File(cursor.getString(cursor.getColumnIndex(Recordings.AUDIO_PATH))));
                    mAudioProfile = cursor.getInt(cursor.getColumnIndex(Recordings.AUDIO_PROFILE));
                    mDuration = cursor.getLong(cursor.getColumnIndex(Recordings.DURATION));
                    cursor.close();
                } else {
                    showToast("Error getting recording");
                }
            }

            Log.d(TAG, "service bound Current state " + mCurrentState + " " + mRecordingId + " " + mCreateService.getPlaybackLocalId());
            if (mCreateService.isRecording() && mRecordingId == 0) {
                mCurrentState = CreateState.RECORD;
                setRecordFile(new File(mCreateService.getRecordingPath()));
                getSoundCloudApplication().setRecordListener(recListener);
                setRequestedOrientation(getResources().getConfiguration().orientation);
            } else if (mCreateService.isPlayingBack() && mRecordingId == mCreateService.getPlaybackLocalId()) {
                mCurrentState = CreateState.PLAYBACK;
                setRecordFile(new File(mCreateService.getPlaybackPath()));
                configurePlaybackInfo();
                startProgressThread();
                takeAction = true;
            } else if (!mRecordDir.exists()) {
                // can happen when there's no mounted sd card
                btnAction.setEnabled(false);
            } else {
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
                            stopProgressThread();
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

        if (hiQ && SoundCloudApplication.DEV_MODE
                && !PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("defaultRecordingHighQualityType", "compressed")
                        .contentEquals("compressed")) {
            //force raw for developer mode
            mAudioProfile = Profile.RAW;
        } else  {
            mAudioProfile = hiQ ? Profile.best() : Profile.low();
        }

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
        if (getSoundCloudApplication().getRecordListener() == recListener) getSoundCloudApplication().setRecordListener(null);
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
                    getDuration() / 1000);
            mProgressBar.setMax((int) (getDuration()));

            if (mCreateService.getCurrentPlaybackPosition() > 0 && mCreateService.getCurrentPlaybackPosition() < getDuration())
                mProgressBar.setProgress(mCreateService.getCurrentPlaybackPosition());
            else
                mProgressBar.setProgress(0);

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
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (mCurrentState == CreateState.PLAYBACK) {
                                    mChrono.setText(CloudUtils.makeTimeString(
                                            pos < 3600 * 1000 ? mDurationFormatShort
                                                    : mDurationFormatLong, pos / 1000)
                                            + " / " + mCurrentDurationString);
                                    mProgressBar.setProgress((int) pos);
                                }
                            }
                        });
                        Thread.sleep(100);
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

    private void checkUnsavedFiles() {
        File file = null;

        String[] columns = { Recordings.ID };
        Cursor cursor;

        MediaPlayer mp = null;
        mUnsavedRecordings = new ArrayList<Recording>();
        for (File f : mRecordDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return isRawFilename(name) || isCompressedFilename(name);
            }
        })) {
            cursor = getContentResolver().query(Content.RECORDINGS, columns,
                    Recordings.AUDIO_PATH + "='" + f.getAbsolutePath() + "'", null, null);
            if ((cursor == null || cursor.getCount() == 0)
                    && (file == null || f.lastModified() < file.lastModified())) {
                Recording r = new Recording();
                r.audio_path = f.getAbsolutePath();
                r.audio_profile = isRawFilename(f.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
                r.timestamp = f.lastModified();
                r.user_id = getUserId();

                try {
                    mp = mp == null ? new MediaPlayer() : mp;
                    mp.reset();
                    mp.setDataSource(f.getAbsolutePath());
                    mp.prepare();
                    r.duration = mp.getDuration();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mUnsavedRecordings.add(r);
            }
            if (cursor != null)
                cursor.close();
        }

        if (mUnsavedRecordings.size() > 0){
            Collections.sort(mUnsavedRecordings, new Comparator<Recording>(){
                public int compare(Recording r1, Recording r2)
                {
                    return Long.valueOf(r1.timestamp).compareTo(r2.timestamp);
                } });
            showDialog(CloudUtils.Dialogs.DIALOG_UNSAVED_RECORDING);
        }
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
            case CloudUtils.Dialogs.DIALOG_UNSAVED_RECORDING:
                final CharSequence[] fileIds = new CharSequence[mUnsavedRecordings.size()];
                final boolean[] checked = new boolean[mUnsavedRecordings.size()];
                for (int i=0; i < mUnsavedRecordings.size(); i++) {
                    fileIds[i] = new Date(mUnsavedRecordings.get(i).timestamp).toLocaleString() + ", " + mUnsavedRecordings.get(i).formattedDuration();;
                    checked[i] = true;
                }

                return new AlertDialog.Builder(this).setTitle(
                                R.string.dialog_unsaved_recordings_message).setMultiChoiceItems(
                                fileIds, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                                checked[whichButton] = isChecked;
                            }
                        }).setPositiveButton(R.string.btn_save,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                for (int i = 0; i < mUnsavedRecordings.size(); i++){
                                    if (checked[i]){
                                        getContentResolver().insert(Content.RECORDINGS,mUnsavedRecordings.get(i).buildContentValues());
                                    } else {
                                        new File(mUnsavedRecordings.get(i).audio_path).delete();
                                    }
                                }
                                mUnsavedRecordings = null;
                            }
                        }).create();
            case CloudUtils.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_reset_recording_title)
                        .setMessage(R.string.dialog_reset_recording_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (mRecordFile != null){
                                            if (mRecordFile.exists())mRecordFile.delete();
                                            mRecordFile = null;
                                        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
           finish();
        }
    }
}
