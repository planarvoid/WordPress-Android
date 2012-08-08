package com.soundcloud.android.service.upload;


import static com.soundcloud.android.service.upload.UploadService.TAG;

import com.soundcloud.android.jni.EncoderOptions;
import com.soundcloud.android.jni.ProgressListener;
import com.soundcloud.android.jni.VorbisEncoder;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.IOUtils;

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
    private long mLastProgressSent;

    public Encoder(Context context, Recording recording) {
        mRecording = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mBroadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
    }

    @Override
    public void run() {
        Log.d(TAG, "Encoder.run(" + mRecording + ")");

        final File in  = mRecording.getFile();
        final File out = mRecording.getPlaybackStream().isFiltered() ?
                mRecording.getProcessedFile() : mRecording.getEncodedFile();

        EncoderOptions options = new EncoderOptions(EncoderOptions.DEFAULT.quality,
                mRecording.getPlaybackStream().getStartPos(),
                mRecording.getPlaybackStream().getEndPos(),
                this,
                mRecording.getPlaybackStream().getPlaybackFilter());

        File tmp = null;
        try {
            tmp = File.createTempFile("encoder-"+mRecording.id, ".ogg", out.getParentFile());
            broadcast(UploadService.PROCESSING_STARTED);
            long now = System.currentTimeMillis();
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG,"encoding from source " + in.getAbsolutePath());

            VorbisEncoder.encodeWav(in, tmp, options);
            // double check file output
            if (tmp.exists() && tmp.length() > 0) {
                Log.d(TAG, "encoding finished in " + (System.currentTimeMillis() - now) + " msecs");
                if (tmp.renameTo(out)) {
                    broadcast(UploadService.PROCESSING_SUCCESS);
                } else {
                    Log.w(TAG, "could not rename "+tmp+" to "+out);
                    mRecording.setUploadException(null);
                    broadcast(UploadService.PROCESSING_ERROR);
                }
            } else {
                Log.w(TAG, "encoded file "+tmp+" does not exist or is empty");
                mRecording.setUploadException(null);
                broadcast(UploadService.PROCESSING_ERROR);
            }
        } catch (UserCanceledException e) {
            mRecording.setUploadException(e);
            broadcast(UploadService.PROCESSING_CANCELED);
        } catch (IOException e) {
            mRecording.setUploadException(e);
            broadcast(UploadService.PROCESSING_ERROR);
        } finally {
            IOUtils.deleteFile(tmp);
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
            Log.d(TAG, "Encoder#onProgress(" + current + ", " + max + ")");
        }
        if (mCancelled) throw new UserCanceledException();

        if (mLastProgressSent == 0 || System.currentTimeMillis() - mLastProgressSent > 1000) {
            final int percent = (int) Math.min(100, Math.round(100 * (current / (double) max)));
            mBroadcastManager.sendBroadcast(new Intent(UploadService.PROCESSING_PROGRESS)
                    .putExtra(UploadService.EXTRA_RECORDING, mRecording)
                    .putExtra(UploadService.EXTRA_PROGRESS, percent));

            mLastProgressSent = System.currentTimeMillis();
        }
    }
}