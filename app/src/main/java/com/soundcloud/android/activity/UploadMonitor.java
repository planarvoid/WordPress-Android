package com.soundcloud.android.activity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.upload.Poller;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tracking.Click;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UploadMonitor extends Activity {
    private Recording mUpload;

    private ProgressBar mProgressBarProcessing;
    private ProgressBar mProgressBarUploading;
    private RelativeLayout mUploadingLayout;
    private RelativeLayout mFinishedLayout;
    private ButtonBar mButtonBar;

    private TextView mProgressProcessingText;
    private TextView mProgressUploadingText;
    private TextView mTrackTitle;
    private final Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_monitor);

        mUploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        mFinishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);
        mButtonBar = (ButtonBar) findViewById(R.id.bottom_bar);

        mFinishedLayout.setVisibility(View.GONE);
        mButtonBar.setVisibility(View.GONE);

        mProgressBarProcessing = (ProgressBar) findViewById(R.id.progress_bar_processing);
        mProgressBarProcessing.setMax(100);

        mProgressBarUploading = (ProgressBar) findViewById(R.id.progress_bar_uploading);
        mProgressBarUploading.setMax(100);

        mProgressProcessingText = (TextView) findViewById(R.id.txt_progress_processing);
        mProgressUploadingText = (TextView) findViewById(R.id.txt_progress_uploading);

        mTrackTitle = (TextView) findViewById(R.id.track);

        findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUpload.cancelUpload(UploadMonitor.this);
            }
        });

        mButtonBar.addItem(new ButtonBar.MenuItem(0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }), R.string.btn_share_cancel);

        mButtonBar.addItem(new ButtonBar.MenuItem(0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButtonBar.setVisibility(View.GONE);
                mUpload.upload(UploadMonitor.this);
            }
        }), R.string.btn_share_cancel);


        mUpload = getIntent().getParcelableExtra(UploadService.EXTRA_RECORDING);
        fillDataFromUpload(mUpload);

        LocalBroadcastManager.getInstance(this).registerReceiver(mUploadStatusListener,
                        UploadService.getIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUploadStatusListener);
    }

    private void fillDataFromUpload(final Recording upload) {
        mTrackTitle.setText(upload.sharingNote(getResources()));
        if (upload.artwork_path != null) {
            ImageUtils.setImage(upload.artwork_path, ((ImageView) findViewById(R.id.icon)),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_width),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_height));
        }

        if (upload.isUploaded()) {
            onUploadFinished(true);
        } else if (upload.isError()) {
            onUploadFinished(false);
        } else {
            onProcessing(); // indeterminate state, wait for broadcasts
        }
    }

    private final BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
            if (!mUpload.equals(recording)) return;

            String action = intent.getAction();
            final int progress = intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0);

            if (UploadService.PROCESSING_STARTED.equals(action)) {
                onProcessing();

            } else if (UploadService.PROCESSING_PROGRESS.equals(action)) {
                mProgressBarProcessing.setProgress(progress);
                mProgressBarUploading.setProgress(0);

                mProgressProcessingText.setText(getString(R.string.uploader_event_processing_percent, progress));
                mProgressUploadingText.setText(R.string.uploader_event_not_yet_uploading);

            } else if (UploadService.PROCESSING_SUCCESS.equals(action)) {
                mProgressBarProcessing.setProgress(100);
                mProgressBarUploading.setProgress(0);

                mProgressProcessingText.setText(R.string.uploader_event_processing_finished);
                mProgressUploadingText.setText(R.string.uploader_event_not_yet_uploading);

            } else if (UploadService.PROCESSING_ERROR.equals(action)) {
                onUploadFinished(false);

            } else if (UploadService.TRANSFER_STARTED.equals(action)) {
                mProgressBarProcessing.setProgress(100);
                mProgressBarUploading.setProgress(0);

                mProgressProcessingText.setText(R.string.uploader_event_processing_failed);
                mProgressUploadingText.setText(R.string.uploader_event_not_yet_uploading);

            } else if (UploadService.TRANSFER_PROGRESS.equals(action)) {
                mProgressBarProcessing.setProgress(100);
                mProgressBarUploading.setProgress(progress);

                mProgressProcessingText.setText(R.string.uploader_event_processing_finished);
                mProgressUploadingText.setText(getString(R.string.uploader_event_uploading_percent, progress));

            } else if (UploadService.TRANSFER_SUCCESS.equals(action)) {
                onUploadFinished(true);

            } else if (UploadService.TRANSFER_ERROR.equals(action)) {
                onUploadFinished(false);

            } else if (UploadService.TRANSFER_CANCELLED.equals(action)) {
                finish();
            }
        }
    };

    private void onProcessing() {
        mUploadingLayout.setVisibility(View.VISIBLE);
        mFinishedLayout.setVisibility(View.GONE);

        mProgressBarProcessing.setIndeterminate(true);
        mProgressBarUploading.setProgress(0);

        mProgressProcessingText.setText(R.string.uploader_event_processing);
        mProgressUploadingText.setText(R.string.uploader_event_not_yet_uploading);
    }

    private void onUploadFinished(boolean success) {
        mUploadingLayout.setVisibility(View.GONE);
        mFinishedLayout.setVisibility(View.VISIBLE);
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
}
