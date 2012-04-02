package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UploadMonitor extends Activity {
    private CloudCreateService mCreateService;
    private long mUploadId;
    private Upload mUpload;

    private ProgressBar mProgressBar;
    private RelativeLayout mUploadingLayout;
    private RelativeLayout mFinishedLayout;
    private RelativeLayout mControlLayout;
    private TextView mProgressText;
    private boolean mProgressModeEncoding;
    private final Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_monitor);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudCreateService.UPLOAD_STARTED);
        playbackFilter.addAction(CloudCreateService.UPLOAD_PROGRESS);
        playbackFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        playbackFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        playbackFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        registerReceiver(mUploadStatusListener, new IntentFilter(playbackFilter));

        mUploadingLayout = (RelativeLayout) findViewById(R.id.uploading_layout);
        mFinishedLayout = (RelativeLayout) findViewById(R.id.finished_layout);
        mControlLayout = (RelativeLayout) findViewById(R.id.control_layout);

        mFinishedLayout.setVisibility(View.GONE);
        mControlLayout.setVisibility(View.GONE);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);

        mProgressText = (TextView) findViewById(R.id.progress_txt);

        findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCreateService != null) {
                    mCreateService.cancelUploadById(mUploadId);
                }
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
                if (mCreateService == null) return;
                mControlLayout.setVisibility(View.GONE);
                if (mCreateService.startUpload(mUpload)) return;
                Toast.makeText(UploadMonitor.this, getString(R.string.wait_for_upload_to_finish), Toast.LENGTH_LONG);
            }
        });

        Intent i = getIntent();
        if (i.hasExtra("upload")){
            mUpload = i.getParcelableExtra("upload");
            fillDataFromUpload(mUpload);
        } else {
            mUploadId = i.getLongExtra("upload_id", 0);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUploadStatusListener);
    }

    private void fillDataFromUpload(final Upload upload) {
        if (upload == null) {
            finish();
            return;
        }
        mUploadId = upload.id;

        ((TextView) findViewById(R.id.track)).setText(upload.title);
        if (upload.artworkFile != null) {
            ImageUtils.setImage(upload.artworkFile, ((ImageView) findViewById(R.id.icon)),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_width),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_height));
        }

        if (upload.status == Upload.Status.UPLOADED) {
            onUploadFinished(true);
        } else if (upload.status != Upload.Status.UPLOADING && upload.isError()) {
            onUploadFinished(false);
        }
    }

     private ServiceConnection createOsc = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mCreateService = ((CloudCreateService.LocalBinder) binder).getService();
            if (mUpload == null) {
                mUpload = mCreateService.getUploadById(mUploadId);
                if (mUpload != null) {
                    fillDataFromUpload(mUpload);
                }
            }
        }
        public void onServiceDisconnected(ComponentName className) { }
    };

    @Override
    protected void onStart() {
        super.onStart();
        CloudUtils.bindToService(this, CloudCreateService.class, createOsc);
    }

    /**
     * Unbind our services
     */
    @Override
    protected void onStop() {
        super.onStop();
        CloudUtils.unbindFromService(this, CloudCreateService.class);
        mCreateService = null;
    }

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCreateService == null) return;

            String action = intent.getAction();
            if (action.equals(CloudCreateService.UPLOAD_STARTED)) {
                if (intent.getLongExtra("upload_id", -1) == mUploadId) {
                    // from a retry
                    mUploadingLayout.setVisibility(View.VISIBLE);
                    mFinishedLayout.setVisibility(View.GONE);
                    mProgressBar.setProgress(0);
                }
            } else if (action.equals(CloudCreateService.UPLOAD_PROGRESS)) {
                 if (intent.getLongExtra("upload_id", -1) == mUploadId){
                      mProgressBar.setProgress(intent.getIntExtra("progress",0));
                 }
                if (!mProgressModeEncoding && intent.hasExtra("encoding")){
                    mProgressModeEncoding = true;
                    mProgressText.setText(R.string.share_encoding);
                } else if (mProgressModeEncoding && !intent.hasExtra("encoding")){
                    mProgressModeEncoding = false;
                    mProgressText.setText(R.string.share_uploading);
                }
            } else if (action.equals(CloudCreateService.UPLOAD_SUCCESS)) {
                 onUploadFinished(true);
            } else if (action.equals(CloudCreateService.UPLOAD_ERROR)) {
                 onUploadFinished(false);
            } else if (action.equals(CloudCreateService.UPLOAD_CANCELLED)) {
                finish();
            }
        }
    };

    private void onUploadFinished(boolean success) {
        mUploadingLayout.setVisibility(View.GONE);
        mFinishedLayout.setVisibility(View.VISIBLE);
        if (success){
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
