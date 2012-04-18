package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.jni.VorbisEncoder;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.record.AudioConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Encoder extends BroadcastReceiver implements Runnable {
    private final Recording mRecording;
    private final File mOut;

    private LocalBroadcastManager mBroadcastManager;

    public Encoder(Context context, Recording recording, File out) {
        this.mRecording = recording;
        this.mOut = out;
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mBroadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
    }

    @Override
    public void run() {
        try {
            final File in = mRecording.audio_path;
            broadcast(UploadService.ENCODING_STARTED);
            VorbisEncoder.encodeWav(in, mOut, AudioConfig.DEFAULT.quality);
            broadcast(UploadService.ENCODING_SUCCESS);
        } catch (IOException e) {
            broadcast(UploadService.ENCODING_ERROR);
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
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, mRecording));
    }
}