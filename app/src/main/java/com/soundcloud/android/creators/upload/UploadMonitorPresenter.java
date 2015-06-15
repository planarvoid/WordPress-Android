package com.soundcloud.android.creators.upload;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.image.PlaceholderGenerator;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ViewHelper;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import org.jetbrains.annotations.NotNull;
import rx.Subscription;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class UploadMonitorPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {
    public static final String RECORDING_KEY = "recording";

    private final Handler handler = new Handler();
    private final PlaceholderGenerator placeholderGenerator;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final ViewHelper viewHelper;
    private boolean isUploading = false;
    private boolean isCancelling = false;
    private Recording recording;
    private Subscription subscription;
    private UploadMonitorFragment uploadMonitorFragment;

    @InjectView(R.id.track) TextView trackTitle;
    @InjectView(R.id.track_username) TextView trackUsername;
    @InjectView(R.id.track_duration) TextView trackDuration;
    @InjectView(R.id.icon) ImageView icon;
    @InjectView(R.id.upload_status_text) TextView uploadStatusText;
    @InjectView(R.id.upload_progress) CircularProgressBar uploadProgress;
    @InjectView(R.id.btn_action) ImageButton actionButton;
    @InjectView(R.id.btn_cancel) Button cancelButton;

    @Inject
    public UploadMonitorPresenter(EventBus eventBus,
                                  PlaceholderGenerator placeholderGenerator,
                                  AccountOperations accountOperations,
                                  ViewHelper viewHelper) {
        this.eventBus = eventBus;
        this.placeholderGenerator = placeholderGenerator;
        this.accountOperations = accountOperations;
        this.viewHelper = viewHelper;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle savedInstanceState) {
        super.onCreate(fragment, savedInstanceState);

        uploadMonitorFragment = (UploadMonitorFragment) fragment;
        uploadMonitorFragment.getActivity().setTitle(R.string.upload);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.inject(this, view);

        setCircularShape(actionButton, R.dimen.rec_upload_button_dimension);
        setCircularShape(uploadProgress, R.dimen.rec_upload_progress_dimension);
        updateRecording((Recording) uploadMonitorFragment.getArguments().getParcelable(RECORDING_KEY));
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        ((RecordActivity) uploadMonitorFragment.getActivity()).trackScreen(ScreenEvent.create(Screen.RECORD_PROGRESS));
        isCancelling = false;
        subscription = eventBus.subscribe(EventQueue.UPLOAD, new EventSubscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        subscription.unsubscribe();
        super.onPause(fragment);
    }

    private void updateRecording(@Nullable Recording recording) {
        if (recording != null) {
            setRecording(recording);
        }
    }

    private void setRecording(@NotNull final Recording recording) {
        if (recording.equals(this.recording)) {
            return;
        }

        this.recording = recording;

        trackTitle.setText(recording.sharingNote(uploadMonitorFragment.getResources()));
        trackUsername.setText(accountOperations.getLoggedInUser().getDisplayName());
        trackDuration.setText(recording.formattedDuration());

        if (recording.hasArtwork()) {
            ImageUtils.setImage(recording.getArtwork(), icon,
                    getDimension(R.dimen.record_progress_icon_width),
                    getDimension(R.dimen.share_progress_icon_height));
        } else {
            icon.setImageDrawable(placeholderGenerator.generateDrawable(String.valueOf(recording.hashCode())));
        }
    }

    private void setUploadingState() {
        cancelButton.setVisibility(View.VISIBLE);
        actionButton.setBackgroundResource(R.drawable.white_button);
        actionButton.setImageResource(R.drawable.ic_record_upload_white);
        actionButton.setEnabled(false);
        uploadProgress.setVisibility(View.VISIBLE);
    }

    private void setIndeterminateState() {
        setUploadingState();
        uploadProgress.setIndeterminate(true);
        uploadStatusText.setText(R.string.uploader_event_processing_your_sound);
    }

    private void setTransferState(int progress) {
        if (progress < 0) {
            setIndeterminateState();
        } else {
            int progressWithLimit = Math.max(0, Math.min(100, progress));
            setUploadingState();
            uploadProgress.setIndeterminate(false);
            uploadProgress.setProgress(progressWithLimit);
            uploadStatusText.setText(uploadMonitorFragment.getString(R.string.uploader_event_uploading_percent, progressWithLimit));
        }
    }

    private void setCancellingState() {
        cancelButton.setVisibility(View.GONE);
        actionButton.setEnabled(false);
        uploadProgress.setVisibility(View.VISIBLE);
        uploadProgress.setIndeterminate(true);
        uploadStatusText.setText(R.string.uploader_event_cancelling);
    }

    private void onUploadFinished(boolean success) {
        uploadProgress.setVisibility(View.INVISIBLE);

        if (success) {
            cancelButton.setVisibility(View.GONE);
            actionButton.setBackgroundResource(R.drawable.rec_button_states);
            actionButton.setImageResource(R.drawable.ic_record_check_white);
            actionButton.setEnabled(false);
            uploadStatusText.setText(R.string.recording_upload_finished);
            displayRecordScreenWithDelay();
        } else {
            cancelButton.setVisibility(View.VISIBLE);
            actionButton.setBackgroundResource(R.drawable.rec_button_states);
            actionButton.setImageResource(R.drawable.ic_record_refresh_white);
            actionButton.setEnabled(true);
            uploadStatusText.setText(R.string.recording_upload_failed);
        }
    }

    private final class EventSubscriber extends DefaultSubscriber<UploadEvent> {
        @Override
        public void onNext(UploadEvent uploadEvent) {
            if (isCancelling) {
                return;
            }

            int progress = uploadEvent.getProgress();
            isUploading = uploadEvent.isUploading();
            updateRecording(uploadEvent.getRecording());

            if (uploadEvent.isStarted()) {
                setIndeterminateState();
            } else if (uploadEvent.isProcessing()) {
                setIndeterminateState();
            } else if (uploadEvent.isTransfer()) {
                setTransferState(progress);
            } else if (uploadEvent.isFinished()) {
                onUploadFinished(uploadEvent.isUploadSuccess());
            } else if (uploadEvent.isCancelled()) {
                isCancelling = true;
                displayRecordScreenWithDelay();
            }
        }
    }

    private void displayRecordScreen() {
        RecordActivity activity = (RecordActivity) uploadMonitorFragment.getActivity();
        if (activity != null) {
            activity.onUploadToRecord();
        }
    }

    private void displayRecordScreenWithDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayRecordScreen();
            }
        }, 2000);
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(uploadMonitorFragment.getActivity())
                .setTitle(null)
                .setMessage(R.string.dialog_cancel_upload_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isUploading) {
                            isUploading = false;
                            setCancellingState();
                            eventBus.publish(EventQueue.UPLOAD, UploadEvent.cancelled(recording));
                        }
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    private int getDimension(int dimensionId) {
        return uploadMonitorFragment.getResources().getDimensionPixelSize(dimensionId);
    }

    private void setCircularShape(View view, int dimensionId) {
        viewHelper.setCircularButtonOutline(view, getDimension(dimensionId));
    }

    @OnClick(R.id.btn_cancel)
    public void onCancel() {
        if (isUploading) {
            showCancelDialog();
        } else {
            displayRecordScreen();
        }
    }

    @OnClick(R.id.btn_action)
    public void onRetry() {
        if (!isUploading) {
            RecordActivity activity = (RecordActivity) uploadMonitorFragment.getActivity();
            activity.startUpload(recording);
            activity.onMonitorToUpload(recording);
        }
    }
}
