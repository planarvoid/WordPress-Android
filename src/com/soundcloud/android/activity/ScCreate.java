package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.CloudRecorder;
import com.soundcloud.android.record.RemainingTimeCalculator;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.create.CreateWaveDisplay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Tracking(page = Page.Record_main)
public class ScCreate extends ScActivity implements CreateWaveDisplay.Listener {

    public static final int REQUEST_UPLOAD_FILE = 1;
    public static final String EXTRA_PRIVATE_MESSAGE_RECIPIENT = "privateMessageRecipient";

    private Recording mRecording;
    private User mPrivateUser;

    private CreateState mLastState, mCurrentState;
    private long mLastDisplayedTime;

    private TextView txtInstructions, txtRecordMessage, mChrono;
    private ViewGroup mEditControls;
    private ViewGroup mFileLayout;
    private ImageButton mActionButton;
    private CreateWaveDisplay mWaveDisplay;
    private Button mResetButton, mDeleteButton, mPlayButton, mEditButton, mSaveButton, mPlayEditButton;
    private ToggleButton mToggleOptimize, mToggleFade;
    private String mRecordErrorMessage, mCurrentDurationString;

    private boolean mSampleInterrupted, mActive, mHasEditControlGroup;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private List<Recording> mUnsavedRecordings;

    private Handler mHandler;
    private long mLastPos, mLastProgressTimestamp, mLastTrackTime;

    private static final long PROGRESS_PERIOD = 1000 / 60; // aim for 60 fps.

    private Drawable mRecStatesDrawable, mRecStopStatesDrawable, mPlayBgDrawable, mPauseBgDrawable;
    private String[] mRecordSuggestions;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sc_create);

        final Uri recordingUri = getIntent().getData();
        if (recordingUri != null){
            mRecording = Recording.fromUri(recordingUri, getContentResolver());
            if (mRecording == null){
                showToast(R.string.error_getting_recording);
            }
        }

        if (getIntent().hasExtra(EXTRA_PRIVATE_MESSAGE_RECIPIENT)){
            mPrivateUser = getIntent().getParcelableExtra(EXTRA_PRIVATE_MESSAGE_RECIPIENT);
        }

        mHandler = new Handler();

        mRecStatesDrawable = getResources().getDrawable(R.drawable.btn_rec_states);
        mRecStopStatesDrawable = getResources().getDrawable(R.drawable.btn_rec_pause_states);

        mRemainingTimeCalculator = AudioConfig.DEFAULT.createCalculator();

        txtInstructions = (TextView) findViewById(R.id.txt_instructions);
        if (mPrivateUser != null){
            txtInstructions.setText(getResources().getString(R.string.private_message_title, mPrivateUser.username));
        }


        txtRecordMessage = (TextView) findViewById(R.id.txt_record_message);

        mChrono = (TextView) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.INVISIBLE);

        mFileLayout = (ViewGroup) findViewById(R.id.file_layout);
        mEditControls = (ViewGroup) findViewById(R.id.edit_controls);
        mHasEditControlGroup = mEditControls != null;

        mActionButton = setupActionButton();
        mResetButton  = setupResetButton();
        mDeleteButton = setupDeleteButton();
        mEditButton = setupEditButton();
        mSaveButton = setupSaveButton();
        mPlayButton = setupPlaybutton(R.id.btn_play);
        mPlayEditButton = setupPlaybutton(R.id.btn_play_edit);
        mToggleFade = setupToggleFade();
        mToggleOptimize = setupToggleOptimize();

        mWaveDisplay = new CreateWaveDisplay(this);
        mWaveDisplay.setTrimListener(this);
        ((ViewGroup) findViewById(R.id.gauge_holder)).addView(mWaveDisplay);

        mRecordSuggestions = getResources().getStringArray(R.array.record_suggestions);


        mCurrentState = CreateState.IDLE_RECORD;

        setResetState();
        updateUi(false);
    }

    public void onStart(){
        super.onStart();
        IntentFilter recordFilter = CloudRecorder.getIntentFilter();

        // XXX still using global broadcast
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(UploadService.UPLOAD_ERROR);
        uploadFilter.addAction(UploadService.UPLOAD_CANCELLED);
        uploadFilter.addAction(UploadService.UPLOAD_SUCCESS);

        this.registerReceiver(mStatusListener, uploadFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStatusListener, recordFilter);
    }

    public void onPause() {
        super.onPause();
        mActive = false;
        if (mCurrentState != CreateState.RECORD && mCreateService != null){
            mCreateService.stopRecording(); // this will stop the amplitude reading loop
        }
    }

    public void onResume() {
        super.onResume();
        if (mCreateService != null){
            configureState();
        }
        mActive = true;
    }

    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mSmoothProgress);
        this.unregisterReceiver(mStatusListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusListener);
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        state.putString("createCurrentRecordFilePath", mRecording != null ? mRecording.getAbsolutePath() : "");
        mWaveDisplay.onSaveInstanceState(state);
    }

    public void onRestoreInstanceState(Bundle state) {
        if (state.isEmpty()) return;
        if (!TextUtils.isEmpty(state.getString("createCurrentRecordFilePath"))) {
            mRecording = new Recording(new File(state.getString("createCurrentRecordFilePath")));
        }
        if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
            mCurrentState = CreateState.valueOf(state.getString("createCurrentCreateState"));
            updateUi(false);
        }
        mWaveDisplay.onRestoreInstanceState(state);
    }

    public void onRecordingError() {
        mSampleInterrupted = true;
        mRecordErrorMessage = this.getResources().getString(R.string.error_recording_message);
        IOUtils.deleteFile(mRecording.audio_path);
        mRecording = null;
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }

    @Override
    public void onSeek(float pct) {
        if (mCreateService != null) {
            mLastTrackTime = -1;
            mCreateService.seekTo(pct);
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


    @Override
    protected void onCreateServiceBound() {
        if (mActive) configureState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.SELECT_FILE:
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
                    final Intent intent = (new Intent(Actions.EDIT))
                            .putExtra(Intent.EXTRA_STREAM, uri);

                    final String file = uri.getLastPathSegment();
                    if (file != null && file.lastIndexOf(".") != -1) {
                        intent.putExtra(Actions.EXTRA_TITLE,
                                file.substring(0, file.lastIndexOf(".")));
                    }
                    startActivity(intent);
                }
        }
    }


    private Button setupResetButton() {
        final Button button = ((Button) findViewById(R.id.btn_reset));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState.isEdit()) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                } else {
                    ScCreate.this.track(Click.Record_discard);
                    showDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                }
                updateUi(true);
            }
        });
        return button;
    }
    private ImageButton setupActionButton() {
        final ImageButton button = (ImageButton) findViewById(R.id.btn_action);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentState == CreateState.IDLE_STANDBY_REC) {
                    stopRecording();
                    configureState();
                } else if (mCurrentState == CreateState.IDLE_STANDBY_PLAY) {
                    ScCreate.this.track(Click.Record_play_stop);
                    stopPlayback();
                    onCreateServiceBound();
                } else {
                    switch (mCurrentState) {
                        case IDLE_RECORD:
                        case IDLE_PLAYBACK:
                        case PLAYBACK:
                            ScCreate.this.track(Click.Record_rec);
                            mCurrentState = CreateState.RECORD;
                            break;
                        case RECORD:
                            ScCreate.this.track(Click.Record_rec_stop);
                            mCurrentState = CreateState.IDLE_PLAYBACK;
                            break;
                    }
                    updateUi(true);
                }
            }
        });
        return button;
    }
    private Button setupDeleteButton() {
        final Button button = ((Button) findViewById(R.id.btn_delete));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScCreate.this.track(Click.Record_delete);
                ScCreate.this.showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
            }
        });
        return button;
    }

    private Button setupPlaybutton(int id) {
        final Button button = ((Button) findViewById(id));
        if (button != null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (mCurrentState) {
                        case IDLE_PLAYBACK:
                            ScCreate.this.track(Click.Record_play);
                            mCurrentState = CreateState.PLAYBACK;
                            break;
                        case PLAYBACK:
                            ScCreate.this.track(Click.Record_play_stop);
                            mCurrentState = CreateState.IDLE_PLAYBACK;
                            break;
                        case EDIT:
                            ScCreate.this.track(Click.Record_play);
                            mCurrentState = CreateState.EDIT_PLAYBACK;
                            break;
                        case EDIT_PLAYBACK:
                            ScCreate.this.track(Click.Record_play_stop);
                            mCurrentState = CreateState.EDIT;
                            break;

                    }
                    updateUi(true);

                }
            });
        }
        return button;
    }

    private Button setupEditButton() {
        Button button = ((Button) findViewById(R.id.btn_edit));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScCreate.this.track(Click.Record_edit);
                mCurrentState = CreateState.EDIT;
                updateUi(true);
            }
        });
        return button;
    }

    private Button setupSaveButton() {
        final Button button = (Button) findViewById(R.id.btn_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentState.isEdit()) {
                    saveEditState();

                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    updateUi(true);
                } else {
                    ScCreate.this.track(Click.Record_next);
                    boolean isNew = !mRecording.isSaved();
                    if (isNew) {
                        // XXX off UI thread
                        mRecording.user_id = ScCreate.this.getCurrentUserId();
                        if (mPrivateUser != null) {
                            SoundCloudDB.upsertUser(ScCreate.this.getContentResolver(), mPrivateUser);
                            mRecording.private_user_id = mPrivateUser.id;
                            mRecording.is_private = true;
                        }
                        // set duration because ogg files report incorrect
                        // duration in mediaplayer if playback is attempted
                        // after encoding
                        mRecording.duration = ScCreate.this.getCreateService().getPlaybackDuration();
                        mRecording = SoundCloudDB.insertRecording(ScCreate.this.getContentResolver(), mRecording);
                    }
                    onSave(mRecording, isNew);
                }
            }
        });
        return button;
    }

    private ToggleButton setupToggleFade() {
        final ToggleButton tb = (ToggleButton) findViewById(R.id.toggle_fade);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        return tb;
    }

    private ToggleButton setupToggleOptimize() {
        final ToggleButton tb = (ToggleButton) findViewById(R.id.toggle_optimize);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        return tb;
    }

    private void reset() {
        mCreateService.resetRecorder();
        mCurrentState = CreateState.IDLE_RECORD;
        mRecording = null;
        mWaveDisplay.reset();
        updateUi(true);
        setResetState();
    }


    private void setResetState() {
        final boolean saved = mRecording != null && mRecording.isSaved();
        mResetButton.setVisibility(saved ? View.GONE : View.VISIBLE);
        mDeleteButton.setVisibility(saved ? View.VISIBLE : View.GONE);
    }

    private void configureState() {
        if (mCreateService == null) return;

        boolean takeAction = false;

        if (mCreateService.isRecording()) {
            if (shouldReactToRecording()) {
                mCurrentState = CreateState.RECORD;
                if (mRecording == null) mRecording = mCreateService.getRecording();
            } else {
                mCurrentState = CreateState.IDLE_STANDBY_REC;
            }
        } else if (mCreateService.isPlaying()) {
            if (shouldReactToPlayback()) {
                if (mCurrentState != CreateState.EDIT_PLAYBACK) mCurrentState = CreateState.PLAYBACK;
                mRecording = mCreateService.getRecording();
                configurePlaybackInfo();
                mHandler.postDelayed(mSmoothProgress, 0);
                takeAction = true;
            } else {
                mCurrentState = CreateState.IDLE_STANDBY_PLAY;
            }
        } else if (!CloudCreateService.RECORD_DIR.exists()) {
            // can happen when there's no mounted sd card
            mActionButton.setEnabled(false);
        } else {
            if (mRecording == null && mPrivateUser != null) {
                mRecording = mCreateService.checkForUnusedPrivateRecording(mPrivateUser);
            }

            if (mRecording != null) {
                if (mCurrentState != CreateState.EDIT) mCurrentState = CreateState.IDLE_PLAYBACK;
                configurePlaybackInfo();
            } else {
                mCurrentState = CreateState.IDLE_RECORD;
                takeAction = true;
            }
        }

        if (!(mCurrentState == CreateState.RECORD ||
                mCurrentState == CreateState.IDLE_STANDBY_REC ||
                mCurrentState == CreateState.IDLE_STANDBY_PLAY)
                && mPrivateUser == null) {

            /*
            mUnsavedRecordings = mCreateService.getUnsavedRecordings();
            if (mUnsavedRecordings.isEmpty()) {
                this.showDialog(Consts.Dialogs.DIALOG_UNSAVED_RECORDING);
            }
            */
        }

        setResetState();
        updateUi(takeAction);
    }

    private boolean shouldReactToRecording() {
        return shouldReactToPath(mCreateService.getRecordingFile());
    }

    private boolean shouldReactToPlayback() {
        return shouldReactToPath(mCreateService.getCurrentPlaybackPath());
    }

    private boolean shouldReactToPath(File file) {
        if (file == null) return false;
        final long userIdFromPath = CloudCreateService.getUserIdFromFile(file);
        return ((userIdFromPath == -1 && mPrivateUser == null) || (mPrivateUser != null && userIdFromPath == mPrivateUser.id));
    }

    private void saveEditState() {

    }

    private void updateUi(boolean takeAction) {
        switch (mCurrentState) {
            case IDLE_RECORD:
                if (takeAction) stopPlayback();
                if (!TextUtils.isEmpty(mRecordErrorMessage)) {
                    txtRecordMessage.setText(mRecordErrorMessage);
                } else {
                    txtRecordMessage.setText(mRecordSuggestions[((int) Math.floor(Math.random() * mRecordSuggestions.length))]);
                }

                setPlayButtonDrawable(false);
                mActionButton.setImageDrawable(mRecStatesDrawable);

                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideView(mChrono, false, View.INVISIBLE);
                hideEditControls(false, View.GONE);


                showView(mActionButton, false);
                showView(txtInstructions, takeAction && mLastState != CreateState.IDLE_RECORD);
                showView(txtRecordMessage, takeAction && mLastState != CreateState.IDLE_RECORD);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mActive && mRecording == null && mCreateService != null) {
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
                mActionButton.setImageDrawable(mRecStatesDrawable);

                txtRecordMessage.setVisibility(View.VISIBLE);
                mFileLayout.setVisibility(View.INVISIBLE);
                mChrono.setVisibility(View.INVISIBLE);
                txtInstructions.setVisibility(View.VISIBLE);
                txtRecordMessage.setText(mCurrentState == CreateState.IDLE_STANDBY_REC ?
                        this.getString(R.string.recording_in_progress) : this.getString(R.string.playback_in_progress));
                break;

            case RECORD:

                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mFileLayout, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideEditControls(false, View.GONE);
                hideView(txtInstructions, false, View.GONE);

                showView(mChrono, takeAction && mLastState == CreateState.IDLE_RECORD);
                showView(mActionButton, false);
                showView(txtRecordMessage, false);

                mActionButton.setImageDrawable(mRecStopStatesDrawable);
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
                showView(mChrono, false);

                hideView(txtInstructions, false, View.GONE);
                hideView(txtRecordMessage, false, View.INVISIBLE);
                hideEditControls(false, View.GONE);

                setPlayButtonDrawable(false);
                mChrono.setText(mCurrentDurationString);
                mActionButton.setImageDrawable(mRecStatesDrawable);


                setResetState();
                break;

            case PLAYBACK:
                showView(mActionButton,false);
                showView(mPlayButton,false);
                showView(mEditButton,false);
                showView(mFileLayout,false);
                showView(mChrono,false);

                hideView(txtInstructions,false,View.GONE);
                hideEditControls(false, View.GONE);
                hideView(txtRecordMessage,false,View.INVISIBLE);

                setPlayButtonDrawable(true);
                mActionButton.setImageDrawable(mRecStatesDrawable);

                setResetState();

                if (takeAction) startPlayback();
                break;

            case EDIT:
            case EDIT_PLAYBACK:

                showView(mResetButton,false);
                showView(mFileLayout,false);

                if (mHasEditControlGroup){
                   // portrait
                   showView(mEditControls, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
               } else {
                   showView(mToggleFade, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                   showView(mToggleOptimize, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                   showView(mPlayEditButton, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));

               }
                hideView(mPlayButton, false, View.GONE);
                hideView(mActionButton, false, View.GONE);
                hideView(mEditButton, false, View.GONE);
                hideView(mDeleteButton,false, View.GONE);
                hideView(txtInstructions,false,View.GONE);
                hideView(txtRecordMessage,false,View.INVISIBLE);

                final boolean isPlaying = mCurrentState == CreateState.EDIT_PLAYBACK;
                setPlayButtonDrawable(isPlaying);

                if (takeAction) {
                    if (isPlaying) {
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
        mResetButton.setText(inEditState ? this.getResources().getString(R.string.btn_revert_to_original) : this.getResources().getString(R.string.reset) );
        mSaveButton.setText(inEditState ? this.getResources().getString(R.string.btn_save) : this.getResources().getString(R.string.btn_next));
        mWaveDisplay.setIsEditing(inEditState);

        mLastState = mCurrentState;
        mActionButton.setEnabled(true);
    }

    private void setPlayButtonDrawable(boolean playing){
        if (playing){
            if (mPauseBgDrawable == null) mPauseBgDrawable = this.getResources().getDrawable(R.drawable.btn_rec_play_pause_states);
            mPlayButton.setBackgroundDrawable(mPauseBgDrawable);
            mPlayEditButton.setBackgroundDrawable(mPauseBgDrawable);
        } else {
            if (mPlayBgDrawable == null) mPlayBgDrawable = this.getResources().getDrawable(R.drawable.btn_rec_play_states);
            mPlayButton.setBackgroundDrawable(mPlayBgDrawable);
            mPlayEditButton.setBackgroundDrawable(mPlayBgDrawable);
        }
    }

    private void hideEditControls(boolean animate, int finalState){
       if (mHasEditControlGroup){
           // portrait
           hideView(mEditControls,animate,finalState);
       } else {
           hideView(mToggleFade,animate,finalState);
           hideView(mToggleOptimize,animate,finalState);
           hideView(mPlayEditButton,animate,finalState);
       }
    }

    private void startRecording() {
        this.pausePlayback();
        mRecordErrorMessage = null;
        mSampleInterrupted = false;
        mLastDisplayedTime = 0;
        mChrono.setText("0.00");
        mRemainingTimeCalculator.reset();
        updateTimeRemaining();

        mWaveDisplay.gotoRecordMode();

        if (!IOUtils.isSDCardAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = this.getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = this.getResources().getString(R.string.record_storage_is_full);
        }

        if (mRecording == null) {
            mRecording = CloudCreateService.createRecording(mPrivateUser);
        }

        if (mSampleInterrupted) {
            mCurrentState = CreateState.IDLE_RECORD;
            updateUi(true);
        } else {
            mCreateService.startRecording(mRecording);
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
            // no more space
            mSampleInterrupted = true;
            switch (mRemainingTimeCalculator.currentLowerLimit()) {
                case RemainingTimeCalculator.DISK_SPACE_LIMIT:
                    mRecordErrorMessage = this.getString(R.string.record_storage_is_full);
                    break;
                case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                    mRecordErrorMessage = this.getString(R.string.record_max_length_reached);
                    break;
                default:
                    mRecordErrorMessage = null;
                    break;
            }
            mCurrentState = mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK;
            updateUi(true);
        } else if (t < 300) {
            String msg;
            if (t < 60) {
                msg = this.getResources().getQuantityString(R.plurals.seconds_available, (int) t, t);
            } else {
                final int minutes = (int) (t / 60 + 1);
                msg = this.getResources().getQuantityString(R.plurals.minutes_available, minutes, minutes);
            }
            txtRecordMessage.setText(msg);
            txtRecordMessage.setVisibility(View.VISIBLE);
        } else {
            txtRecordMessage.setVisibility(View.INVISIBLE);
        }
    }

    private void onFrameUpdate(float maxAmplitude, long elapsed) {
        if (mCurrentState == CreateState.IDLE_RECORD || mCurrentState == CreateState.RECORD) {
            mWaveDisplay.updateAmplitude(maxAmplitude, mCurrentState == CreateState.RECORD);
            if (mCurrentState == CreateState.RECORD) onRecProgressUpdate(elapsed);
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
        if ((currentPlaybackPosition > 0 || mCreateService.isPlaying()) && currentPlaybackPosition < getDuration()) {
            mWaveDisplay.setProgress(((float) currentPlaybackPosition) / getDuration());
        } else {
            mWaveDisplay.setProgress(-1f);
        }
    }

    private long getDuration() {
        return mCreateService.getPlaybackDuration();
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
            this.track(Click.Record_play);

            try {
                mCreateService.startPlayback(mRecording);
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
            mWaveDisplay.setProgress(((float) Math.max(0, Math.min(pos, duration))) / duration);
        }
    }

    private void stopPlayback() {
        mHandler.removeCallbacks(mSmoothProgress);
        mHandler.removeCallbacks(mRefreshPositionFromService);
        mCreateService.stopPlayback();
    }

    private void showView(final View v, boolean animate) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
            if (animate) AnimUtils.runFadeInAnimationOn(this, v);
        }
    }

    private void hideView(final View v, boolean animate, final int visibilityOnComplete){
        if (v.getVisibility() == View.VISIBLE){
            if (animate){
                v.setEnabled(false);
                AnimUtils.runFadeOutAnimationOn(this, v);
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

    private void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.formatTimestamp(elapsed));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
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
            } else if (CloudCreateService.RECORD_SAMPLE.equals(action)) {
                onFrameUpdate(
                        intent.getFloatExtra(CloudCreateService.EXTRA_AMPLITUDE, -1f),
                        intent.getLongExtra(CloudCreateService.EXTRA_ELAPSEDTIME, -1l));

            } else if (CloudCreateService.RECORD_ERROR.equals(action)) {
                onRecordingError();
            } else if (CloudCreateService.PLAYBACK_STARTED.equals(action)) {
                mLastTrackTime = intent.getLongExtra(CloudCreateService.EXTRA_POSITION,0);
                mLastProgressTimestamp = System.currentTimeMillis();
                mHandler.postDelayed(mSmoothProgress, 100);
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

    private void onSave(final Recording recording, boolean isNew) {
        if (isNew) {
            startActivity(new Intent(this, ScUpload.class).setData(recording.toUri()));
            reset();
        } else {
            startActivityForResult(new Intent(this, ScUpload.class).setData(recording.toUri()), 0);
        }
    }

    @Override
    public Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNSAVED_RECORDING:
                final List<Recording> recordings = mUnsavedRecordings;

                if (recordings == null || recordings.isEmpty()) return null;
                final CharSequence[] fileIds = new CharSequence[recordings.size()];
                final boolean[] checked = new boolean[recordings.size()];
                for (int i=0; i < recordings.size(); i++) {
                    fileIds[i] = new Date(recordings.get(i).lastModified()).toLocaleString() + ", " + recordings.get(i).formattedDuration();
                    checked[i] = true;
                }

                return new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_unsaved_recordings_message)
                        .setMultiChoiceItems(fileIds, checked,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                                    checked[whichButton] = isChecked;
                                }
                            })
                        .setPositiveButton(R.string.btn_save,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    for (int i = 0; i < recordings.size(); i++) {
                                        if (checked[i]) {
                                            SoundCloudDB.insertRecording(ScCreate.this.getContentResolver(), recordings.get(i));
                                        } else {
                                            recordings.get(i).delete(null);
                                        }
                                    }
                                    mUnsavedRecordings = null;
                                }
                            })
                        .create();
            case Consts.Dialogs.DIALOG_RESET_RECORDING:
                return new AlertDialog.Builder(this)
                        .setTitle(null)
                        .setMessage(R.string.dialog_reset_recording_message)
                        .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int whichButton) {
                                    ScCreate.this.track(Click.Record_discard__ok);
                                    IOUtils.deleteFile(mRecording.audio_path);
                                    ScCreate.this.removeDialog(Consts.Dialogs.DIALOG_RESET_RECORDING);
                                    reset();
                                }
                        })
                        .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    ScCreate.this.track(Click.Record_discard_cancel);
                                }
                        })
                        .create();

            case Consts.Dialogs.DIALOG_DELETE_RECORDING:
             return new AlertDialog.Builder(this)
                        .setTitle(null)
                        .setMessage(R.string.dialog_confirm_delete_recording_message)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (mRecording != null) mRecording.delete(ScCreate.this.getContentResolver());
                                        finish();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

            default:
                return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(menu.size(), Consts.OptionsMenu.SELECT_FILE, 0, R.string.menu_select_file).setIcon(R.drawable.ic_menu_incoming);
        return super.onCreateOptionsMenu(menu);
    }

}
