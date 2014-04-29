
package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.filter.FadeFilter;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.playback.service.managers.FroyoAudioManager;
import com.soundcloud.android.playback.service.managers.IAudioManager;
import com.soundcloud.android.preferences.DevSettings;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.utils.BufferUtils;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SoundRecorder implements IAudioManager.MusicFocusable, RecordStream.onAmplitudeGenerationListener {
    /* package */ static final String TAG = SoundRecorder.class.getSimpleName();

    public static final int PIXELS_PER_SECOND = hasFPUSupport() ? 30 : 15;
    public static final int MAX_PLAYBACK_READ_SIZE = 1024;

    public static final File RECORD_DIR = IOUtils.ensureUpdatedDirectory(
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".rec"));

    private static SoundRecorder instance;
    public static final String NOTIFICATION_STATE = "com.soundcloud.android.notificationState";
    public static final String RECORD_STARTED    = "com.soundcloud.android.recordstarted";
    public static final String RECORD_SAMPLE     = "com.soundcloud.android.recordsample";
    public static final String RECORD_ERROR      = "com.soundcloud.android.recorderror";
    public static final String RECORD_PROGRESS   = "com.soundcloud.android.recordprogress";
    public static final String RECORD_FINISHED   = "com.soundcloud.android.recordfinished";
    public static final String PLAYBACK_STARTED  = "com.soundcloud.android.playbackstarted";
    public static final String PLAYBACK_STOPPED  = "com.soundcloud.android.playbackstopped";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_PROGRESS = "com.soundcloud.android.playbackprogress";
    public static final String PLAYBACK_ERROR    = "com.soundcloud.android.playbackerror";
    public static final String WAVEFORM_GENERATED    = "com.soundcloud.android.waveformgenerated";

    public static final String EXTRA_SHOULD_NOTIFY = "shouldUseNotifications";
    public static final String EXTRA_POSITION    = "position";
    public static final String EXTRA_STATE       = "state";
    public static final String EXTRA_AMPLITUDE   = "amplitude";
    public static final String EXTRA_ELAPSEDTIME = "elapsedTime";
    public static final String EXTRA_DURATION    = "duration";
    public static final String EXTRA_RECORDING   = Recording.EXTRA;
    public static final String EXTRA_TIME_REMAINING = "time_remaining";

    public static final String[] ALL_ACTIONS = {
      NOTIFICATION_STATE, RECORD_STARTED, RECORD_ERROR, RECORD_SAMPLE, RECORD_PROGRESS, RECORD_FINISHED,
      PLAYBACK_STARTED, PLAYBACK_STOPPED, PLAYBACK_COMPLETE, PLAYBACK_PROGRESS, PLAYBACK_PROGRESS, WAVEFORM_GENERATED
    };
    public static final int MAX_PLAYBACK_RATE = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM);
    private IAudioManager audioFocusManager;
    private final RecordingStorage recordingStorage = new RecordingStorage();


    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING, PLAYING, SEEKING, TRIMMING, GENERATING_WAVEFORM;

        public static final EnumSet<State> ACTIVE = EnumSet.of(RECORDING, PLAYING, SEEKING, TRIMMING);

        public static final EnumSet<State> PLAYBACK = EnumSet.of(PLAYING, SEEKING);
        public boolean isActive() { return ACTIVE.contains(this); }

        public boolean isPlaying() { return PLAYBACK.contains(this); }
        public boolean isTrimming() { return this == TRIMMING; }
        public boolean isRecording() { return this == RECORDING; }
        public boolean isGeneratingWaveform() { return this == GENERATING_WAVEFORM; }
    }

    private static float[] EMPTY_TRIM_WINDOW = new float[]{0f,1f};

    private final Context context;
    private RecordAppWidgetProvider appWidgetProvider = RecordAppWidgetProvider.getInstance();

    private volatile @NotNull State state;
    private final AudioRecord audioRecord;

    private final ScAudioTrack audioTrack;
    private final RemainingTimeCalculator remainingTimeCalculator;
    private final int valuesPerSecond;

    private @Nullable Recording recording;

    private @NotNull RecordStream recordStream;
    private @Nullable PlaybackStream playbackStream;
    private PlayerThread playbackThread;
    /*package*/ @Nullable ReaderThread readerThread;
    private final AudioConfig audioConfig;

    private final ByteBuffer recBuffer;
    private final int recBufferReadSize;

    private final ByteBuffer playBuffer;
    private final int playBufferReadSize;

    private boolean shouldUseNotifications = true;

    private long seekToPos = -1;
    private long remainingTime = -1;

    private final LocalBroadcastManager broadcastManager;

    public static synchronized SoundRecorder getInstance(final Context context) {
        if (instance == null) {
            // this must be tied to the application context so it can be kept alive by the service
            instance = new SoundRecorder(context.getApplicationContext(), AudioConfig.detect());
        }
        return instance;
    }

    protected SoundRecorder(final Context context, AudioConfig audioConfig) {
        this.context = context;
        this.audioConfig = audioConfig;

        state = State.IDLE;
        audioRecord = audioConfig.createAudioRecord();
        audioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioRecord audioRecord) {
            }

            @Override
            public void onPeriodicNotification(AudioRecord audioRecord) {
                if (state == State.RECORDING) {
                    remainingTime = remainingTimeCalculator.timeRemaining();
                    broadcastManager.sendBroadcast(new Intent(RECORD_PROGRESS)
                            .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime())
                            .putExtra(EXTRA_DURATION, getPlaybackDuration())
                            .putExtra(EXTRA_TIME_REMAINING, remainingTime));
                }
            }
        });

        final int playbackBufferSize = audioConfig.getPlaybackMinBufferSize();
        audioTrack = audioConfig.createAudioTrack(playbackBufferSize);
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                if (state == State.PLAYING) {
                    sendPlaybackProgress();
                }
            }
        });
        audioTrack.setPositionNotificationPeriod(this.audioConfig.sampleRate / 60);
        broadcastManager = LocalBroadcastManager.getInstance(context);
        remainingTimeCalculator = audioConfig.createCalculator();

        valuesPerSecond = (int) (PIXELS_PER_SECOND * context.getResources().getDisplayMetrics().density);
        recBufferReadSize = (int) audioConfig.validBytePosition((long) (this.audioConfig.bytesPerSecond / valuesPerSecond));
        recBuffer = BufferUtils.allocateAudioBuffer(recBufferReadSize);

        // small reads for performance, but no larger than our audiotrack buffer size
        playBufferReadSize = playbackBufferSize < MAX_PLAYBACK_READ_SIZE ? playbackBufferSize : MAX_PLAYBACK_READ_SIZE;
        playBuffer = BufferUtils.allocateAudioBuffer(playBufferReadSize);

        // we just need focus, which is provided by going directly to the FroyoAudioManager
        audioFocusManager = new FroyoAudioManager(context);

        recordStream = new RecordStream(this.audioConfig);
        reset();
    }

    private void sendPlaybackProgress() {
        broadcastManager.sendBroadcast(new Intent(PLAYBACK_PROGRESS)
                .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(EXTRA_DURATION, getPlaybackDuration()));
    }

    public void reset(){
        reset(false);
    }

    public void reset(boolean deleteRecording){
        if (isRecording()) stopRecording();
        if (isPlaying())   stopPlayback();
        state = audioRecord.getState() != AudioRecord.STATE_INITIALIZED ? State.ERROR : State.IDLE;

        recordStream.reset();
        if (playbackStream != null) {
            playbackStream.close();
            playbackStream = null;
        }

        if (recording != null) {
            if (deleteRecording) recordingStorage.delete(recording);
            recording = null;
        }
    }

    public RecordStream getRecordStream() {
        return recordStream;
    }

    public void setRecording(Recording recording) {
        if (this.recording == null || recording.getId() != this.recording.getId()) {

            if (isActive()) reset();
            this.recording = recording;
            recordStream = new RecordStream(audioConfig,
                    recording.getRawFile(),
                    shouldEncodeWhileRecording() ? recording.getEncodedFile() : null,
                    this.recording.getAmplitudeFile());

            if (!recordStream.hasValidAmplitudeData()) {
                state = State.GENERATING_WAVEFORM;
                recordStream.regenerateAmplitudeDataAsync(this.recording.getAmplitudeFile(), this);
            }

            playbackStream = recording.getPlaybackStream();
        }
    }

    public boolean isGeneratingWaveform() {
        return state.isGeneratingWaveform();
    }

    @Override
    public void onGenerationFinished(boolean success) {
        // we might have been reset, so make sure we are still waiting
        if (state == State.GENERATING_WAVEFORM){
            state = State.IDLE;
            broadcast(WAVEFORM_GENERATED);
        }
    }

    public boolean isActive() {
        return state.isActive();
    }

    public boolean isRecording() {
        return state.isRecording();
    }

    public State startReading() {
        if (state == State.IDLE) {
            startReadingInternal(State.READING);
        }
        return state;
    }

    // Sets output file path, call directly after construction/reset.
    public @NotNull Recording startRecording(@Nullable String tip_key) throws IOException {
        if (!IOUtils.isSDCardAvailable()) {
            throw new IOException(context.getString(R.string.record_insert_sd_card));
        } else if (!remainingTimeCalculator.isDiskSpaceAvailable()) {
            throw new IOException(context.getString(R.string.record_storage_is_full));
        }

        remainingTimeCalculator.reset();
        if (state != State.RECORDING) {
            if (recording == null) {
                recording = Recording.create(tip_key);

                recordStream.setWriters(recording.getRawFile(),
                        shouldEncodeWhileRecording() ? recording.getEncodedFile() : null);
            } else {
                // truncate if we are appending
                if (playbackStream != null) {
                    try {
                        if (playbackStream.getTrimRight() > 0) {
                            recordStream.truncate(playbackStream.getEndPos(), valuesPerSecond);
                            playbackStream.setTrim(playbackStream.getStartPos(), playbackStream.getTotalDuration());
                            playbackStream.reopen();
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "error setting position");
                    }
                }
            }

            if (shouldEncodeWhileRecording()) remainingTimeCalculator.setEncodedFile(recording.getEncodedFile());
            remainingTime = remainingTimeCalculator.timeRemaining();

            // the service will ensure the recording lifecycle and notifications
            context.startService(new Intent(context, SoundRecorderService.class).setAction(RECORD_STARTED));

            startReadingInternal(State.RECORDING);

            broadcast(RECORD_STARTED);

            assert recording != null;
            return recording;
        } else throw new IllegalStateException("cannot record to file, in state " + state);
    }

    public boolean hasRecording() {
        return recording != null;
    }

    public Recording getRecording() {
        return recording;
    }

    public boolean isSaved() {
        return recording != null && recording.isSaved();
    }

    public void stopReading() {
        if (state == State.READING) state = State.STOPPING;
    }

    public void stopRecording() {
        if (state == State.RECORDING || state == State.READING) {
            state = State.STOPPING;
        }
    }

    public void stopPlayback() {
        if (state == State.PLAYING || state == State.SEEKING) {
            state = State.STOPPING;
        }
    }

    public boolean reload() {
        if (!state.isPlaying() && playbackStream != null) {
            try {
                playbackStream.reopen();
                return true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void onDestroy() {
        stopPlayback();
        stopRecording();
        //release();
    }

    private void release() {
        if (audioRecord != null) audioRecord.release();
        audioTrack.release();
    }

    private State startReadingInternal(State newState) {
        Log.d(TAG, "startReading("+newState+")");

        // check to see if we are already reading
        state = newState;
        if (readerThread == null) {
            readerThread = new ReaderThread();
            readerThread.start();
        }
        return state;
    }

    public long getRecordingElapsedTime() {
        return recordStream.getDuration();
    }

    public long getPlaybackDuration() {
        return playbackStream == null ? -1 : playbackStream.getDuration();
    }

    public long getCurrentPlaybackPosition() {
        return seekToPos != -1 ? seekToPos :
                playbackStream != null ? playbackStream.getPosition() : -1;
    }

    public void revertFile() {
        if (playbackStream != null) {
            playbackStream.reset();
        }
    }

    public boolean isPlaying() {
        return state.isPlaying();
    }

    public void togglePlayback() {
        if (isPlaying()) {
            stopPlayback();
        } else {
            play();
        }
    }

    public void play() {
        if (!isPlaying()) {
            // the service will ensure the playback lifecycle and notifications
            context.startService(new Intent(context, SoundRecorderService.class).setAction(PLAYBACK_STARTED));
            seekToPos = -1;
            startPlaybackThread();
        }
    }

    public void seekTo(float pct) {
        if (playbackStream != null) {
            final long position = (long) (getPlaybackDuration() * pct);
            final long absPosition = position + playbackStream.getStartPos();
            if ((isPlaying() || state.isTrimming()) && position >= 0) {
                seekToPos = absPosition;
                state = State.SEEKING;
            } else {
                playbackStream.setCurrentPosition(absPosition);
                sendPlaybackProgress();
            }
        }
    }


    public void onNewStartPosition(float newPos, long moveTime) {
        if (playbackStream != null) {
            previewTrim(playbackStream.setStartPositionByPercent(newPos, moveTime));
        }
    }

    public void onNewEndPosition(float newPos, long moveTime) {
        if (playbackStream != null) {
            previewTrim(playbackStream.setEndPositionByPercent(newPos, moveTime));
        }
    }

    private void previewTrim(TrimPreview trimPreview) {
        final boolean startThread = !(isPlaying() || state.isTrimming());

        if (startThread) {
            state = State.TRIMMING; //keep both state setters to avoid race condition in tests
            startPlaybackThread(trimPreview);
        } else {
            playbackThread.addPreview(trimPreview);
            if (isPlaying()) broadcast(PLAYBACK_STOPPED);
            state = State.TRIMMING;
        }
    }

    private void startPlaybackThread() {
        playbackThread = new PlayerThread();
        playbackThread.start();
    }

    private void startPlaybackThread(TrimPreview preview) {
        playbackThread = new PlayerThread(preview);
        playbackThread.start();
    }

    /***
     * @return the remaining recording time, in seconds
     */
    public long timeRemaining() {
        return remainingTimeCalculator.timeRemaining();
    }

    public float[] getTrimWindow(){
        if (playbackStream == null){
            return EMPTY_TRIM_WINDOW;
        } else {
            return playbackStream.getTrimWindow();
        }

    }

    public @Nullable Recording saveState() {

        if (recording != null) {

            if (shouldEncodeWhileRecording()){
                final long trimmed = Recording.trimWaveFiles(RECORD_DIR, recording);
                if (trimmed > 0) Log.i(TAG,"Trimmed " + trimmed + " bytes of wav data");
            }

            recording.setPlaybackStream(playbackStream);

            recordingStorage.createFromBaseValues(recording);

            final Uri uri = recording.toUri();
            if (uri != null) {
                recording.setId(Long.parseLong(uri.getLastPathSegment()));
                return recording;
            }
        }
        return null;

    }

    // Used by the service to determine whether to show notifications or not
    // this is stored here because of the Recorder's lifecycle.
    public void shouldUseNotifications(boolean b) {
        if (shouldUseNotifications != b){
            shouldUseNotifications = b;
            broadcast(NOTIFICATION_STATE);
        }
    }

    public boolean toggleFade() {
        if (playbackStream != null){
            final boolean enabled = !playbackStream.isFading();
            playbackStream.setFading(enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleOptimize() {
        if (playbackStream != null) {
            final boolean enabled = !playbackStream.isOptimized();
            playbackStream.setOptimize(enabled);
            return enabled;
        }
        return false;
    }

    public boolean isOptimized() {
        return playbackStream != null && playbackStream.isOptimized();
    }

    public boolean isFading() {
        return playbackStream != null && playbackStream.isFading();
    }

    /* package, for testing */ void setPlaybackStream(PlaybackStream stream) {
        playbackStream = stream;
    }

    private void broadcast(String action) {
        final Intent intent = new Intent(action)
                .putExtra(EXTRA_SHOULD_NOTIFY, shouldUseNotifications)
                .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(EXTRA_DURATION, getPlaybackDuration())
                .putExtra(EXTRA_STATE, state.name())
                .putExtra(EXTRA_TIME_REMAINING, remainingTime)
                .putExtra(EXTRA_RECORDING, recording);

        broadcastManager.sendBroadcast(intent);
        appWidgetProvider.notifyChange(context, intent);
    }

    private class PlayerThread extends Thread {
        private final Queue<TrimPreview> previewQueue = new ConcurrentLinkedQueue<TrimPreview>();

        PlayerThread() {
            super("PlayerThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        PlayerThread(TrimPreview preview) {
            this();
            previewQueue.add(preview);
        }

        private void playLoop(@NotNull PlaybackStream playbackStream) throws IOException {
            audioTrack.setPlaybackRate(audioConfig.sampleRate);
            playbackStream.initializePlayback();
            state = SoundRecorder.State.PLAYING;
            broadcast(PLAYBACK_STARTED);

            int n;
            while (!isInterrupted() && state == SoundRecorder.State.PLAYING
                                    && (n = playbackStream.readForPlayback(playBuffer, playBufferReadSize)) > -1) {
                int written = audioTrack.write(playBuffer, n);
                if (written < 0) onWriteError(written);
                playBuffer.clear();
            }
        }

        private void playTrimPreviews(PlaybackStream playbackStream) throws IOException {
            TrimPreview preview;
            while ((preview = previewQueue.poll()) != null) {
                final FadeFilter fadeFilter = preview.getFadeFilter();
                final int byteRange = (int) preview.getByteRange(audioConfig);
                playbackStream.initializePlayback(preview.lowPos(audioConfig));

                int read = 0;
                int lastRead;
                byte[] readBuff = new byte[byteRange];
                // read in the whole preview
                while (read < byteRange && (lastRead = playbackStream.read(playBuffer, Math.min(playBufferReadSize,byteRange - read))) > 0) {
                    final int size = Math.min(lastRead, byteRange - read);
                    fadeFilter.apply(playBuffer, read, byteRange); // fade out to avoid distortion when chaining samples
                    playBuffer.get(readBuff, read, size);
                    read += lastRead;
                    playBuffer.clear();
                }

                // try to get the speed close to the actual speed of the swipe movement
                audioTrack.setPlaybackRate(preview.playbackRate);
                if (preview.isReverse()) {
                    for (int i = (byteRange / audioConfig.sampleSize) - 1; i >= 0; i--) {
                        int written = audioTrack.write(readBuff, i * audioConfig.sampleSize, audioConfig.sampleSize);
                        if (written < 0) onWriteError(written);
                    }
                } else {
                    for (int i = 0; i < byteRange / audioConfig.sampleSize; i++) {
                        int written = audioTrack.write(readBuff, i * audioConfig.sampleSize, audioConfig.sampleSize);
                        if (written < 0) onWriteError(written);
                    }
                }
                playBuffer.clear();
            }
        }

        private void onWriteError(int written) {
            Log.e(TAG, "AudioTrack#write() returned " +
                    (written == AudioTrack.ERROR_INVALID_OPERATION ? "ERROR_INVALID_OPERATION" :
                            written == AudioTrack.ERROR_BAD_VALUE ? "ERROR_BAD_VALUE" : "error " + written));

            state = SoundRecorder.State.ERROR;
        }

        public void run() {
            synchronized (audioRecord) {
                if (!audioFocusManager.requestMusicFocus(SoundRecorder.this, IAudioManager.FOCUS_GAIN)) {
                    Log.e(TAG, "could not obtain audio focus");
                    broadcast(PLAYBACK_ERROR);
                    return;
                }

                audioTrack.play();
                // XXX disentangle this
                try {
                    do {
                        switch (state) {
                            case TRIMMING:
                                playTrimPreviews(playbackStream);
                                break;
                            case SEEKING:
                                if (playbackStream == null) break;
                                playbackStream.setCurrentPosition(seekToPos);
                                seekToPos = -1;

                            //noinspection fallthrough
                            default:
                                if (playbackStream == null) break;
                                playLoop(playbackStream);
                        }
                    } while (!isInterrupted() && state == SoundRecorder.State.SEEKING ||
                            (state == SoundRecorder.State.TRIMMING && !previewQueue.isEmpty()));

                    if (state == SoundRecorder.State.TRIMMING) state = SoundRecorder.State.IDLE;

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    state = SoundRecorder.State.ERROR;

                } finally {
                    // TODO, close on destroy, playbackStream.close();
                    audioTrack.stop();
                    audioFocusManager.abandonMusicFocus(false);
                }

                //noinspection ObjectEquality
                if (this == playbackThread && playbackStream != null) {

                    if (state != SoundRecorder.State.IDLE) {
                        if (state == SoundRecorder.State.PLAYING && playbackStream.isFinished()) {
                            playbackStream.resetPlayback();
                            broadcast(PLAYBACK_COMPLETE);
                        } else if (state == SoundRecorder.State.STOPPING) {
                            broadcast(PLAYBACK_STOPPED);
                        }

                        if (state != SoundRecorder.State.RECORDING) state = SoundRecorder.State.IDLE;
                    }
                } else {
                    Log.d(TAG, "player loop exit: no stream available");
                }
            }
        }

        public void addPreview(TrimPreview trimPreview) {
            previewQueue.add(trimPreview);

            long currentDuration = 0;
            for (TrimPreview preview : previewQueue){
                currentDuration += preview.duration;
            }

            // try to keep up with the users scrubbing by dropping old previews
            while (currentDuration > TrimPreview.MAX_PREVIEW_DURATION && previewQueue.size() > 1){
                currentDuration -= previewQueue.poll().duration;
            }
        }
    }

    /*package*/ class ReaderThread extends Thread {
        ReaderThread() {
            super("ReaderThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            synchronized (audioRecord) {
                Log.d(TAG, "starting reader thread: state="+ state);

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "audiorecorder is not initialized");
                    state = SoundRecorder.State.ERROR;
                    broadcast(RECORD_ERROR);
                    return;
                }

                audioRecord.startRecording();
                audioRecord.setPositionNotificationPeriod(audioConfig.sampleRate);
                while (state == SoundRecorder.State.READING || state == SoundRecorder.State.RECORDING) {
                    recBuffer.rewind();
                    final int read = audioRecord.read(recBuffer, recBufferReadSize);
                    if (read < 0) {
                        Log.w(TAG, "AudioRecord.read() returned error: " + read);
                        state = SoundRecorder.State.ERROR;
                    } else if (read == 0) {
                        Log.w(TAG, "AudioRecord.read() returned no data");
                    } else if (state == SoundRecorder.State.RECORDING &&
                               remainingTime <= 0) {
                        Log.w(TAG, "No more recording time, stopping");
                        state = SoundRecorder.State.STOPPING;
                    } else {
                        try {
                            recBuffer.limit(read);
                            final int written = recordStream.write(recBuffer, read);
                            if (written >= 0 && written < read) {
                                Log.w(TAG, "partial write "+written);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                            state = SoundRecorder.State.ERROR;
                            break;
                        }

                        Intent intent = new Intent(RECORD_SAMPLE)
                                .putExtra(EXTRA_AMPLITUDE, recordStream.getLastAmplitude())
                                .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime());

                        broadcastManager.sendBroadcast(intent);
                    }
                }
                Log.d(TAG, "exiting reader loop, stopping recording (state=" + state + ")");
                audioRecord.stop();
                String message = null;
                if (recording != null) {
                    if (state != SoundRecorder.State.ERROR) {
                        try {
                            recordStream.finalizeStream(recording.getAmplitudeFile());
                            if (playbackStream == null) {
                                playbackStream = new PlaybackStream(recordStream.getAudioReader());
                            } else {
                                playbackStream.reopen();
                                playbackStream.resetBounds();
                            }
                            saveState();
                            message = RECORD_FINISHED;
                        } catch (IOException e) {
                            state = SoundRecorder.State.ERROR;
                            message = RECORD_ERROR;
                            Log.w(TAG,e);
                        }

                    } else {
                        playbackStream = null;
                        message = RECORD_ERROR;
                    }
                }
                state = SoundRecorder.State.IDLE;
                readerThread = null;
                if (!TextUtils.isEmpty(message)) broadcast(message);
            }
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        for (String action : ALL_ACTIONS) {
            filter.addAction(action);
        }
        return filter;
    }

    public boolean shouldEncodeWhileRecording() {
        return hasFPUSupport() &&
                !DevSettings.DEV_RECORDING_TYPE_RAW.equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(DevSettings.DEV_RECORDING_TYPE, null));
    }

    public static boolean hasFPUSupport() {
        return !"armeabi".equals(Build.CPU_ABI);
    }

    @Override
    public void focusGained() {
        Log.d(TAG,"Audio Focus gained ");
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        Log.d(TAG,"Focus Lost " + isTransient + " and " + canDuck);
        if (!canDuck && isActive()){
            gotoIdleState();
        }
    }

    public void gotoIdleState() {
        if (isRecording()){
            stopRecording();
        } else if (isPlaying()){
            stopPlayback();
        }
    }
}
