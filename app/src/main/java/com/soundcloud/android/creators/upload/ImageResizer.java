package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.images.ImageUtils;

import android.util.Log;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class ImageResizer implements Runnable {
    private final Recording recording;

    @Inject EventBus eventBus;

    public ImageResizer(Recording recording) {
        this.recording = recording;
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    public ImageResizer(Recording recording, EventBus eventBus) {
        this.recording = recording;
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        Log.d(UploadService.TAG, "ImageResizer.run(" + recording + ")");

        if (!recording.hasArtwork()) {
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
        } else {
            resize();
        }
    }

    private void resize() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "resizing " + recording.artwork_path);
        }
        try {
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.resizeStarted(recording));

            File resized = File.createTempFile("upload_tmp_" + recording.getId(), ".jpg");
            final long start = System.currentTimeMillis();
            if (ImageUtils.resizeImageFile(recording.artwork_path, resized, ImageUtils.RECOMMENDED_IMAGE_SIZE, ImageUtils.RECOMMENDED_IMAGE_SIZE)) {
                recording.resized_artwork_path = resized;
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("resized %s => %s  in %d ms", recording.artwork_path, recording.resized_artwork_path,
                            System.currentTimeMillis() - start));
                }
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.resizeSuccess(recording));
            } else {
                Log.w(TAG, "did not resize image " + recording.artwork_path);
                recording.resized_artwork_path = recording.artwork_path;
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.resizeSuccess(recording));
            }
        } catch (IOException e) {
            Log.e(TAG, "error resizing", e);
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
        }
    }
}
