package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UploadMonitor extends Activity {
    private Recording mUpload;

    private ProgressBar mProgressBar;
    private RelativeLayout mUploadingLayout;
    private RelativeLayout mFinishedLayout;
    private RelativeLayout mControlLayout;
    private TextView mProgressText;
    private TextView mTrackTitle;
    private boolean mProgressModeEncoding;
    private final Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_monitor);

        LocalBroadcastManager.getInstance(this).registerReceiver(mUploadStatusListener,
                UploadService.getIntentFilter());

        mUploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        mFinishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);
        mControlLayout = (RelativeLayout) findViewById(R.id.control_layout);

        mFinishedLayout.setVisibility(View.GONE);
        mControlLayout.setVisibility(View.GONE);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);

        mProgressText = (TextView) findViewById(R.id.progress_txt);
        mTrackTitle = (TextView) findViewById(R.id.track);

        findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUpload.cancelUpload(UploadMonitor.this);
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btn_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mControlLayout.setVisibility(View.GONE);
                mUpload.upload(UploadMonitor.this);
            }
        });

        mUpload = getIntent().getParcelableExtra(UploadService.EXTRA_RECORDING);
        fillDataFromUpload(mUpload);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUploadStatusListener);
    }

    private void fillDataFromUpload(final Recording upload) {
        mTrackTitle.setText(upload.title);
        if (upload.artwork_path != null) {
            ImageUtils.setImage(upload.artwork_path, ((ImageView) findViewById(R.id.icon)),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_width),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_height));
        }

        if (upload.isUploaded()) {
            onUploadFinished(true);
        } else if (upload.isError()) {
            onUploadFinished(false);
        }
    }

    private final BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
            if (!mUpload.equals(recording)) return;

            String action = intent.getAction();
            if (UploadService.UPLOAD_STARTED.equals(action)) {
                // from a retry
                mUploadingLayout.setVisibility(View.VISIBLE);
                mFinishedLayout.setVisibility(View.GONE);
                mProgressBar.setProgress(0);
            } else if (UploadService.UPLOAD_PROGRESS.equals(action)) {
                mProgressBar.setProgress(intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0));

                if (!mProgressModeEncoding && intent.hasExtra("encoding")) {
                    mProgressModeEncoding = true;
                    mProgressText.setText(R.string.share_encoding);
                } else if (mProgressModeEncoding && !intent.hasExtra("encoding")) {
                    mProgressModeEncoding = false;
                    mProgressText.setText(R.string.share_uploading);
                }
            } else if (UploadService.UPLOAD_SUCCESS.equals(action)) {
                onUploadFinished(true);
            } else if (UploadService.UPLOAD_ERROR.equals(action)) {
                onUploadFinished(false);
            } else if (UploadService.UPLOAD_CANCELLED.equals(action)) {
                finish();
            }
        }
    };

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
            mControlLayout.setVisibility(View.VISIBLE);
        }
    }
}
