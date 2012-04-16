package com.soundcloud.android.service.upload;

import static com.soundcloud.android.service.upload.UploadService.TAG;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class ImageResizer implements Runnable {
    private static final int RECOMMENDED_SIZE = 800;
    private final Recording recording;
    private Context context;
    private LocalBroadcastManager mBroadcastManager;

    public ImageResizer(Context context, Recording recording) {
        this.recording = recording;
        this.context = context.getApplicationContext();
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {
        if (!recording.hasArtwork()) {
            broadcast(UploadService.RESIZE_ERROR);
        } else {
            resize();
        }
    }

    private void resize() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "resizing "+recording.artwork_path);
        try {
            broadcast(UploadService.RESIZE_STARTED);
            File resized = IOUtils.getCacheFile(context, "upload_tmp_"+recording.id+".png");
            final long start = System.currentTimeMillis();
            if (ImageUtils.resizeImageFile(recording.artwork_path, resized, RECOMMENDED_SIZE, RECOMMENDED_SIZE)) {
                recording.resized_artwork_path = resized;
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "resized "+recording.artwork_path+" => "+resized+" in "
                            +(System.currentTimeMillis() - start)+" ms");
                }
                // XXX slow: 6secs on Galaxy Nexus 1
                broadcast(UploadService.RESIZE_SUCCESS);
            } else {
                Log.w(TAG, "did not resize image "+recording.artwork_path);
                recording.resized_artwork_path = recording.artwork_path;
                broadcast(UploadService.RESIZE_SUCCESS);
            }
        } catch (IOException e) {
            Log.e(TAG, "error resizing", e);
            broadcast(UploadService.RESIZE_ERROR);
        }
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action).putExtra(UploadService.EXTRA_RECORDING, recording));
    }
}
