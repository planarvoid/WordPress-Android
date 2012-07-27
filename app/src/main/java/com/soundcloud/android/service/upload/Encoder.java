package com.soundcloud.android.service.upload;


import static com.soundcloud.android.service.upload.UploadService.TAG;

import com.soundcloud.android.jni.ProgressListener;
import com.soundcloud.android.jni.VorbisEncoder;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.audio.AudioConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Encoder extends BroadcastReceiver implements Runnable, ProgressListener {
    private final Recording mRecording;
    private LocalBroadcastManager mBroadcastManager;
    private volatile boolean mCancelled;

    public Encoder(Context context, Recording recording) {
        mRecording = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mBroadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
    }

    @Override
    public void run() {
        Log.d(TAG, "Encoder.run("+mRecording+")");

        try {
            final File in = mRecording.getFile();
            final File out = mRecording.getEncodedFile();

            long now = System.currentTimeMillis();
            broadcast(UploadService.PROCESSING_STARTED);
            VorbisEncoder.encodeWav(in, out, AudioConfig.DEFAULT.quality, this);
            // double check
            if (out.exists() && out.length() > 0) {
                Log.d(TAG, "encoding finished in " + (System.currentTimeMillis()-now)+ " msecs");
                broadcast(UploadService.PROCESSING_SUCCESS);
            } else {
                Log.w(TAG, "encoded file does not exist or is empty");
                broadcast(UploadService.PROCESSING_ERROR);
            }
        } catch (UserCanceledException e) {
            broadcast(UploadService.PROCESSING_CANCELED);
        } catch (IOException e) {
            broadcast(UploadService.PROCESSING_ERROR);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
        if (mRecording.equals(recording)) {
            Log.d(TAG, "canceling encoding of " + recording);
            cancel();
        }
    }

    private void cancel() {
        mCancelled = true;
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, mRecording));
    }

    @Override
    public void onProgress(long current, long max) throws UserCanceledException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Encoder#onProgress("+current+", "+max+")");
        }
        mBroadcastManager.sendBroadcast(new Intent(UploadService.PROCESSING_PROGRESS)
                .putExtra(UploadService.EXTRA_RECORDING, mRecording)
                .putExtra(UploadService.EXTRA_PROGRESS, (int) Math.min(100, Math.round(100 * current) / (float)max)));

       if (mCancelled) throw new UserCanceledException();
    }
}