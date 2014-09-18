package com.soundcloud.android.creators.upload;


import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.creators.record.jni.EncoderOptions;
import com.soundcloud.android.creators.record.jni.ProgressListener;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.utils.IOUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Encoder extends BroadcastReceiver implements Runnable, ProgressListener {
    private final Recording recording;
    private final LocalBroadcastManager broadcastManager;
    private volatile boolean cancelled;
    private long lastProgressSent;

    public Encoder(Context context, Recording recording) {
        this.recording = recording;
        broadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
    }

    @Override
    public void run() {
        Log.d(TAG, "Encoder.run(" + recording + ")");

        final File wav = recording.getFile();
        final File ogg = recording.getEncodedFile();

        File tmp = null;
        try {
            PlaybackStream stream = recording.getPlaybackStream();
            if (stream == null) {
                throw new IOException("No playbackstream available");
            }
            final File in;
            if (wav.exists()) {
                in = wav;
            } else if (ogg.exists()) {
                in = ogg;
            } else {
                throw new FileNotFoundException("No encoding file found");
            }

            final File out = stream.isFiltered() ?
                    recording.getProcessedFile() : recording.getEncodedFile();

            EncoderOptions options = new EncoderOptions(EncoderOptions.DEFAULT.quality,
                    stream.getStartPos(),
                    stream.getEndPos(),
                    this,
                    stream.getPlaybackFilter());

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "encoding from source " + in.getAbsolutePath());
            }
            tmp = File.createTempFile("encoder-" + recording.getId(), ".ogg", out.getParentFile());
            broadcast(UploadService.PROCESSING_STARTED);
            long now = System.currentTimeMillis();
            VorbisEncoder.encodeFile(in, tmp, options);

            // double check file output
            if (tmp.exists() && tmp.length() > 0) {
                Log.d(TAG, "encoding finished in " + (System.currentTimeMillis() - now) + " msecs");
                if (tmp.renameTo(out)) {
                    broadcast(UploadService.PROCESSING_SUCCESS);
                } else {
                    Log.w(TAG, "could not rename " + tmp + " to " + out);
                    broadcast(UploadService.PROCESSING_ERROR);
                }
            } else {
                Log.w(TAG, "encoded file " + tmp + " does not exist or is empty");
                broadcast(UploadService.PROCESSING_ERROR);
            }
        } catch (UserCanceledException e) {
            broadcast(UploadService.PROCESSING_CANCELED);
        } catch (IOException e) {
            Log.w(TAG, "error encoding file", e);
            broadcast(UploadService.PROCESSING_ERROR);
        } finally {
            IOUtils.deleteFile(tmp);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
        if (this.recording.equals(recording)) {
            Log.d(TAG, "canceling encoding of " + recording);
            cancel();
        }
    }

    private void cancel() {
        cancelled = true;
    }

    private void broadcast(String action) {
        broadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, recording));
    }

    @Override
    public void onProgress(long current, long max) throws UserCanceledException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Encoder#onProgress(" + current + ", " + max + ")");
        }
        if (cancelled) {
            throw new UserCanceledException();
        }

        if (lastProgressSent == 0 || System.currentTimeMillis() - lastProgressSent > 1000) {
            final int percent = (int) Math.min(100, Math.round(100 * (current / (double) max)));
            broadcastManager.sendBroadcast(new Intent(UploadService.PROCESSING_PROGRESS)
                    .putExtra(UploadService.EXTRA_RECORDING, recording)
                    .putExtra(UploadService.EXTRA_PROGRESS, percent));

            lastProgressSent = System.currentTimeMillis();
        }
    }
}
