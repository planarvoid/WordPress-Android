package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.ICloudCreateService;
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
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ExternalUploadProgress extends Activity {

    protected ICloudCreateService mCreateService;
    private long mUploadId;
    private Upload mUpload;

    private ProgressBar mProgressBar;
    private RelativeLayout mUploadingLayout;
    private RelativeLayout mFinishedLayout;
    private RelativeLayout mControlLayout;
    private TextView mProgressText;
    private boolean mProgressModeEncoding;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_upload_progress);


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
                if (mCreateService == null) return;
                try {
                    mCreateService.cancelUploadById(mUploadId);
                } catch (RemoteException e) {
                    e.printStackTrace();
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
                try {
                    if (mCreateService.startUpload(mUpload)) return;
                } catch (RemoteException e) {
                    Log.e(TAG, "error", e);
                }

                Toast.makeText(ExternalUploadProgress.this, getString(R.string.wait_for_upload_to_finish), Toast.LENGTH_LONG);

            }
        });

        Intent i = getIntent();
        if (i.hasExtra("upload")){
            mUpload = i.getParcelableExtra("upload");
            fillDataFromUpload();
        } else {
            mUploadId = i.getLongExtra("upload_id", 0);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUploadStatusListener);
    }

    private void fillDataFromUpload(){
        if (mUpload == null){
            throw new IllegalArgumentException("No Upload found");
        }

        mUploadId = mUpload.id;

        ((TextView) findViewById(R.id.track)).setText(mUpload.title);
        if (!TextUtils.isEmpty(mUpload.artworkPath)) {
            final File artworkFile = new File(mUpload.artworkPath);
            ImageUtils.setImage(new File(mUpload.artworkPath), ((ImageView) findViewById(R.id.icon)),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_width),
                    (int) getResources().getDimension(R.dimen.share_progress_icon_height));
        }

        if (mUpload.upload_status == Upload.UploadStatus.UPLOADED) {
            onUploadFinished(true);
        } else if (mUpload.upload_status != Upload.UploadStatus.UPLOADING && mUpload.upload_error) {
            onUploadFinished(false);
        }
    }

     private ServiceConnection createOsc = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mCreateService = (ICloudCreateService) binder;
            if (mUpload == null){
                try {
                    mUpload = mCreateService.getUploadById(mUploadId);
                    fillDataFromUpload();
                } catch (RemoteException e) {
                    e.printStackTrace();
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
        } else {
            ((ImageView) findViewById(R.id.result_icon)).setImageResource(R.drawable.fail);
            ((TextView) findViewById(R.id.result_message)).setText(R.string.share_fail_message);
            mControlLayout.setVisibility(View.VISIBLE);
        }
    }

}
