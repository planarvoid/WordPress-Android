package com.soundcloud.android.activity.create;


import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.landing.ScLandingPage;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.dao.RecordingStorage;
import com.soundcloud.android.model.DeprecatedRecordingProfile;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.ButtonBar;
import com.soundcloud.android.view.create.Chronometer;
import com.soundcloud.android.view.create.CreateWaveDisplay;
import com.soundcloud.android.view.create.RecordMessageView;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Tracking(page = Page.Record_main)
public class ScCreate extends ScActivity implements CreateWaveDisplay.Listener, ScLandingPage {

    public static final int REQUEST_UPLOAD_SOUND  = 1;

    private static final int MSG_ANIMATE_OUT_SAVE_MESSAGE = 0;
    private static final long SAVE_MSG_DISPLAY_TIME = 3000; //ms

    private SoundRecorder mRecorder;

    private CreateState mLastState, mCurrentState;
    private TextView mTxtInstructions;
    private RecordMessageView mTxtRecordMessage;

    private Chronometer mChrono;
    private ViewGroup mEditControls, mGaugeHolder, mSavedMessageLayout;
    private ImageButton mActionButton;
    private CreateWaveDisplay mWaveDisplay;
    private ImageButton mPlayButton, mEditButton, mPlayEditButton;
    private ToggleButton mToggleOptimize, mToggleFade;
    private String mRecordErrorMessage;

    private ButtonBar mButtonBar;
    private boolean mActive, mHasEditControlGroup, mSeenSavedMessage;
    private List<Recording> mUnsavedRecordings;
    private AccountOperations mAccountOperations;

    private ProgressBar mGeneratingWaveformProgressBar;

    public enum CreateState {
        GENERATING_WAVEFORM,
        IDLE_RECORD,
        RECORD,
        IDLE_PLAYBACK,
        PLAYBACK,
        EDIT,
        EDIT_PLAYBACK;

        public boolean isEdit() { return this == EDIT || this == EDIT_PLAYBACK; }
    }

    static interface MenuItems {
        int RESET = 1;
        int DELETE = 2;
        int SAVE = 3;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sc_create);
        mAccountOperations = new AccountOperations(this);
        mRecorder = SoundRecorder.getInstance(this);
        mTxtInstructions = (TextView) findViewById(R.id.txt_instructions);
        mTxtRecordMessage = (RecordMessageView) findViewById(R.id.txt_record_message);

        mChrono = (Chronometer) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.INVISIBLE);

        mEditControls = (ViewGroup) findViewById(R.id.edit_controls);
        mHasEditControlGroup = mEditControls != null;

        mActionButton = setupActionButton();

        mButtonBar = setupButtonBar();
        mEditButton = setupEditButton();
        mPlayButton = setupPlaybutton(R.id.btn_play);
        mPlayEditButton = setupPlaybutton(R.id.btn_play_edit);
        mToggleFade = (ToggleButton) findViewById(R.id.toggle_fade);
        mToggleOptimize = (ToggleButton) findViewById(R.id.toggle_optimize);

        mWaveDisplay = new CreateWaveDisplay(this);
        mWaveDisplay.setTrimListener(this);

        mGaugeHolder = ((ViewGroup) findViewById(R.id.gauge_holder));
        mGaugeHolder.addView(mWaveDisplay);

        mSavedMessageLayout = (ViewGroup) findViewById(R.id.saved_message_layout);
        updateUi(CreateState.IDLE_RECORD, false);
        handleIntent();
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_record;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }

    private void handleIntent() {
        final Intent intent = getIntent();
        if (Actions.RECORD_START.equals(intent.getAction())) {
            if (!mRecorder.isRecording()) {
                reset();
                startRecording();
            }
            // don't want to receive the RECORD_START action on config changes, so set it as a normal record intent
            intent.setAction(Actions.RECORD);
        } else if (Actions.RECORD_STOP.equals(intent.getAction())) {
            if (mRecorder.isRecording()) {
                mRecorder.stopRecording();
            }
            intent.setAction(Actions.RECORD);
        } else {
            if (intent.getBooleanExtra("reset", false) && !mRecorder.isActive()) {
                intent.removeExtra("reset");
                reset();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = SoundRecorder.getIntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStatusListener, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActive = true;
        configureInitialState(getIntent());

        if (Consts.SdkSwitches.canDetermineActivityBackground) {
            mRecorder.shouldUseNotifications(false);
        }
    }

    @Override @TargetApi(11)
    public void onPause() {
        super.onPause();
        mActive = false;
        mRecorder.stopReading(); // this will stop the amplitude reading loop

        /*  if we are either backing out or getting killed (finishing), or we are pausing cause we are leaving
            and not because of a configuration change, then we know we are going to the background, so tell the recorder
            to provide notifications. isChangingConfigurations availability dependent on SDK */
        if (Consts.SdkSwitches.canDetermineActivityBackground && (isFinishing() || !isChangingConfigurations())){
            mRecorder.shouldUseNotifications(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWaveDisplay.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("createSeenSavedMessage", mSeenSavedMessage);
        state.putString("createCurrentCreateState", mCurrentState.toString());
        mWaveDisplay.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!state.isEmpty()) {
            mSeenSavedMessage = state.getBoolean("createSeenSavedMessage");
            if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
                updateUi(CreateState.valueOf(state.getString("createCurrentCreateState")), false);
            }
            mWaveDisplay.onRestoreInstanceState(state);
        }
    }

    @Override
    public void onSeek(float pct) {
        mRecorder.seekTo(pct);
    }

    @Override
    public void onAdjustTrimLeft(float newPos, long moveTime) {
        mRecorder.onNewStartPosition(newPos, moveTime);
        track(Click.Record_Edit_Interact);
        configurePlaybackInfo();
    }

    @Override
    public void onAdjustTrimRight(float newPos, long moveTime) {
        mRecorder.onNewEndPosition(newPos, moveTime);
        track(Click.Record_Edit_Interact);
        configurePlaybackInfo();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) finish();
                break;

            case REQUEST_UPLOAD_SOUND:
                if (resultCode == RESULT_OK) {
                    reset();
                    if (data.getBooleanExtra(Actions.UPLOAD_EXTRA_UPLOADING, false)) {
                        finish(); // upload started, finish
                    }
                } else {
                    // back button pressed, do nothing
                }
                break;
        }
    }

    private void onRecordingError(String message) {
        mRecordErrorMessage = message;
        updateUi(CreateState.IDLE_RECORD);
    }

    private ButtonBar setupButtonBar() {
        ButtonBar buttonBar = (ButtonBar) findViewById(R.id.bottom_bar);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.RESET, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCurrentState.isEdit()) {
                    track(Click.Record_Pause_Delete, mTxtRecordMessage.getCurrentSuggestionKey());
                    showDialog(Consts.Dialogs.DIALOG_DISCARD_RECORDING);
                } else {
                    track(Click.Record_Edit_Revert_To_Original);
                    showDialog(Consts.Dialogs.DIALOG_REVERT_RECORDING);
                }
            }
        }), R.string.reset);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.DELETE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_Pause_Delete, mTxtRecordMessage.getCurrentSuggestionKey());
                showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
            }
        }), R.string.delete);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.SAVE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Recording rec = mRecorder.saveState();
                if (rec != null) {
                    final PlaybackStream ps = rec.getPlaybackStream();
                    if (mCurrentState.isEdit()) {
                        track(Click.Record_Edit_Apply, ps != null && ps.isTrimmed() ? "trimmed" : "not_trimmed");
                        updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
                    } else {
                        track(Click.Record_Pause_Publish,
                                mTxtRecordMessage.getCurrentSuggestionKey(),
                                ps != null && ps.isTrimmed() ? "trimmed" : "not_trimmed");

                        startActivityForResult(new Intent(ScCreate.this, ScUpload.class)
                                .setData(rec.toUri()), REQUEST_UPLOAD_SOUND);
                    }
                } else {
                    onRecordingError("Error saving recording");
                    // state could not be saved

                }
            }
        }), R.string.btn_publish);
        return buttonBar;
    }

    private ImageButton setupActionButton() {
        final ImageButton button = (ImageButton) findViewById(R.id.btn_action);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mCurrentState) {
                    case IDLE_RECORD:
                        track(Click.Record_Main_Record_Start, mTxtRecordMessage.getCurrentSuggestionKey());
                        startRecording();
                        break;

                    case IDLE_PLAYBACK:
                    case PLAYBACK:
                        track(Click.Record_Pause_Record_More);
                        startRecording();
                        break;

                    case RECORD:
                        track(Click.Record_Main_Record_Pause, mTxtRecordMessage.getCurrentSuggestionKey());
                        mRecorder.stopRecording();
                        // XXX use prefs
                        if (mAccountOperations.getAccountDataBoolean(User.DataKeys.SEEN_CREATE_AUTOSAVE)) {
                            showToast(R.string.create_autosave_message);
                            mAccountOperations.setAccountData(User.DataKeys.SEEN_CREATE_AUTOSAVE, Boolean.TRUE.toString());
                        }
                        break;
                }
            }
        });
        return button;
    }

    private ImageButton setupPlaybutton(int id) {
        final ImageButton button = (ImageButton) findViewById(id);
        if (button != null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (!mRecorder.isPlaying()) {
                        track(Click.Record_Pause_Play);
                    }
                    mRecorder.togglePlayback();
                }
            });
        }
        return button;
    }

    private ImageButton setupEditButton() {
        ImageButton button = (ImageButton) findViewById(R.id.btn_edit);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUi(isPlayState() ? CreateState.EDIT_PLAYBACK : CreateState.EDIT);
            }
        });
        return button;
    }

    private void setupToggleFade(boolean isFading) {
        mToggleFade.setChecked(isFading);
        mToggleFade.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mRecorder.toggleFade();
            }
        });
    }

    private void setupToggleOptimize(boolean isOptimized) {
        mToggleOptimize.setChecked(isOptimized);
        mToggleOptimize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mRecorder.toggleOptimize();
            }
        });
    }

    /* package */ void reset() {
        reset(false);
    }
    /* package */ void reset(boolean deleteRecording) {
        mSeenSavedMessage = false;
        mRecorder.reset(deleteRecording);
        mWaveDisplay.reset();
        updateUi(CreateState.IDLE_RECORD);
    }

    public SoundRecorder getRecorder() {
        return mRecorder;
    }

    private void configureInitialState(Intent intent) {
        if (mRecorder == null || !mActive) return;

        CreateState newState = null;
        if (mRecorder.isRecording()) {
            newState = CreateState.RECORD;
        } else {

            Recording recording = Recording.fromIntent(intent, this, getCurrentUserId());
            if (recording != null){

                // failsafe, if they try to play an uploading recording
                if (recording.upload_status == Recording.Status.UPLOADING) {
                    startActivity(recording.getMonitorIntent());
                    finish();
                    return;
                }

                mRecorder.setRecording(recording);
                mSeenSavedMessage = true;
            }

            if (mRecorder.isPlaying()) {
                // is this after orientation change during edit playback
                if (mCurrentState != CreateState.EDIT_PLAYBACK) {
                    newState = CreateState.PLAYBACK;
                }
                configurePlaybackInfo();
                mWaveDisplay.gotoPlaybackMode(false);
            } else {
                // we have an inactive recorder, see what is loaded in it
                if (mRecorder.hasRecording()) {
                    if (mRecorder.isGeneratingWaveform()) {
                        newState = CreateState.GENERATING_WAVEFORM;
                    } else {
                        if (mCurrentState != CreateState.EDIT) newState = CreateState.IDLE_PLAYBACK;
                        configurePlaybackInfo();
                        mWaveDisplay.gotoPlaybackMode(false);
                    }
                } else {
                    newState = CreateState.IDLE_RECORD;
                }
            }
        }

        Recording.clearRecordingFromIntent(intent);

        if (newState == CreateState.IDLE_RECORD) {
            RecordingStorage recordings = new RecordingStorage();
            mUnsavedRecordings = recordings.getUnsavedRecordings(
                    SoundRecorder.RECORD_DIR,
                    mRecorder.getRecording(),
                    getCurrentUserId());

            if (!mUnsavedRecordings.isEmpty()) {
                showDialog(Consts.Dialogs.DIALOG_UNSAVED_RECORDING);
            }
        }

        setupToggleFade(mRecorder.isFading());
        setupToggleOptimize(mRecorder.isOptimized());
        updateUi(newState);
    }

    private void updateUi(CreateState newState) {
        updateUi(newState ,true);
    }

    private void updateUi(CreateState newState, boolean animate) {
        if (newState != null) mCurrentState = newState;
        switch (mCurrentState) {
            case GENERATING_WAVEFORM:
                setTitle(R.string.rec_title_generating_waveform);
                hideView(mPlayButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideEditControls();
                hideView(mTxtInstructions, false, View.GONE);
                hideView(mChrono, false, View.GONE);
                hideView(mActionButton, false, View.GONE);
                hideSavedMessage();

                mActionButton.setClickable(false);
                mActionButton.setImageResource(R.drawable.btn_recording_rec_deactivated);

                showView(mTxtRecordMessage, mLastState != CreateState.IDLE_RECORD);
                mTxtRecordMessage.setMessage(R.string.create_regenerating_waveform_message);

                if (mGeneratingWaveformProgressBar == null){
                    mGeneratingWaveformProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
                    mGeneratingWaveformProgressBar.setIndeterminate(true);
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
                    mGaugeHolder.addView(mGeneratingWaveformProgressBar, lp);
                }
                break;

            case IDLE_RECORD:
                configureRecordButton(false);

                setTitle(R.string.rec_title_idle_rec);
                setPlayButtonDrawable(false);

                hideView(mPlayButton, animate && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, animate && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, animate && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideView(mChrono, false, View.INVISIBLE);
                hideEditControls();

                //mYouButton.setImageResource(R.drawable.ic_rec_you_dark);   TODO replace with action bar icon display
                showView(mActionButton, false);
                showView(mTxtInstructions, animate && mLastState != CreateState.IDLE_RECORD);
                showView(mTxtRecordMessage, animate && mLastState != CreateState.IDLE_RECORD);

                if (mActive && mRecorder.getRecording() == null) {
                    mRecorder.startReading();
                }
                break;

            case RECORD:
                setTitle(R.string.rec_title_recording);
                hideView(mPlayButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideEditControls();
                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);

                showView(mChrono, mLastState == CreateState.IDLE_RECORD);
                showView(mActionButton, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you_dark);

                mActionButton.setImageResource(R.drawable.btn_rec_pause_states);
                mTxtRecordMessage.setMessage("");
                mChrono.setDurationOnly(mRecorder.getRecordingElapsedTime());
                break;

            case IDLE_PLAYBACK:
                configureRecordButton(true);
                setTitle(R.string.rec_title_idle_play);
                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton, (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mEditButton, (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mActionButton, (mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mButtonBar, (mLastState == CreateState.RECORD));
                if (mLastState == CreateState.RECORD && !mSeenSavedMessage) {
                    showSavedMessage();
                }
                showView(mChrono, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);

                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);
                hideEditControls();

                setPlayButtonDrawable(false);
                mActionButton.setImageResource(R.drawable.btn_rec_resume_states);
                mWaveDisplay.gotoPlaybackMode(animate);
                configurePlaybackInfo();
                break;

            case PLAYBACK:
                setTitle(R.string.rec_title_playing);
                showView(mActionButton, false);
                showView(mPlayButton,false);
                showView(mEditButton,false);
                showView(mButtonBar, false);
                showView(mChrono, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);
                hideSavedMessage();

                hideView(mTxtInstructions, false, View.GONE);
                hideEditControls();
                hideView(mTxtRecordMessage, false, View.INVISIBLE);

                setPlayButtonDrawable(true);
                mActionButton.setImageResource(R.drawable.btn_rec_resume_states);
                break;

            case EDIT:
            case EDIT_PLAYBACK:
                setTitle(R.string.rec_title_editing);
                showView(mButtonBar, false);
                hideSavedMessage();

                if (mHasEditControlGroup) {
                    // portrait
                    showView(mEditControls, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                } else {
                    showView(mToggleFade, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                    showView(mPlayEditButton, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                    //showView(mToggleOptimize, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                }
                hideView(mPlayButton, false, View.GONE);
                hideView(mActionButton, false, View.GONE);
                hideView(mEditButton, false, View.GONE);

                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);

                final boolean isPlaying = mCurrentState == CreateState.EDIT_PLAYBACK;
                setPlayButtonDrawable(isPlaying);
                if (!isPlaying) {
                    configurePlaybackInfo();
                }
                break;
        }

        final boolean inEditState = mCurrentState.isEdit();
        configureButtonBar(inEditState);
        mWaveDisplay.setIsEditing(inEditState);

        mLastState = mCurrentState;
        mActionButton.setEnabled(true);

        invalidateOptionsMenu(); // adjusts color of you button

        if (mCurrentState != CreateState.GENERATING_WAVEFORM && mGeneratingWaveformProgressBar != null) {
            if (mGeneratingWaveformProgressBar.getParent() == mGaugeHolder) mGaugeHolder.removeView(mGeneratingWaveformProgressBar);
            mGeneratingWaveformProgressBar = null;
        }
    }

    private boolean isPlayState(){
        return mCurrentState == CreateState.EDIT_PLAYBACK || mCurrentState == CreateState.PLAYBACK;
    }

    private void configureRecordButton(boolean isResume){
        if (!IOUtils.isSDCardAvailable()) {
            // state list drawables won't work with the image button
            mActionButton.setClickable(false);
            mActionButton.setImageResource(isResume ? R.drawable.btn_recording_resume_deactivated : R.drawable.btn_recording_rec_deactivated);
            mTxtRecordMessage.setMessage(R.string.record_insert_sd_card);
        } else {
            mActionButton.setClickable(true);
            mActionButton.setImageResource(isResume ? R.drawable.btn_rec_resume_states : R.drawable.btn_rec_states);
            if (!TextUtils.isEmpty(mRecordErrorMessage)) {
                mTxtRecordMessage.setMessage(mRecordErrorMessage);
            } else {
                mTxtRecordMessage.loadSuggestion(null);
            }
        }
    }

    private void configureButtonBar(boolean isEditing) {
        mButtonBar.setTextById(MenuItems.RESET, isEditing ? R.string.btn_revert_to_original : R.string.reset);
        mButtonBar.setTextById(MenuItems.SAVE, isEditing ? R.string.btn_apply : R.string.btn_publish);

        final boolean showDelete = !isEditing && mRecorder.isSaved();
        mButtonBar.toggleVisibility(MenuItems.RESET, !showDelete, false);
        mButtonBar.toggleVisibility(MenuItems.DELETE, showDelete, true);
    }


    private void setPlayButtonDrawable(boolean playing){
        if (playing){
            mPlayButton.setImageResource(R.drawable.btn_rec_play_pause_states);
            mPlayEditButton.setImageResource(R.drawable.btn_rec_play_pause_states);
        } else {
            mPlayButton.setImageResource(R.drawable.btn_rec_play_states);
            mPlayEditButton.setImageResource(R.drawable.btn_rec_play_states);
        }
    }

    private void hideEditControls(){
       if (mHasEditControlGroup){
           // portrait
           hideView(mEditControls, false, View.GONE);
       } else {
           hideView(mToggleFade, false, View.GONE);
           hideView(mToggleOptimize, false, View.GONE);
           hideView(mPlayEditButton, false, View.GONE);
       }
    }

    private void startRecording() {
        mRecordErrorMessage = null;

        try {
            mRecorder.startRecording(mTxtRecordMessage.getCurrentSuggestionKey());
            mWaveDisplay.gotoRecordMode();
        } catch (IOException e) {
            onRecordingError(e.getMessage());
            updateUi(CreateState.IDLE_RECORD);
        }
    }

    private long updateTimeRemaining(long t) {
        if (t < 300) {
            // 5 minutes, display countdown
            String msg;
            if (t < 60) {
                msg = getResources().getQuantityString(R.plurals.seconds_available, (int) t, t);
            } else {
                final int minutes = (int) Math.floor(t / 60d);
                msg = getResources().getQuantityString(R.plurals.minutes_available, minutes, minutes);
            }
            mTxtRecordMessage.setMessage(msg);
            mTxtRecordMessage.setVisibility(View.VISIBLE);
            return t;
        } else {
            mTxtRecordMessage.setVisibility(View.INVISIBLE);
            return t;
        }
    }

    private void configurePlaybackInfo() {
        final long currentPlaybackPosition = mRecorder.getCurrentPlaybackPosition();
        final long duration = mRecorder.getPlaybackDuration();

        if ((currentPlaybackPosition > 0 || mRecorder.isPlaying()) && currentPlaybackPosition < duration) {
            mChrono.setPlaybackProgress(currentPlaybackPosition, duration);
            mWaveDisplay.setProgress(((float) currentPlaybackPosition) / duration);
        } else {
            mChrono.setDurationOnly(duration);
            mWaveDisplay.setProgress(-1f);
        }
    }

    private void setProgressInternal(long pos, long duration) {
        if (duration != 0){
            mChrono.setPlaybackProgress(pos, duration);
            mWaveDisplay.setProgress(((float) Math.max(0, Math.min(pos, duration))) / duration);
        }
    }

    private void showView(final View v, boolean animate) {
        v.clearAnimation();
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
                        if (v.getAnimation() == animation){
                            v.setVisibility(visibilityOnComplete);
                            v.setEnabled(true);
                        }
                    }
                });
            } else {
                v.setVisibility(visibilityOnComplete);
            }

        }
    }

    private void showSavedMessage() {
        mSeenSavedMessage = true;
        if (mSavedMessageLayout.getVisibility() != View.VISIBLE) {
            mSavedMessageLayout.setVisibility(View.VISIBLE);
            mSavedMessageLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top));
            if (!mAnimateHandler.hasMessages(MSG_ANIMATE_OUT_SAVE_MESSAGE)){
                mAnimateHandler.sendMessageDelayed(mAnimateHandler.obtainMessage(MSG_ANIMATE_OUT_SAVE_MESSAGE),
                        SAVE_MSG_DISPLAY_TIME);
            }
        }
    }

    private void hideSavedMessage(){
        mAnimateHandler.removeMessages(MSG_ANIMATE_OUT_SAVE_MESSAGE);
        if (mSavedMessageLayout.getVisibility() == View.VISIBLE){
            final Animation slideOutAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_top);
            slideOutAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}

                @Override public void onAnimationRepeat(Animation animation) {}

                @Override public void onAnimationEnd(Animation animation) {
                    mSavedMessageLayout.setVisibility(View.INVISIBLE);
                }
            });
            mSavedMessageLayout.startAnimation(slideOutAnim);
            }
        }

    private Handler mAnimateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ANIMATE_OUT_SAVE_MESSAGE:
                    hideSavedMessage();
                    break;
            }
        }

    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (SoundRecorder.RECORD_STARTED.equals(action)) {
                updateUi(CreateState.RECORD);

            } else if (SoundRecorder.RECORD_SAMPLE.equals(action)) {
                if (mCurrentState == CreateState.IDLE_RECORD || mCurrentState == CreateState.RECORD) {
                    mWaveDisplay.updateAmplitude(intent.getFloatExtra(SoundRecorder.EXTRA_AMPLITUDE, -1f), mCurrentState == CreateState.RECORD);
                }
            } else if (SoundRecorder.RECORD_PROGRESS.equals(action)) {
                mChrono.setDurationOnly(intent.getLongExtra(SoundRecorder.EXTRA_ELAPSEDTIME, -1l));
                updateTimeRemaining(intent.getLongExtra(SoundRecorder.EXTRA_TIME_REMAINING, 0l));
            } else if (SoundRecorder.RECORD_ERROR.equals(action)) {
                onRecordingError(getString(R.string.error_recording_message));
            } else if (SoundRecorder.RECORD_FINISHED.equals(action)) {
                // has the time run out?
                if (intent.getLongExtra(SoundRecorder.EXTRA_TIME_REMAINING, -1) == 0) {
                    AndroidUtils.showToast(ScCreate.this, R.string.record_storage_is_full);
                }

                updateUi(CreateState.IDLE_PLAYBACK);

            } else if (SoundRecorder.PLAYBACK_STARTED.equals(action)) {
                updateUi((mCurrentState == CreateState.EDIT || mCurrentState == CreateState.EDIT_PLAYBACK) ?
                        CreateState.EDIT_PLAYBACK : CreateState.PLAYBACK);

            } else if (SoundRecorder.PLAYBACK_PROGRESS.equals(action)) {
                setProgressInternal(intent.getLongExtra(SoundRecorder.EXTRA_POSITION, 0),
                        intent.getLongExtra(SoundRecorder.EXTRA_DURATION, 0));

            } else if (SoundRecorder.PLAYBACK_COMPLETE.equals(action) ||
                       SoundRecorder.PLAYBACK_STOPPED.equals(action) ||
                       SoundRecorder.PLAYBACK_ERROR.equals(action)) {

                if (mCurrentState == CreateState.PLAYBACK ||
                    mCurrentState == CreateState.EDIT_PLAYBACK) {
                    updateUi(mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK);
                }
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_REMOVED.equals(action)){
                // for messaging and action button activation
                if (mCurrentState == CreateState.IDLE_RECORD) updateUi(CreateState.IDLE_RECORD);

            } else if (SoundRecorder.WAVEFORM_GENERATED.equals(action)) {
                // we are now free to play back
                if (mCurrentState == CreateState.GENERATING_WAVEFORM) updateUi(CreateState.IDLE_PLAYBACK);
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (mCurrentState.isEdit()){
            updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
        } else {
            super.onBackPressed();
        }
    }

    @Override public Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_UNSAVED_RECORDING:
                return createUnsavedRecordingDialog();

            case Consts.Dialogs.DIALOG_DISCARD_RECORDING:
                return createDiscardRecordingDialog();

            case Consts.Dialogs.DIALOG_REVERT_RECORDING:
                return createRevertRecordingDialog();

            case Consts.Dialogs.DIALOG_DELETE_RECORDING:
                return createDeleteRecordingDialog();

            case Consts.Dialogs.DIALOG_INSTALL_PROCESSOR:
                return createInstallProcessorDialog();

            default:
                return null;
        }
    }

    private Dialog createInstallProcessorDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_install_processor)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    private Dialog createDeleteRecordingDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_confirm_delete_recording_message)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                track(Click.Record_Pause_Delete, mTxtRecordMessage.getCurrentSuggestionKey());
                                reset(true);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    private Dialog createRevertRecordingDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_revert_recording_message)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                track(Click.Record_Edit_Revert_To_Original);
                                mRecorder.revertFile();
                                updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    private Dialog createDiscardRecordingDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_reset_recording_message)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                reset(true);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    private Dialog createUnsavedRecordingDialog() {
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
                                onSaveRecordingButtonClicked(recordings, checked);
                            }
                        })
                .create();
    }

    private void onSaveRecordingButtonClicked(List<Recording> recordings, boolean[] checked) {
        RecordingStorage storage = new RecordingStorage();
        for (int i = 0; i < recordings.size(); i++) {
            if (checked[i]) {
                DeprecatedRecordingProfile.migrate(recordings.get(i)); // migrate deprecated format, otherwise this is harmless
                storage.create(recordings.get(i)).subscribe(ScActions.NO_OP);
            } else {
                storage.delete(recordings.get(i)).subscribe(ScActions.NO_OP);
            }
        }
        mUnsavedRecordings = null;
    }

    /* package, for testing */ CreateState getState() { return mCurrentState; }
}
