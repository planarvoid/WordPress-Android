package com.soundcloud.android.view.create;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.IOUtils.mkdirs;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.CloudRecorder;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
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
    private CloudCreateService mCreateService;
    private Recording mRecording;
    private File mRecordFile;
    private User mPrivateUser;

    private File mRecordDir;
    private CreateState mLastState, mCurrentState;
    private long mLastDisplayedTime;
    private long mDuration;

    private TextView txtInstructions, txtRecordMessage, mChrono;
    private ViewGroup mFileLayout;
    private ImageButton mActionButton;
    private CreateWaveDisplay mWaveDisplay;
    private Button mResetButton, mDeleteButton, mPlayButton, mEditButton, mSaveButton;
    private String mRecordErrorMessage, mCurrentDurationString;

    private boolean mSampleInterrupted, mActive;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private List<Recording> mUnsavedRecordings;

    private Handler mHandler;
    private long mLastPos, mLastProgressTimestamp, mLastTrackTime;

    private static final long PROGRESS_PERIOD = 1000 / 60; // aim for 60 fps.

    private CreateListener mCreateListener;

    private Drawable btn_rec_states_drawable, btn_rec_stop_states_drawable;

    public enum CreateState {
        IDLE_STANDBY_REC,
        IDLE_STANDBY_PLAY,
        IDLE_RECORD,
        RECORD,
        IDLE_PLAYBACK,
        PLAYBACK,
        EDIT,
        EDIT_PLAYBACK;

        public boolean isEdit() { return this == EDIT || this == EDIT_PLAYBACK; }
    }

    public static int PCM_REC_MAX_FILE_SIZE = -1;

    public CreateController(ScActivity c, ViewGroup vg, Uri recordingUri) {
        this(c, vg, recordingUri, null);
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

        mRemainingTimeCalculator = CloudCreateService.DEFAULT_CONFIG.createCalculator();

        txtInstructions = (TextView) vg.findViewById(R.id.txt_instructions);
        txtRecordMessage = (TextView) vg.findViewById(R.id.txt_record_message);

        mChrono = (TextView) vg.findViewById(R.id.chronometer);
        mChrono.setVisibility(View.INVISIBLE);

        mFileLayout = (ViewGroup) vg.findViewById(R.id.file_layout);

        mActionButton = setupActionButton(vg);
        mResetButton  = setupResetButton(vg);
        mDeleteButton = setupDeleteButton(vg);
        mPlayButton = setupPlaybutton(vg);
        mEditButton = setupEditButton(vg);
        mSaveButton = setupSaveButton(vg);

        mWaveDisplay = new CreateWaveDisplay(mActivity);
        mWaveDisplay.setTrimListener(this);
        ((ViewGroup) vg.findViewById(R.id.gauge_holder)).addView(mWaveDisplay);

        mCurrentState = CreateState.IDLE_RECORD;
        mRecordDir = IOUtils.ensureUpdatedDirectory(
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".rec"));
        mkdirs(mRecordDir);
        setResetState();
        updateUi(false);
    }

    private Button setupResetButton(ViewGroup vg) {
        final Button button = ((Button) vg.findViewById(R.id.btn_reset));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState.isEdit()) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                } else {
                    mActivity.track(Click.Record_discard);
                    mActivity.showDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                }
                updateUi(true);
            }
        });
        return button;
    }
    private ImageButton setupActionButton(ViewGroup vg) {
        final ImageButton button = (ImageButton) vg.findViewById(R.id.btn_action);
        button.setOnClickListener(new View.OnClickListener() {
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
        return button;
    }
    private Button setupDeleteButton(ViewGroup vg) {
        final Button button = ((Button) vg.findViewById(R.id.btn_delete));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.track(Click.Record_delete);
                mActivity.showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
            }
        });
        return button;
    }
    private Button setupPlaybutton(ViewGroup vg) {
        final Button button = ((Button) vg.findViewById(R.id.btn_play));
        button.setOnClickListener(new View.OnClickListener() {
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
        return button;
    }
    private Button setupEditButton(ViewGroup vg) {
        Button button = ((Button) vg.findViewById(R.id.btn_edit));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.track(Click.Record_edit);
                mCurrentState = CreateState.EDIT;
                updateUi(true);
            }
        });
        return button;
    }
    private Button setupSaveButton(ViewGroup vg) {
        final Button button = (Button) vg.findViewById(R.id.btn_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentState.isEdit()) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    updateUi(true);
                } else {
                    mActivity.track(Click.Record_next);
                    if (mRecording == null) {
                        Recording r = new Recording(mRecordFile);
                        r.user_id = mActivity.getCurrentUserId();

                        if (mPrivateUser != null) {
                            SoundCloudDB.upsertUser(mActivity.getContentResolver(), mPrivateUser);
                            r.private_user_id = mPrivateUser.id;
                            r.is_private = true;
                        }

                        // set duration because ogg files report incorrect
                        // duration in mediaplayer if playback is attempted
                        // after encoding
                        r.duration = mActivity.getCreateService().getPlaybackDuration();

                        mRecording = SoundCloudDB.insertRecording(mActivity.getContentResolver(), r);

                        if (mCreateListener != null) {
                            mCreateListener.onSave(mRecording, true);
                        }
                    } else {
                        //start for result, because if an upload starts, finish, playback should not longer be possible
                        if (mCreateListener != null) {
                            mCreateListener.onSave(mRecording, false);
                        }
                    }
                }
            }
        });
        return button;
    }

    public void reset() {
        mCurrentState = CreateState.IDLE_RECORD;
        mRecordFile = null;
        mWaveDisplay.reset();
        updateUi(true);
        setResetState();
    }

    @Override
    public void onSeek(float pos) {
        if (mCreateService != null) {
            mHandler.removeCallbacks(mRefreshPositionFromService);
            mHandler.removeCallbacks(mSmoothProgress);
            mLastTrackTime = -1;
            mCreateService.seekTo(pos);
        }
    }

    @Override
    public void onAdjustTrimLeft(float pos) {
        if (mCreateService != null) { mCreateService.setPlaybackStart(pos); }
    }

    @Override
    public void onAdjustTrimRight(float pos) {
        if (mCreateService != null) { mCreateService.setPlaybackEnd(pos); }
    }

    private void setResetState(){
        mResetButton.setVisibility(mRecording == null ? View.VISIBLE : View.GONE);
        mDeleteButton.setVisibility(mRecording == null ? View.GONE : View.VISIBLE);
    }

    public void setInstructionsText(String s){
        txtInstructions.setText(s);
    }

    public void onCreateServiceBound(CloudCreateService createService) {
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

        if (mRecording != null) {
            setRecordFile(mRecording.audio_path);
            mDuration = mRecording.duration;
        }

        if (mCreateService.isRecording() && mRecording == null){
            if (shouldReactToRecording()) {
                mCurrentState = CreateState.RECORD;
                setRecordFile(mCreateService.getRecordingFile());
            } else {
                mCurrentState = CreateState.IDLE_STANDBY_REC;
            }
        } else if ( mCreateService.isPlaying()) {
            if (shouldReactToPlayback()) {
                if (mCurrentState != CreateState.EDIT_PLAYBACK) mCurrentState = CreateState.PLAYBACK;
                setRecordFile(mCreateService.getCurrentPlaybackPath());
                configurePlaybackInfo();
                mHandler.postDelayed(mSmoothProgress, 0);
                takeAction = true;
            } else {
                mCurrentState = CreateState.IDLE_STANDBY_PLAY;
            }
        } else if (!mRecordDir.exists()) {
            // can happen when there's no mounted sd card
            mActionButton.setEnabled(false);
        } else {

            if (mRecordFile == null && mPrivateUser != null) {
                checkForUnusedPrivateFile();
            }

            if (mRecordFile != null) {
                if (mCurrentState != CreateState.EDIT) mCurrentState = CreateState.IDLE_PLAYBACK;
                configurePlaybackInfo();
            } else {
                mCurrentState = CreateState.IDLE_RECORD;
                takeAction = true;
            }
        }

        if (!(mCurrentState == CreateState.RECORD || mCurrentState == CreateState.IDLE_STANDBY_REC || mCurrentState == CreateState.IDLE_STANDBY_PLAY)
                && mRecordDir != null && mRecordDir.exists() && mPrivateUser == null) {
//                checkUnsavedFiles();
        }

        setResetState();
        updateUi(takeAction);
    }

    private boolean shouldReactToRecording(){
        return shouldReactToPath(mCreateService.getRecordingFile());
    }

    private boolean shouldReactToPlayback(){
        return shouldReactToPath(mCreateService.getCurrentPlaybackPath());
    }

    private boolean shouldReactToPath(File file) {
        if (file == null) return false;
        final long userIdFromPath = getPrivateUserIdFromPath(file);
        return ((userIdFromPath == -1 && mPrivateUser == null) || (mPrivateUser != null && userIdFromPath == mPrivateUser.id));
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        state.putString("createCurrentRecordFilePath", mRecordFile != null ? mRecordFile.getAbsolutePath() : "");
        mWaveDisplay.onSaveInstanceState(state);
    }

    public void onRestoreInstanceState(Bundle state) {
        if (state.isEmpty()) return;
        if (!TextUtils.isEmpty(state.getString("createCurrentRecordFilePath"))) {
            setRecordFile(new File(state.getString("createCurrentRecordFilePath")));
        }
        if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
            mCurrentState = CreateState.valueOf(state.getString("createCurrentCreateState"));
            updateUi(false);
        }
        mWaveDisplay.onRestoreInstanceState(state);
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
                    txtRecordMessage.setText(mRecordErrorMessage);
                } else {
                    txtRecordMessage.setText(getRandomSuggestion());
                }

                mActionButton.setImageDrawable(btn_rec_states_drawable);

                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideView(mChrono, false, View.INVISIBLE);

                showView(mActionButton, false);
                showView(txtInstructions, takeAction && mLastState != CreateState.IDLE_RECORD);
                showView(txtRecordMessage, takeAction && mLastState != CreateState.IDLE_RECORD);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mRecordFile == null && mCreateService != null) {
                            mCreateService.startReading();
                        }
                    }
                });

                break;

            case IDLE_STANDBY_REC:
            case IDLE_STANDBY_PLAY:
                mPlayButton.setVisibility(View.GONE);
                mEditButton.setVisibility(View.GONE);

                mActionButton.setVisibility(View.VISIBLE);
                mActionButton.setImageDrawable(btn_rec_states_drawable);

                txtRecordMessage.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.INVISIBLE);
                mChrono.setVisibility(View.INVISIBLE);
                txtInstructions.setVisibility(View.VISIBLE);
                txtRecordMessage.setText(mCurrentState == CreateState.IDLE_STANDBY_REC ?
                        mActivity.getString(R.string.recording_in_progress) : mActivity.getString(R.string.playback_in_progress));
                break;

            case RECORD:
                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideView(txtInstructions, false, View.GONE);

                showView(mChrono, takeAction && mLastState == CreateState.IDLE_RECORD);
                showView(mActionButton, false);
                showView(txtRecordMessage,false);

                mActionButton.setImageDrawable(btn_rec_stop_states_drawable);
                txtRecordMessage.setText("");
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
                            if (mCreateService != null) {
                                mCreateService.revertFile();
                                if (mCreateService.isPlaying()) {
                                    mCreateService.pausePlayback();
                                }
                            }
                            mHandler.removeCallbacks(mSmoothProgress);
                            break;
                    }
                    mWaveDisplay.gotoPlaybackMode();
                }

                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton, takeAction && (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mEditButton, takeAction && (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mActionButton, takeAction && (mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mFileLayout, takeAction && (mLastState == CreateState.RECORD));
                showView(mChrono,false);

                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordMessage,false,View.INVISIBLE);

                mPlayButton.setText(TEMP_PLAY);
                mChrono.setText(mCurrentDurationString);
                mActionButton.setImageDrawable(btn_rec_states_drawable);


                setResetState();
                break;

            case PLAYBACK:
                showView(mActionButton,false);
                showView(mPlayButton,false);
                showView(mEditButton,false);
                showView(mFileLayout,false);
                showView(mChrono,false);

                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordMessage,false,View.INVISIBLE);

                mPlayButton.setText(TEMP_STOP);
                mActionButton.setImageDrawable(btn_rec_states_drawable);

                setResetState();

                if (takeAction) startPlayback();
                break;

            case EDIT:
            case EDIT_PLAYBACK:
                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                showView(mResetButton,false);
                showView(mFileLayout,false);

                hideView(mActionButton, false, View.GONE);
                hideView(mEditButton, false, View.GONE);
                hideView(mDeleteButton,false, View.GONE);
                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordMessage,false,View.INVISIBLE);

                mPlayButton.setText(mCurrentState == CreateState.EDIT ? TEMP_PLAY : TEMP_STOP);

                if (takeAction) {
                    if (mCurrentState == CreateState.EDIT_PLAYBACK) {
                        startPlayback();
                    } else {
                        if (mCreateService != null && mCreateService.isPlaying()) {
                            mCreateService.pausePlayback();
                        }
                        mHandler.removeCallbacks(mSmoothProgress);
                        break;
                    }
                }
                break;
        }

        final boolean inEditState = mCurrentState.isEdit();
        mResetButton.setText(inEditState ? mActivity.getResources().getString(R.string.btn_revert_to_original) : mActivity.getResources().getString(R.string.reset) );
        mSaveButton.setText(inEditState ? mActivity.getResources().getString(R.string.btn_save) : mActivity.getResources().getString(R.string.btn_next));
        mWaveDisplay.setIsEditing(inEditState);

        mLastState = mCurrentState;
        mActionButton.setEnabled(true);
    }

    private CharSequence getRandomSuggestion() {
        // XXX
        return "Why don't you record the sounds of your street?";
    }

    private void startRecording() {
        mActivity.pausePlayback();
        mRecordErrorMessage = null;
        mSampleInterrupted = false;
        mLastDisplayedTime = 0;
        mChrono.setText("0.00");
        mRemainingTimeCalculator.reset();
        updateTimeRemaining();

        mWaveDisplay.gotoRecordMode();

        if (!IOUtils.isSDCardAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = mActivity.getResources().getString(R.string.record_storage_is_full);
        }

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

            if (PCM_REC_MAX_FILE_SIZE > 0) {
                mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, PCM_REC_MAX_FILE_SIZE);
            }

            mCreateService.startRecording(mRecordFile);
            //noinspection ResultOfMethodCallIgnored
            mRecordFile.setLastModified(System.currentTimeMillis());
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

        if (t < 300){
            if (t < 60) {
                txtRecordMessage.setText(mActivity.getResources().getQuantityString(R.plurals.seconds_available, (int) t, t));
            } else {
                txtRecordMessage.setText(mActivity.getResources().getQuantityString(R.plurals.minutes_available, (int) (t / 60 + 1),
                        (t / 60 + 1)));
            }
            txtRecordMessage.setVisibility(View.VISIBLE);
        } else {
            txtRecordMessage.setVisibility(View.INVISIBLE);
        }
    }

    public void onFrameUpdate(float maxAmplitude, long elapsed) {
        if (mCurrentState == CreateState.IDLE_RECORD || mCurrentState == CreateState.RECORD) {
            mWaveDisplay.updateAmplitude(maxAmplitude, mCurrentState == CreateState.RECORD);
            if (mCurrentState == CreateState.RECORD)  onRecProgressUpdate(elapsed);
        }
    }
    private void stopRecording() {
        if (mCreateService != null) {
            mCreateService.stopRecording();
        }

        // disable actions during processing and playback preparation
        mActionButton.setEnabled(false);
        configurePlaybackInfo();
    }


    private void configurePlaybackInfo() {
        mCurrentDurationString = CloudUtils.formatTimestamp(getDuration());
        final long currentPlaybackPosition = mCreateService.getCurrentPlaybackPosition();
        if (currentPlaybackPosition >= 0 && currentPlaybackPosition < getDuration()) {
            mWaveDisplay.setProgress(((float) currentPlaybackPosition) / getDuration());
        } else {
            mWaveDisplay.setProgress(-1f);
        }
    }

    private long getDuration() {
        if (mDuration <= 0){
            mDuration = mCreateService.getPlaybackDuration();
        }
        return mDuration;
    }


    private void onPlaybackComplete(){
        mHandler.removeCallbacks(mSmoothProgress);
        if (mCurrentState == CreateState.PLAYBACK || mCurrentState == CreateState.EDIT_PLAYBACK) {
            mCurrentState = mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK;
            configurePlaybackInfo();
            updateUi(true);
        }
    }

    private void startPlayback() {
        mLastPos = -1;
        mLastTrackTime = -1;
        if (!mCreateService.isPlaying()) {  //might already be playing back if activity just created
            mActivity.track(Click.Record_play);

            try {
                mCreateService.startPlayback(mRecordFile);
            } catch (IOException e) {
                CloudUtils.showToast(mCreateService, "Could not start playback");
               Log.w(TAG, e);
            }
        }
    }

    private final Runnable mSmoothProgress = new Runnable() {
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
                mHandler.postDelayed(this, PROGRESS_PERIOD);
            }
        }
    };

    private final Runnable mRefreshPositionFromService = new Runnable() {
        @Override
        public void run() {
            final boolean stillPlaying;
            stillPlaying = mCreateService.isPlaying();
            mLastTrackTime = stillPlaying ? mCreateService.getCurrentPlaybackPosition() : getDuration();
            mLastProgressTimestamp = System.currentTimeMillis();
            if (stillPlaying) mHandler.postDelayed(this, 500);
        }
    };

    private void setProgressInternal(long pos) {
        final long duration = getDuration();
        if (duration != 0){
            mWaveDisplay.setProgress(((float) Math.max(0,Math.min(pos,duration))) / duration);
        }
    }

    private void stopPlayback() {
        mHandler.removeCallbacks(mSmoothProgress);
        mDuration = 0;
        mCreateService.stopPlayback();
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
            if (f.equals(mRecordFile) || getPrivateUserIdFromPath(f) != -1) continue; // ignore current file

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

            final long filePrivateUserId = getPrivateUserIdFromPath(f);
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


    private void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.formatTimestamp(elapsed));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
        }
    }

    public void onStart(){
        IntentFilter recordFilter = CloudRecorder.getIntentFilter();

        // XXX still using global broadcast
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);

        mActivity.registerReceiver(mStatusListener, uploadFilter);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mStatusListener, recordFilter);
    }

    public void onPause() {
        mActive = false;
        if (mCurrentState != CreateState.RECORD && mCreateService != null){
            mCreateService.stopRecording(); // this will stop the amplitude reading loop
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
        mActivity.unregisterReceiver(mStatusListener);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mStatusListener);
    }

    public void onDestroy() {
    }

    public static long getPrivateUserIdFromPath(File file) {
        final String path = file.getAbsolutePath();

        if (!path.contains("_") || path.indexOf("_") + 1 >= path.length()) {
            return -1;
        } else {
            try {
                return Long.valueOf(path.substring(path.indexOf("_")+1,path.contains(".") ? path.indexOf(".") : path.length()));
            } catch (NumberFormatException ignored) {

            } catch (StringIndexOutOfBoundsException ignored) {
                // LAZY XXX
            }
            return -1;
        }
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CloudCreateService.RECORD_STARTED.equals(action) &&
                    (mCurrentState == CreateState.IDLE_PLAYBACK || mCurrentState == CreateState.PLAYBACK)) {
                // this will happen if recording starts from somewhere else. just reset as the player will have to be reloaded anyway
                stopPlayback();
                reset();
            } else if (CloudCreateService.RECORD_PROGRESS.equals(action)) {
                onFrameUpdate(
                        intent.getFloatExtra(CloudCreateService.EXTRA_AMPLITUDE, -1f),
                        intent.getLongExtra(CloudCreateService.EXTRA_ELAPSEDTIME, -1l));

            } else if (CloudCreateService.RECORD_ERROR.equals(action)) {
                onRecordingError();
            } else if (CloudCreateService.PLAYBACK_STARTED.equals(action)) {
                mLastTrackTime = intent.getLongExtra(CloudCreateService.EXTRA_POSITION,0);
                mLastProgressTimestamp = System.currentTimeMillis();
                mHandler.postDelayed(mSmoothProgress, 0);
            } else if (CloudCreateService.PLAYBACK_COMPLETE.equals(action) || CloudCreateService.PLAYBACK_ERROR.equals(action)) {
                String path = intent.getStringExtra(CloudCreateService.EXTRA_PATH);
                if (path != null && shouldReactToPath(new File(path))) {
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
                                        if (checked[i]) {
                                            SoundCloudDB.insertRecording(mActivity.getContentResolver(), mUnsavedRecordings.get(i));
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
        void onSave(Recording recording, boolean isNew);
        void onCancel();
        void onDelete();
    }
}
