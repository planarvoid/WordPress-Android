package com.soundcloud.android.view.create;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.IOUtils.mkdirs;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.service.record.ICloudCreateService;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
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
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CreateController implements CreateWaveDisplay.Listener {

    public static final String TEMP_PLAY = "Play";
    public static final String TEMP_STOP = "Stop";
    private ScActivity mActivity;
    private ICloudCreateService mCreateService;
    private Recording mRecording;
    private User mPrivateUser;

    private TextView txtInstructions, txtRecordStatus, mChrono;
    private ViewGroup mFileLayout;
    private CreateWaveDisplay mWaveDisplay;
    private ImageButton btnAction;
    private File mRecordFile, mRecordDir;
    private CreateState mLastState, mCurrentState;
    private long mLastDisplayedTime,mDuration, mLastSeekEventTime;
    private String mRecordErrorMessage, mCurrentDurationString;
    private boolean mSampleInterrupted, mActive;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private Thread mProgressThread;
    private List<Recording> mUnsavedRecordings;
    private Button mResetButton, mDeleteButton, mPlayButton, mEditButton, mSaveButton;

    private android.os.Handler mHandler;
    private long mLastPos, mLastProgressTimestamp, mLastTrackTime;
    private long mProgressPeriod = 1000 / 60; // aim for 60 fps.

    private CreateListener mCreateListener;

    private Drawable
            btn_rec_states_drawable,
            btn_rec_stop_states_drawable;

    public enum CreateState {
        IDLE_STANDBY_REC,
        IDLE_STANDBY_PLAY,
        IDLE_RECORD,
        RECORD,
        IDLE_PLAYBACK,
        PLAYBACK,
        EDIT,
        EDIT_PLAYBACK
    }

    public static int REC_SAMPLE_RATE = 44100;
    public static int PCM_REC_CHANNELS = 2;
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

        mHandler = mActivity.getHandler();

        btn_rec_states_drawable = c.getResources().getDrawable(R.drawable.btn_rec_states);
        btn_rec_stop_states_drawable = c.getResources().getDrawable(R.drawable.btn_rec_pause_states);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);

        txtInstructions = (TextView) vg.findViewById(R.id.txt_instructions);
        txtRecordStatus = (TextView) vg.findViewById(R.id.txt_record_status);

        mChrono = (TextView) vg.findViewById(R.id.chronometer);
        mChrono.setVisibility(View.INVISIBLE);

        mFileLayout = (ViewGroup) vg.findViewById(R.id.file_layout);

        btnAction = (ImageButton) vg.findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentState == CreateState.IDLE_STANDBY_REC) {
                    stopRecording();
                    configureState();
                } else if (mCurrentState == CreateState.IDLE_STANDBY_PLAY) {
                    mActivity.track(Click.Record_play_stop);
                    stopPlayback();
                    onCreateServiceBound(mCreateService);
                } else {
                    switch (mCurrentState) {
                        case IDLE_RECORD:
                        case IDLE_PLAYBACK:
                        case PLAYBACK:
                            mActivity.track(Click.Record_rec);
                            mCurrentState = CreateState.RECORD;
                            break;
                        case RECORD:
                            mActivity.track(Click.Record_rec_stop);
                            mCurrentState = CreateState.IDLE_PLAYBACK;
                            break;
                    }
                    updateUi(true);
                }
            }
        });

        mResetButton = ((Button) vg.findViewById(R.id.btn_reset));
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInEditState()){
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                } else {
                    mActivity.track(Click.Record_discard);
                    mActivity.showDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);

                }
                updateUi(true);
            }
        });

        mDeleteButton = ((Button) vg.findViewById(R.id.btn_delete));
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.track(Click.Record_delete);
                mActivity.showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
            }
        });

        setResetState();

        mPlayButton = ((Button) vg.findViewById(R.id.btn_play));
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mCurrentState) {
                    case IDLE_PLAYBACK:
                        mActivity.track(Click.Record_play);
                        mCurrentState = CreateState.PLAYBACK;
                        break;
                    case PLAYBACK:
                        mActivity.track(Click.Record_play_stop);
                        mCurrentState = CreateState.IDLE_PLAYBACK;
                        break;
                    case EDIT:
                        mActivity.track(Click.Record_play);
                        mCurrentState = CreateState.EDIT_PLAYBACK;
                        break;
                    case EDIT_PLAYBACK:
                        mActivity.track(Click.Record_play_stop);
                        mCurrentState = CreateState.EDIT;
                        break;

                }
                updateUi(true);

            }
        });

        mEditButton = ((Button) vg.findViewById(R.id.btn_edit));
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.track(Click.Record_edit);
                mCurrentState = CreateState.EDIT;
                updateUi(true);
            }
        });

        mSaveButton = (Button) vg.findViewById(R.id.btn_save);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInEditState()) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    updateUi(true);
                } else {

                    mActivity.track(Click.Record_next);
                    if (mRecording == null) {
                        Recording r = new Recording(mRecordFile);
                        r.user_id = mActivity.getCurrentUserId();
                        if (mPrivateUser != null) {
                            SoundCloudDB.upsertUser(mActivity.getContentResolver(),
                                    mPrivateUser
                            );
                            r.private_user_id = mPrivateUser.id;
                            r.is_private = true;
                        }

                        try { // set duration because ogg files report incorrect
                            // duration in mediaplayer if playback is attempted
                            // after encoding
                            r.duration = mActivity.getCreateService().getPlaybackDuration();
                        } catch (RemoteException ignored) {
                        }

                        Uri newRecordingUri = mActivity.getContentResolver().insert(Content.RECORDINGS.uri, r.buildContentValues());
                        mRecording = r;
                        mRecording.id = Long.parseLong(newRecordingUri.getLastPathSegment());

                        if (mCreateListener != null) {
                            mCreateListener.onSave(newRecordingUri, mRecording, true);
                        }
                    } else {
                        //start for result, because if an upload starts, finish, playback should not longer be possible
                        if (mCreateListener != null) {
                            mCreateListener.onSave(mRecording.toUri(), mRecording, false);
                        }
                    }
                }
            }
        });

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mWaveDisplay = new CreateWaveDisplay(mActivity);
        mWaveDisplay.setTrimListener(this);
        ((FrameLayout) vg.findViewById(R.id.gauge_holder)).addView(mWaveDisplay);

        mCurrentState = CreateState.IDLE_RECORD;
        mRecordDir = IOUtils.ensureUpdatedDirectory(
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
        mWaveDisplay.reset();
        updateUi(true);
        setResetState();
    }

    @Override
    public void onSeek(float pos) {
        if (mCreateService != null) try {
            mHandler.removeCallbacks(mRefreshPositionFromService);
            mHandler.removeCallbacks(mSmoothProgress);
            mLastTrackTime = -1;
            mCreateService.seekTo(pos);
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onAdjustTrimLeft(float pos) {
        if (mCreateService != null) try { mCreateService.setPlaybackStart(pos); } catch (RemoteException ignored) {}
    }

    @Override
    public void onAdjustTrimRight(float pos) {
        if (mCreateService != null) try { mCreateService.setPlaybackEnd(pos); } catch (RemoteException ignored) {}
    }

    private void setResetState(){
        mResetButton.setVisibility(mRecording == null ? View.VISIBLE : View.GONE);
        mDeleteButton.setVisibility(mRecording == null ? View.GONE : View.VISIBLE);
    }

    public void setInstructionsText(String s){
        txtInstructions.setText(s);
    }

    public void onCreateServiceBound(ICloudCreateService createService) {
        mCreateService = createService;
        mActivity.getApp().setRecordListener(recListener);
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

            if (mRecording != null) {
                setRecordFile(mRecording.audio_path);
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
                    mHandler.postDelayed(mSmoothProgress, 0);
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

        setResetState();
        updateUi(takeAction);
    }

    private boolean shouldReactToRecording(){
        boolean react = false;
        try {
            react = shouldReactToPath(mCreateService.getRecordingPath());
        } catch (RemoteException ignored) {
        }
        return react;
    }

    private boolean shouldReactToPlayback(){
        boolean react = false;
        try {
            react = shouldReactToPath(mCreateService.getPlaybackPath());
        } catch (RemoteException ignored) {
        }
        return react;
    }

    private boolean shouldReactToPath(String path) {
        if (TextUtils.isEmpty(path)) return false;
        final long userIdFromPath = getPrivateUserIdFromPath(path);
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
        IOUtils.deleteFile(mRecordFile);
        mRecordFile = null;
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }

    void updateUi(boolean takeAction) {
        switch (mCurrentState) {
            case IDLE_RECORD:
                if (takeAction) stopPlayback();
                if (!TextUtils.isEmpty(mRecordErrorMessage)) {
                    txtRecordStatus.setText(mRecordErrorMessage);
                } else {
                    txtRecordStatus.setText(null);
                }

                btnAction.setImageDrawable(btn_rec_states_drawable);

                hideView(mPlayButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideView(mChrono, false, View.INVISIBLE);

                showView(btnAction, false);
                showView(txtInstructions, mLastState != CreateState.IDLE_RECORD);
                showView(txtRecordStatus, mLastState != CreateState.IDLE_RECORD);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mRecordFile == null && mCreateService != null) {
                            try {
                                mCreateService.startReading();
                            } catch (RemoteException ignored1) {
                            }
                        }
                    }
                });

                break;

            case IDLE_STANDBY_REC:
            case IDLE_STANDBY_PLAY:
                mPlayButton.setVisibility(View.GONE);
                mEditButton.setVisibility(View.GONE);

                btnAction.setVisibility(View.VISIBLE);
                btnAction.setImageDrawable(btn_rec_states_drawable);

                txtRecordStatus.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.INVISIBLE);
                mChrono.setVisibility(View.INVISIBLE);
                txtInstructions.setVisibility(View.VISIBLE);
                txtRecordStatus.setText(mCurrentState == CreateState.IDLE_STANDBY_REC ?
                        mActivity.getString(R.string.recording_in_progress) : mActivity.getString(R.string.playback_in_progress));
                break;

            case RECORD:
                hideView(mPlayButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideView(txtInstructions, false, View.GONE);

                showView(mChrono,mLastState == CreateState.IDLE_RECORD);
                showView(btnAction, false);
                showView(txtRecordStatus,false);

                btnAction.setImageDrawable(btn_rec_stop_states_drawable);
                txtRecordStatus.setText("");
                mChrono.setText("");

                if (takeAction) {
                    stopPlayback();
                    startRecording();
                }
                break;

            case IDLE_PLAYBACK:
                if (takeAction) {
                    switch (mLastState) {
                        case RECORD: stopRecording(); break;
                        case PLAYBACK:
                        case EDIT:
                        case EDIT_PLAYBACK:
                            mWaveDisplay.resetTrim();
                            try {
                                if (mCreateService != null && mCreateService.isPlayingBack())  {
                                    mCreateService.pausePlayback();
                                }
                            } catch (RemoteException ignored) {}
                            mHandler.removeCallbacks(mSmoothProgress);
                            break;
                    }
                }

                boolean animate = mLastState == CreateState.RECORD
                        || mLastState == CreateState.EDIT
                        || mLastState == CreateState.EDIT_PLAYBACK;

                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton,(mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mEditButton,(mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(btnAction,(mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mFileLayout,(mLastState == CreateState.RECORD));
                showView(mChrono,false);

                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordStatus,false,View.INVISIBLE);

                mPlayButton.setText(TEMP_PLAY);
                mChrono.setText(mCurrentDurationString);
                btnAction.setImageDrawable(btn_rec_states_drawable);

                mWaveDisplay.gotoPlaybackMode();
                setResetState();
                break;

            case PLAYBACK:
                showView(btnAction,false);
                showView(mPlayButton,false);
                showView(mEditButton,false);
                showView(mFileLayout,false);
                showView(mChrono,false);

                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordStatus,false,View.INVISIBLE);

                mPlayButton.setText(TEMP_STOP);
                btnAction.setImageDrawable(btn_rec_states_drawable);

                setResetState();

                if (takeAction) startPlayback();
                break;

            case EDIT:
            case EDIT_PLAYBACK:
                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                showView(mResetButton,false);
                showView(mFileLayout,false);

                hideView(btnAction, false, View.GONE);
                hideView(mEditButton, false, View.GONE);
                hideView(mDeleteButton,false, View.GONE);

                mPlayButton.setText(mCurrentState == CreateState.EDIT ? TEMP_PLAY : TEMP_STOP);

                if (takeAction) {
                    if (mCurrentState == CreateState.EDIT_PLAYBACK) {
                        startPlayback();
                    } else {
                        try {
                            if (mCreateService != null && mCreateService.isPlayingBack()) {
                                mCreateService.pausePlayback();
                            }
                        } catch (RemoteException ignored) {
                        }
                        mHandler.removeCallbacks(mSmoothProgress);
                        break;
                    }
                }
                break;
        }

        final boolean inEditState = isInEditState();
        mResetButton.setText(inEditState ? mActivity.getResources().getString(R.string.btn_revert_to_original) : mActivity.getResources().getString(R.string.reset) );
        mSaveButton.setText(inEditState ? mActivity.getResources().getString(R.string.btn_save) : mActivity.getResources().getString(R.string.btn_next));
        mWaveDisplay.setInEditMode(inEditState);

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    private boolean isInEditState(){
        return mCurrentState == CreateState.EDIT || mCurrentState == CreateState.EDIT_PLAYBACK;
    }

    private void startRecording() {
        mActivity.pause();

        mRecordErrorMessage = "";
        mSampleInterrupted = false;

        mLastDisplayedTime = 0;
        mChrono.setText("0.00");

        mRemainingTimeCalculator.reset();
        mWaveDisplay.gotoRecordMode();

        if (!IOUtils.isSDCardAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_storage_is_full);
        }

        final boolean hiQ = PreferenceManager.getDefaultSharedPreferences(mActivity)
            .getString("defaultRecordingQuality", "high")
            .equals("high");
        /*
        if (hiQ && SoundCloudApplication.DEV_MODE
                && !PreferenceManager.getDefaultSharedPreferences(mActivity)
                        .getString("dev.defaultRecordingHighQualityType", "compressed")
                        .equals("compressed")) {
            //force raw for developer mode
            mAudioProfile = CloudRecorder.Profile.RAW;
        } else  {
            mAudioProfile = hiQ ? CloudRecorder.Profile.best() : CloudRecorder.Profile.low();
        }  */

        if (mRecordFile == null){
            if (mPrivateUser != null) {
                mRecordFile = new File(mRecordDir, System.currentTimeMillis() + "_" + mPrivateUser.id);
            } else {
                mRecordFile = new File(mRecordDir, String.valueOf(System.currentTimeMillis()));
            }
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
                mCreateService.startRecording(mRecordFile.getAbsolutePath());
                //noinspection ResultOfMethodCallIgnored
                mRecordFile.setLastModified(System.currentTimeMillis());
            } catch (RemoteException ignored) {}
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

            mCurrentState = mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK;
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
                if (mCurrentState == CreateState.IDLE_RECORD || mCurrentState == CreateState.RECORD) {
                    mWaveDisplay.updateAmplitude(maxAmplitude, mCurrentState == CreateState.RECORD);
                    if (mCurrentState == CreateState.RECORD)  onRecProgressUpdate(elapsed);
                }
            }
        }
    };

    private void stopRecording() {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        try {
            if (mCreateService != null) {
                mCreateService.stopRecording();
            }
        } catch (RemoteException ignored) { }

        // disable actions during processing and playback preparation
        btnAction.setEnabled(false);
        loadPlaybackTrack();
    }

    private void loadPlaybackTrack() {
        try {
            if (mCreateService != null && mRecordFile != null) {
                // might be loaded and paused already
                if (TextUtils.isEmpty(mCreateService.getPlaybackPath()) ||
                    !mCreateService.getPlaybackPath().equals(mRecordFile.getAbsolutePath())) {
                    mCreateService.loadPlaybackTrack(mRecordFile.getAbsolutePath());
                }
                configurePlaybackInfo();
            }
        } catch (RemoteException ignored) { }
    }

    private void configurePlaybackInfo(){
        try {
            mCurrentDurationString =  CloudUtils.formatTimestamp(getDuration());
            final long currentPlaybackPosition = mCreateService.getCurrentPlaybackPosition();
            if (currentPlaybackPosition >= 0 && currentPlaybackPosition < getDuration()) {
                mWaveDisplay.setProgress(((float) currentPlaybackPosition) / getDuration());
            } else {
                mWaveDisplay.setProgress(-1f);
            }
        } catch (RemoteException ignored) {}
    }

    private long getDuration() throws RemoteException{
        if (mDuration <= 0){
            mDuration = mCreateService.getPlaybackDuration();
        }
        return mDuration;
    }


    private void onPlaybackComplete(){
        mHandler.removeCallbacks(mSmoothProgress);
        if (mCurrentState == CreateState.PLAYBACK || mCurrentState == CreateState.EDIT_PLAYBACK) {
            mCurrentState = mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK;
            loadPlaybackTrack();
            updateUi(true);
        }
    }

    private void startPlayback() {
        mLastPos = -1;
        mLastTrackTime = -1;
        try {
            if (!mCreateService.isPlayingBack()) {
                mActivity.track(Click.Record_play);
                mCreateService.startPlayback(); //might already be playing back if activity just created
            }
        } catch (RemoteException ignores) { }

    }

    private Runnable mSmoothProgress = new Runnable() {
        public void run() {
            if (mCurrentState == CreateState.PLAYBACK || mCurrentState == CreateState.EDIT_PLAYBACK) {
                if (mLastTrackTime == -1) {
                    mHandler.post(mRefreshPositionFromService);
                } else {
                    final long posMs = mLastTrackTime + System.currentTimeMillis() - mLastProgressTimestamp;
                    final long pos = posMs / 1000;

                    if (mLastPos != pos) {
                        mLastPos = pos;
                        mChrono.setText(new StringBuilder()
                                .append(CloudUtils.formatTimestamp(posMs))
                                .append(" / ")
                                .append(mCurrentDurationString));
                    }
                    setProgressInternal(posMs);
                }
                mHandler.postDelayed(this, mProgressPeriod);
            }
        }
    };

    private Runnable mRefreshPositionFromService = new Runnable() {
        @Override
        public void run() {
            final boolean stillPlaying;
            try {
                stillPlaying = mCreateService.isPlayingBack();
                mLastTrackTime = stillPlaying ? mCreateService.getCurrentPlaybackPosition() : getDuration();
                mLastProgressTimestamp = System.currentTimeMillis();
                if (stillPlaying) mHandler.postDelayed(this, 500);

            } catch (RemoteException ignored) {}
        }
    };

    protected void setProgressInternal(long pos) {

        try {
            final long duration = getDuration();
            if (duration != 0){
                mWaveDisplay.setProgress(((float) Math.max(0,Math.min(pos,duration))) / duration);
            }

        } catch (RemoteException ignored) { }
    }

    private void stopPlayback() {
        mHandler.removeCallbacks(mSmoothProgress);
        mDuration = 0;
        try {
            mCreateService.stopPlayback();
        } catch (RemoteException ignored) { }
    }


    private void checkUnsavedFiles() {
        String[] columns = { DBHelper.Recordings._ID };
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

            cursor = mActivity.getContentResolver().query(Content.RECORDINGS.uri,
                    columns,
                    DBHelper.Recordings.AUDIO_PATH + " = ?",
                    new String[]{f.getAbsolutePath()}, null);

            // XXX TODO exclude currently uploading file!
            if ((cursor == null || cursor.getCount() == 0)) {
                Recording r = new Recording(f);
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
            }
        }
    }

    private void showView(final View v, boolean animate) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
            if (animate) AnimUtils.runFadeInAnimationOn(mActivity, v);
        }
    }

    private void hideView(final View v, boolean animate, final int visibilityOnComplete){
        if (v.getVisibility() == View.VISIBLE){
            if (animate){
                v.setEnabled(false);
                AnimUtils.runFadeOutAnimationOn(mActivity, v);
                v.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        v.setVisibility(visibilityOnComplete);
                        v.setEnabled(true);
                    }
                });
            } else {
                v.setVisibility(visibilityOnComplete);
            }
        }
    }

    /* package */ void setRecordFile(File f) {
        mRecordFile = f;
    }


    public void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.formatTimestamp(elapsed).toString());
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
        uploadFilter.addAction(CloudCreateService.PLAYBACK_STARTED);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_COMPLETE);
        uploadFilter.addAction(CloudCreateService.PLAYBACK_ERROR);
        mActivity.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));
    }

    public void onPause() {
        mActive = false;
        if (mCurrentState != CreateState.RECORD && mCreateService != null){
            try {
                mCreateService.stopRecording(); // this will stop the amplitude reading loop
            } catch (RemoteException ignored) {}
        }
    }

    public void onResume() {
        if (mCreateService != null){
            configureState();
        }
        mActive = true;
    }

    public void onStop() {
        mHandler.removeCallbacks(mSmoothProgress);
        mActivity.unregisterReceiver(mUploadStatusListener);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void onDestroy() {
        if (mActivity.getApp().getRecordListener() == recListener) {
            mActivity.getApp().setRecordListener(null);
        }
    }

    public static long getPrivateUserIdFromPath(String path){
        if (!path.contains("_") || path.indexOf("_") + 1 >= path.length()) {
            return -1;
        } else {
            try {
                return Long.valueOf(path.substring(path.indexOf("_")+1,path.contains(".") ? path.indexOf(".") : path.length()));
            } catch (NumberFormatException ignored) {

            } catch (StringIndexOutOfBoundsException ignored) {

            }
            return -1;
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
            } else if (action.equals(CloudCreateService.PLAYBACK_STARTED)) {
                mLastTrackTime = intent.getLongExtra("position",0);
                mLastProgressTimestamp = System.currentTimeMillis();
                mHandler.postDelayed(mSmoothProgress, 0);

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
                                            mActivity.getContentResolver().insert(Content.RECORDINGS.uri, mUnsavedRecordings.get(i).buildContentValues());
                                        } else {
                                            mUnsavedRecordings.get(i).delete(null);
                                        }
                                    }
                                }
                                mUnsavedRecordings = null;
                            }
                        }).create();
            case Consts.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(mActivity)
                        .setTitle(null)
                        .setMessage(R.string.dialog_reset_recording_message).setPositiveButton(
                                android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mActivity.track(Click.Record_discard__ok);

                                IOUtils.deleteFile(mRecordFile);
                                mActivity.removeDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                                if (mCreateListener != null) mCreateListener.onCancel();
                            }
                        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mActivity.track(Click.Record_discard_cancel);
                            }
                        })
                        .create();

            case Consts.Dialogs.DIALOG_DELETE_RECORDING:
             return new AlertDialog.Builder(mActivity)
                            .setTitle(null)
                            .setMessage(R.string.dialog_confirm_delete_recording_message)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            if (mRecording != null) mRecording.delete(mActivity.getContentResolver());
                                            if (mCreateListener != null) mCreateListener.onDelete();
                                        }
                                    })
                            .setNegativeButton(android.R.string.no, null)
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
