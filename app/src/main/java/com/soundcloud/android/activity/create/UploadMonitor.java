package com.soundcloud.android.activity.create;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.ButtonBar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UploadMonitor extends Activity {
    private static final int MAX = 100;
    public static final int BUTTON_BAR_CANCEL_ID = 0;
    public static final int BUTTON_BAR_RETRY_ID = 1;
    public static final String EXTRA_IN_TRANSFER_STATE = "transferState";
    public static final String EXTRA_PROGRESS = "progress";
    private Recording mRecording;

    private ProgressBar mProcessingProgress, mTransferProgress;
    private RelativeLayout mUploadingLayout, mFinishedLayout;
    private ButtonBar mButtonBar;

    private TextView mProcessingProgressText, mUploadingProgressText;
    private TextView mTrackTitle;

    private boolean mTransferState;
    private int mProgress;

    private final Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_monitor);

        mUploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        mFinishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);

        mButtonBar = (ButtonBar) findViewById(R.id.bottom_bar);

        mProcessingProgress = (ProgressBar) findViewById(R.id.progress_bar_processing);
        mProcessingProgress.setMax(MAX);

        mTransferProgress = (ProgressBar) findViewById(R.id.progress_bar_uploading);
        mTransferProgress.setMax(MAX);

        mProcessingProgressText = (TextView) findViewById(R.id.txt_progress_processing);
        mUploadingProgressText = (TextView) findViewById(R.id.txt_progress_uploading);

        mTrackTitle = (TextView) findViewById(R.id.track);
        mButtonBar.addItem(new ButtonBar.MenuItem(BUTTON_BAR_CANCEL_ID, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording.isUploading()) {
                    confirmCancel();
                } else {
                    finish();
                }
            }
        }), R.string.cancel);

        mButtonBar.addItem(new ButtonBar.MenuItem(BUTTON_BAR_RETRY_ID, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUploading();
                setProcessProgress(-1);

                mRecording.upload(UploadMonitor.this);
            }
        }), R.string.retry);

        showUploading();

        final Intent intent = getIntent();
        Recording recording;
        if ((recording = Recording.fromIntent(intent, this, SoundCloudApplication.getUserId())) != null) {
            setRecording(recording);

            // check for initial progress to display
            if (intent.hasExtra(UploadService.EXTRA_STAGE)) {
                if (intent.getIntExtra(UploadService.EXTRA_STAGE,0) == UploadService.UPLOAD_STAGE_PROCESSING){
                    setProcessProgress(intent.getIntExtra(UploadService.EXTRA_PROGRESS,-1));
                } else {
                    setTransferProgress(intent.getIntExtra(UploadService.EXTRA_PROGRESS, -1));
                }
                intent.removeExtra(UploadService.EXTRA_STAGE);
                intent.removeExtra(UploadService.EXTRA_PROGRESS);
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(mUploadStatusListener,
                    UploadService.getIntentFilter());
        } else {
            Log.d(TAG, "recording not found");
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageUtils.recycleImageViewBitmap((ImageView) findViewById(R.id.icon));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUploadStatusListener);
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt(EXTRA_PROGRESS, mProgress);
        state.putBoolean(EXTRA_IN_TRANSFER_STATE, mTransferState);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!state.isEmpty()) {
            if (!state.getBoolean(EXTRA_IN_TRANSFER_STATE)){
                setProcessProgress(state.getInt(EXTRA_PROGRESS));
            } else {
                setTransferProgress(state.getInt(EXTRA_PROGRESS));
            }
        }
    }

    private void setRecording(final Recording recording) {
        mRecording = recording;
        mTrackTitle.setText(recording.sharingNote(getResources()));

        if (recording.external_upload) {
            ViewStub stub = (ViewStub) findViewById(R.id.share_header_stub);
            stub.inflate();
        }

        if (recording.hasArtwork()) {
            ImageUtils.setImage(recording.artwork_path, ((ImageView) findViewById(R.id.icon)),
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

    private final BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
            if (!mRecording.equals(recording)) return;

            // update with latest broadcasted attributes
            mRecording = recording;

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

    private void setProcessProgress(int progress) {
        mProgress = progress;
        if (progress < 0) {
            mProcessingProgress.setIndeterminate(true);
            mProcessingProgressText.setText(R.string.uploader_event_processing);

            mTransferProgress.setProgress(0);
            mUploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        } else {
            mProcessingProgress.setIndeterminate(false);
            mProcessingProgress.setProgress(progress);

            if (progress == mProcessingProgress.getMax()) {
                mProcessingProgressText.setText(R.string.uploader_event_processing_finished);
            } else {
                mProcessingProgressText.setText(getString(R.string.uploader_event_processing_percent, progress));
            }
            mUploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        }
    }

    private void setTransferProgress(int progress) {
        mProgress = progress;
        if (!mTransferState) {
            mTransferState = true;

            mProcessingProgress.setIndeterminate(false);
            mProcessingProgress.setProgress(100);
            mProcessingProgressText.setTextColor(getResources().getColor(R.color.upload_monitor_text_inactive));
            mProcessingProgressText.setText(R.string.uploader_event_processing_finished);

            mUploadingProgressText.setTextColor(getResources().getColor(R.color.upload_monitor_text));
        }

        if (progress < 0) {
            mTransferProgress.setIndeterminate(true);
            mUploadingProgressText.setText(R.string.uploader_event_not_yet_uploading);
        } else {
            mTransferProgress.setIndeterminate(false);
            mTransferProgress.setProgress(progress);
            mUploadingProgressText.setText(getString(R.string.uploader_event_uploading_percent, progress));
        }
    }

    private void onCancelling() {
        if (!isFinishing()) {
            showUploading();

            mProcessingProgress.setIndeterminate(true);
            mTransferProgress.setIndeterminate(true);

            mProcessingProgressText.setText(R.string.uploader_event_cancelling);
            mUploadingProgressText.setText(null);
        }
    }

    private void onUploadFinished(boolean success) {
        showFinished();
        if (success) {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.success);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_success_message);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        finish();
                    }
                }
            }, 2000);
            mButtonBar.setVisibility(View.GONE);
        } else {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.fail);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_fail_message);
            mButtonBar.setVisibility(View.VISIBLE);
            mButtonBar.toggleVisibility(BUTTON_BAR_RETRY_ID, true, true);
        }
    }

    private void showFinished() {
        mUploadingLayout.setVisibility(View.GONE);
        mFinishedLayout.setVisibility(View.VISIBLE);
    }

    private void showUploading() {
        mUploadingLayout.setVisibility(View.VISIBLE);
        mFinishedLayout.setVisibility(View.GONE);
        mButtonBar.toggleVisibility(BUTTON_BAR_RETRY_ID, false, true);
        mButtonBar.setVisibility(View.VISIBLE);
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_cancel_upload_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mRecording.isUploading()) {
                            mRecording.cancelUpload(UploadMonitor.this);
                            onCancelling();
                        }
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

}
