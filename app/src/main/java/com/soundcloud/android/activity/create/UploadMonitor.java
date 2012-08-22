package com.soundcloud.android.activity.create;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.ButtonBar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
    private Recording mRecording;

    private ProgressBar mProcessingProgress, mTransferProgress;
    private RelativeLayout mUploadingLayout, mFinishedLayout;
    private ButtonBar mButtonBar;

    private TextView mProcessingProgressText, mUploadingProgressText;
    private TextView mTrackTitle;

    private boolean mTransferState;

    private final Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_monitor);

        mUploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        mFinishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);
        showUploading();

        mButtonBar = (ButtonBar) findViewById(R.id.bottom_bar);
        mButtonBar.setVisibility(View.GONE);

        mProcessingProgress = (ProgressBar) findViewById(R.id.progress_bar_processing);
        mProcessingProgress.setMax(MAX);

        mTransferProgress = (ProgressBar) findViewById(R.id.progress_bar_uploading);
        mTransferProgress.setMax(MAX);

        mProcessingProgressText = (TextView) findViewById(R.id.txt_progress_processing);
        mUploadingProgressText = (TextView) findViewById(R.id.txt_progress_uploading);

        mTrackTitle = (TextView) findViewById(R.id.track);

        findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording.isUploading()) {
                    mRecording.cancelUpload(UploadMonitor.this);
                    onCancelling();
                }
            }
        });

        mButtonBar.addItem(new ButtonBar.MenuItem(0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }), R.string.cancel);

        mButtonBar.addItem(new ButtonBar.MenuItem(0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUploading();
                setProcessProgress(-1);

                mRecording.upload(UploadMonitor.this);
            }
        }), R.string.retry);

        final Intent intent = getIntent();
        Recording intentRecording;
        if (intent != null && (intentRecording = Recording.fromIntent(intent, getContentResolver(), SoundCloudApplication.getUserId())) != null) {
            setRecording(intentRecording);
        }


        LocalBroadcastManager.getInstance(this).registerReceiver(mUploadStatusListener,
                UploadService.getIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUploadStatusListener);
    }

    protected void setRecording(final Recording recording) {
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
        } else if (recording.isUploading()){
            showUploading();
            setProcessProgress(-1);
        } else {
            // idle state, kick them out to their track list
            // this should only happen if they try to resume this activity from their recents
            startActivity(new Intent(Actions.MY_PROFILE)
                    .putExtra(UserBrowser.Tab.EXTRA, UserBrowser.Tab.tracks)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            );
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
        if (!mTransferState){
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
        } else {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.fail);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_fail_message);
            mButtonBar.setVisibility(View.VISIBLE);
        }
    }

    private void showFinished() {
        mUploadingLayout.setVisibility(View.GONE);
        mFinishedLayout.setVisibility(View.VISIBLE);
    }

    private void showUploading() {
        mUploadingLayout.setVisibility(View.VISIBLE);
        mFinishedLayout.setVisibility(View.GONE);
    }
}
