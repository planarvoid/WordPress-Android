
package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.preferences.DevSettings;
import com.soundcloud.android.creators.record.filter.FadeFilter;
import com.soundcloud.android.playback.service.managers.AudioManagerFactory;
import com.soundcloud.android.playback.service.managers.IAudioManager;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.tracking.Event;
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
    private final IAudioManager mAudioFocusManager;
    private final RecordingStorage mRecordingStorage = new RecordingStorage();


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

    private final Context mContext;
    private RecordAppWidgetProvider mAppWidgetProvider = RecordAppWidgetProvider.getInstance();

    private volatile @NotNull State mState;
    private final AudioRecord mAudioRecord;

    private final ScAudioTrack mAudioTrack;
    private final RemainingTimeCalculator mRemainingTimeCalculator;
    private final int valuesPerSecond;

    private @Nullable Recording mRecording;

    private @NotNull RecordStream mRecordStream;
    private @Nullable PlaybackStream mPlaybackStream;
    private PlayerThread mPlaybackThread;
    /*package*/ @Nullable ReaderThread mReaderThread;
    final private AudioConfig mConfig;

    final private ByteBuffer mRecBuffer;
    final private int mRecBufferReadSize;

    final private ByteBuffer mPlayBuffer;
    final private int mPlayBufferReadSize;

    private boolean mShouldUseNotifications = true;

    private long mSeekToPos = -1;
    private long mRemainingTime = -1;

    private final LocalBroadcastManager mBroadcastManager;

    public static synchronized SoundRecorder getInstance(Context context) {
        if (instance == null) {
            // this must be tied to the application context so it can be kept alive by the service
            instance = new SoundRecorder(context.getApplicationContext(), AudioConfig.detect());
        }
        return instance;
    }
    protected SoundRecorder(Context context, AudioConfig config) {
        mContext = context;
        mConfig = config;
        mState = State.IDLE;
        mAudioRecord = config.createAudioRecord();
        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override public void onMarkerReached(AudioRecord audioRecord) { }
            @Override public void onPeriodicNotification(AudioRecord audioRecord) {
                if (mState == State.RECORDING) {
                    mRemainingTime = mRemainingTimeCalculator.timeRemaining();
                    mBroadcastManager.sendBroadcast(new Intent(RECORD_PROGRESS)
                            .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime())
                            .putExtra(EXTRA_DURATION, getPlaybackDuration())
                            .putExtra(EXTRA_TIME_REMAINING, mRemainingTime));
                }
            }
        });

        final int playbackBufferSize = config.getPlaybackMinBufferSize();
        mAudioTrack = config.createAudioTrack(playbackBufferSize);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override public void onMarkerReached(AudioTrack track) {
            }
            @Override public void onPeriodicNotification(AudioTrack track) {
                if (mState == State.PLAYING) {
                    sendPlaybackProgress();
                }
            }
        });
        mAudioTrack.setPositionNotificationPeriod(mConfig.sampleRate / 60);
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mRemainingTimeCalculator = config.createCalculator();

        valuesPerSecond = (int) (PIXELS_PER_SECOND * context.getResources().getDisplayMetrics().density);
        mRecBufferReadSize = (int) config.validBytePosition((long) (mConfig.bytesPerSecond / valuesPerSecond));
        mRecBuffer = BufferUtils.allocateAudioBuffer(mRecBufferReadSize);

        // small reads for performance, but no larger than our audiotrack buffer size
        mPlayBufferReadSize = playbackBufferSize < MAX_PLAYBACK_READ_SIZE ? playbackBufferSize : MAX_PLAYBACK_READ_SIZE;
        mPlayBuffer = BufferUtils.allocateAudioBuffer(mPlayBufferReadSize);

        mAudioFocusManager = AudioManagerFactory.createAudioManager(context);

        mRecordStream = new RecordStream(mConfig);
        reset();
    }

    private void sendPlaybackProgress() {
        mBroadcastManager.sendBroadcast(new Intent(PLAYBACK_PROGRESS)
                .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(EXTRA_DURATION, getPlaybackDuration()));
    }

    public void reset(){
        reset(false);
    }

    public void reset(boolean deleteRecording){
        if (isRecording()) stopRecording();
        if (isPlaying())   stopPlayback();
        mState = mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED ? State.ERROR : State.IDLE;

        mRecordStream.reset();
        if (mPlaybackStream != null) {
            mPlaybackStream.close();
            mPlaybackStream = null;
        }

        if (mRecording != null) {
            if (deleteRecording) mRecordingStorage.delete(mRecording);
            mRecording = null;
        }
    }

    public RecordStream getRecordStream() {
        return mRecordStream;
    }

    public void setRecording(Recording recording) {
        if (mRecording == null || recording.getId() != mRecording.getId()) {

            if (isActive()) reset();
            mRecording = recording;
            mRecordStream = new RecordStream(mConfig,
                    recording.getRawFile(),
                    shouldEncodeWhileRecording() ? recording.getEncodedFile() : null,
                    mRecording.getAmplitudeFile());

            if (!mRecordStream.hasValidAmplitudeData()) {
                mState = State.GENERATING_WAVEFORM;
                mRecordStream.regenerateAmplitudeDataAsync(mRecording.getAmplitudeFile(), this);
            }

            mPlaybackStream = recording.getPlaybackStream();
        }
    }

    public boolean isGeneratingWaveform() {
        return mState.isGeneratingWaveform();
    }

    @Override
    public void onGenerationFinished(boolean success) {
        // we might have been reset, so make sure we are still waiting
        if (mState == State.GENERATING_WAVEFORM){
            mState = State.IDLE;
            broadcast(WAVEFORM_GENERATED);
        }
    }

    public boolean isActive() {
        return mState.isActive();
    }

    public boolean isRecording() {
        return mState.isRecording();
    }

    public State startReading() {
        if (mState == State.IDLE) {
            startReadingInternal(State.READING);
        }
        return mState;
    }

    // Sets output file path, call directly after construction/reset.
    public @NotNull Recording startRecording(@Nullable String tip_key) throws IOException {
        if (!IOUtils.isSDCardAvailable()) {
            throw new IOException(mContext.getString(R.string.record_insert_sd_card));
        } else if (!mRemainingTimeCalculator.isDiskSpaceAvailable()) {
            throw new IOException(mContext.getString(R.string.record_storage_is_full));
        }

        mRemainingTimeCalculator.reset();
        if (mState != State.RECORDING) {
            if (mRecording == null) {
                mRecording = Recording.create(tip_key);

                mRecordStream.setWriters(mRecording.getRawFile(),
                        shouldEncodeWhileRecording() ? mRecording.getEncodedFile() : null);
            } else {
                // truncate if we are appending
                if (mPlaybackStream != null) {
                    try {
                        if (mPlaybackStream.getTrimRight() > 0) {
                            mRecordStream.truncate(mPlaybackStream.getEndPos(), valuesPerSecond);
                            mPlaybackStream.setTrim(mPlaybackStream.getStartPos(), mPlaybackStream.getTotalDuration());
                            mPlaybackStream.reopen();
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "error setting position");
                    }
                }
            }

            if (shouldEncodeWhileRecording()) mRemainingTimeCalculator.setEncodedFile(mRecording.getEncodedFile());
            mRemainingTime = mRemainingTimeCalculator.timeRemaining();

            // the service will ensure the recording lifecycle and notifications
            mContext.startService(new Intent(mContext, SoundRecorderService.class).setAction(RECORD_STARTED));

            startReadingInternal(State.RECORDING);

            broadcast(RECORD_STARTED);

            assert mRecording != null;
            return mRecording;
        } else throw new IllegalStateException("cannot record to file, in state " + mState);
    }

    public boolean hasRecording() {
        return mRecording != null;
    }

    public Recording getRecording() {
        return mRecording;
    }

    public boolean isSaved() {
        return mRecording != null && mRecording.isSaved();
    }

    public void stopReading() {
        if (mState == State.READING) mState = State.STOPPING;
    }

    public void stopRecording() {
        if (mState == State.RECORDING ||mState == State.READING) {
            mState = State.STOPPING;
        }
    }

    public void stopPlayback() {
        if (mState == State.PLAYING || mState == State.SEEKING) {
            mState = State.STOPPING;
        }
    }

    public boolean reload() {
        if (!mState.isPlaying() && mPlaybackStream != null) {
            try {
                mPlaybackStream.reopen();
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
        if (mAudioRecord != null) mAudioRecord.release();
        mAudioTrack.release();
    }

    private State startReadingInternal(State newState) {
        Log.d(TAG, "startReading("+newState+")");

        // check to see if we are already reading
        mState = newState;
        if (mReaderThread == null) {
            mReaderThread = new ReaderThread();
            mReaderThread.start();
        }
        return mState;
    }

    public long getRecordingElapsedTime() {
        return mRecordStream.getDuration();
    }

    public long getPlaybackDuration() {
        return mPlaybackStream == null ? -1 : mPlaybackStream.getDuration();
    }

    public long getCurrentPlaybackPosition() {
        return mSeekToPos != -1 ? mSeekToPos :
                mPlaybackStream != null ? mPlaybackStream.getPosition() : -1;
    }

    public void revertFile() {
        if (mPlaybackStream != null) {
            mPlaybackStream.reset();
        }
    }

    public boolean isPlaying() {
        return mState.isPlaying();
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
            mContext.startService(new Intent(mContext, SoundRecorderService.class).setAction(PLAYBACK_STARTED));
            mSeekToPos = -1;
            startPlaybackThread();
        }
    }

    public void seekTo(float pct) {
        if (mPlaybackStream != null) {
            final long position = (long) (getPlaybackDuration() * pct);
            final long absPosition = position + mPlaybackStream.getStartPos();
            if ((isPlaying() || mState.isTrimming()) && position >= 0) {
                mSeekToPos = absPosition;
                mState = State.SEEKING;
            } else {
                mPlaybackStream.setCurrentPosition(absPosition);
                sendPlaybackProgress();
            }
        }
    }


    public void onNewStartPosition(float newPos, long moveTime) {
        if (mPlaybackStream != null) {
            previewTrim(mPlaybackStream.setStartPositionByPercent(newPos, moveTime));
        }
    }

    public void onNewEndPosition(float newPos, long moveTime) {
        if (mPlaybackStream != null) {
            previewTrim(mPlaybackStream.setEndPositionByPercent(newPos, moveTime));
        }
    }

    private void previewTrim(TrimPreview trimPreview) {
        final boolean startThread = !(isPlaying() || mState.isTrimming());

        if (startThread) {
            mState = State.TRIMMING; //keep both state setters to avoid race condition in tests
            startPlaybackThread(trimPreview);
        } else {
            mPlaybackThread.addPreview(trimPreview);
            if (isPlaying()) broadcast(PLAYBACK_STOPPED);
            mState = State.TRIMMING;
        }
    }

    private void startPlaybackThread() {
        mPlaybackThread = new PlayerThread();
        mPlaybackThread.start();
    }

    private void startPlaybackThread(TrimPreview preview) {
        mPlaybackThread = new PlayerThread(preview);
        mPlaybackThread.start();
    }

    /***
     * @return the remaining recording time, in seconds
     */
    public long timeRemaining() {
        return mRemainingTimeCalculator.timeRemaining();
    }

    public float[] getTrimWindow(){
        if (mPlaybackStream == null){
            return EMPTY_TRIM_WINDOW;
        } else {
            return mPlaybackStream.getTrimWindow();
        }

    }

    public @Nullable Recording saveState() {

        if (mRecording != null) {

            if (shouldEncodeWhileRecording()){
                final long trimmed = Recording.trimWaveFiles(RECORD_DIR, mRecording);
                if (trimmed > 0) Log.i(TAG,"Trimmed " + trimmed + " bytes of wav data");
            }

            mRecording.setPlaybackStream(mPlaybackStream);

            mRecordingStorage.createFromBaseValues(mRecording);

            final Uri uri = mRecording.toUri();
            if (uri != null) {
                mRecording.setId(Long.parseLong(uri.getLastPathSegment()));
                return mRecording;
            }
        }
        return null;

    }

    // Used by the service to determine whether to show notifications or not
    // this is stored here because of the Recorder's lifecycle.
    public void shouldUseNotifications(boolean b) {
        if (mShouldUseNotifications != b){
            mShouldUseNotifications = b;
            broadcast(NOTIFICATION_STATE);
        }
    }

    public boolean toggleFade() {
        if (mPlaybackStream != null){
            final boolean enabled = !mPlaybackStream.isFading();
            mPlaybackStream.setFading(enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleOptimize() {
        if (mPlaybackStream != null) {
            final boolean enabled = !mPlaybackStream.isOptimized();
            mPlaybackStream.setOptimize(enabled);
            return enabled;
        }
        return false;
    }

    public boolean isOptimized() {
        return mPlaybackStream != null && mPlaybackStream.isOptimized();
    }

    public boolean isFading() {
        return mPlaybackStream != null && mPlaybackStream.isFading();
    }

    /* package, for testing */ void setPlaybackStream(PlaybackStream stream) {
        mPlaybackStream = stream;
    }

    private void broadcast(String action) {
        final Intent intent = new Intent(action)
                .putExtra(EXTRA_SHOULD_NOTIFY, mShouldUseNotifications)
                .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(EXTRA_DURATION, getPlaybackDuration())
                .putExtra(EXTRA_STATE, mState.name())
                .putExtra(EXTRA_TIME_REMAINING, mRemainingTime)
                .putExtra(EXTRA_RECORDING, mRecording);

        mBroadcastManager.sendBroadcast(intent);
        mAppWidgetProvider.notifyChange(mContext, intent);
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
            mAudioTrack.setPlaybackRate(mConfig.sampleRate);
            playbackStream.initializePlayback();
            mState = SoundRecorder.State.PLAYING;
            broadcast(PLAYBACK_STARTED);

            int n;
            while (!isInterrupted() && mState == SoundRecorder.State.PLAYING
                                    && (n = playbackStream.readForPlayback(mPlayBuffer, mPlayBufferReadSize)) > -1) {
                int written = mAudioTrack.write(mPlayBuffer, n);
                if (written < 0) onWriteError(written);
                mPlayBuffer.clear();
            }
        }

        private void playTrimPreviews(PlaybackStream playbackStream) throws IOException {
            TrimPreview preview;
            while ((preview = previewQueue.poll()) != null) {
                final FadeFilter fadeFilter = preview.getFadeFilter();
                final int byteRange = (int) preview.getByteRange(mConfig);
                playbackStream.initializePlayback(preview.lowPos(mConfig));

                int read = 0;
                int lastRead;
                byte[] readBuff = new byte[byteRange];
                // read in the whole preview
                while (read < byteRange && (lastRead = playbackStream.read(mPlayBuffer, Math.min(mPlayBufferReadSize,byteRange - read))) > 0) {
                    final int size = Math.min(lastRead, byteRange - read);
                    fadeFilter.apply(mPlayBuffer, read, byteRange); // fade out to avoid distortion when chaining samples
                    mPlayBuffer.get(readBuff, read, size);
                    read += lastRead;
                    mPlayBuffer.clear();
                }

                // try to get the speed close to the actual speed of the swipe movement
                mAudioTrack.setPlaybackRate(preview.playbackRate);
                if (preview.isReverse()) {
                    for (int i = (byteRange / mConfig.sampleSize) - 1; i >= 0; i--) {
                        int written = mAudioTrack.write(readBuff, i * mConfig.sampleSize, mConfig.sampleSize);
                        if (written < 0) onWriteError(written);
                    }
                } else {
                    for (int i = 0; i < byteRange / mConfig.sampleSize; i++) {
                        int written = mAudioTrack.write(readBuff, i * mConfig.sampleSize, mConfig.sampleSize);
                        if (written < 0) onWriteError(written);
                    }
                }
                mPlayBuffer.clear();
            }
        }

        private void onWriteError(int written) {
            Log.e(TAG, "AudioTrack#write() returned " +
                    (written == AudioTrack.ERROR_INVALID_OPERATION ? "ERROR_INVALID_OPERATION" :
                            written == AudioTrack.ERROR_BAD_VALUE ? "ERROR_BAD_VALUE" : "error " + written));

            mState = SoundRecorder.State.ERROR;
        }

        public void run() {
            synchronized (mAudioRecord) {
                if (!mAudioFocusManager.requestMusicFocus(SoundRecorder.this, IAudioManager.FOCUS_GAIN)) {
                    Log.e(TAG, "could not obtain audio focus");
                    broadcast(PLAYBACK_ERROR);
                    return;
                }

                mAudioTrack.play();
                // XXX disentangle this
                try {
                    do {
                        switch (mState) {
                            case TRIMMING:
                                playTrimPreviews(mPlaybackStream);
                                break;
                            case SEEKING:
                                if (mPlaybackStream == null) break;
                                mPlaybackStream.setCurrentPosition(mSeekToPos);
                                mSeekToPos = -1;

                            //noinspection fallthrough
                            default:
                                if (mPlaybackStream == null) break;
                                playLoop(mPlaybackStream);
                        }
                    } while (!isInterrupted() && mState == SoundRecorder.State.SEEKING ||
                            (mState == SoundRecorder.State.TRIMMING && !previewQueue.isEmpty()));

                    if (mState == SoundRecorder.State.TRIMMING) mState = SoundRecorder.State.IDLE;

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = SoundRecorder.State.ERROR;

                } finally {
                    // TODO, close on destroy, mPlaybackStream.close();
                    mAudioTrack.stop();
                    mAudioFocusManager.abandonMusicFocus(false);
                }

                //noinspection ObjectEquality
                if (this == mPlaybackThread && mPlaybackStream != null) {

                    if (mState != SoundRecorder.State.IDLE) {
                        if (mState == SoundRecorder.State.PLAYING && mPlaybackStream.isFinished()) {
                            mPlaybackStream.resetPlayback();
                            broadcast(PLAYBACK_COMPLETE);
                        } else if (mState == SoundRecorder.State.STOPPING) {
                            broadcast(PLAYBACK_STOPPED);
                        }

                        if (mState != SoundRecorder.State.RECORDING) mState = SoundRecorder.State.IDLE;
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
            synchronized (mAudioRecord) {
                Log.d(TAG, "starting reader thread: state="+mState);

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "audiorecorder is not initialized");
                    mState = SoundRecorder.State.ERROR;
                    broadcast(RECORD_ERROR);
                    return;
                }

                mAudioRecord.startRecording();
                mAudioRecord.setPositionNotificationPeriod(mConfig.sampleRate);
                while (mState == SoundRecorder.State.READING || mState == SoundRecorder.State.RECORDING) {
                    mRecBuffer.rewind();
                    final int read = mAudioRecord.read(mRecBuffer, mRecBufferReadSize);
                    if (read < 0) {
                        Log.w(TAG, "AudioRecord.read() returned error: " + read);
                        mState = SoundRecorder.State.ERROR;
                    } else if (read == 0) {
                        Log.w(TAG, "AudioRecord.read() returned no data");
                    } else if (mState == SoundRecorder.State.RECORDING &&
                               mRemainingTime <= 0) {
                        Log.w(TAG, "No more recording time, stopping");
                        mState = SoundRecorder.State.STOPPING;
                    } else {
                        try {
                            mRecBuffer.limit(read);
                            final int written = mRecordStream.write(mRecBuffer, read);
                            if (written >= 0 && written < read) {
                                Log.w(TAG, "partial write "+written);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                            mState = SoundRecorder.State.ERROR;
                            break;
                        }

                        Intent intent = new Intent(RECORD_SAMPLE)
                                .putExtra(EXTRA_AMPLITUDE, mRecordStream.getLastAmplitude())
                                .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime());

                        mBroadcastManager.sendBroadcast(intent);
                    }
                }
                Log.d(TAG, "exiting reader loop, stopping recording (mState=" + mState + ")");
                mAudioRecord.stop();
                String message = null;
                if (mRecording != null) {
                    if (mState != SoundRecorder.State.ERROR) {
                        try {
                            mRecordStream.finalizeStream(mRecording.getAmplitudeFile());
                            if (mPlaybackStream == null) {
                                mPlaybackStream = new PlaybackStream(mRecordStream.getAudioReader());
                            } else {
                                mPlaybackStream.reopen();
                                mPlaybackStream.resetBounds();
                            }
                            saveState();
                            message = RECORD_FINISHED;
                        } catch (IOException e) {
                            mState = SoundRecorder.State.ERROR;
                            message = RECORD_ERROR;
                            Log.w(TAG,e);
                        }

                    } else {
                        mPlaybackStream = null;
                        message = RECORD_ERROR;
                    }
                }
                mState = SoundRecorder.State.IDLE;
                mReaderThread = null;
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
                !DevSettings.DEV_RECORDING_TYPE_RAW.equals(PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(DevSettings.DEV_RECORDING_TYPE, null));
    }

    public static boolean hasFPUSupport() {
        return !"armeabi".equals(Build.CPU_ABI);
    }

    public void track(Event event, Object... args) {
        SoundCloudApplication.fromContext(mContext).track(event, args);
    }

    public void track(Class<?> klazz, Object... args) {
        SoundCloudApplication.fromContext(mContext).track(klazz, args);
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
