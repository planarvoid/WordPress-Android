
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

    public static final int TIMER_INTERVAL = 20;
    private static final int REFRESH = 1;
    private static final int TRIM_PREVIEW_LENGTH = 500;
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(Math.sqrt(32768.0));

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

    final private AudioConfig mConfig;
    final private ByteBuffer buffer;

    private int mLastMax;
    private int mCurrentMaxAmplitude;
    private int mCurrentAdjustedMaxAmplitude;
    private long mCurrentPosition, mTotalBytes, mStartPos, mEndPos, mNextSeek, mDuration;

    private LocalBroadcastManager mBroadcastManager;

    public static synchronized CloudRecorder getInstance(Context context) {
        if (instance == null) {
            instance = new CloudRecorder(context, CloudCreateService.DEFAULT_CONFIG);
        }
        return instance;
    }

    private CloudRecorder(Context context, AudioConfig config) {
        final int bufferSize = config.getMinBufferSize() * 3;
        mAudioRecord = config.createAudioRecord(config.source, bufferSize);
        mAudioTrack = config.createAudioTrack(bufferSize);
        mBroadcastManager = LocalBroadcastManager.getInstance(context);

        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        mState = State.IDLE;
        mConfig = config;
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
            mRecordStream = new RecordStream(path, mConfig);
            startReadingInternal(State.RECORDING);
        } else throw new IllegalStateException("cannot record to file, in state " + mState);

        return mState;
    }

    public void stopRecording() {
        if (mState == State.RECORDING ||
            mState == State.READING) {
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
            switch (msg.what) {
                case REFRESH:
                    if (mState != State.RECORDING && mState != State.READING) {
                        refreshHandler.removeMessages(REFRESH);
                        return;
                    }

                    int mCurrentMax = mCurrentMaxAmplitude;
                    mCurrentMaxAmplitude = 0;

                    // max amplitude returns false 0's sometimes, so just
                    // use the last value. It is usually only for a frame
                    if (mCurrentMax == 0) {
                        mCurrentMax = mLastMax;
                    } else {
                        mLastMax = mCurrentMax;
                    }

                    // Simple peak follower, cf. http://www.musicdsp.org/showone.php?id=19
                    if ( mCurrentMax >= mCurrentAdjustedMaxAmplitude ) {
                        /* When we hit a peak, ride the peak to the top. */
                        mCurrentAdjustedMaxAmplitude = mCurrentMax;
                    } else {
                        /*  decay of output when signal is low. */
                        mCurrentAdjustedMaxAmplitude = (int) (mCurrentAdjustedMaxAmplitude * .4);
                    }

                    if (mState == State.RECORDING && writeIndex == -1) {
                        writeIndex = amplitudes.size();
                    }
                    long elapsedTime = mState != State.RECORDING ? -1 : mConfig.bytesToMs(mRecordStream.length());
                    final float frameAmplitude = Math.max(.1f,
                            ((float) Math.sqrt(Math.sqrt(mCurrentAdjustedMaxAmplitude)))
                                    / MAX_ADJUSTED_AMPLITUDE);

                    amplitudes.add(frameAmplitude);
                    Intent intent = new Intent(CloudCreateService.RECORD_PROGRESS)
                            .putExtra(CloudCreateService.EXTRA_AMPLITUDE, frameAmplitude)
                            .putExtra(CloudCreateService.EXTRA_ELAPSEDTIME, elapsedTime);

                    mBroadcastManager.sendBroadcast(intent);
                    queueNextRefresh(TIMER_INTERVAL);
                    break;
                default:
            }
        }
    };

    private void queueNextRefresh(long delay) {
        Message msg = refreshHandler.obtainMessage(REFRESH);
        refreshHandler.removeMessages(REFRESH);
        refreshHandler.sendMessageDelayed(msg, delay);
    }

    public long getDuration() {
        return mDuration;
    }

    public long getCurrentPlaybackPosition() {
        return mCurrentPosition == -1 ? -1 :  mConfig.bytesToMs(mCurrentPosition);
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
            stopRecording();
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
        seekTo(mConfig.startPosition((long) (percentage * mTotalBytes)));
    }

    public void seekTo(long position) {
        mNextSeek = position;
        mState = State.SEEKING;
    }

    public void onNewStartPosition(double percent) {
        mStartPos = mConfig.startPosition((long) (percent * mTotalBytes));
        if (mState == State.PLAYING) {
            seekTo(mStartPos);
        }
    }

    public void onNewEndPosition(double percent) {
        mEndPos = mConfig.startPosition((long) (percent * mTotalBytes));
        if (mState == State.PLAYING) {
            seekTo(Math.max(mStartPos, mEndPos - mConfig.msToByte(TRIM_PREVIEW_LENGTH)));
        }
    }

    private class PlayerThread extends Thread {
        PlayerThread() {
            super("PlayerThread");
            setPriority(Thread.MAX_PRIORITY);
        }

        private void play(RandomAccessFile file, long pos) throws IOException {
            if (pos < file.length()) {
                final int bufferSize = (int) (mConfig.bytesPerSecond / 0.25);
                byte[] buffer = new byte[bufferSize];
                file.seek(pos);
                mCurrentPosition = pos;
                mState = CloudRecorder.State.PLAYING;
                int n;

                broadcast(CloudCreateService.PLAYBACK_STARTED);

                while (mState == CloudRecorder.State.PLAYING && (n = file.read(buffer, 0, bufferSize)) > -1 && (mCurrentPosition < mEndPos)) {
                    mCurrentPosition += mAudioTrack.write(buffer, 0, n);
                    mDuration = mConfig.bytesToMs(mCurrentPosition);
                    broadcast(CloudCreateService.PLAYBACK_PROGRESS);
                }
            } else {
                Log.w(TAG, "dataStart > length: " + pos + ">" + file.length());
                throw new IOException("pos > length: " + pos + ">" + file.length());
            }
        }

        public void run() {
            synchronized (mAudioRecord) {
                Log.d(TAG, String.format("starting player thread (%d)", mNextSeek));
                mAudioTrack.play();

                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(mRecordStream.file, "r");
                    do {
                        play(file, mNextSeek);
                    } while (mState == CloudRecorder.State.SEEKING);

                } catch (IOException e) {
                    Log.w(TAG, "error during playback", e);
                    mState = CloudRecorder.State.ERROR;
                } finally {
                    IOUtils.close(file);
                    mAudioTrack.stop();
                }

                Log.d(TAG, "player loop exit: state="+mState);
                if (mState == CloudRecorder.State.PLAYING && mCurrentPosition >= mEndPos) {
                    broadcast(CloudCreateService.PLAYBACK_COMPLETE);
                } else {
                    broadcast(CloudCreateService.PLAYBACK_STOPPED);
                }
                mState = CloudRecorder.State.IDLE;
            }
        }

        private void broadcast(String action) {
            mBroadcastManager.sendBroadcast(new Intent(action)
                    .putExtra(CloudCreateService.EXTRA_POSITION, mCurrentPosition)
                    .putExtra(CloudCreateService.EXTRA_STATE, mState.name())
                    .putExtra(CloudCreateService.EXTRA_PATH, mRecordStream == null ? null : mRecordStream.file.getAbsolutePath()));
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

                mAudioRecord.startRecording();
                queueNextRefresh(0); // start polling for data
                while (mState == CloudRecorder.State.READING || mState == CloudRecorder.State.RECORDING) {
                    final long start = System.currentTimeMillis();

                    buffer.rewind();
                    final int read = mAudioRecord.read(buffer, buffer.capacity());

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

                        buffer.rewind();
                        buffer.limit(read);
                        while (buffer.position() < buffer.limit()) {
                            int value;
                            switch (mConfig.bitsPerSample) {
                                case 16:
                                    value = buffer.getShort();
                                    break;
                                case 8:
                                    value = buffer.get();
                                    break;
                                default:
                                    value = 0;
                            }
                            if (value > mCurrentMaxAmplitude) mCurrentMaxAmplitude = value;
                        }
                    }
                    SystemClock.sleep(Math.max(5,
                            TIMER_INTERVAL - (System.currentTimeMillis() - start)));
                }
                Log.d(TAG, "exiting reader loop, stopping recording (mState="+mState+")");
                refreshHandler.removeMessages(REFRESH);
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

        private void broadcast(String action) {
            mBroadcastManager.sendBroadcast(new Intent(action)
                    .putExtra(CloudCreateService.EXTRA_POSITION, mCurrentPosition)
                    .putExtra(CloudCreateService.EXTRA_STATE, mState.name())
                    .putExtra(CloudCreateService.EXTRA_PATH, mRecordStream == null ? null : mRecordStream.file.getAbsolutePath()));
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
