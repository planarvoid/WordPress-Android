package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.jni.EncoderException;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import java.io.File;

public class Processor implements Runnable {
    private final Recording recording;

    @Inject EventBus eventBus;

    public Processor(Recording recording) {
        this.recording = recording;
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    public Processor(Recording recording, EventBus eventBus) {
        this.recording = recording;
        this.eventBus = eventBus;
    }

    @Override
    public void run() {

        File encoded = recording.getEncodedFile();
        File processFile = recording.getProcessedFile();

        long start = recording.getPlaybackStream().getStartPos();
        long end   = recording.getPlaybackStream().getEndPos();

        Log.d(TAG, String.format("Processor.run(%s, start=%d, end=%d)", recording, start, end));

        if (start > 0 || end != -1) {
            try {
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingStarted(recording));
                VorbisEncoder.extract(recording.getEncodedFile(), processFile, start / 1000d, end / 1000d);
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingSuccess(recording));
            } catch (EncoderException e) {
                Log.w(TAG, "error processing "+encoded, e);
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
            }
        } else {
            Log.d(TAG, "no processing to be done");
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingSuccess(recording));
        }
    }
}
