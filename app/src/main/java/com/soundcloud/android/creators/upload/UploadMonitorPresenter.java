package com.soundcloud.android.creators.upload;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.image.PlaceholderGenerator;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.ViewHelper;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.CircularProgressBar;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import org.jetbrains.annotations.NotNull;

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
    private final EventBusV2 eventBus;
    private final AccountOperations accountOperations;
    private final UserRepository userRepository;
    private final ViewHelper viewHelper;
    private boolean isUploading = false;
    private boolean isCancelling = false;
    private Recording recording;
    private Disposable uploadSubscription = Disposables.disposed();
    private Disposable userDisposable = Disposables.disposed();
    private UploadMonitorFragment uploadMonitorFragment;

    @BindView(R.id.track) TextView trackTitle;
    @BindView(R.id.track_username) TextView trackUsername;
    @BindView(R.id.track_duration) TextView trackDuration;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.upload_status_text) TextView uploadStatusText;
    @BindView(R.id.upload_progress) CircularProgressBar uploadProgress;
    @BindView(R.id.btn_action) ImageButton actionButton;
    @BindView(R.id.btn_cancel) Button cancelButton;

    @Inject
    public UploadMonitorPresenter(EventBusV2 eventBus,
                                  PlaceholderGenerator placeholderGenerator,
                                  AccountOperations accountOperations,
                                  UserRepository userRepository,
                                  ViewHelper viewHelper) {
        this.eventBus = eventBus;
        this.placeholderGenerator = placeholderGenerator;
        this.accountOperations = accountOperations;
        this.userRepository = userRepository;
        this.viewHelper = viewHelper;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle savedInstanceState) {
        super.onCreate(fragment, savedInstanceState);

        uploadMonitorFragment = (UploadMonitorFragment) fragment;
        uploadMonitorFragment.getActivity().setTitle(R.string.btn_upload);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);

        setCircularShape(actionButton, R.dimen.rec_upload_button_dimension);
        setCircularShape(uploadProgress, R.dimen.rec_upload_progress_dimension);
        updateRecording(uploadMonitorFragment.getArguments().getParcelable(RECORDING_KEY));
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        ((RecordActivity) uploadMonitorFragment.getActivity()).trackScreen(ScreenEvent.create(Screen.RECORD_PROGRESS));
        isCancelling = false;
        uploadSubscription = eventBus.subscribe(EventQueue.UPLOAD, new EventSubscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        uploadSubscription.dispose();
        userDisposable.dispose();
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

        trackTitle.setText(recording.sharingNote(uploadMonitorFragment.getContext()));
        trackDuration.setText(recording.formattedDuration());

        userDisposable.dispose();
        userDisposable = userRepository.userInfo(accountOperations.getLoggedInUserUrn())
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribeWith(
                                               new DefaultMaybeObserver<User>(){
                                                   @Override
                                                   public void onSuccess(@NonNull User user) {
                                                       trackUsername.setText(user.username());
                                                   }
                                               });

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
        actionButton.setBackgroundResource(R.drawable.rec_white_button);
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
            uploadStatusText.setText(uploadMonitorFragment.getString(R.string.uploader_event_uploading_percent,
                                                                     progressWithLimit));
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

    private final class EventSubscriber extends DefaultObserver<UploadEvent> {
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
        handler.postDelayed(() -> displayRecordScreen(), 2000);
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(uploadMonitorFragment.getActivity())
                .setView(new CustomFontViewBuilder(uploadMonitorFragment.getActivity()).setTitle(R.string.dialog_cancel_upload_message)
                                                                                       .get())
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                    if (isUploading) {
                        isUploading = false;
                        setCancellingState();
                        eventBus.publish(EventQueue.UPLOAD, UploadEvent.cancelled(recording));
                    }
                })
                .setNegativeButton(R.string.btn_no, null)
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
