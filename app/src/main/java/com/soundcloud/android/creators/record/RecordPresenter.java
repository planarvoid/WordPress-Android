package com.soundcloud.android.creators.record;

import static com.soundcloud.android.creators.record.RecordFragment.CreateState;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.EDIT;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.EDIT_PLAYBACK;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.IDLE_RECORD;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.PLAYBACK;
import static com.soundcloud.android.creators.record.RecordFragment.CreateState.RECORD;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Optional;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ViewHelper;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import javax.inject.Inject;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class RecordPresenter extends SupportFragmentLightCycleDispatcher<Fragment> implements CreateWaveDisplay.Listener {

    public static final String RECORD_STATE_KEY = "createCurrentCreateState";

    @InjectView(R.id.gauge_holder) ViewGroup gaugeHolder;
    @InjectView(R.id.chronometer) ChronometerView chrono;
    @InjectView(R.id.btn_action) ImageButton actionButton;
    @InjectView(R.id.action_text) TextView actionText;

    // play mode
    @InjectView(R.id.btn_next) View next;
    @InjectView(R.id.btn_delete) View delete;
    @InjectView(R.id.btn_play) ImageButton playButton;
    @InjectView(R.id.btn_edit) ImageButton editButton;

    // edit mode
    @InjectView(R.id.btn_revert) View revert;
    @InjectView(R.id.btn_apply) View apply;
    @InjectView(R.id.toggle_fade) SwitchCompat toggleFade;
    @InjectView(R.id.edit_controls) @Optional ViewGroup editControls;
    @InjectView(R.id.btn_play_edit) ImageButton playEditButton;

    private final RecordingOperations recordingOperations;
    private final ViewHelper viewHelper;
    private final SoundRecorder recorder;

    private CreateWaveDisplay waveDisplay;
    private CreateState currentState;
    private Subscription cleanupRecordingsSubscription = Subscriptions.empty();
    private Map<View, Pair<BitSet, Integer>> visibilities;
    private RecordFragment recordFragment;

    @Inject
    public RecordPresenter(RecordingOperations recordingOperations, ViewHelper viewHelper, SoundRecorder recorder) {
        this.recordingOperations = recordingOperations;
        this.viewHelper = viewHelper;
        this.recorder = recorder;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle state) {
        super.onCreate(fragment, state);
        this.recordFragment = (RecordFragment) fragment;

        if (state == null) {
            currentState = CreateState.IDLE_RECORD;
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);

        if (currentState == null) {
            currentState = CreateState.IDLE_RECORD;
        }

        recorder.shouldUseNotifications(false);
        if (recorder.hasRecording()) {
            configureStateBasedOnRecorder();
        } else {
            checkForUnsavedRecordings();
        }

        trackScreen(currentState.isEdit() ? ScreenEvent.create(Screen.RECORD_EDIT) : ScreenEvent.create(Screen.RECORD_MAIN));
    }

    @Override
    public void onPause(Fragment fragment) {
        cleanupRecordingsSubscription.unsubscribe();

        recorder.stopReading(); // this will stop the amplitude reading loop

        if (recordFragment.getActivity().isFinishing() || !recordFragment.getActivity().isChangingConfigurations()) {
            recorder.shouldUseNotifications(true);
        }

        super.onPause(fragment);
    }

    void checkForUnsavedRecordings() {
        // we may have a leftover recording, so defer state configuration until we check
        cleanupRecordingsSubscription = recordingOperations.cleanupRecordings(SoundRecorder.RECORD_DIR)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getCleanupRecordingsSubscriber());
    }

    @NotNull
    private DefaultSubscriber<CleanupRecordingsResult> getCleanupRecordingsSubscriber() {
        return new DefaultSubscriber<CleanupRecordingsResult>() {
            @Override
            public void onNext(CleanupRecordingsResult result) {
                if (!result.unsavedRecordings.isEmpty()) {
                    final Recording recording = result.unsavedRecordings.get(0);
                    recorder.setRecording(recording);
                }
                configureStateBasedOnRecorder();
            }
        };
    }

    private void trackScreen(ScreenEvent screenEvent) {
        ((RecordActivity) recordFragment.getActivity()).trackScreen(screenEvent);
    }

    @Override
    public void onStart(Fragment fragment) {
        super.onStart(fragment);
        this.recordFragment = (RecordFragment) fragment;

        IntentFilter intentFilter = SoundRecorder.getIntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        LocalBroadcastManager.getInstance(recordFragment.getActivity()).registerReceiver(statusListener, intentFilter);
    }

    @Override
    public void onStop(Fragment fragment) {
        super.onStop(fragment);
        LocalBroadcastManager.getInstance(recordFragment.getActivity()).unregisterReceiver(statusListener);
        this.recordFragment = null;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        ButterKnife.inject(this, view);

        visibilities = new HashMap<>();

        initializeView(actionButton, View.GONE, IDLE_RECORD, IDLE_PLAYBACK, RECORD, PLAYBACK);
        actionButton.setEnabled(false);

        initializeAndHideView(chrono, View.INVISIBLE, IDLE_RECORD, IDLE_PLAYBACK, RECORD, PLAYBACK, EDIT, EDIT_PLAYBACK);

        initializeAndHideView(next, View.GONE, PLAYBACK, IDLE_PLAYBACK);
        initializeAndHideView(delete, View.GONE, PLAYBACK, IDLE_PLAYBACK);

        initializeAndHideView(revert, View.GONE, EDIT, EDIT_PLAYBACK);
        initializeAndHideView(apply, View.GONE, EDIT, EDIT_PLAYBACK);
        initializeAndHideView(toggleFade, View.GONE, EDIT, EDIT_PLAYBACK);
        initializeAndHideView(playEditButton, View.GONE, EDIT, EDIT_PLAYBACK);

        if (editControls != null) {
            initializeAndHideView(editControls, View.GONE, EDIT, EDIT_PLAYBACK);
            initializeAndHideView(playButton, View.GONE, IDLE_PLAYBACK, PLAYBACK);
            initializeAndHideView(editButton, View.GONE, IDLE_PLAYBACK, PLAYBACK);
            initializeView(actionText, View.GONE, IDLE_RECORD, IDLE_PLAYBACK, RECORD, PLAYBACK);
        } else {
            initializeAndHideView(playButton, View.GONE, IDLE_RECORD, IDLE_PLAYBACK, PLAYBACK);
            initializeAndHideView(editButton, View.GONE, IDLE_RECORD, IDLE_PLAYBACK, PLAYBACK);
            initializeAndHideView(actionText, View.GONE);
        }

        final int actionButtonDimension = view.getResources().getDimensionPixelSize(R.dimen.rec_record_button_dimension);
        viewHelper.setCircularButtonOutline(this.actionButton, actionButtonDimension);

        waveDisplay = new CreateWaveDisplay(view.getContext());
        waveDisplay.setTrimListener(this);
        if (savedInstanceState != null) {
            waveDisplay.onRestoreInstanceState(savedInstanceState);
        }
        gaugeHolder.addView(waveDisplay);

        handleIntent(fragment.getActivity().getIntent());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        waveDisplay.onDestroy();
        super.onDestroyView(fragment);
    }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle state) {
        super.onRestoreInstanceState(fragment, state);

        if (state != null && !state.containsKey(RECORD_STATE_KEY)) {
            final String string = state.getString(RECORD_STATE_KEY);
            currentState = CreateState.valueOf(string);
        } else {
            currentState = CreateState.IDLE_RECORD;
        }
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle state) {
        super.onSaveInstanceState(fragment, state);

        state.putString(RECORD_STATE_KEY, currentState.name());
        if (waveDisplay != null) {
            waveDisplay.onSaveInstanceState(state);
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

    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        waveDisplay.updateAmplitude(maxAmplitude, isRecording);
    }

    public void reset() {
        recorder.reset(false);
        waveDisplay.reset();
        updateUi(CreateState.IDLE_RECORD);
    }

    protected void startRecording() {
        try {
            recorder.startRecording();
            waveDisplay.gotoRecordMode();
        } catch (IOException e) {
            updateUi(CreateState.IDLE_RECORD);
        }
    }

    public void updateRecordProgress(long duration) {
        chrono.setDurationOnly(duration);
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

    public void setProgress(long pos, long duration) {
        if (duration != 0) {
            chrono.setPlaybackProgress(pos, duration);
            waveDisplay.setProgress(((float) Math.max(0, Math.min(pos, duration))) / duration);
        }
    }

    public void setProgress(float progress) {
        waveDisplay.setProgress(progress);
    }

    private void initializeAndHideView(View view, int visibilityWhenHidden, CreateState... createStates) {
        initializeView(view, visibilityWhenHidden, createStates);
        viewHelper.hideView(view, visibilityWhenHidden, false);
    }

    private void initializeView(View view, int visibilityWhenHidden, CreateState... createStates) {
        visibilities.put(view, new Pair<>(createVisibilitySet(createStates), visibilityWhenHidden));
    }

    private void updateUi(CreateState newState) {
        currentState = newState;

        switch (currentState) {
            case IDLE_RECORD:
                configureRecordButton(false);
                if (recordFragment.isResumed() && recorder.getRecording() == null) {
                    recorder.startReading();
                }
                chrono.setText(R.string.record_instructions);
                break;

            case RECORD:
                configureRecordButton(true);
                chrono.setDurationOnly(recorder.getRecordingElapsedTime());
                break;

            case IDLE_PLAYBACK:
                configureRecordButton(false);
                waveDisplay.gotoPlaybackMode(true);
                configurePlaybackInfo();
                break;

            case PLAYBACK:
                actionButton.setImageResource(R.drawable.ic_record_record_white);
                break;

            case EDIT:
                configurePlaybackInfo();
                break;

            default:
                break;
        }

        toggleFade.setChecked(recorder.isFading());

        configureTitle();
        configureViewVisibilities();

        waveDisplay.setIsEditing(currentState.isEdit());
        setPlayButtonDrawable(currentState.isPlayState());
    }

    private void configureTitle() {
        recordFragment.getActivity().setTitle(currentState.getTitleId());
    }

    private void configureViewVisibilities() {
        for (Map.Entry<View, Pair<BitSet, Integer>> entry : visibilities.entrySet()) {
            final boolean b = entry.getValue().first.get(currentState.ordinal());
            if (b) {
                viewHelper.showView(entry.getKey(), true);
            } else {
                viewHelper.hideView(entry.getKey(), entry.getValue().second, false);
            }
        }
    }

    private void setPlayButtonDrawable(boolean playing) {
        if (playing) {
            playButton.setImageResource(R.drawable.ic_record_pause);
            playEditButton.setImageResource(R.drawable.ic_record_pause);
        } else {
            playButton.setImageResource(R.drawable.ic_record_play);
            playEditButton.setImageResource(R.drawable.ic_record_play);
        }
    }

    private BitSet createVisibilitySet(CreateState... visibleStates) {
        final BitSet bitSet = new BitSet(CreateState.values().length);

        for (CreateState createState : visibleStates) {
            bitSet.set(createState.ordinal());
        }

        return bitSet;
    }

    private void configureRecordButton(boolean isRecording) {
        if (isRecording) {
            actionButton.setBackgroundResource(R.drawable.rec_white_button);
            actionButton.setImageResource(R.drawable.ic_record_record_orange);
            actionText.setText(recordFragment.getString(R.string.record_tap_to_pause));

        } else {
            actionButton.setBackgroundResource(R.drawable.rec_button_states);
            actionButton.setImageResource(R.drawable.ic_record_record_white);

            if (currentState == IDLE_PLAYBACK || currentState == PLAYBACK) {
                actionText.setText(recordFragment.getString(R.string.record_tap_to_resume));
            } else {
                actionText.setText(recordFragment.getString(R.string.record_tap_to_record));
            }
        }

        actionButton.setEnabled(IOUtils.isSDCardAvailable());
    }

    void configureStateBasedOnRecorder() {
        CreateState newState = currentState;
        if (recorder.isRecording()) {
            newState = RECORD;
        } else {
            if (recorder.isPlaying()) {
                // is this after orientation change during edit playback
                if (currentState != EDIT_PLAYBACK) {
                    newState = CreateState.PLAYBACK;
                }
                configurePlaybackInfo();
                waveDisplay.gotoPlaybackMode(false);
            } else {
                // we have an inactive recorder, see what is loaded in it
                if (recorder.hasRecording()) {
                    if (currentState != EDIT) {
                        newState = CreateState.IDLE_PLAYBACK;
                    }
                    configurePlaybackInfo();
                    waveDisplay.gotoPlaybackMode(false);
                } else {
                    newState = CreateState.IDLE_RECORD;

                }
            }
        }

        updateUi(newState);
    }

    @OnClick(R.id.btn_action)
    void onActionButton() {
        if (currentState == CreateState.RECORD) {
            recorder.stopRecording();
        } else {
            startRecording();
        }
    }

    @OnClick(R.id.btn_delete)
    void showDeleteRecordingDialog() {
        showRemoveRecordingDialog(R.string.dialog_confirm_delete_recording_message);
    }

    @OnClick(R.id.btn_revert)
    void revert() {
        showRevertRecordingDialog();
    }

    @OnClick(R.id.btn_next)
    void next() {
        ((RecordActivity) recordFragment.getActivity()).onRecordToMetadata();
    }

    @OnClick(R.id.btn_apply)
    void save() {
        updateUi(currentState.isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
    }

    @OnClick({R.id.btn_play_edit, R.id.btn_play})
    void playEditButton() {
        recorder.togglePlayback();
    }

    @OnClick(R.id.btn_edit)
    void onEdit() {
        updateUi(currentState.isPlayState() ? EDIT_PLAYBACK : EDIT);
        ((RecordActivity) recordFragment.getActivity()).trackScreen(ScreenEvent.create(Screen.RECORD_EDIT));
    }

    @OnCheckedChanged(R.id.toggle_fade)
    void toggleFade() {
        toggleFade.setChecked(recorder.toggleFade());
    }


    private void showRemoveRecordingDialog(int message) {
        new AlertDialog.Builder(recordFragment.getActivity())
                .setMessage(message)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recorder.reset(true);
                        waveDisplay.reset();
                        checkForUnsavedRecordings();
                    }
                })
                .show();
    }

    private void showRevertRecordingDialog() {
        new AlertDialog.Builder(recordFragment.getActivity())
                .setMessage(R.string.dialog_revert_recording_message)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recorder.revertFile();
                        updateUi(currentState.isPlayState() ? CreateState.PLAYBACK : CreateState.IDLE_PLAYBACK);
                    }
                })
                .show();
    }

    private final BroadcastReceiver statusListener = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (SoundRecorder.RECORD_STARTED.equals(action)) {
                updateUi(RECORD);
            } else if (SoundRecorder.RECORD_SAMPLE.equals(action)) {
                if (currentState == CreateState.IDLE_RECORD || currentState == RECORD) {
                    updateAmplitude(intent.getFloatExtra(SoundRecorder.EXTRA_AMPLITUDE, -1f), currentState == RECORD);
                }
            } else if (SoundRecorder.RECORD_PROGRESS.equals(action)) {
                updateRecordProgress(intent.getLongExtra(SoundRecorder.EXTRA_ELAPSEDTIME, -1l));
            } else if (SoundRecorder.RECORD_ERROR.equals(action)) {
                updateUi(CreateState.IDLE_RECORD);
            } else if (SoundRecorder.RECORD_FINISHED.equals(action)) {
                // has the time run out?
                if (intent.getLongExtra(SoundRecorder.EXTRA_TIME_REMAINING, -1) == 0) {
                    AndroidUtils.showToast(recordFragment.getActivity(), R.string.record_storage_is_full);
                }

                updateUi(CreateState.IDLE_PLAYBACK);
            } else if (SoundRecorder.PLAYBACK_STARTED.equals(action)) {
                updateUi((currentState == EDIT || currentState == EDIT_PLAYBACK) ?
                        EDIT_PLAYBACK : CreateState.PLAYBACK);
            } else if (SoundRecorder.PLAYBACK_PROGRESS.equals(action)) {
                setProgress(intent.getLongExtra(SoundRecorder.EXTRA_POSITION, 0),
                        intent.getLongExtra(SoundRecorder.EXTRA_DURATION, 0));
            } else if (SoundRecorder.PLAYBACK_COMPLETE.equals(action) ||
                    SoundRecorder.PLAYBACK_STOPPED.equals(action) ||
                    SoundRecorder.PLAYBACK_ERROR.equals(action)) {

                if (currentState == CreateState.PLAYBACK ||
                        currentState == EDIT_PLAYBACK) {
                    updateUi(currentState == EDIT_PLAYBACK ? EDIT : CreateState.IDLE_PLAYBACK);
                }
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_REMOVED.equals(action)) {
                // for messaging and action button activation
                if (currentState == CreateState.IDLE_RECORD) {
                    updateUi(CreateState.IDLE_RECORD);
                }
            }
        }
    };

    private void handleIntent(Intent intent) {
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
}
