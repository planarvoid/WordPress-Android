
package com.soundcloud.android.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioFile;
import com.soundcloud.android.audio.ScAudioTrack;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.playback.AudioManagerFactory;
import com.soundcloud.android.service.playback.IAudioManager;
import com.soundcloud.android.service.record.SoundRecorderService;
import com.soundcloud.android.utils.IOUtils;

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

public class SoundRecorder implements IAudioManager.MusicFocusable {
    /* package */ static final String TAG = SoundRecorder.class.getSimpleName();

    public static final int PIXELS_PER_SECOND = 30;
    private static final int TRIM_PREVIEW_LENGTH = 500;


    public static final File RECORD_DIR = IOUtils.ensureUpdatedDirectory(
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings"),
                new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".rec"));

    private static SoundRecorder instance;
    public static final String NOTIFICATION_STATE    = "com.soundcloud.android.notificationState";
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

    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING, PLAYING, SEEKING;

        public static final EnumSet<State> ACTIVE = EnumSet.of(RECORDING, PLAYING, SEEKING);
        public static final EnumSet<State> PLAYBACK = EnumSet.of(PLAYING, SEEKING);

        public boolean isActive() { return ACTIVE.contains(this); }
        public boolean isPlaying() { return PLAYBACK.contains(this); }
        public boolean isRecording() { return this == RECORDING; }
    }

    private Context mContext;
    private volatile State mState;

    public final AmplitudeData amplitudeData;
    public int writeIndex;

    private final AudioRecord mAudioRecord;
    private final ScAudioTrack mAudioTrack;
    private RemainingTimeCalculator mRemainingTimeCalculator;
    private RecordStream mRecordStream;
    private Recording mRecording;
    private final AmplitudeAnalyzer mAmplitudeAnalyzer;

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;
    final private int bufferReadSize;

    private IAudioManager mAudioManager;
    private ReaderThread mReaderThread;

    private boolean mShouldUseNotifications;

    private long mCurrentPosition, mDuration, mSeekToPos = -1;

    private LocalBroadcastManager mBroadcastManager;

    public static synchronized SoundRecorder getInstance(Context context) {
        if (instance == null) {
            // this must be tied to the application context so it can be kept alive by the service
            instance = new SoundRecorder(context.getApplicationContext(), AudioConfig.DEFAULT);
        }
        return instance;
    }

    private SoundRecorder(Context context, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize();
        mContext = context;
        mConfig = config;
        mAudioRecord = config.createAudioRecord(bufferSize * 4);
        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override public void onMarkerReached(AudioRecord audioRecord) { }
            @Override public void onPeriodicNotification(AudioRecord audioRecord) {
                if (mState == State.RECORDING) {
                    mBroadcastManager.sendBroadcast(new Intent(RECORD_PROGRESS)
                            .putExtra(EXTRA_ELAPSEDTIME, getTimeElapsed())
                            .putExtra(EXTRA_DURATION, getDuration()));
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
                            .putExtra(EXTRA_DURATION, getDuration()));
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
        if (isRecording()) stopRecording();
        if (isPlaying())   stopPlayback();
        mState = mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED ? State.ERROR : State.IDLE;
        amplitudeData.clear();
        mCurrentPosition = writeIndex = -1;
        mRecording = null;
        if (mRecordStream != null) {
            mRecordStream.release();
            mRecordStream = null;
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
    public Recording startRecording(User user) throws IOException {
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
                Recording recording = Recording.create(user);
                if (mRecordStream != null && !mRecordStream.getFile().equals(recording.audio_path)) {
                    mRecordStream.release();
                    mRecordStream = null;
                }

                if (mRecordStream == null) {
                    mRecordStream = new RecordStream(
                            recording.audio_path,
                            recording.encoded_audio_path, /* pass in null for no encoding */
                            mConfig
                    );
                }
                mRecording = recording;
            }

            // the service will ensure the recording lifecycle and notifications
            mContext.startService(new Intent(mContext, SoundRecorderService.class).setAction(RECORD_STARTED));

            startReadingInternal(State.RECORDING);

            broadcast(RECORD_STARTED);
            return mRecording;
        } else throw new IllegalStateException("cannot record to file, in state " + mState);
    }

    public Recording getRecording() {
        return mRecording;
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


    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mState == State.RECORDING && writeIndex == -1) {
                writeIndex = amplitudeData.size();
            }

            final float frameAmplitude = mAmplitudeAnalyzer.frameAmplitude();
            amplitudeData.add(frameAmplitude);

            Intent intent = new Intent(RECORD_SAMPLE)
                    .putExtra(EXTRA_AMPLITUDE, frameAmplitude)
                    .putExtra(EXTRA_ELAPSEDTIME, getTimeElapsed());

            mBroadcastManager.sendBroadcast(intent);
        }
    };

    public long getDuration() {
        return mRecordStream == null ? -1 : mRecordStream.getDuration();
    }

    public long getTimeElapsed() {
        return mRecordStream == null ? -1 : mRecordStream.elapsedTime();
    }


    public long getCurrentPlaybackPosition() {
        return mSeekToPos != -1 ? mSeekToPos : mCurrentPosition == -1 ? -1 :  mCurrentPosition;
    }

    public void revertFile() {
        resetPlaybackBounds();
        mRecordStream.applyFades = false;
        //TODO reset optimize
    }

    public void applyEdits() {
        //TODO: anything? Maybe ust don't revert modification layer
    }


    public void resetPlaybackBounds() {
        if (mRecordStream != null) mRecordStream.resetPlaybackBounds();
    }

    public boolean isPlaying() {
        return mState != null && (mState == State.PLAYING || mState == State.SEEKING);
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
            new PlayerThread().start();
        }
    }

    public void seekTo(float pct) {
        long position = (long) (getDuration() * pct);
        if (isPlaying()) {
            mSeekToPos = position;
            mState = State.SEEKING;
        } else {
            mCurrentPosition = position;
        }
    }

    public void onNewStartPosition(double percent) {
        if (mRecordStream != null) {
            mRecordStream.setStartPositionByPercent(percent);
            if (mState == State.PLAYING) {
                mSeekToPos = Math.max(mRecordStream.startPosition, mRecordStream.endPosition - TRIM_PREVIEW_LENGTH);
                mState = State.SEEKING;
            }
        }
    }

    public void onNewEndPosition(double percent) {
        if (mRecordStream != null) {
            mRecordStream.setEndPositionByPercent(percent);
            if (mState == State.PLAYING) {
                mSeekToPos = Math.max(mRecordStream.startPosition, mRecordStream.endPosition - TRIM_PREVIEW_LENGTH);
                mState = State.SEEKING;
            }
        }
    }

    public long timeRemaining() {
        return mRemainingTimeCalculator.timeRemaining() + 2; // adding 2 seconds to make up for lag
    }

    public int currentLowerLimit() {
        return mRemainingTimeCalculator.currentLowerLimit();
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

    public boolean shouldUseNotifications() {
        return mShouldUseNotifications;
    }

    private void broadcast(String action) {
        final Intent intent = new Intent(action)
                .putExtra(EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(EXTRA_DURATION, getDuration())
                .putExtra(EXTRA_STATE, mState.name())
                .putExtra(EXTRA_RECORDING, mRecording);

        mBroadcastManager.sendBroadcast(intent);
    }

    private class PlayerThread extends Thread {
        PlayerThread() {
            super("PlayerThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        private void play(AudioFile file) throws IOException {
            if (mCurrentPosition >= 0 && mCurrentPosition < file.getDuration()) {
                final int bufferSize = 1024; //arbitrary small buffer. makes for more accurate progress reporting
                ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

                file.seek(mCurrentPosition);
                mState = SoundRecorder.State.PLAYING;
                int n = 0;

                while (mState == SoundRecorder.State.PLAYING
                        && (mCurrentPosition < mRecordStream.endPosition)
                        && (n = file.read(buffer, bufferSize)) > -1) {

                    buffer.flip();
                    int written = mAudioTrack.write(buffer, n);
                    if (written < 0) {
                        Log.e(TAG, "AudioTrack#write() returned "+
                                (written == AudioTrack.ERROR_INVALID_OPERATION ? "ERROR_INVALID_OPERATION" :
                                 written == AudioTrack.ERROR_BAD_VALUE ? "ERROR_BAD_VALUE" : "error "+written));

                        mState = SoundRecorder.State.ERROR;
                    } else {
                        mCurrentPosition = file.getPosition();
                    }
                    buffer.clear();
                }

                if (n == -1) {
                    // TODO, This should not be necessary, mCurrentPosition should be correct
                    Log.w(TAG, "got EOF, pos="+mCurrentPosition+", endPos="+mRecordStream.endPosition);
                    mCurrentPosition = mRecordStream.endPosition;
                }

            } else {
                Log.w(TAG, "dataStart > length: " + mCurrentPosition + ">" + file.getDuration());
                throw new IOException("pos > length: " + mCurrentPosition + ">" + file.getDuration());
            }

        }

        public void run() {
            synchronized (mAudioRecord) {
                Log.d(TAG, String.format("starting player thread (%d)", mRecordStream.startPosition));

                if (!mAudioManager.requestMusicFocus(SoundRecorder.this, IAudioManager.FOCUS_GAIN)) {
                    Log.e(TAG, "could not obtain audio focus");
                    broadcast(PLAYBACK_ERROR);
                    return;
                }

                mAudioTrack.play();

                AudioFile file = null;
                try {
                    file =  mRecordStream.getAudioFile();
                    mState = SoundRecorder.State.PLAYING;

                    final long start = mRecordStream.startPosition;
                    // resume from current position if it is valid, otherwise use start position. this is done before broadcast for reporting
                    mCurrentPosition = (mCurrentPosition > start &&
                                        mCurrentPosition < mRecordStream.endPosition) ? mCurrentPosition : start;

                    broadcast(PLAYBACK_STARTED);

                    do {
                        if (mState == SoundRecorder.State.SEEKING) {
                            mCurrentPosition = mSeekToPos;
                            mSeekToPos = -1;
                        }
                        play(file);

                    } while (mState == SoundRecorder.State.SEEKING);

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = SoundRecorder.State.ERROR;
                } finally {
                    IOUtils.close(file);
                    mAudioTrack.stop();
                    mAudioManager.abandonMusicFocus(false);
                }

                if (mRecordStream != null){
                    Log.d(TAG, "player loop exit: state=" + mState + ", position=" + mCurrentPosition + " of " + (mRecordStream.endPosition));
                    if (mState == SoundRecorder.State.PLAYING && mCurrentPosition >= mRecordStream.endPosition) {
                        mCurrentPosition = -1;
                        broadcast(PLAYBACK_COMPLETE);
                    } else {
                        broadcast(PLAYBACK_STOPPED);
                    }
                    mState = SoundRecorder.State.IDLE;
                } else {
                    mCurrentPosition = -1;
                    Log.d(TAG, "player loop exit: no stream available");
                }
            }
        }
    }

    private class ReaderThread extends Thread {
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

                if (mRecordStream != null) {
                    if (mState != SoundRecorder.State.ERROR) {
                        mRecordStream.finalizeStream();
                        resetPlaybackBounds();
                        broadcast(RECORD_FINISHED);
                    } else {
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
