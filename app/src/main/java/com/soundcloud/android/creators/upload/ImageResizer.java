package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class ImageResizer implements Runnable {
    private final Recording recording;
    private final Context context;
    private final LocalBroadcastManager broadcastManager;

    public ImageResizer(Context context, Recording recording) {
        this.recording = recording;
        this.context = context.getApplicationContext();
        broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {
        Log.d(UploadService.TAG, "ImageResizer.run(" + recording + ")");

        if (!recording.hasArtwork()) {
            broadcast(UploadService.RESIZE_ERROR);
        } else {
            resize();
        }
    }

    private void resize() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "resizing " + recording.artwork_path);
        }
        try {
            broadcast(UploadService.RESIZE_STARTED);
            File resized = File.createTempFile("upload_tmp_" + recording.getId(), ".jpg");
            final long start = System.currentTimeMillis();
            if (ImageUtils.resizeImageFile(recording.artwork_path, resized, ImageUtils.RECOMMENDED_IMAGE_SIZE, ImageUtils.RECOMMENDED_IMAGE_SIZE)) {
                recording.resized_artwork_path = resized;
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("resized %s => %s  in %d ms", recording.artwork_path, recording.resized_artwork_path,
                            System.currentTimeMillis() - start));
                }
                broadcast(UploadService.RESIZE_SUCCESS);
            } else {
                Log.w(TAG, "did not resize image " + recording.artwork_path);
                recording.resized_artwork_path = recording.artwork_path;
                broadcast(UploadService.RESIZE_SUCCESS);
            }
        } catch (IOException e) {
            Log.e(TAG, "error resizing", e);
            broadcast(UploadService.RESIZE_ERROR);
        }
    }

    private void broadcast(String action) {
        broadcastManager.sendBroadcast(new Intent(action).putExtra(UploadService.EXTRA_RECORDING, recording));
    }
}
