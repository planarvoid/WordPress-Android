package com.soundcloud.android.creators.record;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.DeprecatedRecordingProfile;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.ButtonBar;

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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class RecordActivity extends ScActivity implements CreateWaveDisplay.Listener {

    public static final int REQUEST_UPLOAD_SOUND = 1;

    private static final int MSG_ANIMATE_OUT_SAVE_MESSAGE = 0;
    private final Handler animateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ANIMATE_OUT_SAVE_MESSAGE:
                    hideSavedMessage();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown msg.what: " + msg.what);
            }
        }

    };
    private static final long SAVE_MSG_DISPLAY_TIME = 3000; //ms
    private final BroadcastReceiver statusListener = new BroadcastReceiver() {
        @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (SoundRecorder.RECORD_STARTED.equals(action)) {
                updateUi(CreateState.RECORD);

            } else if (SoundRecorder.RECORD_SAMPLE.equals(action)) {
                if (currentState == CreateState.IDLE_RECORD || currentState == CreateState.RECORD) {
                    waveDisplay.updateAmplitude(intent.getFloatExtra(SoundRecorder.EXTRA_AMPLITUDE, -1f), currentState == CreateState.RECORD);
                }
            } else if (SoundRecorder.RECORD_PROGRESS.equals(action)) {
                chrono.setDurationOnly(intent.getLongExtra(SoundRecorder.EXTRA_ELAPSEDTIME, -1l));
                updateTimeRemaining(intent.getLongExtra(SoundRecorder.EXTRA_TIME_REMAINING, 0l));
            } else if (SoundRecorder.RECORD_ERROR.equals(action)) {
                onRecordingError(getString(R.string.error_recording_message));
            } else if (SoundRecorder.RECORD_FINISHED.equals(action)) {
                // has the time run out?
                if (intent.getLongExtra(SoundRecorder.EXTRA_TIME_REMAINING, -1) == 0) {
                    AndroidUtils.showToast(RecordActivity.this, R.string.record_storage_is_full);
                }

                updateUi(CreateState.IDLE_PLAYBACK);

            } else if (SoundRecorder.PLAYBACK_STARTED.equals(action)) {
                updateUi((currentState == CreateState.EDIT || currentState == CreateState.EDIT_PLAYBACK) ?
                        CreateState.EDIT_PLAYBACK : CreateState.PLAYBACK);

            } else if (SoundRecorder.PLAYBACK_PROGRESS.equals(action)) {
                setProgressInternal(intent.getLongExtra(SoundRecorder.EXTRA_POSITION, 0),
                        intent.getLongExtra(SoundRecorder.EXTRA_DURATION, 0));

            } else if (SoundRecorder.PLAYBACK_COMPLETE.equals(action) ||
                    SoundRecorder.PLAYBACK_STOPPED.equals(action) ||
                    SoundRecorder.PLAYBACK_ERROR.equals(action)) {

                if (currentState == CreateState.PLAYBACK ||
                        currentState == CreateState.EDIT_PLAYBACK) {
                    updateUi(currentState == CreateState.EDIT_PLAYBACK ? CreateState.EDIT : CreateState.IDLE_PLAYBACK);
                }
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_REMOVED.equals(action)) {
                // for messaging and action button activation
                if (currentState == CreateState.IDLE_RECORD) {
                    updateUi(CreateState.IDLE_RECORD);
                }

            } else if (SoundRecorder.WAVEFORM_GENERATED.equals(action)) {
                // we are now free to play back
                if (currentState == CreateState.GENERATING_WAVEFORM) {
                    updateUi(CreateState.IDLE_PLAYBACK);
                }
            }
        }
    };
    private SoundRecorder recorder;
    private CreateState lastState, currentState;
    private TextView txtInstructions;
    private RecordMessageView txtRecordMessage;
    private ChronometerView chrono;
    private ViewGroup editControls, gaugeHolder, savedMessageLayout;
    private ImageButton actionButton;
    private CreateWaveDisplay waveDisplay;
    private ImageButton playButton, editButton, playEditButton;
    private ToggleButton toggleOptimize, toggleFade;
    private String recordErrorMessage;
    private ButtonBar buttonBar;
    private boolean active, hasEditControlGroup, seenSavedMessage;
    private List<Recording> unsavedRecordings;
    private ProgressBar generatingWaveformProgressBar;
    @Inject @LightCycle ActionBarController actionBarController;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = SoundRecorder.getIntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusListener, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        active = true;
        configureInitialState(getIntent());

        recorder.shouldUseNotifications(false);
        if (shouldTrackScreen()) {
            trackScreen();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
        recorder.stopReading(); // this will stop the amplitude reading loop

        /*  if we are either backing out or getting killed (finishing), or we are pausing cause we are leaving
            and not because of a configuration change, then we know we are going to the background, so tell the recorder
            to provide notifications. isChangingConfigurations availability dependent on SDK */
        if (isFinishing() || !isChangingConfigurations()) {
            recorder.shouldUseNotifications(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        waveDisplay.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("createSeenSavedMessage", seenSavedMessage);
        state.putString("createCurrentCreateState", currentState.toString());
        waveDisplay.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!state.isEmpty()) {
            seenSavedMessage = state.getBoolean("createSeenSavedMessage");
            if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
                updateUi(CreateState.valueOf(state.getString("createCurrentCreateState")), false);
            }
            waveDisplay.onRestoreInstanceState(state);
        }
    }

    @Override
    public void onSeek(float pct) {
        recorder.seekTo(pct);
    }

    @Override
    public void onAdjustTrimLeft(float newPos, long moveTime) {
        recorder.onNewStartPosition(newPos, moveTime);
        configurePlaybackInfo();
    }

    @Override
    public void onAdjustTrimRight(float newPos, long moveTime) {
        recorder.onNewEndPosition(newPos, moveTime);
        configurePlaybackInfo();
    }

    @Override
    public void onBackPressed() {
        if (currentState.isEdit()) {
            updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public Dialog onCreateDialog(int which) {
        if (which == Dialogs.UNSAVED_RECORDING.ordinal()) {
            return createUnsavedRecordingDialog();
        } else {
            throw new IllegalArgumentException("Unexpected dialog request code " + which);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        recorder = SoundRecorder.getInstance(this);
        txtInstructions = (TextView) findViewById(R.id.txt_instructions);
        txtRecordMessage = (RecordMessageView) findViewById(R.id.txt_record_message);

        chrono = (ChronometerView) findViewById(R.id.chronometer);
        chrono.setVisibility(View.INVISIBLE);

        editControls = (ViewGroup) findViewById(R.id.edit_controls);
        hasEditControlGroup = editControls != null;

        actionButton = setupActionButton();

        buttonBar = setupButtonBar();
        editButton = setupEditButton();
        playButton = setupPlaybutton(R.id.btn_play);
        playEditButton = setupPlaybutton(R.id.btn_play_edit);
        toggleFade = (ToggleButton) findViewById(R.id.toggle_fade);
        toggleOptimize = (ToggleButton) findViewById(R.id.toggle_optimize);

        waveDisplay = new CreateWaveDisplay(this);
        waveDisplay.setTrimListener(this);

        gaugeHolder = ((ViewGroup) findViewById(R.id.gauge_holder));
        gaugeHolder.addView(waveDisplay);

        savedMessageLayout = (ViewGroup) findViewById(R.id.saved_message_layout);
        updateUi(CreateState.IDLE_RECORD, false);
        handleIntent();
    }

    @Override
    protected void setContentView() {
        super.setContentView(R.layout.sc_create);
        presenter.setToolBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;

            case REQUEST_UPLOAD_SOUND:
                if (resultCode == RESULT_OK) {
                    reset();
                    if (data.getBooleanExtra(Actions.UPLOAD_EXTRA_UPLOADING, false)) {
                        finish(); // upload started, finish
                    }
                }
                // back button pressed, do nothing
                break;

            default:
                throw new IllegalArgumentException("Unknown requestCode: " + requestCode);
        }
    }

    private void handleIntent() {
        final Intent intent = getIntent();
        if (Actions.RECORD_START.equals(intent.getAction())) {
            if (!recorder.isRecording()) {
                reset();
                startRecording();
            }
            // don't want to receive the RECORD_START action on config changes, so set it as a normal record intent
            intent.setAction(Actions.RECORD);
        } else if (Actions.RECORD_STOP.equals(intent.getAction())) {
            if (recorder.isRecording()) {
                recorder.stopRecording();
            }
            intent.setAction(Actions.RECORD);
        } else {
            if (intent.getBooleanExtra("reset", false) && !recorder.isActive()) {
                intent.removeExtra("reset");
                reset();
            }
        }
    }

    private void trackScreen() {
        if (currentState.isEdit()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.RECORD_EDIT));
        } else {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.RECORD_MAIN));
        }
    }

    private void onRecordingError(String message) {
        recordErrorMessage = message;
        updateUi(CreateState.IDLE_RECORD);
    }

    private ButtonBar setupButtonBar() {
        ButtonBar buttonBar = (ButtonBar) findViewById(R.id.bottom_bar);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.RESET, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentState.isEdit()) {
                    showDiscardRecordingDialog();
                } else {
                    showRevertRecordingDialog();
                }
            }
        }), R.string.reset);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.DELETE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteRecordingDialog();
            }
        }), R.string.delete);
        buttonBar.addItem(new ButtonBar.MenuItem(MenuItems.SAVE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Recording rec = recorder.saveState();
                if (rec != null) {
                    if (currentState.isEdit()) {
                        updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
                    } else {
                        startActivityForResult(new Intent(RecordActivity.this, UploadActivity.class)
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
            @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
            public void onClick(View v) {
                switch (currentState) {
                    case IDLE_RECORD:
                        startRecording();
                        break;

                    case IDLE_PLAYBACK:
                    case PLAYBACK:
                        startRecording();
                        break;

                    case RECORD:
                        recorder.stopRecording();
                        // XXX use prefs
                        if (accountOperations.getAccountDataBoolean(PublicApiUser.DataKeys.SEEN_CREATE_AUTOSAVE)) {
                            AndroidUtils.showToast(RecordActivity.this, R.string.create_autosave_message);
                            accountOperations.setAccountData(PublicApiUser.DataKeys.SEEN_CREATE_AUTOSAVE, Boolean.TRUE.toString());
                        }
                        break;
                }
            }
        });
        return button;
    }

    private ImageButton setupPlaybutton(int id) {
        final ImageButton button = (ImageButton) findViewById(id);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recorder.togglePlayback();
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
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.RECORD_EDIT));
            }
        });
        return button;
    }

    private void setupToggleFade(boolean isFading) {
        toggleFade.setChecked(isFading);
        toggleFade.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                recorder.toggleFade();
            }
        });
    }

    private void setupToggleOptimize(boolean isOptimized) {
        toggleOptimize.setChecked(isOptimized);
        toggleOptimize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                recorder.toggleOptimize();
            }
        });
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private void configureInitialState(Intent intent) {
        if (recorder == null || !active) {
            return;
        }

        CreateState newState = null;
        if (recorder.isRecording()) {
            newState = CreateState.RECORD;
        } else {

            Recording recording = Recording.fromIntent(intent, this, accountOperations.getLoggedInUserUrn().getNumericId());
            if (recording != null) {

                // failsafe, if they try to play an uploading recording
                if (recording.upload_status == Recording.Status.UPLOADING) {
                    startActivity(recording.getMonitorIntent());
                    finish();
                    return;
                }

                recorder.setRecording(recording);
                seenSavedMessage = true;
            }

            if (recorder.isPlaying()) {
                // is this after orientation change during edit playback
                if (currentState != CreateState.EDIT_PLAYBACK) {
                    newState = CreateState.PLAYBACK;
                }
                configurePlaybackInfo();
                waveDisplay.gotoPlaybackMode(false);
            } else {
                // we have an inactive recorder, see what is loaded in it
                if (recorder.hasRecording()) {
                    if (recorder.isGeneratingWaveform()) {
                        newState = CreateState.GENERATING_WAVEFORM;
                    } else {
                        if (currentState != CreateState.EDIT) {
                            newState = CreateState.IDLE_PLAYBACK;
                        }
                        configurePlaybackInfo();
                        waveDisplay.gotoPlaybackMode(false);
                    }
                } else {
                    newState = CreateState.IDLE_RECORD;
                }
            }
        }

        Recording.clearRecordingFromIntent(intent);

        if (newState == CreateState.IDLE_RECORD) {
            RecordingStorage recordings = new RecordingStorage();
            unsavedRecordings = recordings.getUnsavedRecordings(
                    SoundRecorder.RECORD_DIR,
                    recorder.getRecording(),
                    accountOperations.getLoggedInUserUrn().getNumericId());

            if (!unsavedRecordings.isEmpty()) {
                showDialog(Dialogs.UNSAVED_RECORDING.ordinal());
            }
        }

        setupToggleFade(recorder.isFading());
        setupToggleOptimize(recorder.isOptimized());
        updateUi(newState);
    }

    private void updateUi(CreateState newState) {
        updateUi(newState, true);
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private void updateUi(CreateState newState, boolean animate) {
        if (newState != null) {
            currentState = newState;
        }
        switch (currentState) {
            case GENERATING_WAVEFORM:
                setTitle(R.string.rec_title_generating_waveform);
                hideView(playButton, lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(editButton, lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(buttonBar, lastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideEditControls();
                hideView(txtInstructions, false, View.GONE);
                hideView(chrono, false, View.GONE);
                hideView(actionButton, false, View.GONE);
                hideSavedMessage();

                actionButton.setClickable(false);
                actionButton.setImageResource(R.drawable.btn_recording_rec_deactivated);

                showView(txtRecordMessage, lastState != CreateState.IDLE_RECORD);
                txtRecordMessage.setMessage(R.string.create_regenerating_waveform_message);

                if (generatingWaveformProgressBar == null) {
                    generatingWaveformProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
                    generatingWaveformProgressBar.setIndeterminate(true);
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
                    gaugeHolder.addView(generatingWaveformProgressBar, lp);
                }
                break;

            case IDLE_RECORD:
                configureRecordButton(false);

                setTitle(R.string.rec_title_idle_rec);
                setPlayButtonDrawable(false);

                hideView(playButton, animate && lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(editButton, animate && lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(buttonBar, animate && lastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideView(chrono, false, View.INVISIBLE);
                hideEditControls();

                //mYouButton.setImageResource(R.drawable.ic_rec_you_dark);   TODO replace with action bar icon display
                showView(actionButton, false);
                showView(txtInstructions, animate && lastState != CreateState.IDLE_RECORD);
                showView(txtRecordMessage, animate && lastState != CreateState.IDLE_RECORD);

                if (active && recorder.getRecording() == null) {
                    recorder.startReading();
                }
                break;

            case RECORD:
                setTitle(R.string.rec_title_recording);
                hideView(playButton, lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(editButton, lastState != CreateState.IDLE_RECORD, View.GONE);
                hideView(buttonBar, lastState != CreateState.IDLE_RECORD, View.INVISIBLE);
                hideSavedMessage();
                hideEditControls();
                hideView(txtInstructions, false, View.GONE);
                hideView(txtRecordMessage, false, View.INVISIBLE);

                showView(chrono, lastState == CreateState.IDLE_RECORD);
                showView(actionButton, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you_dark);

                actionButton.setImageResource(R.drawable.btn_rec_pause_states);
                txtRecordMessage.setMessage("");
                chrono.setDurationOnly(recorder.getRecordingElapsedTime());
                break;

            case IDLE_PLAYBACK:
                configureRecordButton(true);
                setTitle(R.string.rec_title_idle_play);
                playButton.setVisibility(View.GONE); // just to fool the animation
                showView(playButton, (lastState == CreateState.RECORD || lastState == CreateState.EDIT || lastState == CreateState.EDIT_PLAYBACK));
                showView(editButton, (lastState == CreateState.RECORD || lastState == CreateState.EDIT || lastState == CreateState.EDIT_PLAYBACK));
                showView(actionButton, (lastState == CreateState.EDIT || lastState == CreateState.EDIT_PLAYBACK));
                showView(buttonBar, (lastState == CreateState.RECORD));
                if (lastState == CreateState.RECORD && !seenSavedMessage) {
                    showSavedMessage();
                }
                showView(chrono, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);

                hideView(txtInstructions, false, View.GONE);
                hideView(txtRecordMessage, false, View.INVISIBLE);
                hideEditControls();

                setPlayButtonDrawable(false);
                actionButton.setImageResource(R.drawable.btn_rec_resume_states);
                waveDisplay.gotoPlaybackMode(animate);
                configurePlaybackInfo();
                break;

            case PLAYBACK:
                setTitle(R.string.rec_title_playing);
                showView(actionButton, false);
                showView(playButton, false);
                showView(editButton, false);
                showView(buttonBar, false);
                showView(chrono, false);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);
                hideSavedMessage();

                hideView(txtInstructions, false, View.GONE);
                hideEditControls();
                hideView(txtRecordMessage, false, View.INVISIBLE);

                setPlayButtonDrawable(true);
                actionButton.setImageResource(R.drawable.btn_rec_resume_states);
                break;

            case EDIT:
            case EDIT_PLAYBACK:
                setTitle(R.string.rec_title_editing);
                showView(buttonBar, false);
                hideSavedMessage();

                if (hasEditControlGroup) {
                    // portrait
                    showView(editControls, (lastState != CreateState.EDIT && lastState != CreateState.EDIT_PLAYBACK));
                } else {
                    showView(toggleFade, (lastState != CreateState.EDIT && lastState != CreateState.EDIT_PLAYBACK));
                    showView(playEditButton, (lastState != CreateState.EDIT && lastState != CreateState.EDIT_PLAYBACK));
                    //showView(mToggleOptimize, (mLastState != CreateState.EDIT && mLastState != CreateState.EDIT_PLAYBACK));
                }
                hideView(playButton, false, View.GONE);
                hideView(actionButton, false, View.GONE);
                hideView(editButton, false, View.GONE);

                hideView(txtInstructions, false, View.GONE);
                hideView(txtRecordMessage, false, View.INVISIBLE);
                //mYouButton.setImageResource(R.drawable.ic_rec_you);

                final boolean isPlaying = currentState == CreateState.EDIT_PLAYBACK;
                setPlayButtonDrawable(isPlaying);
                if (!isPlaying) {
                    configurePlaybackInfo();
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown currentState: " + currentState);
        }

        final boolean inEditState = currentState.isEdit();
        configureButtonBar(inEditState);
        waveDisplay.setIsEditing(inEditState);

        lastState = currentState;
        actionButton.setEnabled(true);

        supportInvalidateOptionsMenu(); // adjusts color of you button

        if (currentState != CreateState.GENERATING_WAVEFORM && generatingWaveformProgressBar != null) {
            if (generatingWaveformProgressBar.getParent() == gaugeHolder) {
                gaugeHolder.removeView(generatingWaveformProgressBar);
            }
            generatingWaveformProgressBar = null;
        }
    }

    private boolean isPlayState() {
        return currentState == CreateState.EDIT_PLAYBACK || currentState == CreateState.PLAYBACK;
    }

    private void configureRecordButton(boolean isResume) {
        if (!IOUtils.isSDCardAvailable()) {
            // state list drawables won't work with the image button
            actionButton.setClickable(false);
            actionButton.setImageResource(isResume ? R.drawable.btn_recording_resume_deactivated : R.drawable.btn_recording_rec_deactivated);
            txtRecordMessage.setMessage(R.string.record_insert_sd_card);
        } else {
            actionButton.setClickable(true);
            actionButton.setImageResource(isResume ? R.drawable.btn_rec_resume_states : R.drawable.btn_rec_states);
            if (!TextUtils.isEmpty(recordErrorMessage)) {
                txtRecordMessage.setMessage(recordErrorMessage);
            } else {
                txtRecordMessage.loadSuggestion();
            }
        }
    }

    private void configureButtonBar(boolean isEditing) {
        buttonBar.setTextById(MenuItems.RESET, isEditing ? R.string.btn_revert_to_original : R.string.reset);
        buttonBar.setTextById(MenuItems.SAVE, isEditing ? R.string.btn_apply : R.string.btn_publish);

        final boolean showDelete = !isEditing && recorder.isSaved();
        buttonBar.toggleVisibility(MenuItems.RESET, !showDelete, false);
        buttonBar.toggleVisibility(MenuItems.DELETE, showDelete, true);
    }

    private void setPlayButtonDrawable(boolean playing) {
        if (playing) {
            playButton.setImageResource(R.drawable.btn_rec_play_pause_states);
            playEditButton.setImageResource(R.drawable.btn_rec_play_pause_states);
        } else {
            playButton.setImageResource(R.drawable.btn_rec_play_states);
            playEditButton.setImageResource(R.drawable.btn_rec_play_states);
        }
    }

    private void hideEditControls() {
        if (hasEditControlGroup) {
            // portrait
            hideView(editControls, false, View.GONE);
        } else {
            hideView(toggleFade, false, View.GONE);
            hideView(toggleOptimize, false, View.GONE);
            hideView(playEditButton, false, View.GONE);
        }
    }

    private void startRecording() {
        recordErrorMessage = null;

        try {
            recorder.startRecording(txtRecordMessage.getCurrentSuggestion());
            waveDisplay.gotoRecordMode();
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
            txtRecordMessage.setMessage(msg);
            txtRecordMessage.setVisibility(View.VISIBLE);
            return t;
        } else {
            txtRecordMessage.setVisibility(View.INVISIBLE);
            return t;
        }
    }

    private void configurePlaybackInfo() {
        final long currentPlaybackPosition = recorder.getCurrentPlaybackPosition();
        final long duration = recorder.getPlaybackDuration();

        if ((currentPlaybackPosition > 0 || recorder.isPlaying()) && currentPlaybackPosition < duration) {
            chrono.setPlaybackProgress(currentPlaybackPosition, duration);
            waveDisplay.setProgress(((float) currentPlaybackPosition) / duration);
        } else {
            chrono.setDurationOnly(duration);
            waveDisplay.setProgress(-1f);
        }
    }

    private void setProgressInternal(long pos, long duration) {
        if (duration != 0) {
            chrono.setPlaybackProgress(pos, duration);
            waveDisplay.setProgress(((float) Math.max(0, Math.min(pos, duration))) / duration);
        }
    }

    private void showView(final View v, boolean animate) {
        v.clearAnimation();
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
            if (animate) {
                AnimUtils.runFadeInAnimationOn(this, v);
            }
        }
    }

    private void hideView(final View v, boolean animate, final int visibilityOnComplete) {
        if (v.getVisibility() == View.VISIBLE) {
            if (animate) {
                v.setEnabled(false);
                AnimUtils.runFadeOutAnimationOn(this, v);
                v.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (v.getAnimation() == animation) {
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
        seenSavedMessage = true;
        if (savedMessageLayout.getVisibility() != View.VISIBLE) {
            savedMessageLayout.setVisibility(View.VISIBLE);
            savedMessageLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top));
            if (!animateHandler.hasMessages(MSG_ANIMATE_OUT_SAVE_MESSAGE)) {
                animateHandler.sendMessageDelayed(animateHandler.obtainMessage(MSG_ANIMATE_OUT_SAVE_MESSAGE),
                        SAVE_MSG_DISPLAY_TIME);
            }
        }
    }

    private void hideSavedMessage() {
        animateHandler.removeMessages(MSG_ANIMATE_OUT_SAVE_MESSAGE);
        if (savedMessageLayout.getVisibility() == View.VISIBLE) {
            final Animation slideOutAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_top);
            slideOutAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    savedMessageLayout.setVisibility(View.INVISIBLE);
                }
            });
            savedMessageLayout.startAnimation(slideOutAnim);
        }
    }

    private void showDeleteRecordingDialog() {
        showRemoveRecordingDialog(R.string.dialog_confirm_delete_recording_message);
    }

    private void showDiscardRecordingDialog() {
        showRemoveRecordingDialog(R.string.dialog_reset_recording_message);
    }

    private void showRemoveRecordingDialog(int message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        reset(true);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    private void showRevertRecordingDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_revert_recording_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recorder.revertFile();
                        updateUi(isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    private Dialog createUnsavedRecordingDialog() {
        final List<Recording> recordings = unsavedRecordings;

        if (recordings == null || recordings.isEmpty()) {
            return null;
        }
        final String[] fileIds = new String[recordings.size()];
        final boolean[] checked = new boolean[recordings.size()];
        for (int i = 0; i < recordings.size(); i++) {
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
                storage.store(recordings.get(i));
            } else {
                storage.delete(recordings.get(i));
            }
        }
        unsavedRecordings = null;
    }

    @VisibleForTesting
    public void reset() {
        reset(false);
    }

    /* package */ void reset(boolean deleteRecording) {
        seenSavedMessage = false;
        recorder.reset(deleteRecording);
        waveDisplay.reset();
        updateUi(CreateState.IDLE_RECORD);
    }

    @VisibleForTesting
    public CreateState getState() {
        return currentState;
    }

    @Deprecated
    private static enum Dialogs {
        UNSAVED_RECORDING // will be removed with local storage removal
    }

    public enum CreateState {
        GENERATING_WAVEFORM,
        IDLE_RECORD,
        RECORD,
        IDLE_PLAYBACK,
        PLAYBACK,
        EDIT,
        EDIT_PLAYBACK;

        public boolean isEdit() {
            return this == EDIT || this == EDIT_PLAYBACK;
        }

    }

    static interface MenuItems {

        int RESET = 1;
        int DELETE = 2;
        int SAVE = 3;
    }
}
