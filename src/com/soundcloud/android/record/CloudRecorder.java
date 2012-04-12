
package com.soundcloud.android.record;

import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class CloudRecorder {
    /* package */ static final String TAG = CloudRecorder.class.getSimpleName();

    public static final int FPS = 60;
    public static final int TIMER_INTERVAL = 1000 / FPS;
    private static final int TRIM_PREVIEW_LENGTH = 500;

    private static CloudRecorder instance;

    public enum State {
        IDLE, READING, RECORDING, ERROR, STOPPING, PLAYING, SEEKING
    }

    private volatile State mState;

    // XXX memory
    public List<Float> amplitudes = new ArrayList<Float>();
    public int writeIndex;

    private final AudioRecord mAudioRecord;
    private final AudioTrack mAudioTrack;

    private RecordStream mRecordStream;
    private AmplitudeAnalyzer mAmplitudeAnalyzer;

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;
    final private int bufferReadSize;

    private long mCurrentPosition, mTotalBytes, mStartPos, mEndPos, mDuration, mSeekToPos;

    private LocalBroadcastManager mBroadcastManager;

    public static synchronized CloudRecorder getInstance(Context context) {
        if (instance == null) {
            instance = new CloudRecorder(context, CloudCreateService.DEFAULT_CONFIG);
        }
        return instance;
    }

    private CloudRecorder(Context context, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize();
        mConfig = config;
        mAudioRecord = config.createAudioRecord(bufferSize);
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
        bufferReadSize = (int) config.validBytePosition(mConfig.sampleRate / (FPS));
        mAmplitudeAnalyzer = new AmplitudeAnalyzer(config);
        mState = State.IDLE;
    }

    public void startReading() {
        amplitudes.clear();
        writeIndex = -1;
        startReadingInternal(State.READING);
    }

    // Sets output file path, call directly after construction/reset.
    public State startRecording(final File path) {
        if (path == null) throw new IllegalArgumentException("path is null");

        if (mState != State.RECORDING) {
            if (mRecordStream != null && !mRecordStream.file.equals(path)) {
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
//        mAudioRecord.release();
//        mAudioTrack.release();
    }

    private State startReadingInternal(State newState) {
        // check to see if we are already reading
        final boolean startReading = mState != State.READING && mState != State.RECORDING;
        mState = newState;
        if (startReading) {
            new ReaderThread().start();
        }
        return mState;
    }

    private final Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mState == State.RECORDING && writeIndex == -1) {
                writeIndex = amplitudes.size();
            }

            final float frameAmplitude = mAmplitudeAnalyzer.frameAmplitude();
            amplitudes.add(frameAmplitude);

            Intent intent = new Intent(CloudCreateService.RECORD_PROGRESS)
                    .putExtra(CloudCreateService.EXTRA_AMPLITUDE, frameAmplitude)
                    .putExtra(CloudCreateService.EXTRA_ELAPSEDTIME, mRecordStream == null ? -1 : mRecordStream.elapsedTime());

            mBroadcastManager.sendBroadcast(intent);
        }
    };

    public long getDuration() {
        return mDuration;
    }

    public long getCurrentPlaybackPosition() {
        return mSeekToPos != -1 ? mConfig.bytesToMs(mSeekToPos) : mCurrentPosition == -1 ? -1 :  mConfig.bytesToMs(mCurrentPosition);
    }

    public void resetPlaybackBounds() {
        mStartPos = 0;
        mEndPos = mTotalBytes;
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
            new PlayerThread().start();
        }
    }

    public void seekToPercentage(float percentage) {
        seekTo((long) (percentage * mTotalBytes));
    }

    public void seekTo(long position) {
        position = mConfig.validBytePosition(position);
        if (isPlaying()) {
            mSeekToPos = position;
            mState = State.SEEKING;
        } else {
            mCurrentPosition = position;
        }
    }

    public void onNewStartPosition(double percent) {
        mStartPos = mConfig.validBytePosition((long) (percent * mTotalBytes));
        if (mState == State.PLAYING) {
            seekTo(mStartPos);
        }
    }

    public void onNewEndPosition(double percent) {
        mEndPos = mConfig.validBytePosition((long) (percent * mTotalBytes));
        if (mState == State.PLAYING) {
            seekTo(Math.max(mStartPos, mEndPos - mConfig.msToByte(TRIM_PREVIEW_LENGTH)));
        }
    }

    private void broadcast(String action) {
        final Intent intent = new Intent(action)
                .putExtra(CloudCreateService.EXTRA_POSITION, getCurrentPlaybackPosition())
                .putExtra(CloudCreateService.EXTRA_STATE, mState.name())
                .putExtra(CloudCreateService.EXTRA_PATH, mRecordStream == null ? null : mRecordStream.file.getAbsolutePath());
        Log.d(TAG, "broadcast "+intent);
        mBroadcastManager.sendBroadcast(intent);
    }

    private class PlayerThread extends Thread {
        PlayerThread() {
            super("PlayerThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        private void play(RandomAccessFile file, long pos) throws IOException {
            if (pos >= 0 && pos < file.length()) {
                final int bufferSize = 1024; //arbitrary small buffer. makes for more accurate progress reporting
                byte[] buffer = new byte[bufferSize];

                file.seek(pos);
                mCurrentPosition = pos;
                mState = CloudRecorder.State.PLAYING;
                int n;

                while (mState == CloudRecorder.State.PLAYING && (n = file.read(buffer, 0, bufferSize)) > -1 && (mCurrentPosition < mEndPos)) {

                    int written = mAudioTrack.write(buffer, 0, n);
                    if (written < 0) {
                        Log.d(TAG, "write() returned "+written);
                        mState = CloudRecorder.State.ERROR;
                    } else {
                        mCurrentPosition += written;
                    }
                }
            } else {
                Log.w(TAG, "dataStart > length: " + pos + ">" + file.length());
                throw new IOException("pos > length: " + pos + ">" + file.length());
            }
        }

        public void run() {
            synchronized (mAudioRecord) {
                Log.d(TAG, String.format("starting player thread (%d)", mStartPos));
                mAudioTrack.play();

                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(mRecordStream.file, "r");
                    broadcast(CloudCreateService.PLAYBACK_STARTED);
                    do {
                        if (mState == CloudRecorder.State.SEEKING) {
                            mCurrentPosition = mSeekToPos;
                            mSeekToPos = -1;
                        }
                        final long start = mConfig.validBytePosition(mStartPos)+WaveHeader.LENGTH;
                        play(file, mCurrentPosition > start && mCurrentPosition < mEndPos ? mCurrentPosition : start);
                    } while (mState == CloudRecorder.State.SEEKING);

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = CloudRecorder.State.ERROR;
                } finally {
                    IOUtils.close(file);
                    mAudioTrack.stop();
                }

                Log.d(TAG, "player loop exit: state="+mState+", position="+mCurrentPosition+" of " + mEndPos);
                if (mState == CloudRecorder.State.PLAYING && mCurrentPosition >= mEndPos) {
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
                Log.d(TAG, "starting reader thread");

                broadcast(CloudCreateService.RECORD_STARTED);
                mAudioRecord.startRecording();
                while (mState == CloudRecorder.State.READING || mState == CloudRecorder.State.RECORDING) {
                    final long start = System.currentTimeMillis();

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
                                mDuration = mConfig.bytesToMs(mRecordStream.length() - WaveHeader.LENGTH);
                            } catch (IOException e) {
                                Log.e(TAG, "Error occured in updateListener, recording is aborted : ", e);
                                mState = CloudRecorder.State.ERROR;
                                break;
                            }
                        }
                        mAmplitudeAnalyzer.updateCurrentMax(buffer, read);
                        refreshHandler.sendEmptyMessage(0);
                    }
                    SystemClock.sleep(Math.max(5,
                            TIMER_INTERVAL - (System.currentTimeMillis() - start)));
                }
                Log.d(TAG, "exiting reader loop, stopping recording (mState="+mState+")");
                mAudioRecord.stop();

                if (mState != CloudRecorder.State.ERROR) {
                    mTotalBytes = mRecordStream.finalizeStream();
                    resetPlaybackBounds();
                    broadcast(CloudCreateService.RECORD_FINISHED);
                } else {
                    broadcast(CloudCreateService.RECORD_ERROR);
                }
                mState = CloudRecorder.State.IDLE;
            }
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(CloudCreateService.RECORD_STARTED);
        filter.addAction(CloudCreateService.RECORD_PROGRESS);
        filter.addAction(CloudCreateService.RECORD_ERROR);

        filter.addAction(CloudCreateService.PLAYBACK_STARTED);
        filter.addAction(CloudCreateService.PLAYBACK_COMPLETE);
        filter.addAction(CloudCreateService.PLAYBACK_PROGRESS);
        filter.addAction(CloudCreateService.PLAYBACK_ERROR);

        return filter;
    }
}
