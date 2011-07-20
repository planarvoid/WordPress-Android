package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
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
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class ExternalUploadProgress extends Activity {

    protected ICloudCreateService mCreateService;
    private String mUploadSourcePath;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_upload_progress);

        Intent i = getIntent();

        ((TextView) findViewById(R.id.track)).setText(i.getStringExtra("title"));
        if (!TextUtils.isEmpty(i.getStringExtra("artworkPath"))) {
            final File artworkFile = new File(i.getStringExtra("artworkPath"));
            ImageLoader.BindResult result;
            ImageLoader.Options options = new ImageLoader.Options();
            try {
                options.decodeInSampleSize = ImageUtils.determineResizeOptions(
                        artworkFile,
                        (int) (getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE),
                        (int) (getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE)).inSampleSize;
            } catch (IOException e) {
                Log.w(TAG, "error", e);
            }
            ImageLoader.get(getApplicationContext()).bind(((ImageView) findViewById(R.id.icon)), artworkFile.getAbsolutePath(), new ImageLoader.ImageViewCallback() {
                @Override
                public void onImageLoaded(ImageView view, String url) {
                }

                @Override
                public void onImageError(ImageView view, String url, Throwable error) {
                }
            }, options);
        }

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudCreateService.UPLOAD_PROGRESS);
        playbackFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        playbackFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        playbackFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        registerReceiver(mUploadStatusListener, new IntentFilter(playbackFilter));

    }

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCreateService == null) return;

            String action = intent.getAction();
            if (action.equals(CloudCreateService.UPLOAD_PROGRESS)) {

            } else if (action.equals(CloudCreateService.UPLOAD_SUCCESS)) {

            }
        }
    };
}
