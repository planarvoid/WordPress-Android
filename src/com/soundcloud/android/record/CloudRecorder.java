
package com.soundcloud.android.record;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioFile;
import com.soundcloud.android.audio.ScAudioTrack;
import com.soundcloud.android.service.record.CloudCreateService;
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

public class CloudRecorder {
    /* package */ static final String TAG = CloudRecorder.class.getSimpleName();

    public static final int FPS = 40;
    private static final int TRIM_PREVIEW_LENGTH = 500;

    private static CloudRecorder instance;

    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING, PLAYING, SEEKING
    }

    private volatile State mState;

    public final AmplitudeData amplitudeData;
    public int writeIndex;

    private final AudioRecord mAudioRecord;
    private final ScAudioTrack mAudioTrack;

    private RecordStream mRecordStream;
    private AmplitudeAnalyzer mAmplitudeAnalyzer;

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;
    final private int bufferReadSize;


    private ReaderThread mReaderThread;

    private long mCurrentPosition, mDuration, mSeekToPos = -1;

    private LocalBroadcastManager mBroadcastManager;

    public static synchronized CloudRecorder getInstance(Context context) {
        if (instance == null) {
            instance = new CloudRecorder(context, AudioConfig.DEFAULT);
        }
        return instance;
    }

    private CloudRecorder(Context context, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize();
        mConfig = config;
        mAudioRecord = config.createAudioRecord(bufferSize * 4);
        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override public void onMarkerReached(AudioRecord audioRecord) { }
            @Override public void onPeriodicNotification(AudioRecord audioRecord) {
                if (mState == State.RECORDING) {
                    Intent intent = new Intent(CloudCreateService.RECORD_PROGRESS)
                        .putExtra(CloudCreateService.EXTRA_ELAPSEDTIME,
                                mRecordStream == null ? -1 : mRecordStream.elapsedTime());
                    mBroadcastManager.sendBroadcast(intent);
                }
            }
        });
        mAudioTrack = config.createAudioTrack(bufferSize);
        mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override public void onMarkerReached(AudioTrack track) {
            }
            @Override public void onPeriodicNotification(AudioTrack track) {
                broadcast(CloudCreateService.PLAYBACK_PROGRESS);
            }
        });
        mAudioTrack.setPositionNotificationPeriod(mConfig.sampleRate);
        mBroadcastManager = LocalBroadcastManager.getInstance(context);

        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        bufferReadSize =  (int) config.validBytePosition(mConfig.bytesPerSecond / (FPS));
        mAmplitudeAnalyzer = new AmplitudeAnalyzer(config);
        mState = State.IDLE;
        amplitudeData = new AmplitudeData();
    }

    public void startReading() {
        if (mState == State.IDLE) {
            amplitudeData.clear();
            writeIndex = -1;
            mRecordStream = null;

            startReadingInternal(State.READING);
        }
    }

    // Sets output file path, call directly after construction/reset.
    public State startRecording(final File path) {
        if (path == null) throw new IllegalArgumentException("path is null");

        if (mState != State.RECORDING) {
            if (mRecordStream != null && !mRecordStream.getFile().equals(path)) {
                mRecordStream.release();
                mRecordStream = null;
            }

            if (mRecordStream == null) {
                mRecordStream = new RecordStream(path, mConfig);
            }
            startReadingInternal(State.RECORDING);
        } else throw new IllegalStateException("cannot record to file, in state " + mState);

        return mState;
    }

    public void stopRecording() {
        // by default, only stop if actually recording (writing)
        stopRecording(false);
    }

    public void stopRecording(boolean stopIfReading) {
        if (mState == State.RECORDING ||
            (mState == State.READING && stopIfReading)) {
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
        stopRecording(true);
        //release();
    }

    private void release() {
        mAudioRecord.release();
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

            Intent intent = new Intent(CloudCreateService.RECORD_SAMPLE)
                    .putExtra(CloudCreateService.EXTRA_AMPLITUDE, frameAmplitude)
                    .putExtra(CloudCreateService.EXTRA_ELAPSEDTIME, mRecordStream == null ? -1 : mRecordStream.elapsedTime());

            mBroadcastManager.sendBroadcast(intent);
        }
    };

    public long getDuration() {
        return mDuration;
    }

    public long getCurrentPlaybackPosition() {
        return mSeekToPos != -1 ? mSeekToPos : mCurrentPosition == -1 ? -1 :  mCurrentPosition;
    }

    public void resetPlaybackBounds() {
        if (mRecordStream != null) mRecordStream.resetPlaybackBounds();
    }

    public boolean isPlaying() {
        return mState == State.PLAYING || mState == State.SEEKING;
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
        mRecordStream.setStartPositionByPercent(percent);
        if (mState == State.PLAYING) {
            seekTo(mRecordStream.startPosition);
        }
    }

    public void onNewEndPosition(double percent) {
        mRecordStream.setEndPositionByPercent(percent);
        if (mState == State.PLAYING) {
            seekTo(Math.max(mRecordStream.startPosition, mRecordStream.endPosition - mConfig.msToByte(TRIM_PREVIEW_LENGTH)));
        }
    }

    private void broadcast(String action) {
        final Intent intent = new Intent(action)
                .putExtra(CloudCreateService.EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(CloudCreateService.EXTRA_STATE, mState.name())
                .putExtra(CloudCreateService.EXTRA_PATH, mRecordStream == null ? null : mRecordStream.getFile().getAbsolutePath());
        Log.d(TAG, "broadcast "+intent);
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
                mState = CloudRecorder.State.PLAYING;
                int n;
                while (mState == CloudRecorder.State.PLAYING
                        && (n = file.read(buffer, bufferSize)) > -1
                        && (mCurrentPosition < mRecordStream.endPosition)) {

                    int written = mAudioTrack.write(buffer, n);
                    if (written < 0) {
                        Log.e(TAG, "AudioTrack#write() returned "+
                                (written == AudioTrack.ERROR_INVALID_OPERATION ? "ERROR_INVALID_OPERATION" :
                                 written == AudioTrack.ERROR_BAD_VALUE ? "ERROR_BAD_VALUE" : "error "+written));

                        mState = CloudRecorder.State.ERROR;
                    } else {
                        mCurrentPosition += file.getConfig().bytesToMs(written);
                    }
                    buffer.rewind();
                }
            } else {
                Log.w(TAG, "dataStart > length: " + mCurrentPosition + ">" + file.getDuration());
                throw new IOException("pos > length: " + mCurrentPosition + ">" + file.getDuration());
            }
        }

        public void run() {
            synchronized (mAudioRecord) {
                Log.d(TAG, String.format("starting player thread (%d)", mRecordStream.startPosition));
                mAudioTrack.play();

                AudioFile file = null;
                try {
                    file =  mRecordStream.getAudioFile();
                    mState = CloudRecorder.State.PLAYING;

                    final long start = mRecordStream.startPosition;
                    // resume from current position if it is valid, otherwise use start position. this is done before broadcast for reporting
                    mCurrentPosition = (mCurrentPosition > start &&
                                        mCurrentPosition < mRecordStream.endPosition) ? mCurrentPosition : start;

                    broadcast(CloudCreateService.PLAYBACK_STARTED);

                    do {
                        if (mState == CloudRecorder.State.SEEKING) {
                            mCurrentPosition = mSeekToPos;
                            mSeekToPos = -1;
                        }
                        play(file);

                    } while (mState == CloudRecorder.State.SEEKING);

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = CloudRecorder.State.ERROR;
                } finally {
                    IOUtils.close(file);
                    mAudioTrack.stop();
                }
                Log.d(TAG, "player loop exit: state="+mState+", position="+mCurrentPosition+" of " +  mRecordStream.endPosition);
                if (mState == CloudRecorder.State.PLAYING && mCurrentPosition >=  mRecordStream.endPosition) {
                    mCurrentPosition = -1;
                    broadcast(CloudCreateService.PLAYBACK_COMPLETE);
                } else {
                    broadcast(CloudCreateService.PLAYBACK_STOPPED);
                }
                mState = CloudRecorder.State.IDLE;
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
                    mState = CloudRecorder.State.ERROR;
                    return;
                }

                if (mState == CloudRecorder.State.RECORDING) {
                    broadcast(CloudCreateService.RECORD_STARTED);
                }

                mAudioRecord.startRecording();
                mAudioRecord.setPositionNotificationPeriod(mConfig.sampleRate);
                while (mState == CloudRecorder.State.READING || mState == CloudRecorder.State.RECORDING) {
                    buffer.rewind();
                    final int read = mAudioRecord.read(buffer, bufferReadSize);
                    if (read < 0) {
                        Log.w(TAG, "AudioRecord.read() returned error: " + read);
                        mState = CloudRecorder.State.ERROR;
                    } else if (read == 0) {
                        Log.w(TAG, "AudioRecord.read() returned no data");
                    } else {
                        if (mState == CloudRecorder.State.RECORDING && mRecordStream != null) {
                            try {
                                final int written = mRecordStream.write(buffer, read);
                                if (written < read) {
                                    Log.w(TAG, "partial write "+written);
                                }
                                mDuration = mRecordStream.getDuration();
                            } catch (IOException e) {
                                Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                                mState = CloudRecorder.State.ERROR;
                                break;
                            }
                        }
                        mAmplitudeAnalyzer.updateCurrentMax(buffer, read);
                        refreshHandler.sendEmptyMessage(0);
                    }
                }
                Log.d(TAG, "exiting reader loop, stopping recording (mState="+mState+")");
                mAudioRecord.stop();

                if (mRecordStream != null) {
                    if (mState != CloudRecorder.State.ERROR) {
                        mRecordStream.finalizeStream();
                        resetPlaybackBounds();
                        broadcast(CloudCreateService.RECORD_FINISHED);
                    } else {
                        broadcast(CloudCreateService.RECORD_ERROR);
                    }
                }

                mState = CloudRecorder.State.IDLE;
                mReaderThread = null;
            }
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        for (String action : CloudCreateService.ALL_ACTIONS) {
            filter.addAction(action);
        }
        return filter;
    }
}
