package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordOperations;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.ButtonBar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.inject.Inject;

public class UploadMonitorActivity extends TrackedActivity {

    public static final int BUTTON_BAR_CANCEL_ID = 0;
    public static final int BUTTON_BAR_RETRY_ID = 1;
    public static final String EXTRA_IN_TRANSFER_STATE = "transferState";
    public static final String EXTRA_PROGRESS = "progress";
    private static final int MAX = 100;
    private final Handler handler = new Handler();
    private Recording recording;
    private final BroadcastReceiver uploadStatusListener = new BroadcastReceiver() {
        @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void onReceive(Context context, Intent intent) {
            Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
            if (!UploadMonitorActivity.this.recording.equals(recording)) {
                return;
            }

            // update with latest broadcasted attributes
            UploadMonitorActivity.this.recording = recording;

            final String action = intent.getAction();
            final int progress = intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0);
            showUploading();

            if (UploadService.PROCESSING_STARTED.equals(action)) {
                setProcessProgress(-1);
            } else if (UploadService.PROCESSING_PROGRESS.equals(action)) {
                setProcessProgress(progress);
            } else if (UploadService.PROCESSING_SUCCESS.equals(action)) {
                setProcessProgress(MAX);
            } else if (UploadService.PROCESSING_ERROR.equals(action)) {
                onUploadFinished(false);
            } else if (UploadService.TRANSFER_STARTED.equals(action)) {
                setProcessProgress(MAX);
                setTransferProgress(-1);
            } else if (UploadService.TRANSFER_PROGRESS.equals(action)) {
                setTransferProgress(progress);
            } else if (UploadService.TRANSFER_SUCCESS.equals(action)) {
                setTransferProgress(MAX);
                onUploadFinished(true);
            } else if (UploadService.TRANSFER_ERROR.equals(action)) {
                onUploadFinished(false);
            } else if (UploadService.TRANSFER_CANCELLED.equals(action) || UploadService.PROCESSING_CANCELED.equals(action)) {
                finish();
            }
        }
    };
    private ProgressBar processingProgress, transferProgress;
    private RelativeLayout uploadingLayout, finishedLayout;
    private ButtonBar buttonBar;
    private TextView processingProgressText, uploadingProgressText;
    private TextView trackTitle;
    private boolean transferState;
    private int progress;
    @Inject RecordOperations recordOperations;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SoundCloudApplication.getObjectGraph().inject(this);
        init();
    }

    @VisibleForTesting void init() {
        setContentView(R.layout.upload_monitor);

        uploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        finishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);

        buttonBar = (ButtonBar) findViewById(R.id.bottom_bar);

        processingProgress = (ProgressBar) findViewById(R.id.progress_bar_processing);
        processingProgress.setMax(MAX);

        transferProgress = (ProgressBar) findViewById(R.id.progress_bar_uploading);
        transferProgress.setMax(MAX);

        processingProgressText = (TextView) findViewById(R.id.txt_progress_processing);
        uploadingProgressText = (TextView) findViewById(R.id.txt_progress_uploading);

        trackTitle = (TextView) findViewById(R.id.track);
        buttonBar.addItem(new ButtonBar.MenuItem(BUTTON_BAR_CANCEL_ID, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording.isUploading()) {
                    confirmCancel();
                } else {
                    finish();
                }
            }
        }), R.string.cancel);

        buttonBar.addItem(new ButtonBar.MenuItem(BUTTON_BAR_RETRY_ID, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUploading();
                setProcessProgress(-1);

                recordOperations.upload(UploadMonitorActivity.this, recording);
            }
        }), R.string.retry);

        showUploading();

        final Intent intent = getIntent();
        Recording recording;
        final long currentUserId = SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId();
        if ((recording = Recording.fromIntent(intent, this, currentUserId)) != null) {
            setRecording(recording);

            // check for initial progress to display
            if (intent.hasExtra(UploadService.EXTRA_STAGE)) {
                if (intent.getIntExtra(UploadService.EXTRA_STAGE, 0) == UploadService.UPLOAD_STAGE_PROCESSING) {
                    setProcessProgress(intent.getIntExtra(UploadService.EXTRA_PROGRESS, -1));
                } else {
                    setTransferProgress(intent.getIntExtra(UploadService.EXTRA_PROGRESS, -1));
                }
                intent.removeExtra(UploadService.EXTRA_STAGE);
                intent.removeExtra(UploadService.EXTRA_PROGRESS);
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(uploadStatusListener,
                    UploadService.getIntentFilter());
        } else {
            Log.d(TAG, "recording not found");
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt(EXTRA_PROGRESS, progress);
        state.putBoolean(EXTRA_IN_TRANSFER_STATE, transferState);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!state.isEmpty()) {
            if (!state.getBoolean(EXTRA_IN_TRANSFER_STATE)) {
                setProcessProgress(state.getInt(EXTRA_PROGRESS));
            } else {
                setTransferProgress(state.getInt(EXTRA_PROGRESS));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageUtils.recycleImageViewBitmap((ImageView) findViewById(R.id.icon));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadStatusListener);
    }

    private void setRecording(final Recording recording) {
        this.recording = recording;
        trackTitle.setText(recording.sharingNote(getResources()));

        if (recording.external_upload) {
            ViewStub stub = (ViewStub) findViewById(R.id.share_header_stub);
            stub.inflate();
        }

        if (recording.hasArtwork()) {
            ImageUtils.setImage(recording.getArtwork(), ((ImageView) findViewById(R.id.icon)),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_width),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_height));
        }

        if (recording.isUploaded()) {
            onUploadFinished(true);
        } else if (recording.isError()) {
            onUploadFinished(false);
        } else if (recording.isUploading()) {
            showUploading();
            setProcessProgress(-1);
        } else {
            // idle state, kick them out to their track list
            // this should only happen if they try to resume this activity from their recents
            startActivity(new Intent(Actions.YOUR_SOUNDS));
            finish();
        }
    }

    private void setProcessProgress(int progress) {
        this.progress = progress;
        if (progress < 0) {
            processingProgress.setIndeterminate(true);
            processingProgressText.setText(R.string.uploader_event_processing);

            transferProgress.setProgress(0);
            uploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        } else {
            processingProgress.setIndeterminate(false);
            processingProgress.setProgress(progress);

            if (progress == processingProgress.getMax()) {
                processingProgressText.setText(R.string.uploader_event_processing_finished);
            } else {
                processingProgressText.setText(getString(R.string.uploader_event_processing_percent, progress));
            }
            uploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        }
    }

    private void setTransferProgress(int progress) {
        this.progress = progress;
        if (!transferState) {
            transferState = true;

            processingProgress.setIndeterminate(false);
            processingProgress.setProgress(100);
            processingProgressText.setTextColor(getResources().getColor(R.color.upload_monitor_text_inactive));
            processingProgressText.setText(R.string.uploader_event_processing_finished);

            uploadingProgressText.setTextColor(getResources().getColor(R.color.upload_monitor_text));
        }

        if (progress < 0) {
            transferProgress.setIndeterminate(true);
            uploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        } else {
            transferProgress.setIndeterminate(false);
            transferProgress.setProgress(progress);
            uploadingProgressText.setText(getString(R.string.uploader_event_uploading_percent, progress));
        }
    }

    private void onCancelling() {
        if (!isFinishing()) {
            showUploading();

            processingProgress.setIndeterminate(true);
            transferProgress.setIndeterminate(true);

            processingProgressText.setText(R.string.uploader_event_cancelling);
            uploadingProgressText.setText(null);
        }
    }

    private void onUploadFinished(boolean success) {
        showFinished();
        if (success) {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.success);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_success_message);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        finish();
                    }
                }
            }, 2000);
            buttonBar.setVisibility(View.GONE);
        } else {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.fail);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_fail_message);
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.toggleVisibility(BUTTON_BAR_RETRY_ID, true, true);
        }
    }

    private void showFinished() {
        uploadingLayout.setVisibility(View.GONE);
        finishedLayout.setVisibility(View.VISIBLE);
    }

    private void showUploading() {
        uploadingLayout.setVisibility(View.VISIBLE);
        finishedLayout.setVisibility(View.GONE);
        buttonBar.toggleVisibility(BUTTON_BAR_RETRY_ID, false, true);
        buttonBar.setVisibility(View.VISIBLE);
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_cancel_upload_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (recording.isUploading()) {
                            recordOperations.cancelUpload(UploadMonitorActivity.this, recording);
                            onCancelling();
                        }
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

}
