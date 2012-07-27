package com.soundcloud.android.activity.create;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.SoundRecorder;
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Tracking(page = Page.Record_main)
public class ScCreate extends ScActivity implements CreateWaveDisplay.Listener {

    public static final int REQUEST_GET_FILE      = 1;
    public static final int REQUEST_PROCESS_SOUND = 2;
    public static final int REQUEST_UPLOAD_SOUND  = 3;

    private static final int MSG_ANIMATE_OUT_SAVE_MESSAGE = 0;
    private static final long SAVE_MSG_DISPLAY_TIME = 3000; //ms

    public static final String EXTRA_PRIVATE_MESSAGE_RECIPIENT = "privateMessageRecipient";

    private User mRecipient;
    private SoundRecorder mRecorder;

    private CreateState mLastState, mCurrentState;
    private TextView mTxtInstructions, mTxtTitle;
    private RecordMessageView mTxtRecordMessage;

    private Chronometer mChrono;
    private ViewGroup mEditControls, mGaugeHolder, mSavedMessageLayout;
    private ImageButton mActionButton;
    private CreateWaveDisplay mWaveDisplay;
    private View mPlayButton, mEditButton, mPlayEditButton;
    private ToggleButton mToggleOptimize, mToggleFade;
    private String mRecordErrorMessage;

    private ButtonBar mButtonBar;
    private boolean mActive, mHasEditControlGroup;
    private List<Recording> mUnsavedRecordings;

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

        mRecorder = SoundRecorder.getInstance(this);
        mTxtTitle = (TextView) findViewById(R.id.txt_title);
        mTxtInstructions = (TextView) findViewById(R.id.txt_instructions);
        Recording recording = null;
        mRecipient = getIntent().getParcelableExtra(EXTRA_PRIVATE_MESSAGE_RECIPIENT);
        if (mRecipient != null) {
            mTxtInstructions.setText(getString(R.string.private_message_title, mRecipient.username));
            recording = Recording.checkForUnusedPrivateRecording(SoundRecorder.RECORD_DIR, mRecipient);
        }
        if (recording == null ) {
            recording = Recording.fromIntent(getIntent(), getContentResolver(), getCurrentUserId());
        }
        if (recording != null) {
            mRecorder.setRecording(recording);
        }
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
        mToggleFade = setupToggleFade();
        mToggleOptimize = setupToggleOptimize();
        setupYouButton();

        mWaveDisplay = new CreateWaveDisplay(this);
        mWaveDisplay.setTrimListener(this);

        mGaugeHolder = ((ViewGroup) findViewById(R.id.gauge_holder));
        mGaugeHolder.addView(mWaveDisplay);

        mSavedMessageLayout = (ViewGroup) findViewById(R.id.saved_message_layout);
        updateUi(CreateState.IDLE_RECORD, false);
        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }

    private void handleIntent() {
        final Intent intent = getIntent();
        if (Actions.RECORD_START.equals(intent.getAction())){
            if (!mRecorder.isRecording()){
                reset();
                startRecording();
            }
            // don't want to receive the RECORD_START action on config changes, so set it as a normal record intent
            intent.setAction(Actions.RECORD);
        } else {
            if (intent.getBooleanExtra("reset", false) && !mRecorder.isActive()){
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
        configureInitialState();

        if (Consts.SdkSwitches.canDetermineActivityBackground) {
            mRecorder.shouldUseNotifications(false);
        }
    }

    @Override @SuppressLint("NewApi")
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
    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        mWaveDisplay.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!state.isEmpty()) {
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
        configurePlaybackInfo();
    }

    @Override
    public void onAdjustTrimRight(float newPos, long moveTime) {
        mRecorder.onNewEndPosition(newPos, moveTime);
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
            case REQUEST_GET_FILE:
                if (resultCode == RESULT_OK) {
                    // what's this for?
                    final Uri uri = data.getData();
                    final Intent intent = (new Intent(Actions.EDIT)).putExtra(Intent.EXTRA_STREAM, uri);
                    final String file = uri.getLastPathSegment();
                    if (file != null && file.lastIndexOf(".") != -1) {
                        intent.putExtra(Actions.EXTRA_TITLE,
                                file.substring(0, file.lastIndexOf(".")));
                    }
                    startActivity(intent);
                }
                break;
            case REQUEST_PROCESS_SOUND:
                if (resultCode == RESULT_OK) {
                    String message = data.getStringExtra("message");
                    if (message != null) {
                        AndroidUtils.showToast(this, R.string.sound_processed_error, message);
                    } else {
                        AndroidUtils.showToast(this, R.string.sound_processed);
                    }
                    String in = data.getStringExtra(Actions.RECORDING_EXTRA_IN);
                    String out = data.getStringExtra(Actions.RECORDING_EXTRA_OUT);
                    Log.d(SoundCloudApplication.TAG, "processed " + in + " => " + out);
                    if (out != null) {
                        if (new File(out).renameTo(new File(in))) {
                            // reload player
                            mRecorder.reload();
                        } else {
                            Log.w(SoundCloudApplication.TAG, "could not rename");
                        }
                    }
                }
        }
    }

    private void onRecordingError(String message) {
        mRecordErrorMessage = message;
        updateUi(CreateState.IDLE_RECORD, true);
    }

    private ButtonBar setupButtonBar() {
        ButtonBar buttonBar = (ButtonBar) findViewById(R.id.bottom_bar);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.RESET, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCurrentState.isEdit()) {
                    track(Click.Record_discard);
                    showDialog(Consts.Dialogs.DIALOG_DISCARD_RECORDING);
                } else {
                    track(Click.Record_revert);
                    showDialog(Consts.Dialogs.DIALOG_REVERT_RECORDING);
                }
            }
        }), R.string.reset);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.DELETE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_delete);
                showDialog(Consts.Dialogs.DIALOG_DELETE_RECORDING);
            }
        }), R.string.delete);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.SAVE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Recording rec = mRecorder.saveState();
                if (rec != null) {
                    if (mCurrentState.isEdit()) {
                        track(Click.Record_save);
                        updateUi(CreateState.IDLE_PLAYBACK, true);
                    } else {
                        track(Click.Record_next);
                        startActivityForResult(new Intent(ScCreate.this, ScUpload.class)
                                .putExtra(SoundRecorder.EXTRA_RECORDING, rec), REQUEST_UPLOAD_SOUND);
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
                    case IDLE_PLAYBACK:
                    case PLAYBACK:
                        track(Click.Record_rec);
                        startRecording();
                        break;

                    case RECORD:
                        track(Click.Record_rec_stop);
                        mRecorder.stopRecording();
                        // XXX use prefs
                        if (getApp().getAccountDataBoolean(User.DataKeys.SEEN_CREATE_AUTOSAVE)) {
                            showToast(R.string.create_autosave_message);
                            getApp().setAccountData(User.DataKeys.SEEN_CREATE_AUTOSAVE, true);
                        }
                        break;
                }
            }
        });
        return button;
    }

    private View setupPlaybutton(int id) {
        final View button = findViewById(id);
        if (button != null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {

                    CreateState newState = mCurrentState;
                    switch (mCurrentState) {
                        case IDLE_PLAYBACK:
                            track(Click.Record_play);
                            newState = CreateState.PLAYBACK;
                            break;
                        case PLAYBACK:
                            track(Click.Record_play_stop);
                            newState = CreateState.IDLE_PLAYBACK;
                            break;
                        case EDIT:
                            track(Click.Record_play);
                            newState = CreateState.EDIT_PLAYBACK;
                            break;
                        case EDIT_PLAYBACK:
                            track(Click.Record_play_stop);
                            newState = CreateState.EDIT;
                            break;

                    }
                    updateUi(newState, true);
                }
            });
        }
        return button;
    }

    private View setupEditButton() {
        View button = findViewById(R.id.btn_edit);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_edit);
                updateUi(CreateState.EDIT, true);
            }
        });
        return button;
    }

    private View setupYouButton() {
        View button = findViewById(R.id.btn_you);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Actions.MY_PROFILE).putExtra(UserBrowser.Tab.EXTRA,UserBrowser.Tab.tracks));
            }
        });
        return button;
    }

    private ToggleButton setupToggleFade() {
            final ToggleButton tb = (ToggleButton) findViewById(R.id.toggle_fade);
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mRecorder.toggleFade();
                }
            });
            tb.setChecked(mRecorder.isFading());
            return tb;
        }

    private ToggleButton setupToggleOptimize() {
        final ToggleButton tb = (ToggleButton) findViewById(R.id.toggle_optimize);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mRecorder.toggleOptimize();
            }
        });
        tb.setChecked(mRecorder.isOptimized());
        return tb;
    }

    /* package */ void reset() {
        reset(false);
    }
    /* package */ void reset(boolean deleteRecording) {
        mRecorder.reset(deleteRecording);
        mWaveDisplay.reset();
        updateUi(CreateState.IDLE_RECORD, true);
    }

    public SoundRecorder getRecorder() {
        return mRecorder;
    }

    private void configureInitialState() {
        if (mRecorder == null || !mActive) return;

        boolean takeAction = false;
        CreateState newState = null;

        if (mRecorder.isRecording()) {
            newState = CreateState.RECORD;

        } else if (mRecorder.isPlaying()) {
            if (mCurrentState != CreateState.EDIT_PLAYBACK) newState = CreateState.PLAYBACK;
            configurePlaybackInfo();
            mWaveDisplay.gotoPlaybackMode();
            takeAction = true;

        } else {
            if (mRecorder.getRecording() != null) {
                if (mRecorder.isGeneratingWaveform()){
                    newState = CreateState.GENERATING_WAVEFORM;
                } else {
                    if (mCurrentState != CreateState.EDIT) newState = CreateState.IDLE_PLAYBACK;
                    configurePlaybackInfo();
                    mWaveDisplay.gotoPlaybackMode();
                }
            } else {
                newState = CreateState.IDLE_RECORD;
                takeAction = true;
            }
        }
        //TODO: re-enable later
        //noinspection ConstantIfStatement
        if (false) {
            if (mCurrentState != CreateState.RECORD && mRecipient == null) {
                mUnsavedRecordings = Recording.getUnsavedRecordings(
                    getContentResolver(),
                    SoundRecorder.RECORD_DIR,
                    mRecorder.getRecording(),
                    getCurrentUserId());

                if (!mUnsavedRecordings.isEmpty()) {
                    showDialog(Consts.Dialogs.DIALOG_UNSAVED_RECORDING);
                }
            }
        }
        updateUi(newState, takeAction);
    }

    private void updateUi(CreateState newState, boolean takeAction) {
        if (newState != null) mCurrentState = newState;
        switch (mCurrentState) {
            case GENERATING_WAVEFORM:
                mTxtTitle.setText(R.string.rec_title_generating_waveform);
                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideEditControls();
                hideView(mTxtInstructions, false, View.GONE);
                hideView(mChrono, false, View.GONE);
                hideView(mActionButton, false, View.GONE);
                hideSavedMessage();

                mActionButton.setClickable(false);
                mActionButton.setImageResource(R.drawable.btn_rec_deactivated);

                showView(mTxtRecordMessage, takeAction && mLastState != CreateState.IDLE_RECORD);
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
                mTxtTitle.setText(R.string.rec_title_idle_rec);
                setPlayButtonDrawable(false);
                if (!IOUtils.isSDCardAvailable()){

                    // state list drawables won't work with the image button
                    mActionButton.setClickable(false);
                    mActionButton.setImageResource(R.drawable.btn_rec_deactivated);
                    mTxtRecordMessage.setMessage(R.string.record_insert_sd_card);
                } else {
                    mActionButton.setClickable(true);
                    mActionButton.setImageResource(R.drawable.btn_rec_states);
                    if (!TextUtils.isEmpty(mRecordErrorMessage)) {
                        mTxtRecordMessage.setMessage(mRecordErrorMessage);
                    } else {
                        mTxtRecordMessage.loadSuggestion(mRecipient == null ? null : mRecipient.getDisplayName());
                    }
                }
                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideView(mChrono, false, View.INVISIBLE);
                hideEditControls();

                showView(mActionButton, false);
                showView(mTxtInstructions, takeAction && mLastState != CreateState.IDLE_RECORD);
                showView(mTxtRecordMessage, takeAction && mLastState != CreateState.IDLE_RECORD);

                if (mActive && mRecorder.getRecording() == null) {
                    mRecorder.startReading();
                }
                break;

            case RECORD:
                mTxtTitle.setText(R.string.rec_title_recording);
                hideView(mPlayButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mEditButton, takeAction && mLastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(mButtonBar, takeAction && mLastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideEditControls();
                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);

                showView(mChrono, takeAction && mLastState == CreateState.IDLE_RECORD);
                showView(mActionButton, false);

                mActionButton.setImageResource(R.drawable.btn_rec_pause_states);
                mTxtRecordMessage.setMessage("");
                mChrono.setDurationOnly(mRecorder.getRecordingElapsedTime());
                break;

            case IDLE_PLAYBACK:
                mTxtTitle.setText(R.string.rec_title_idle_play);
                if (takeAction) {
                    switch (mLastState) {
                        case PLAYBACK:
                        case EDIT:
                        case EDIT_PLAYBACK:
                            if (mRecorder.isPlaying()) {
                                mRecorder.togglePlayback();
                            }
                            break;
                    }
                    mWaveDisplay.gotoPlaybackMode();
                }

                mPlayButton.setVisibility(View.GONE); // just to fool the animation
                showView(mPlayButton, takeAction && (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mEditButton, takeAction && (mLastState == CreateState.RECORD || mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mActionButton, takeAction && (mLastState == CreateState.EDIT || mLastState == CreateState.EDIT_PLAYBACK));
                showView(mButtonBar, takeAction && (mLastState == CreateState.RECORD));
                if (mLastState == CreateState.RECORD) showSavedMessage(takeAction && mLastState == CreateState.RECORD);
                showView(mChrono, false);

                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);
                hideEditControls();

                setPlayButtonDrawable(false);
                mActionButton.setImageResource(R.drawable.btn_rec_states);

                configurePlaybackInfo();
                break;

            case PLAYBACK:
                mTxtTitle.setText(R.string.rec_title_playing);
                showView(mActionButton,false);
                showView(mPlayButton,false);
                showView(mEditButton,false);
                showView(mButtonBar,false);
                showView(mChrono,false);
                hideSavedMessage();

                hideView(mTxtInstructions,false,View.GONE);
                hideEditControls();
                hideView(mTxtRecordMessage,false,View.INVISIBLE);

                setPlayButtonDrawable(true);
                mActionButton.setImageResource(R.drawable.btn_rec_states);

                if (takeAction) startPlayback();
                break;

            case EDIT:
            case EDIT_PLAYBACK:
                mTxtTitle.setText(R.string.rec_title_editing);
                showView(mButtonBar, false);
                hideSavedMessage();

                if (mHasEditControlGroup) {
                    // portrait
                    showView(mEditControls, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                } else {
                    showView(mToggleFade, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                    showView(mPlayEditButton, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                    //showView(mToggleOptimize, takeAction && (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                }
                hideView(mPlayButton, false, View.GONE);
                hideView(mActionButton, false, View.GONE);
                hideView(mEditButton, false, View.GONE);

                hideView(mTxtInstructions, false, View.GONE);
                hideView(mTxtRecordMessage, false, View.INVISIBLE);

                final boolean isPlaying = mCurrentState == CreateState.EDIT_PLAYBACK;
                setPlayButtonDrawable(isPlaying);

                if (!isPlaying) {
                    configurePlaybackInfo();
                }

                if (takeAction) {
                    if (isPlaying) {
                        startPlayback();
                    } else {
                        if (mRecorder.isPlaying()) {
                            mRecorder.togglePlayback();
                        }
                        break;
                    }
                }
                break;
        }

        final boolean inEditState = mCurrentState.isEdit();
        configureButtonBar(inEditState);
        mWaveDisplay.setIsEditing(inEditState);

        mLastState = mCurrentState;
        mActionButton.setEnabled(true);

        if (mCurrentState != CreateState.GENERATING_WAVEFORM && mGeneratingWaveformProgressBar != null) {
            if (mGeneratingWaveformProgressBar.getParent() == mGaugeHolder) mGaugeHolder.removeView(mGeneratingWaveformProgressBar);
            mGeneratingWaveformProgressBar = null;
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
            mPlayButton.setBackgroundResource(R.drawable.btn_rec_play_pause_states);
            mPlayEditButton.setBackgroundResource(R.drawable.btn_rec_play_pause_states);
        } else {
            mPlayButton.setBackgroundResource(R.drawable.btn_rec_play_states);
            mPlayEditButton.setBackgroundResource(R.drawable.btn_rec_play_states);
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
            mRecorder.startRecording(mRecipient);
        } catch (IOException e) {
            onRecordingError(e.getMessage());
            updateUi(CreateState.IDLE_RECORD, true);
        }
        mWaveDisplay.gotoRecordMode();
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
            mChrono.setPlaybackProgress(currentPlaybackPosition,duration);
            mWaveDisplay.setProgress(((float) currentPlaybackPosition) / duration);
        } else {
            mChrono.setDurationOnly(duration);
            mWaveDisplay.setProgress(-1f);
        }
    }

    private void startPlayback() {
        if (!mRecorder.isPlaying()) {  //might already be playing back if activity just created
            mRecorder.play();
            track(Click.Record_play);
        }
    }

    private void setProgressInternal(long pos, long duration) {
        if (duration != 0){
            mChrono.setPlaybackProgress(pos, duration);
            mWaveDisplay.setProgress(((float) Math.max(0, Math.min(pos, duration))) / duration);
        }
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

    private void showSavedMessage(boolean animate) {
        if (mSavedMessageLayout.getVisibility() != View.VISIBLE) {
            mSavedMessageLayout.setVisibility(View.VISIBLE);
            if (animate) {
                mSavedMessageLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top));
            }
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
                updateUi(CreateState.RECORD, true);

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

                updateUi(CreateState.IDLE_PLAYBACK, true);

            } else if (SoundRecorder.PLAYBACK_STARTED.equals(action)) {
            } else if (SoundRecorder.PLAYBACK_PROGRESS.equals(action)) {
                setProgressInternal(intent.getLongExtra(SoundRecorder.EXTRA_POSITION, 0),
                        intent.getLongExtra(SoundRecorder.EXTRA_DURATION, 0));

            } else if (SoundRecorder.PLAYBACK_COMPLETE.equals(action) ||
                       SoundRecorder.PLAYBACK_STOPPED.equals(action) ||
                       SoundRecorder.PLAYBACK_ERROR.equals(action)) {

                if (mCurrentState == CreateState.PLAYBACK ||
                    mCurrentState == CreateState.EDIT_PLAYBACK) {
                    updateUi(mCurrentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK, true);
                }
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_REMOVED.equals(action)){
                // for messaging and action button activation
                if (mCurrentState == CreateState.IDLE_RECORD) updateUi(CreateState.IDLE_RECORD,false);

            } else if (SoundRecorder.WAVEFORM_GENERATED.equals(action)) {
                // we are now free to play back
                if (mCurrentState == CreateState.GENERATING_WAVEFORM) updateUi(CreateState.IDLE_PLAYBACK, true);
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (mCurrentState.isEdit()){
            updateUi(CreateState.IDLE_PLAYBACK, true);
        } else {
            super.onBackPressed();
        }
    }

    @Override public Dialog onCreateDialog(int which) {
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
                                            SoundCloudDB.insertRecording(getContentResolver(), recordings.get(i));
                                        } else {
                                            recordings.get(i).delete(null);
                                        }
                                    }
                                    mUnsavedRecordings = null;
                                }
                            })
                        .create();
            case Consts.Dialogs.DIALOG_DISCARD_RECORDING:
                return new AlertDialog.Builder(this)
                        .setTitle(null)
                        .setMessage(R.string.dialog_reset_recording_message)
                        .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int whichButton) {
                                    track(Click.Record_discard__ok);
                                    reset(true);
                                }
                        })
                        .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    track(Click.Record_discard_cancel);
                                }
                        })
                        .create();

            case Consts.Dialogs.DIALOG_REVERT_RECORDING:
                return new AlertDialog.Builder(this)
                        .setTitle(null)
                        .setMessage(R.string.dialog_revert_recording_message)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        track(Click.Record_revert__ok);
                                        mRecorder.revertFile();
                                        updateUi(CreateState.IDLE_PLAYBACK, true);
                                    }
                                })
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        track(Click.Record_revert_cancel);
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
                                        mRecorder.reset(true);
                                        finish();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

            case Consts.Dialogs.DIALOG_INSTALL_PROCESSOR:
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
            default:
                return null;
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(menu.size(), Consts.OptionsMenu.SELECT_FILE, 0, R.string.menu_select_file)
             .setIcon(android.R.drawable.ic_menu_add);
        menu.add(menu.size(), Consts.OptionsMenu.PROCESS, 0, R.string.process)
             .setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(menu.size(), Consts.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
             .setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(Consts.OptionsMenu.PROCESS);
        item.setVisible(mCurrentState == CreateState.EDIT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.SELECT_FILE:
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("audio/*"), REQUEST_GET_FILE);
                return true;

            case Consts.OptionsMenu.PROCESS:
                Recording recording = mRecorder.getRecording();
                if (recording != null) {
                    startActivityForResult(Intent.createChooser(recording.getProcessIntent(),
                            getString(R.string.sound_processed_pick_app)
                            ), REQUEST_PROCESS_SOUND);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* package, for testing */ CreateState getState() { return mCurrentState; }
}
