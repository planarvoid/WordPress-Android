package com.soundcloud.android.creators.upload;


import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.creators.record.jni.EncoderOptions;
import com.soundcloud.android.creators.record.jni.ProgressListener;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.IOUtils;
import rx.Subscription;

import android.util.Log;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Encoder implements Runnable, ProgressListener {
    private final Recording recording;
    private volatile boolean cancelled;
    private long lastProgressSent;
    private final Subscription subscription;
    private final EventBus eventBus;

    public Encoder(Recording recording, EventBus eventBus) {
        this.recording = recording;
        this.eventBus = eventBus;
        SoundCloudApplication.getObjectGraph().inject(this);
        subscription = this.eventBus.subscribe(EventQueue.UPLOAD, new EventSubscriber());
    }

    @Override
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
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

            eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingStarted(recording));

            long now = System.currentTimeMillis();
            VorbisEncoder.encodeFile(in, tmp, options);

            // double check file output
            if (tmp.exists() && tmp.length() > 0) {
                Log.d(TAG, "encoding finished in " + (System.currentTimeMillis() - now) + " msecs");
                if (tmp.renameTo(out)) {
                    eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingSuccess(recording));
                } else {
                    Log.w(TAG, "could not rename " + tmp + " to " + out);
                    eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
                }
            } else {
                Log.w(TAG, "encoded file " + tmp + " does not exist or is empty");
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
            }
        } catch (UserCanceledException e) {
            Log.w(TAG, "user cancelled encoding", e);
        } catch (IOException e) {
            Log.w(TAG, "error encoding file", e);
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
        } finally {
            IOUtils.deleteFile(tmp);
            subscription.unsubscribe();
        }
    }

    private void cancel() {
        cancelled = true;
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
            eventBus.publish(EventQueue.UPLOAD, UploadEvent.processingProgress(recording, percent));
            lastProgressSent = System.currentTimeMillis();
        }
    }

    private final class EventSubscriber extends DefaultSubscriber<UploadEvent> {
        @Override
        public void onNext(UploadEvent uploadEvent) {
            if (uploadEvent.isCancelled()) {
                if (recording.getId() == uploadEvent.getRecording().getId()) {
                    Log.d(TAG, "canceling encoding of " + recording);
                    cancel();
                }
            }
        }
    }
}
