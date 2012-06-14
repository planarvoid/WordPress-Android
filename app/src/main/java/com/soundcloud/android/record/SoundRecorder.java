
package com.soundcloud.android.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.ScAudioTrack;
import com.soundcloud.android.audio.TrimPreview;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.playback.AudioManagerFactory;
import com.soundcloud.android.service.playback.IAudioManager;
import com.soundcloud.android.service.record.SoundRecorderService;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SoundRecorder implements IAudioManager.MusicFocusable {
    /* package */ static final String TAG = SoundRecorder.class.getSimpleName();

    public static final int PIXELS_PER_SECOND = 30;

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

    public static final String EXTRA_SHOULD_NOTIFY = "shouldUseNotifications";
    public static final String EXTRA_POSITION    = "position";
    public static final String EXTRA_STATE       = "state";
    public static final String EXTRA_AMPLITUDE   = "amplitude";
    public static final String EXTRA_ELAPSEDTIME = "elapsedTime";
    public static final String EXTRA_DURATION    = "duration";
    public static final String EXTRA_RECORDING   = "recording";

    public static final String[] ALL_ACTIONS = {
      NOTIFICATION_STATE, RECORD_STARTED, RECORD_ERROR, RECORD_SAMPLE, RECORD_PROGRESS, RECORD_FINISHED,
      PLAYBACK_STARTED, PLAYBACK_STOPPED, PLAYBACK_COMPLETE, PLAYBACK_PROGRESS, PLAYBACK_PROGRESS
    };
    public static int MAX_PLAYBACK_RATE = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM);


    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING, PLAYING, SEEKING, TRIMMING;

        public static final EnumSet<State> ACTIVE = EnumSet.of(RECORDING, PLAYING, SEEKING, TRIMMING);
        public static final EnumSet<State> PLAYBACK = EnumSet.of(PLAYING, SEEKING, TRIMMING);

        public boolean isActive() { return ACTIVE.contains(this); }
        public boolean isPlaying() { return PLAYBACK.contains(this); }
        public boolean isRecording() { return this == RECORDING; }
    }

    private Context mContext;
    private volatile State mState;

    public AmplitudeData amplitudeData;

    private final AudioRecord mAudioRecord;
    private final ScAudioTrack mAudioTrack;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private final AmplitudeAnalyzer mAmplitudeAnalyzer;

    private @Nullable Recording mRecording;
    private @Nullable RecordStream mRecordStream;
    private @Nullable PlaybackStream mPlaybackStream;
    private PlayerThread mPlaybackThread;
    /*package*/ @Nullable ReaderThread mReaderThread;

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;
    final private int bufferReadSize;

    private IAudioManager mAudioManager;

    private boolean mShouldUseNotifications = true;

    private long mSeekToPos = -1;

    private LocalBroadcastManager mBroadcastManager;

    public static synchronized SoundRecorder getInstance(Context context) {
        if (instance == null) {
            // this must be tied to the application context so it can be kept alive by the service
            instance = new SoundRecorder(context.getApplicationContext(), AudioConfig.DEFAULT);
        }
        return instance;
    }

    SoundRecorder(Context context, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize();
        mContext = context;
        mConfig = config;
        mAudioRecord = config.createAudioRecord(bufferSize * 4);
        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override public void onMarkerReached(AudioRecord audioRecord) { }
            @Override public void onPeriodicNotification(AudioRecord audioRecord) {
                if (mState == State.RECORDING) {
                    mBroadcastManager.sendBroadcast(new Intent(RECORD_PROGRESS)
                            .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime())
                            .putExtra(EXTRA_DURATION, getPlaybackDuration()));
                }
            }
        });

        mAudioTrack = config.createAudioTrack(bufferSize);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override public void onMarkerReached(AudioTrack track) {
            }
            @Override public void onPeriodicNotification(AudioTrack track) {
                if (mState == State.PLAYING) {
                    mBroadcastManager.sendBroadcast(new Intent(PLAYBACK_PROGRESS)
                            .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                            .putExtra(EXTRA_DURATION, getPlaybackDuration()));
                }
            }
        });
        mAudioTrack.setPositionNotificationPeriod(mConfig.sampleRate / 60);
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        mRemainingTimeCalculator = config.createCalculator();

        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        bufferReadSize =  (int) config.validBytePosition((long) (mConfig.bytesPerSecond / (PIXELS_PER_SECOND * context.getResources().getDisplayMetrics().density)));
        mAmplitudeAnalyzer = new AmplitudeAnalyzer(config);
        amplitudeData = new AmplitudeData();
        mAudioManager = AudioManagerFactory.createAudioManager(context);
        reset();
    }

    public void reset(){
        reset(false);
    }

    public void reset(boolean deleteRecording){
        if (isRecording()) stopRecording();
        if (isPlaying())   stopPlayback();
        mState = mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED ? State.ERROR : State.IDLE;
        amplitudeData.clear();

        if (mRecording != null) {
            if (deleteRecording) mRecording.delete(mContext.getContentResolver());
            mRecording = null;
        }
        if (mRecordStream != null) {
            mRecordStream.release();
            mRecordStream = null;
        }
        if (mPlaybackStream != null) {
            mPlaybackStream.close();
            mPlaybackStream = null;
        }
    }

    public void deleteRecording() {
        if (mRecording != null && !mRecording.isSaved()) {
            mRecording.delete(mContext.getContentResolver());
            mRecording = null;
        }
    }

    public void setRecording(Recording recording) {
        mRecording = recording;
        mRecordStream = new RecordStream(recording.getFile(), recording.getEncodedFile(), mConfig);
        mPlaybackStream = recording.getPlaybackStream();
        try {
            amplitudeData = AmplitudeData.fromFile(mRecording.getAmplitudeFile());
        } catch (IOException e) {
            amplitudeData.clear();
            Log.w(TAG, "error reading amplitude data", e);
        }
    }

    public boolean isActive() {
        return mState != null && mState.isActive();
    }

    public boolean isRecording() {
        return mState != null && mState.isRecording();
    }

    public State startReading() {
        if (mState == State.IDLE) {
            startReadingInternal(State.READING);
        }
        return mState;
    }

    // Sets output file path, call directly after construction/reset.
    public @NotNull Recording startRecording(@Nullable User user) throws IOException {
        if (!IOUtils.isSDCardAvailable()) {
            throw new IOException(mContext.getString(R.string.record_insert_sd_card));
        } else if (!mRemainingTimeCalculator.isDiskSpaceAvailable()) {
            throw new IOException(mContext.getString(R.string.record_storage_is_full));
        }

        // mute any playback during recording
        if (!mAudioManager.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            throw new IOException("Could not obtain music focus");
        }

        mRemainingTimeCalculator.reset();

        if (mState != State.RECORDING) {
            if (mRecording == null) {
                mRecording = Recording.create(user);

                if (mRecordStream != null) {
                    mRecordStream.release();
                    mRecordStream = null;
                }

                mRecordStream = new RecordStream(
                        mRecording.getFile(),
                        mRecording.getEncodedFile(), /* pass in null for no encoding */
                        mConfig
                );
            }

            // the service will ensure the recording lifecycle and notifications
            mContext.startService(new Intent(mContext, SoundRecorderService.class).setAction(RECORD_STARTED));

            startReadingInternal(State.RECORDING);

            broadcast(RECORD_STARTED);
            assert mRecording != null;
            return mRecording;
        } else throw new IllegalStateException("cannot record to file, in state " + mState);
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

    public void reload() {
        if (!mState.isPlaying() && mPlaybackStream != null) {
            mPlaybackStream.reopen();
        }
    }

    public void onDestroy() {
        stopPlayback();
        stopRecording();
        //release();
    }

    public int getWriteIndex() {
        return amplitudeData.writeIndex;
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


    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mState == State.RECORDING && amplitudeData.writeIndex == -1) {
                amplitudeData.onWritingStarted();
            }

            final float frameAmplitude = mAmplitudeAnalyzer.frameAmplitude();
            amplitudeData.add(frameAmplitude);

            Intent intent = new Intent(RECORD_SAMPLE)
                    .putExtra(EXTRA_AMPLITUDE, frameAmplitude)
                    .putExtra(EXTRA_ELAPSEDTIME, getRecordingElapsedTime());

            mBroadcastManager.sendBroadcast(intent);
        }
    };

    public long getRecordingElapsedTime() {
        return mRecordStream == null ? -1 : mRecordStream.elapsedTime();
    }

    public long getPlaybackDuration() {
        return mPlaybackStream == null ? -1 : mPlaybackStream.getDuration();
    }

    public long getCurrentPlaybackPosition() {
        return mSeekToPos != -1 ? mSeekToPos :
                mPlaybackStream != null ? mPlaybackStream.getPosition() : -1;
    }

    public void revertFile() {
        if (mPlaybackStream != null){
            mPlaybackStream.reset();
        }
    }

    public boolean isPlaying() {
        return mState != null && (mState.isPlaying());
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
            long position = (long) (getPlaybackDuration() * pct);
            if (isPlaying() && position >= 0) {
                mSeekToPos = position;
                mState = State.SEEKING;
            } else {
                mPlaybackStream.setCurrentPosition(position);
            }
        }
    }

    public void onNewStartPosition(double newPos, long moveTime) {
        if (mPlaybackStream != null) {
            previewTrim(mPlaybackStream.setStartPositionByPercent(newPos, moveTime));
        }
    }

    public void onNewEndPosition(double newPos, long moveTime) {
        if (mPlaybackStream != null) {
            previewTrim(mPlaybackStream.setEndPositionByPercent(newPos, moveTime));
        }
    }

    private void previewTrim(TrimPreview trimPreview) {
        final boolean wasPlaying = isPlaying();
        mState = State.TRIMMING;
        if (!wasPlaying) {
            startPlaybackThread(trimPreview);
        } else {
            mPlaybackThread.addPreview(trimPreview);
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



    public long timeRemaining() {
        return mRemainingTimeCalculator.timeRemaining() + 2; // adding 2 seconds to make up for lag
    }

    public int currentLowerLimit() {
        return mRemainingTimeCalculator.currentLowerLimit();
    }

    public @Nullable Recording saveState() {
        if (mRecording != null) {
            mRecording.setPlaybackStream(mPlaybackStream);
            return SoundCloudDB.insertRecording(mContext.getContentResolver(), mRecording);
        } else {
            return null;
        }
    }

    @Override public void focusGained() {
    }

    @Override public void focusLost(boolean isTransient, boolean canDuck) {
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
        assert mPlaybackStream != null;
        final boolean enabled = !mPlaybackStream.isFading();
        mPlaybackStream.setFading(enabled);
        return enabled;
    }

    public boolean toggleOptimize() {
        assert mPlaybackStream != null;
        final boolean enabled = !mPlaybackStream.isOptimized();
        mPlaybackStream.setOptimize(enabled);
        return enabled;
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
                .putExtra(EXTRA_RECORDING, mRecording);

        mBroadcastManager.sendBroadcast(intent);
    }

    private class PlayerThread extends Thread {

        Queue<TrimPreview> previewQueue = new ConcurrentLinkedQueue<TrimPreview>();

        PlayerThread() {
            super("PlayerThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        PlayerThread(TrimPreview preview) {
            this();
            previewQueue.add(preview);
        }

        private void play(PlaybackStream playbackStream) throws IOException {

            final int bufferSize = 1024; //arbitrary small buffer. makes for more accurate progress reporting
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            mAudioTrack.setPlaybackRate(mConfig.sampleRate);

            playbackStream.initializePlayback();
            mState = SoundRecorder.State.PLAYING;
            broadcast(PLAYBACK_STARTED);

            int n;
            while (mState == SoundRecorder.State.PLAYING && (n = mPlaybackStream.readForPlayback(buffer, bufferSize)) > -1) {
                int written = mAudioTrack.write(buffer, n);
                if (written < 0) onWriteError(written);
                buffer.clear();
            }
        }

        private void previewTrim(PlaybackStream playbackStream) throws IOException {

            // TODO : proper buffer size
            final int bufferSize = 1024;

            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            while (!previewQueue.isEmpty()) {
                TrimPreview preview = previewQueue.poll();
                playbackStream.initializePlayback(preview.lowPos(mConfig));

                final int byteRange = (int) preview.getByteRange(mConfig);
                int read = 0;
                int lastRead;
                byte[] readBuff = new byte[byteRange];
                // read in the whole preview
                while (read < byteRange && (lastRead = playbackStream.read(buffer, Math.min(bufferSize,byteRange - read))) > 0) {
                    buffer.get(readBuff, read, Math.min(lastRead, byteRange - read));
                    read += lastRead;
                    buffer.clear();
                }

                // try to get the speed close to the actual speed of the swipe movement
                mAudioTrack.setPlaybackRate((int) preview.playbackRate);
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
                buffer.clear();
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
                if (!mAudioManager.requestMusicFocus(SoundRecorder.this, IAudioManager.FOCUS_GAIN)) {
                    Log.e(TAG, "could not obtain audio focus");
                    broadcast(PLAYBACK_ERROR);
                    return;
                }

                mAudioTrack.play();
                try {
                    do {
                        if (mState == SoundRecorder.State.TRIMMING) {
                            previewTrim(mPlaybackStream);
                        } else {
                            if (mState == SoundRecorder.State.SEEKING) {
                                assert mPlaybackStream != null;
                                mPlaybackStream.setCurrentPosition(mSeekToPos);
                                mSeekToPos = -1;
                            }
                            play(mPlaybackStream);
                        }
                    } while (mState == SoundRecorder.State.SEEKING || (mState == SoundRecorder.State.TRIMMING && !previewQueue.isEmpty()));

                    if (mState == SoundRecorder.State.TRIMMING) mState = SoundRecorder.State.IDLE;

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = SoundRecorder.State.ERROR;

                } finally {
                    // TODO, close on destroy, mPlaybackStream.close();
                    mAudioTrack.stop();
                    mAudioManager.abandonMusicFocus(false);
                }

                if (this == mPlaybackThread && mPlaybackStream != null){
                    if (mState != SoundRecorder.State.IDLE) {
                        if (mState == SoundRecorder.State.PLAYING && mPlaybackStream.isFinished()) {
                            mPlaybackStream.resetPlayback();
                            broadcast(PLAYBACK_COMPLETE);
                        } else if (mState == SoundRecorder.State.STOPPING) {
                            broadcast(PLAYBACK_STOPPED);
                        }
                        mState = SoundRecorder.State.IDLE;
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
                    buffer.rewind();
                    final int read = mAudioRecord.read(buffer, bufferReadSize);
                    if (read < 0) {
                        Log.w(TAG, "AudioRecord.read() returned error: " + read);
                        mState = SoundRecorder.State.ERROR;
                    } else if (read == 0) {
                        Log.w(TAG, "AudioRecord.read() returned no data");
                    } else {
                        if (mState == SoundRecorder.State.RECORDING && mRecordStream != null) {
                            try {
                                final int written = mRecordStream.write(buffer, read);
                                if (written < read) {
                                    Log.w(TAG, "partial write "+written);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                                mState = SoundRecorder.State.ERROR;
                                break;
                            }
                        }
                        mAmplitudeAnalyzer.updateCurrentMax(buffer, read);
                        refreshHandler.sendEmptyMessage(0);
                    }
                }
                Log.d(TAG, "exiting reader loop, stopping recording (mState=" + mState + ")");
                mAudioRecord.stop();
                mAudioManager.abandonMusicFocus(false);

                if (mRecordStream != null && mRecording != null) {
                    if (mState != SoundRecorder.State.ERROR) {
                        try {
                            mRecordStream.finalizeStream();
                            amplitudeData.store(mRecording.getAmplitudeFile());
                            mPlaybackStream = mRecordStream.getPlaybackStream();
                            broadcast(RECORD_FINISHED);
                        } catch (IOException e) {
                            mState = SoundRecorder.State.ERROR;
                            broadcast(RECORD_ERROR);
                            Log.w(TAG,e);
                        }

                    } else {
                        mPlaybackStream = null;
                        broadcast(RECORD_ERROR);
                    }
                }
                mState = SoundRecorder.State.IDLE;
                mReaderThread = null;
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
}
