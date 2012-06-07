package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

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
        try {
            final File in = mRecording.getFile();
            broadcast(UploadService.PROCESSING_STARTED);
            VorbisEncoder.encodeWav(in, mRecording.getEncodedFile(), AudioConfig.DEFAULT.quality, this);
            /*
            progress testing
            for (int i = 0; i < 100; i++) {
                mBroadcastManager.sendBroadcast(new Intent(UploadService.ENCODING_PROGRESS)
                                                .putExtra(UploadService.EXTRA_RECORDING, mRecording)
                                                .putExtra(UploadService.EXTRA_PROGRESS, i)
                                                .putExtra(UploadService.EXTRA_TOTAL, 100));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
            }
            }*/

            // double check
            if (mRecording.getEncodedFile().exists()) {
                broadcast(UploadService.PROCESSING_SUCCESS);
            } else {
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
        mBroadcastManager.sendBroadcast(new Intent(UploadService.EXTRA_PROGRESS)
                .putExtra(UploadService.EXTRA_RECORDING, mRecording)
                .putExtra(UploadService.EXTRA_PROGRESS, (int) Math.min(100, Math.round(100 * current) / (float)max)));

       if (mCancelled) throw new UserCanceledException();
    }
}