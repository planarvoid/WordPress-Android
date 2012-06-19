package com.soundcloud.android.service.upload;

import static com.soundcloud.android.service.upload.UploadService.TAG;

import com.soundcloud.android.jni.EncoderException;
import com.soundcloud.android.jni.VorbisEncoder;
import com.soundcloud.android.model.Recording;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

public class Processor implements Runnable {
    private final Recording mRecording;
    private LocalBroadcastManager mBroadcastManager;

    public Processor(Context context, Recording recording) {
        mRecording = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {

        File encoded = mRecording.getEncodedFile();
        File processFile = mRecording.getProcessedFile();

        long start = mRecording.getPlaybackStream().getStartPos();
        long end   = mRecording.getPlaybackStream().getEndPos();

        Log.d(TAG, String.format("Processor.run(%s, start=%d, end=%d)", mRecording, start, end));

        if (start > 0 || end != -1) {
            try {
                broadcast(UploadService.PROCESSING_STARTED);
                VorbisEncoder.extract(mRecording.getEncodedFile(), processFile, start, end);
                broadcast(UploadService.PROCESSING_SUCCESS);
            } catch (EncoderException e) {
                Log.w(TAG, "error processing "+encoded, e);
                broadcast(UploadService.PROCESSING_ERROR);
            }
        } else {
            Log.d(TAG, "no processing to be done");
        }
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, mRecording));
    }
}
