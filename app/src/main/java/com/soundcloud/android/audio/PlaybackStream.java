package com.soundcloud.android.audio;

import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.utils.IOUtils;


import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlaybackStream {
    private static final int TRIM_PREVIEW_LENGTH = 500;

    private long mCurrentPos;
    private long mStartPos;
    private long mEndPos;

    private AudioConfig mConfig;
    private AudioFile mPlaybackFile;

    public PlaybackFilter filter;

    public PlaybackStream(AudioFile audioFile, AudioConfig config) throws IOException {
        mPlaybackFile = audioFile;
        resetBounds();
        mCurrentPos = -1;

        mConfig = config;
    }

    public void reset() {
        resetBounds();
        filter = null;
    }

    public void resetBounds() {
        mStartPos = 0;
        mEndPos = mPlaybackFile.getDuration();
    }

    public long getDuration(){
        return mEndPos - mStartPos;
    }

    public long getPosition() {
        return mCurrentPos - mStartPos;
    }

    public TrimPreview setStartPositionByPercent(float newPos, float oldPos, long moveTime) {
        final long old = mStartPos;
        mStartPos = (long) (newPos * mPlaybackFile.getDuration());
        return new TrimPreview(this,old,mStartPos, moveTime);
    }

    public TrimPreview setEndPositionByPercent(float newPos, float oldPos, long moveTime) {
        final long old = mEndPos;
        mEndPos = (long) (newPos * mPlaybackFile.getDuration());
        return new TrimPreview(this, old, mEndPos, moveTime);
    }

    public int read(ByteBuffer buffer, int bufferSize) throws IOException {
        final int n = mPlaybackFile.read(buffer, bufferSize);
        buffer.flip();
        return n;
    }

    public int readForPlayback(ByteBuffer buffer, int bufferSize) throws IOException {
        if (mCurrentPos < mEndPos) {
            final int n = mPlaybackFile.read(buffer, bufferSize);
            buffer.flip();
            if (filter != null) {
                filter.apply(buffer, mConfig.msToByte(mCurrentPos - mStartPos), mConfig.msToByte(mEndPos - mStartPos));
            }
            mCurrentPos = mPlaybackFile.getPosition();
            return n;
        }
        return -1;
    }

    public boolean isFinished() {
        return mCurrentPos >= mEndPos;
    }

    public void resetPlayback() {
        mCurrentPos = -1;
    }

    public void initializePlayback() throws IOException {
        mCurrentPos = getValidPosition(mCurrentPos);
        initializePlayback(mCurrentPos);
    }

    public void initializePlayback(long atPosition) throws IOException {
        mPlaybackFile.seek(atPosition);
    }


    public long getValidPosition(long currentPosition) {
            return (currentPosition < mStartPos || currentPosition >= mEndPos) ? mStartPos : currentPosition;
        }

    public void close() {
        IOUtils.close(mPlaybackFile);
        mPlaybackFile = null;
    }

    public void setCurrentPosition(long pos) {
        mCurrentPos = pos;
    }

    public void reopen() {
        try {
            mPlaybackFile.reopen();
            mPlaybackFile.seek(mCurrentPos);
        } catch (IOException e) {
            Log.w(PlaybackStream.class.getSimpleName(), e);
        }
    }


    public static class TrimPreview {
        PlaybackStream mStream;
        long mStartPos;
        long mEndPos;
        public long duration;
        public long playbackRate;

        public TrimPreview (PlaybackStream stream, long startPosition, long endPosition, long moveTime){
            mStream = stream;
            mStartPos = startPosition;
            mEndPos = endPosition;
            duration = moveTime;

            final long byteRange = getByteRange(stream.mConfig);
            playbackRate = (int) (byteRange * (1000f / duration)) / stream.mConfig.sampleSize;
            if (playbackRate > SoundRecorder.MAX_PLAYBACK_RATE){
                // if this preview is too quick, we have to adjust it to fit the max samplerate. Adjust the duration
                playbackRate = SoundRecorder.MAX_PLAYBACK_RATE;
                duration = (long) (1000f / ((playbackRate * stream.mConfig.sampleSize)/byteRange));
            }
        }

        public long lowPos(AudioConfig config){
            return config.validBytePosition(Math.min(mStartPos,mEndPos));
        }

        public long getByteRange(AudioConfig config) {
            return config.validBytePosition(config.msToByte((int) Math.abs(mEndPos - mStartPos)));
        }

        public boolean isReverse() {
            return mStartPos > mEndPos;
        }

        @Override
        public String toString() {
            return "TrimPreview{" +
                    "mStream=" + mStream +
                    ", mStartPos=" + mStartPos +
                    ", mEndPos=" + mEndPos +
                    ", duration=" + duration +
                    ", playbackRate=" + playbackRate +
                    '}';
        }
    }
}
