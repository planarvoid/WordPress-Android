package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.creators.record.jni.EncoderException;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.api.legacy.model.Recording;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

public class Processor implements Runnable {
    private final Recording recording;
    private final LocalBroadcastManager broadcastManager;


    public Processor(Context context, Recording recording) {
        this.recording = recording;
        broadcastManager = LocalBroadcastManager.getInstance(context);
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
                broadcast(UploadService.PROCESSING_STARTED);
                VorbisEncoder.extract(recording.getEncodedFile(), processFile, start / 1000d, end / 1000d);
                broadcast(UploadService.PROCESSING_SUCCESS);
            } catch (EncoderException e) {
                Log.w(TAG, "error processing "+encoded, e);
                broadcast(UploadService.PROCESSING_ERROR);
            }
        } else {
            Log.d(TAG, "no processing to be done");
            broadcast(UploadService.PROCESSING_SUCCESS);
        }
    }

    private void broadcast(String action) {
        broadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, recording));
    }
}
