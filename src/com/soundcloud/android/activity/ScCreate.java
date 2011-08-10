package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Recording;
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
import android.view.MenuItem;
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
    private String mCurrentDurationString;
    private Uri mRecordingUri;
    private boolean mSampleInterrupted;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private Thread mProgressThread;
    private List<Recording> mUnsavedRecordings;

    private Drawable
            btn_rec_states_drawable,
            btn_rec_stop_states_drawable,
            btn_rec_play_states_drawable;

    public enum CreateState {
        IDLE_RECORD, RECORD, IDLE_PLAYBACK, PLAYBACK
    }

    public static int REC_SAMPLE_RATE = 44100;
    public static int PCM_REC_CHANNELS = 1;
    public static int PCM_REC_BITS_PER_SAMPLE = 16;
    public static int PCM_REC_MAX_FILE_SIZE = -1;

    public static final int REQUEST_UPLOAD_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentState = CreateState.IDLE_RECORD;
        setContentView(R.layout.sc_create);

        mRecordingUri = getIntent().getData();
        initResourceRefs();
        updateUi(false);

        mRecordDir = CloudUtils.ensureUpdatedDirectory(
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY,".rec"));
        mkdirs(mRecordDir);
        mRecordErrorMessage = "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        pageTrack(Consts.TrackingEvents.RECORD);
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
    }

    @Override
    protected void onStop() {
        stopProgressThread();
        this.unregisterReceiver(mUploadStatusListener);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getApp().getRecordListener() == recListener) {
            getApp().setRecordListener(null);
        }
    }

    /*
    * Whenever the UI is re-created (due f.ex. to orientation change) we have
    * to reinitialize references to the views.
    */
    private void initResourceRefs() {
        btn_rec_states_drawable = getResources().getDrawable(R.drawable.btn_rec_states);
        btn_rec_stop_states_drawable = getResources().getDrawable(R.drawable.btn_rec_stop_states);
        btn_rec_play_states_drawable = getResources().getDrawable(R.drawable.btn_rec_play_states);

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
                .setText(getString(mRecordingUri == null ? R.string.reset : R.string.delete));
        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecordingUri == null)
                    showDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                else {
                    new AlertDialog.Builder(ScCreate.this)
                            .setTitle(R.string.dialog_confirm_delete_recording_title)
                            .setMessage(R.string.dialog_confirm_delete_recording_message)
                            .setPositiveButton(getString(R.string.btn_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Recording recording = Recording.fromUri(mRecordingUri, getContentResolver());
                                            if (recording != null) recording.delete(getContentResolver());
                                            finish();
                                        }
                                    })
                            .setNegativeButton(getString(R.string.btn_no), null)
                            .create()
                            .show();
                }
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRecordingUri == null){
                    Recording r = new Recording(mRecordFile);
                    r.audio_profile = mAudioProfile;
                    r.user_id = getCurrentUserId();

                    try { // set duration because ogg files report incorrect
                          // duration in mediaplayer if playback is attempted
                          // after encoding
                        r.duration = mCreateService.getPlaybackDuration();
                    } catch (RemoteException ignored) {
                    }

                    Uri newRecordingUri = getContentResolver().insert(Content.RECORDINGS, r.buildContentValues());
                    startActivity(new Intent(ScCreate.this, ScUpload.class).setData(newRecordingUri));

                    mRecordingUri = null;
                    mRecordFile = null;
                    mCurrentState = CreateState.IDLE_RECORD;
                } else {
                    //start for result, because if an upload starts, finish, playback should not longer be possible
                    startActivityForResult(new Intent(ScCreate.this, ScUpload.class).setData(mRecordingUri), 0);
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
            Uri uri = getIntent().getData();
            long recordingId = 0;
            if (uri != null) {
                recordingId = Long.valueOf(uri.getLastPathSegment());
                Cursor cursor = getContentResolver().query(uri,
                        new String[]{Recordings.ID, Recordings.AUDIO_PATH, Recordings.AUDIO_PROFILE, Recordings.DURATION},
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    setRecordFile(new File(cursor.getString(cursor.getColumnIndex(Recordings.AUDIO_PATH))));
                    mAudioProfile = cursor.getInt(cursor.getColumnIndex(Recordings.AUDIO_PROFILE));
                    mDuration = cursor.getLong(cursor.getColumnIndex(Recordings.DURATION));
                    cursor.close();
                } else {
                    showToast(R.string.error_getting_recording);
                }
            }

            if (mCreateService.isRecording() && mRecordingUri == null) {
                mCurrentState = CreateState.RECORD;
                setRecordFile(new File(mCreateService.getRecordingPath()));
                getApp().setRecordListener(recListener);
                setRequestedOrientation(getResources().getConfiguration().orientation);
            } else if (mCreateService.isPlayingBack() && recordingId == mCreateService.getPlaybackLocalId()) {
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

        if (mRecordDir != null && mRecordDir.exists()) checkUnsavedFiles();
        updateUi(takeAction);
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        state.putString("createCurrentRecordFilePath", mRecordFile != null ? mRecordFile.getAbsolutePath() : "");
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!TextUtils.isEmpty(state.getString("createCurrentRecordFilePath"))) {
            setRecordFile(new File(state.getString("createCurrentRecordFilePath")));
        }
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
        switch (mCurrentState) {
            case IDLE_RECORD:
                if (takeAction) {
                    stopPlayback();
                }
                if (!TextUtils.isEmpty(mRecordErrorMessage)) txtRecordStatus.setText(mRecordErrorMessage);

                btnAction.setBackgroundDrawable(btn_rec_states_drawable);
                txtRecordStatus.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.GONE);
                mChrono.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);
                break;

            case RECORD:
                btnAction.setBackgroundDrawable(btn_rec_stop_states_drawable);
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
                btnAction.setBackgroundDrawable(btn_rec_play_states_drawable);
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
                btnAction.setBackgroundDrawable(btn_rec_stop_states_drawable);

                if (takeAction) startPlayback();
                break;
        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    private void startRecording() {
        pageTrack(Consts.TrackingEvents.RECORD_RECORDING);
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
                        .getString("dev.defaultRecordingHighQualityType", "compressed")
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
                //noinspection ResultOfMethodCallIgnored
                mRecordFile.setLastModified(System.currentTimeMillis());
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }

            getApp().setRecordListener(recListener);
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
        pageTrack(Consts.TrackingEvents.RECORD_COMPLETE);
        if (getApp().getRecordListener() == recListener) {
            getApp().setRecordListener(null);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

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

    private void loadPlaybackTrack(){
        try {
            // might be loaded and paused already
            if (TextUtils.isEmpty(mCreateService.getPlaybackPath()) ||
                !mCreateService.getPlaybackPath().contentEquals(mRecordFile.getAbsolutePath())) {
                mCreateService.loadPlaybackTrack(mRecordFile.getAbsolutePath());
            }
            configurePlaybackInfo();
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
                        mHandler.post(new Runnable() {
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
        String[] columns = { Recordings.ID };
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
            if (f.equals(mRecordFile)) continue; // ignore current file

            cursor = getContentResolver().query(Content.RECORDINGS,
                    columns,
                    Recordings.AUDIO_PATH + "='" + f.getAbsolutePath() + "'",
                    null, null);

            // XXX TODO exclude currently uploading file!
            if ((cursor == null || cursor.getCount() == 0)) {
                Recording r = new Recording(f);
                r.audio_profile = Recording.isRawFilename(f.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
                r.user_id = getCurrentUserId();

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
            Collections.sort(mUnsavedRecordings, new Comparator<Recording>(){
                public int compare(Recording r1, Recording r2)
                {
                    return Long.valueOf(r1.timestamp).compareTo(r2.timestamp);
                } });
            showDialog(Consts.Dialogs.DIALOG_UNSAVED_RECORDING);
        }
    }



    /* package */ void setRecordFile(File f) {
        mRecordFile = f;
        if (f != null) mAudioProfile = Recording.isRawFilename(f.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
    }


    public void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.formatTimestamp(elapsed));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
        }
    }

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudCreateService.RECORD_ERROR)) {
                onRecordingError();
            } else if (action.equals(CloudCreateService.PLAYBACK_COMPLETE)) {
                onPlaybackComplete();
            } else if (action.equals(CloudCreateService.PLAYBACK_ERROR)) {
                onPlaybackComplete(); // might be unknown errors, meriting proper error handling
            }
        }
    };


    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNSAVED_RECORDING:
                if (mUnsavedRecordings == null) return null;

                final CharSequence[] fileIds = new CharSequence[mUnsavedRecordings.size()];
                final boolean[] checked = new boolean[mUnsavedRecordings.size()];
                for (int i=0; i < mUnsavedRecordings.size(); i++) {
                    fileIds[i] = new Date(mUnsavedRecordings.get(i).timestamp).toLocaleString() + ", " + mUnsavedRecordings.get(i).formattedDuration();
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
                                if (mUnsavedRecordings != null) {
                                    for (int i = 0; i < mUnsavedRecordings.size(); i++){
                                        if (checked[i]){
                                            getContentResolver().insert(Content.RECORDINGS,mUnsavedRecordings.get(i).buildContentValues());
                                        } else {
                                            mUnsavedRecordings.get(i).delete(null);
                                        }
                                    }
                                }
                                mUnsavedRecordings = null;
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_reset_recording_title)
                        .setMessage(R.string.dialog_reset_recording_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                CloudUtils.deleteFile(mRecordFile);
                                mRecordFile = null;
                                mCurrentState = CreateState.IDLE_RECORD;
                                updateUi(true);
                                removeDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                            }
                        }).setNegativeButton(getString(R.string.btn_no), null)
                        .create();
            default:
                return super.onCreateDialog(which);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.UPLOAD_FILE:
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("audio/*"), REQUEST_UPLOAD_FILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;
            case REQUEST_UPLOAD_FILE:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    final Intent intent = (new Intent(Consts.ACTION_SHARE))
                            .putExtra(Intent.EXTRA_STREAM, uri);

                    final String file = uri.getLastPathSegment();
                    if (file != null && file.lastIndexOf(".") != -1) {
                        intent.putExtra(Consts.EXTRA_TITLE,
                                file.substring(0, file.lastIndexOf(".")));
                    }
                    startActivity(intent);
                }
        }
    }
}
