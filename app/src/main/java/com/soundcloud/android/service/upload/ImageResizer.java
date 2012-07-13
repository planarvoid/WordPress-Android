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
    private final Recording mRecording;
    private Context mContext;
    private LocalBroadcastManager mBroadcastManager;

    public ImageResizer(Context context, Recording recording) {
        mRecording = recording;
        mContext = context.getApplicationContext();
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {
        Log.d(UploadService.TAG, "ImageResizer.run("+ mRecording +")");

        if (!mRecording.hasArtwork()) {
            broadcast(UploadService.RESIZE_ERROR);
        } else {
            resize();
        }
    }

    private void resize() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "resizing "+ mRecording.artwork_path);
        try {
            broadcast(UploadService.RESIZE_STARTED);
            File resized = IOUtils.getCacheFile(mContext, "upload_tmp_"+ mRecording.id+".jpg");
            final long start = System.currentTimeMillis();
            if (ImageUtils.resizeImageFile(mRecording.artwork_path, resized, ImageUtils.RECOMMENDED_IMAGE_SIZE, ImageUtils.RECOMMENDED_IMAGE_SIZE)) {
                mRecording.resized_artwork_path = resized;
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("resized %s => %s  in %d ms", mRecording.artwork_path, mRecording.resized_artwork_path,
                            System.currentTimeMillis() - start));
                }
                broadcast(UploadService.RESIZE_SUCCESS);
            } else {
                Log.w(TAG, "did not resize image "+ mRecording.artwork_path);
                mRecording.resized_artwork_path = mRecording.artwork_path;
                broadcast(UploadService.RESIZE_SUCCESS);
            }
        } catch (IOException e) {
            Log.e(TAG, "error resizing", e);
            broadcast(UploadService.RESIZE_ERROR);
        }
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action).putExtra(UploadService.EXTRA_RECORDING, mRecording));
    }
}
